package com.chatora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatora.app.data.User
import com.chatora.app.data.Country
import com.chatora.app.domain.repositories.MatchRepository
import com.chatora.app.R
import com.chatora.app.webrtc.WebRtcClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@HiltViewModel
class MatchViewModel @Inject constructor(
    private val repository: MatchRepository,
    private val webRtcClient: WebRtcClient,
    private val userRepository: com.chatora.app.domain.repositories.UserRepository,
    private val tokenManager: com.chatora.app.data.local.TokenManager,
    private val apiService: com.chatora.app.data.remote.ApiService,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    companion object {
        private const val TAG = "MINICHAT_DEBUG"
        private const val FIND_MATCH_DEBOUNCE_MS = 2000L
        private const val INITIATOR_DELAY_MS = 2000L
        private const val CONNECTION_TIMEOUT_MS = 40000L
        private const val MATCHING_TIMEOUT_MS = 30000L
        private const val PARTNER_LEFT_DELAY_MS = 1500L
        private const val FAILED_STATE_DELAY_MS = 3000L
        private const val ICE_RECOVERY_WINDOW_MS = 15000L
        private const val RETRY_SINK_DELAY_MS = 3000L
        private const val RETRY_FINAL_DELAY_MS = 6000L
    }

    private val _currentUser = MutableStateFlow<com.chatora.app.data.User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _selectedCountry = MutableStateFlow(
        com.chatora.app.data.Country("AUTO", R.string.all_countries, "🌍")
    )
    val selectedCountry = _selectedCountry.asStateFlow()

    private val _selectedGender = MutableStateFlow("All")
    val selectedGender = _selectedGender.asStateFlow()

    val countries = listOf(
        com.chatora.app.data.Country("AUTO", R.string.all_countries, "🌍"),
        com.chatora.app.data.Country("US", R.string.country_us, "🇺🇸"),
        com.chatora.app.data.Country("GB", R.string.country_gb, "🇬🇧")
    ) + com.chatora.app.data.StaticData.getCountries().filter { it.code != "US" && it.code != "GB" }

    private val _matchState = MutableStateFlow<MatchUiState>(MatchUiState.Idle)
    val matchState = _matchState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent.asStateFlow()

    private val _hasStarted = MutableStateFlow(false)
    val hasStarted = _hasStarted.asStateFlow()

    private val _remoteVideoReady = MutableStateFlow(false)
    val remoteVideoReady = _remoteVideoReady.asStateFlow()

    private var currentMatchId: String? = null
    private var chatSubscriptionId: String? = null

    private var matchingTimeoutJob: Job? = null

    // ANTI-DUPLICATE: Mutex-protected matchId guard for handleMatchFound()
    private val matchMutex = Mutex()

    // Debounce guard — prevent rapid findMatch() cycling
    private var lastFindMatchTime: Long = 0L

    init {
        android.util.Log.d(TAG, "MatchViewModel initialized")
        listenForEvents()

        // CONNECTION STATE HANDLER — Relaxed reconnection logic to prevent rapid cycling
        webRtcClient.onConnectionStateChanged = { state ->
            android.util.Log.d(TAG, "WebRTC Connection State: $state")
            when (state) {
                org.webrtc.PeerConnection.PeerConnectionState.CONNECTED -> {
                    // Connection is stable — cancel any pending timeout
                    connectionTimeoutJob?.cancel()
                    android.util.Log.d(TAG, "Connection CONNECTED. Timeout cancelled.")
                }
                org.webrtc.PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    // ICE disconnected — ONLY restart ICE, do NOT auto-find next.
                    // This is often a temporary state that recovers on its own.
                    android.util.Log.w(TAG, "Connection DISCONNECTED. Attempting ICE restart (${ICE_RECOVERY_WINDOW_MS / 1000}s window)...")
                    webRtcClient.restartIce()
                    // Give time to recover — if still no video after that, do nothing (wait for FAILED)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(ICE_RECOVERY_WINDOW_MS)
                        val currentState = _matchState.value
                        if (currentState is MatchUiState.Found && !_remoteVideoReady.value) {
                            android.util.Log.w(TAG, "ICE restart did not recover video. Waiting for FAILED state.")
                        }
                    }
                }
                org.webrtc.PeerConnection.PeerConnectionState.FAILED -> {
                    // True connection failure — try ICE restart first before giving up.
                    // Do NOT auto-findNext, which caused cascading match cycling.
                    android.util.Log.w(TAG, "Connection FAILED — attempting recovery (ICE restart + re-offer)")
                    viewModelScope.launch {
                        // Step 1: ICE restart (handled by WebRtcClient)
                        webRtcClient.restartIce()
                        kotlinx.coroutines.delay(FAILED_STATE_DELAY_MS)
                        // Step 2: If still in match and no video, re-create PC + offer
                        if (_matchState.value is MatchUiState.Found && !_remoteVideoReady.value) {
                            android.util.Log.w(TAG, "ICE restart did not recover connection — re-creating offer")
                            webRtcClient.closePeerConnection()
                            if (!webRtcClient.areLocalTracksInitialized) {
                                webRtcClient.awaitLocalTracks()
                            }
                            webRtcClient.createPeerConnection()
                            webRtcClient.createOffer()
                        }
                    }
                }
                else -> { /* No action for other states */ }
            }
        }

        // Load Current User
        viewModelScope.launch {
            userRepository.refreshUser()
            userRepository.currentUser.collect { userEntity ->
                if (userEntity != null) {
                    _currentUser.value = com.chatora.app.data.User(
                        id = userEntity.id,
                        username = userEntity.username,
                        email = userEntity.email,
                        karma = userEntity.karma,
                        diamonds = userEntity.diamonds,
                        country = com.chatora.app.data.Country(userEntity.countryCode, userEntity.countryNameRes, userEntity.countryFlag),
                        gender = userEntity.gender,
                        isPremium = userEntity.isPremium
                    )
                }
            }
        }

        // Initialize WebSocket Connection
        val token = tokenManager.getToken()
        if (token != null) {
            android.util.Log.d(TAG, "Token found (${token.take(10)}...), connecting...")
            repository.connectAndSubscribe(token)
        } else {
            android.util.Log.e(TAG, "Token is NULL in MatchViewModel init!")
        }
    }

    fun updateCountry(country: com.chatora.app.data.Country) {
        val isPremium = currentUser.value?.isPremium ?: false
        val isAllowedCountry = country.code == "AUTO" || country.code == "US" || country.code == "GB"

        if (isPremium || isAllowedCountry) {
            _selectedCountry.value = country
        } else {
            _uiEvent.value = UiEvent.ShowPremiumRequired("Country selection requires Premium")
        }
    }

    fun updateGender(gender: String) {
        val isPremium = currentUser.value?.isPremium ?: false
        if (isPremium || gender == "All") {
            _selectedGender.value = gender
        } else {
            _uiEvent.value = UiEvent.ShowPremiumRequired("Gender filtering requires Premium")
        }
    }

    private fun listenForEvents() {
        viewModelScope.launch {
            repository.matchEvents.collect { event ->
                when (event) {
                    is com.chatora.app.data.remote.WebSocketEvent.Connected -> {
                        // Success connect
                    }
                    is com.chatora.app.data.remote.WebSocketEvent.MatchFound -> {
                        matchingTimeoutJob?.cancel()
                        handleMatchFound(event.data)
                    }
                    is com.chatora.app.data.remote.WebSocketEvent.Signal -> {
                        handleSignal(event.type, event.data)
                    }
                    is com.chatora.app.data.remote.WebSocketEvent.ChatMessage -> {
                        val myName = currentUser.value?.username ?: "me"
                        if (event.sender != myName) {
                            _messages.value += ChatMessage(
                                event.sender,
                                event.message,
                                false,
                                mediaUrl = event.mediaUrl,
                                mediaType = event.mediaType
                            )
                        }
                    }
                    is com.chatora.app.data.remote.WebSocketEvent.Error -> {
                        _matchState.value = MatchUiState.Error(event.reason)
                    }
                }
            }
        }
    }

    private fun handleMatchFound(data: String) {
        android.util.Log.d(TAG, "handleMatchFound called with data: $data")
        viewModelScope.launch {
            try {
                val json = org.json.JSONObject(data)
                val type = json.optString("type")

                if (type == "PARTNER_LEFT") {
                    val eventMatchId = json.optString("matchId", "none")

                    // CRITICAL: Ignore stale PARTNER_LEFT events
                    if (eventMatchId == "none" || eventMatchId.isEmpty()) {
                        android.util.Log.w(TAG, "PARTNER_LEFT ignored: matchId is 'none' (stale event)")
                        return@launch
                    }
                    if (currentMatchId != null && eventMatchId != currentMatchId) {
                        android.util.Log.w(TAG, "PARTNER_LEFT ignored: matchId=$eventMatchId doesn't match current=$currentMatchId")
                        return@launch
                    }

                    android.util.Log.i(TAG, "PARTNER_LEFT received for active match $eventMatchId. Auto-finding next...")
                    cleanupCurrentMatch()
                    _matchState.value = MatchUiState.Searching
                    kotlinx.coroutines.delay(PARTNER_LEFT_DELAY_MS)
                    debouncedFindMatch()
                    return@launch
                }

                if (type == "SEARCHING") {
                    android.util.Log.d(TAG, "Ignored SEARCHING event in handleMatchFound")
                    return@launch
                }

                val incomingMatchId = json.getString("matchId")

                // ATOMIC DUPLICATE GUARD: Use Mutex to prevent race condition
                val shouldProcess = matchMutex.withLock {
                    if (incomingMatchId == currentMatchId) {
                        false
                    } else {
                        currentMatchId = incomingMatchId
                        true
                    }
                }
                if (!shouldProcess) {
                    android.util.Log.w(TAG, "handleMatchFound: DUPLICATE matchId=$incomingMatchId (mutex-rejected)")
                    return@launch
                }

                webRtcClient.currentMatchId = currentMatchId

                val partnerId = json.getString("partnerId")
                val partnerUsername = json.getString("partnerUsername")
                val initiator = json.optBoolean("initiator", false)

                android.util.Log.d(TAG, "Match found! ID=$currentMatchId, Partner=$partnerUsername, Initiator=$initiator")

                val partnerUser = com.chatora.app.data.User(
                    id = partnerId,
                    username = partnerUsername,
                    email = "",
                    karma = 0,
                    diamonds = 0,
                    country = com.chatora.app.data.Country("Unknown", R.string.all_countries, "❓"),
                    isPremium = false
                )

                _remoteVideoReady.value = false

                val partnerIp = json.optString("partnerIp", "")
                val partnerCountryCode = json.optString("partnerCountryCode", "")

                _matchState.value = MatchUiState.Found(
                    partner = partnerUser,
                    partnerIp = partnerIp,
                    partnerCountryCode = partnerCountryCode
                )
                android.util.Log.d(TAG, "UI State updated to Found")

                launchRetryMechanism()

                // Subscribe to chat channel — signaling uses persistent /user/queue/call
                chatSubscriptionId = repository.subscribe("/topic/chat/$currentMatchId")

                // CRITICAL FIX: Always create fresh PeerConnection for each match.
                // Await local tracks to guarantee video+audio are added to PC.
                android.util.Log.d(TAG, "Creating fresh PeerConnection for match $currentMatchId")
                webRtcClient.closePeerConnection()
                if (!webRtcClient.areLocalTracksInitialized) {
                    android.util.Log.d(TAG, "Waiting for local tracks before creating PC...")
                    val tracksReady = webRtcClient.awaitLocalTracks()
                    if (!tracksReady) {
                        android.util.Log.e(TAG, "Local tracks failed — cannot establish call")
                        return@launch
                    }
                }
                webRtcClient.createPeerConnection()

                if (initiator) {
                    android.util.Log.d(TAG, "Initiator: Waiting ${INITIATOR_DELAY_MS}ms before creating Offer")
                    startConnectionTimeout()
                    kotlinx.coroutines.delay(INITIATOR_DELAY_MS)
                    android.util.Log.d(TAG, "Initiator: Creating Offer now")
                    webRtcClient.createOffer()
                } else {
                    android.util.Log.d(TAG, "Receiver: Waiting for Offer")
                    startConnectionTimeout()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in handleMatchFound", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Debounced wrapper for findMatch — prevents rapid cycling.
     * Only starts a new match if at least [FIND_MATCH_DEBOUNCE_MS] has elapsed since the last call.
     * FIXED: No longer calls stopMatching() which would reset state to Idle and
     * send a server leave, causing state corruption during auto-recovery.
     */
    private fun debouncedFindMatch() {
        val now = System.currentTimeMillis()
        if (now - lastFindMatchTime < FIND_MATCH_DEBOUNCE_MS) {
            android.util.Log.w(TAG, "debouncedFindMatch: Skipped (debounce active, ${now - lastFindMatchTime}ms since last)")
            return
        }
        // Clean up current match state (PC + topics) without resetting to Idle
        cleanupCurrentMatch()
        // Notify server we're leaving the current match
        repository.stopMatching(_selectedCountry.value.code, _selectedGender.value)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            findMatch()
        }
    }

    fun findMatch() {
        val now = System.currentTimeMillis()
        if (now - lastFindMatchTime < FIND_MATCH_DEBOUNCE_MS) {
            android.util.Log.w(TAG, "findMatch: Debounce active (${now - lastFindMatchTime}ms), skipping")
            return
        }
        lastFindMatchTime = now

        android.util.Log.d(TAG, "findMatch() called. hasStarted=${_hasStarted.value}")
        if (!_hasStarted.value) _hasStarted.value = true

        matchingTimeoutJob?.cancel()
        _matchState.value = MatchUiState.Searching
        _messages.value = emptyList()
        _remoteVideoReady.value = false

        // Clean up old match state WITHOUT sending leave to server
        cleanupCurrentMatch()

        // CRITICAL FIX: Ensure local tracks are initialized before creating PeerConnection.
        // startLocalVideo() runs async — without awaiting, tracks are null and the partner
        // sees a black screen because no video/audio tracks are added to the PeerConnection.
        viewModelScope.launch {
            try {
                if (!webRtcClient.areLocalTracksInitialized) {
                    android.util.Log.d(TAG, "Waiting for local tracks to initialize...")
                    val tracksReady = webRtcClient.awaitLocalTracks()
                    if (!tracksReady) {
                        android.util.Log.e(TAG, "Local tracks failed to initialize — cannot start call")
                        _matchState.value = MatchUiState.Error("Camera initialization failed")
                        return@launch
                    }
                }
                android.util.Log.d(TAG, "Creating PeerConnection with local tracks ready...")
                webRtcClient.createPeerConnection()
                android.util.Log.d(TAG, "PeerConnection created. Ready=${webRtcClient.isPeerConnectionReady}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error creating PeerConnection", e)
            }
        }

        // Pass "AUTO" to let the server detect country via GeoIP
        val myCountry = "AUTO"
        val targetCountry = _selectedCountry.value.code
        val gender = _selectedGender.value

        android.util.Log.d(TAG, "Calling repository.findMatch($myCountry, $targetCountry, $gender)")

        // Now send the join request — PC is guaranteed ready
        repository.findMatch(myCountry, targetCountry, gender)

        // Timeout after 30 seconds if no match
        matchingTimeoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(MATCHING_TIMEOUT_MS)
            if (_matchState.value is MatchUiState.Searching) {
                _matchState.value = MatchUiState.Error(
                    context.getString(R.string.no_partners_found)
                )
            }
        }
    }

    // Persistent renderer reference — lives for the entire session, never destroyed
    private var remoteRendererRef: org.webrtc.SurfaceViewRenderer? = null

    fun initRemoteVideo(renderer: org.webrtc.SurfaceViewRenderer) {
        remoteRendererRef = renderer
        webRtcClient.initRemoteSurface(renderer)

        // FRAME LISTENER: Detect when the first frame actually arrives to remove placeholder
        renderer.addFrameListener({
            android.util.Log.d(TAG, "Remote Video First Frame Rendered!")
            _remoteVideoReady.value = true
        }, 1.0f)

        // FIX: If track was received BEFORE UI was ready, attach it now
        webRtcClient.remoteVideoTrack?.addSink(renderer)

        // This callback persists across ALL connections — handles track swapping
        webRtcClient.onRemoteVideoTrackReceived = { track ->
            android.util.Log.d(TAG, "onRemoteVideoTrackReceived: new track => adding sink to persistent renderer")
            remoteRendererRef?.let { r ->
                track.addSink(r)
                // Re-install frame listener for new track
                r.addFrameListener({
                    android.util.Log.d(TAG, "Remote Video First Frame Rendered (new track)!")
                    _remoteVideoReady.value = true
                }, 1.0f)
            }
        }

        // RETRY MECHANISM: If no frame in 3 seconds, forcefully re-attach sink
        launchRetryMechanism()
    }

    private var retryJob: Job? = null

    private fun launchRetryMechanism() {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            kotlinx.coroutines.delay(RETRY_SINK_DELAY_MS)
            if (!_remoteVideoReady.value && _matchState.value is MatchUiState.Found) {
                android.util.Log.w(TAG, "No video frame after ${RETRY_SINK_DELAY_MS / 1000}s — retrying sink attachment")
                val renderer = remoteRendererRef ?: return@launch
                webRtcClient.remoteVideoTrack?.let { track ->
                    try {
                        track.removeSink(renderer)
                        track.addSink(renderer)
                        android.util.Log.d(TAG, "Sink re-attached successfully")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Sink re-attach failed", e)
                    }
                }
            }
            kotlinx.coroutines.delay(RETRY_SINK_DELAY_MS)
            if (!_remoteVideoReady.value && _matchState.value is MatchUiState.Found) {
                android.util.Log.w(TAG, "No video frame after ${RETRY_FINAL_DELAY_MS / 1000}s — final retry")
                val renderer = remoteRendererRef ?: return@launch
                webRtcClient.remoteVideoTrack?.let { track ->
                    try {
                        track.removeSink(renderer)
                        track.addSink(renderer)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Final sink re-attach failed", e)
                    }
                }
            }
        }
    }

    fun releaseRemoteVideo(renderer: org.webrtc.SurfaceViewRenderer) {
        _remoteVideoReady.value = false
        retryJob?.cancel()
    }

    private var connectionTimeoutJob: Job? = null

    private fun startConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Connection Timeout Started: ${CONNECTION_TIMEOUT_MS / 1000}s")
                kotlinx.coroutines.delay(CONNECTION_TIMEOUT_MS)
                android.util.Log.e(TAG, "Connection Timeout Reached! No Signal received.")
                debouncedFindMatch()
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d(TAG, "Connection Timeout Cancelled (Signal received)")
            }
        }
    }

    private fun handleSignal(type: String, data: String) {
        android.util.Log.d(TAG, "handleSignal received: type=$type")

        // If we receive ANY signal, the connection is alive
        connectionTimeoutJob?.cancel()

        try {
            when (type) {
                "offer" -> {
                    android.util.Log.d(TAG, "Handling OFFER")
                    webRtcClient.handleOffer(org.json.JSONObject(data).getString("sdp"))
                }
                "answer" -> {
                    android.util.Log.d(TAG, "Handling ANSWER")
                    webRtcClient.handleAnswer(org.json.JSONObject(data).getString("sdp"))
                }
                "ice-candidate" -> {
                    val json = org.json.JSONObject(data)
                    webRtcClient.handleIceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("candidate")
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in handleSignal", e)
            e.printStackTrace()
        }
    }

    /**
     * Lightweight cleanup: closes WebRTC + unsubscribes topics WITHOUT sending
     * /app/match/leave to server. This prevents the cascading PARTNER_LEFT loop.
     * Used when transitioning between matches (findMatch, PARTNER_LEFT handler).
     */
    private fun cleanupCurrentMatch() {
        android.util.Log.d(TAG, "cleanupCurrentMatch() called")

        val mId = currentMatchId
        if (mId != null) {
            viewModelScope.launch {
                try {
                    apiService.cleanupMatchFiles(mId)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "File cleanup failed", e)
                }
            }
        }

        webRtcClient.clearRemoteVideoTrack(null)
        webRtcClient.closePeerConnection()
        currentMatchId = null
        _remoteVideoReady.value = false
        _messages.value = emptyList()
        matchingTimeoutJob?.cancel()
        connectionTimeoutJob?.cancel()
        retryJob?.cancel()
        // Unsubscribe chat topic (call topic is persistent /user/queue/call)
        chatSubscriptionId?.let { repository.unsubscribe(it) }
        chatSubscriptionId = null
    }

    /**
     * Full stop: sends /app/match/leave to server AND cleans up locally.
     * Used when user explicitly presses Stop or navigates away.
     */
    fun stopMatching() {
        android.util.Log.d(TAG, "stopMatching() called")

        cleanupCurrentMatch()

        // Notify server to remove us from queue/match
        repository.stopMatching(_selectedCountry.value.code, _selectedGender.value)

        // Reset UI to IDLE
        _matchState.value = MatchUiState.Idle
        _hasStarted.value = false
    }

    fun nextMatch() {
        android.util.Log.d(TAG, "nextMatch() called")
        // User explicitly pressed "Next" — notify server so partner gets PARTNER_LEFT
        repository.stopMatching(_selectedCountry.value.code, _selectedGender.value)
        // Clean up local state (without sending leave again)
        cleanupCurrentMatch()
        // Find new match
        findMatch()
    }

    fun switchCamera() {
        webRtcClient.switchCamera()
    }

    fun sendMessage(text: String, mediaUrl: String? = null, mediaType: String? = null) {
        val mId = currentMatchId ?: return
        if (text.isBlank() && mediaUrl == null) return

        val isPremium = currentUser.value?.isPremium ?: false
        if (mediaUrl != null && !isPremium) {
            _uiEvent.value = UiEvent.ShowPremiumRequired("Sending media requires Premium")
            return
        }

        val myUsername = currentUser.value?.username ?: "me"
        _messages.value += ChatMessage(
            myUsername,
            text,
            true,
            mediaUrl = mediaUrl,
            mediaType = mediaType
        )

        repository.sendMessage(mId, text, mediaUrl, mediaType)
    }

    fun uploadAndSendMedia(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val body = okhttp3.MultipartBody.Part.createFormData("file", "media", requestFile)

                val currentMid = currentMatchId
                val response = apiService.uploadFile(body, currentMid)
                if (response.isSuccessful) {
                    val url = response.body()?.get("url")
                    if (url != null) {
                        sendMessage("", mediaUrl = url, mediaType = mimeType)
                    }
                } else {
                    android.util.Log.e("MatchViewModel", "Upload failed: ${response.code()} ${response.errorBody()?.string()}")
                    _uiEvent.value = UiEvent.ShowPremiumRequired("Upload failed: ${response.code()}. Check server logs.")
                }
            } catch (e: Exception) {
                android.util.Log.e("MatchViewModel", "Upload failed", e)
                _uiEvent.value = UiEvent.ShowPremiumRequired("Upload error: ${e.message}")
            }
        }
    }

    fun clearUiEvent() {
        _uiEvent.value = null
    }

    fun updateUserGender(gender: String) {
        viewModelScope.launch {
            userRepository.updateGender(gender)
        }
    }

    fun initLocalVideo(renderer: org.webrtc.SurfaceViewRenderer) {
        webRtcClient.startLocalVideo(renderer)
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d(TAG, "MatchViewModel cleared. Cleaning up WebRTC...")
        connectionTimeoutJob?.cancel()
        retryJob?.cancel()
        webRtcClient.cleanup()
        stopMatching()
    }
}

sealed class UiEvent {
    data class ShowPremiumRequired(val message: String) : UiEvent()
}

data class ChatMessage(
    val sender: String,
    val text: String,
    val isFromMe: Boolean,
    val isImage: Boolean = false,
    val mediaUrl: String? = null,
    val mediaType: String? = null
) {
    val isPhoto: Boolean get() = mediaType?.startsWith("image") == true || isImage
    val isVideo: Boolean get() = mediaType?.startsWith("video") == true
}

sealed class MatchUiState {
    object Idle : MatchUiState()
    object Searching : MatchUiState()
    data class Found(
        val partner: User,
        val partnerIp: String = "",
        val partnerCountryCode: String = ""
    ) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}
