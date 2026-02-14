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
    private var callSubscriptionId: String? = null
    private var chatSubscriptionId: String? = null
    
    private var matchingTimeoutJob: kotlinx.coroutines.Job? = null

    init {
        android.util.Log.d("MINICHAT_DEBUG", "MatchViewModel initialized")
        listenForEvents()
        
        // AUTO-NEXT on Connection Failure / ICE Restart on Disconnected
        webRtcClient.onConnectionStateChanged = { state ->
            android.util.Log.d("MINICHAT_DEBUG", "WebRTC Connection State: $state")
            when (state) {
                org.webrtc.PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    // ICE disconnected — try restarting ICE before giving up
                    android.util.Log.w("MINICHAT_DEBUG", "Connection DISCONNECTED. Attempting ICE restart...")
                    webRtcClient.restartIce()
                    // Give ICE restart 8 seconds to recover, then auto-search-next
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(8000)
                        val currentState = _matchState.value
                        if (currentState is MatchUiState.Found && !_remoteVideoReady.value) {
                            android.util.Log.w("MINICHAT_DEBUG", "ICE restart failed. Searching next...")
                            stopMatching()
                            kotlinx.coroutines.delay(500)
                            findMatch()
                        }
                    }
                }
                org.webrtc.PeerConnection.PeerConnectionState.FAILED -> {
                    android.util.Log.w("MINICHAT_DEBUG", "Connection FAILED. Searching next...")
                    viewModelScope.launch {
                        stopMatching()
                        kotlinx.coroutines.delay(500)
                        findMatch()
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
            android.util.Log.d("MINICHAT_DEBUG", "Token found (${token.take(10)}...), connecting...")
            repository.connectAndSubscribe(token)
        } else {
            android.util.Log.e("MINICHAT_DEBUG", "Token is NULL in MatchViewModel init!")
        }
    }

    fun updateCountry(country: com.chatora.app.data.Country) {
        val isPremium = currentUser.value?.isPremium ?: false
        // ALLOWED: AUTO (All Countries), US, GB
        val isAllowedCountry = country.code == "AUTO" || country.code == "US" || country.code == "GB"
        
        if (isPremium || isAllowedCountry) {
            _selectedCountry.value = country
        } else {
            // Signal to UI to show premium screen
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
                        // Avoid duplicates: Only add if message is from the partner
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

    private var connectionInitJob: kotlinx.coroutines.Job? = null

    // ...

    private fun handleMatchFound(data: String) {
        android.util.Log.d("MINICHAT_DEBUG", "handleMatchFound called with data: $data")
        viewModelScope.launch {
            // CRITICAL FIX: Wait for PeerConnection to be created if it's still initializing
            // This prevents race conditions where MATCH_FOUND arrives before createPeerConnection() finishes
            connectionInitJob?.join()
            android.util.Log.d("MINICHAT_DEBUG", "handleMatchFound: Connection init joined")

            try {
                // ... (rest of logic) ...
                val json = org.json.JSONObject(data)
                val type = json.optString("type")
                
                
                if (type == "PARTNER_LEFT") {
                   android.util.Log.i("MINICHAT_DEBUG", "PARTNER_LEFT received. Auto-finding next match...")
                   stopMatching()
                   kotlinx.coroutines.delay(500)
                   findMatch()
                   return@launch
                }
                
                if (type == "SEARCHING") {
                    android.util.Log.d("MINICHAT_DEBUG", "Ignored SEARCHING event in handleMatchFound")
                    return@launch
                }

                currentMatchId = json.getString("matchId")
                webRtcClient.currentMatchId = currentMatchId // CRITICAL FIX: Tell WebRTC where to send signals
                
                val partnerId = json.getString("partnerId")
                val partnerUsername = json.getString("partnerUsername")
                val initiator = json.optBoolean("initiator", false)
                
                android.util.Log.d("MINICHAT_DEBUG", "Match found! ID=$currentMatchId, Partner=$partnerUsername, Initiator=$initiator")
                
                // Create minimal user object for UI (fetching full details can happen later if needed)
                val partnerUser = com.chatora.app.data.User(
                    id = partnerId,
                    username = partnerUsername,
                    email = "",
                    karma = 0,
                    diamonds = 0,
                    country = com.chatora.app.data.Country("Unknown", R.string.all_countries, "❓"),
                    isPremium = false
                )

                // Parse partner country from server response (IP is never sent for privacy)
                val partnerCountryCode = json.optString("partnerCountryCode", "")
                val partnerCountryName = json.optString("partnerCountryName", "")
                
                _matchState.value = MatchUiState.Found(
                    partner = partnerUser,
                    partnerCountryCode = partnerCountryCode,
                    partnerCountryName = partnerCountryName
                )
                android.util.Log.d("MINICHAT_DEBUG", "UI State updated to Found")

                // Reset video state for new connection — renderer persists but needs new track
                _remoteVideoReady.value = false
                launchRetryMechanism()

                // Subscribe to signaling channel
                callSubscriptionId = repository.subscribe("/topic/call/$currentMatchId")
                chatSubscriptionId = repository.subscribe("/topic/chat/$currentMatchId")

                if (initiator) {
                    android.util.Log.d("MINICHAT_DEBUG", "Initiator: Waiting 1s before creating Offer")
                    startConnectionTimeout()
                    kotlinx.coroutines.delay(1000) 
                    android.util.Log.d("MINICHAT_DEBUG", "Initiator: Creating Offer now")
                    // ZERO DELAY: Connection is already created in findMatch()
                    webRtcClient.createOffer()
                } else {
                    android.util.Log.d("MINICHAT_DEBUG", "Receiver: Waiting for Offer")
                    startConnectionTimeout()
                }
            } catch (e: Exception) {
                android.util.Log.e("MINICHAT_DEBUG", "Error in handleMatchFound", e)
                e.printStackTrace()
            }
        }
    }

    // ...

    fun findMatch() {
        android.util.Log.d("MINICHAT_DEBUG", "findMatch() called. hasStarted=${_hasStarted.value}")
        if (!_hasStarted.value) _hasStarted.value = true
        
        matchingTimeoutJob?.cancel()
        _matchState.value = MatchUiState.Searching
        _messages.value = emptyList()
        _remoteVideoReady.value = false
        
        // OPTIMIZATION: Close old connection and Create NEW PeerConnection IMMEDIATELY
        connectionInitJob = viewModelScope.launch {
            try {
                android.util.Log.d("MINICHAT_DEBUG", "Recreating PeerConnection...")
                webRtcClient.closePeerConnection()
                currentMatchId = null
                webRtcClient.createPeerConnection() 
                android.util.Log.d("MINICHAT_DEBUG", "PeerConnection recreated.")
            } catch(e: Exception) {
                android.util.Log.e("MINICHAT_DEBUG", "Error creating PeerConnection", e)
            }
        }
        
        // Pass "AUTO" to let the server detect country via GeoIP
        val myCountry = "AUTO" 
        val targetCountry = _selectedCountry.value.code // Selected in UI filter
        val gender = _selectedGender.value
        
        android.util.Log.d("MINICHAT_DEBUG", "Calling repository.findMatch($myCountry, $targetCountry, $gender)")
        
        // Send request in parallel
        repository.findMatch(myCountry, targetCountry, gender)
        
        // Timeout after 30 seconds if no match
        matchingTimeoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(30000)
            if (_matchState.value is MatchUiState.Searching) {
                _matchState.value = MatchUiState.Error(
                    context.getString(R.string.no_partners_found)
                )
            }
        }
    }

    // ...

    // Persistent renderer reference — lives for the entire session, never destroyed
    private var remoteRendererRef: org.webrtc.SurfaceViewRenderer? = null

    fun initRemoteVideo(renderer: org.webrtc.SurfaceViewRenderer) {
        remoteRendererRef = renderer
        webRtcClient.initRemoteSurface(renderer)
        
        // FRAME LISTENER: Detect when the first frame actually arrives to remove placeholder
        renderer.addFrameListener({
            android.util.Log.d("MINICHAT_DEBUG", "Remote Video First Frame Rendered!")
            _remoteVideoReady.value = true
        }, 1.0f)

        // FIX: If track was received BEFORE UI was ready, attach it now
        webRtcClient.remoteVideoTrack?.addSink(renderer)
        
        // This callback persists across ALL connections — handles track swapping
        webRtcClient.onRemoteVideoTrackReceived = { track ->
            android.util.Log.d("MINICHAT_DEBUG", "onRemoteVideoTrackReceived: new track => adding sink to persistent renderer")
            remoteRendererRef?.let { r ->
                track.addSink(r)
                // Re-install frame listener for new track
                r.addFrameListener({
                    android.util.Log.d("MINICHAT_DEBUG", "Remote Video First Frame Rendered (new track)!")
                    _remoteVideoReady.value = true
                }, 1.0f)
            }
        }

        // RETRY MECHANISM: If no frame in 3 seconds, forcefully re-attach sink
        launchRetryMechanism()
    }

    private var retryJob: kotlinx.coroutines.Job? = null

    private fun launchRetryMechanism() {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            if (!_remoteVideoReady.value && _matchState.value is MatchUiState.Found) {
                android.util.Log.w("MINICHAT_DEBUG", "No video frame after 3s — retrying sink attachment")
                val renderer = remoteRendererRef ?: return@launch
                webRtcClient.remoteVideoTrack?.let { track ->
                    try {
                        track.removeSink(renderer)
                        track.addSink(renderer)
                        android.util.Log.d("MINICHAT_DEBUG", "Sink re-attached successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("MINICHAT_DEBUG", "Sink re-attach failed", e)
                    }
                }
            }
            kotlinx.coroutines.delay(3000)
            if (!_remoteVideoReady.value && _matchState.value is MatchUiState.Found) {
                android.util.Log.w("MINICHAT_DEBUG", "No video frame after 6s — final retry")
                val renderer = remoteRendererRef ?: return@launch
                webRtcClient.remoteVideoTrack?.let { track ->
                    try {
                        track.removeSink(renderer)
                        track.addSink(renderer)
                    } catch (e: Exception) {
                        android.util.Log.e("MINICHAT_DEBUG", "Final sink re-attach failed", e)
                    }
                }
            }
        }
    }

    fun releaseRemoteVideo(renderer: org.webrtc.SurfaceViewRenderer) {
        // Only reset video ready state — renderer stays alive
        _remoteVideoReady.value = false
        retryJob?.cancel()
    }

    private var connectionTimeoutJob: Job? = null

    private fun startConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = viewModelScope.launch {
            try {
                android.util.Log.d("MINICHAT_DEBUG", "Connection Timeout Started: 15s")
                kotlinx.coroutines.delay(15000)
                android.util.Log.e("MINICHAT_DEBUG", "Connection Timeout Reached! No Signal received.")
                // Disconnect and search again
                stopMatching()
                kotlinx.coroutines.delay(500)
                findMatch()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Timeout cancelled (Signal received)
                android.util.Log.d("MINICHAT_DEBUG", "Connection Timeout Cancelled (Signal received)")
            }
        }
    }

    private fun handleSignal(type: String, data: String) {
        android.util.Log.d("MINICHAT_DEBUG", "handleSignal received: type=$type")
        
        // If we receive ANY signal, the connection is alive.
        connectionTimeoutJob?.cancel()
        
        try {
            when (type) {
                "offer" -> {
                    android.util.Log.d("MINICHAT_DEBUG", "Handling OFFER")
                    webRtcClient.handleOffer(org.json.JSONObject(data).getString("sdp"))
                }
                "answer" -> {
                     android.util.Log.d("MINICHAT_DEBUG", "Handling ANSWER")
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
            android.util.Log.e("MINICHAT_DEBUG", "Error in handleSignal", e)
            e.printStackTrace()
        }
    }



    fun stopMatching() {
        android.util.Log.d("MINICHAT_DEBUG", "stopMatching() called")
        
        val mId = currentMatchId
        if (mId != null) {
            viewModelScope.launch {
                try {
                    apiService.cleanupMatchFiles(mId)
                } catch (e: Exception) {
                    android.util.Log.e("MINICHAT_DEBUG", "Cleanup failed", e)
                }
            }
        }

        repository.stopMatching(_selectedCountry.value.code, _selectedGender.value)
        webRtcClient.clearRemoteVideoTrack(null) 
        webRtcClient.closePeerConnection()
        currentMatchId = null
        _remoteVideoReady.value = false
        
        // Reset UI to IDLE and clear messages
        _matchState.value = MatchUiState.Idle
        _messages.value = emptyList()
        _hasStarted.value = false
        matchingTimeoutJob?.cancel()
        callSubscriptionId?.let { repository.unsubscribe(it) }
        chatSubscriptionId?.let { repository.unsubscribe(it) }
        callSubscriptionId = null
        chatSubscriptionId = null
    }

    fun nextMatch() {
        val mId = currentMatchId
        if (mId != null) {
            viewModelScope.launch {
                try {
                    apiService.cleanupMatchFiles(mId)
                } catch (e: Exception) {
                    android.util.Log.e("MINICHAT_DEBUG", "Cleanup failed", e)
                }
            }
        }
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
        
        // Instant/Optimistic update
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
        android.util.Log.d("MINICHAT_DEBUG", "MatchViewModel cleared. Cleaning up WebRTC...")
        connectionTimeoutJob?.cancel()
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
    val isImage: Boolean = false, // Deprecated, but keeping for compatibility if used elsewhere
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
        val partnerCountryCode: String = "",
        val partnerCountryName: String = ""
    ) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}
