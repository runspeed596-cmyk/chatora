package com.chatora.app.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Build
import com.chatora.app.BuildConfig
import com.chatora.app.data.remote.WebSocketManager
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade WebRTC client.
 *
 * Key design decisions:
 * - TURN servers injected via BuildConfig (no hardcoded credentials)
 * - Proper ICE state monitoring with recovery timers
 * - Thread-safe PeerConnection lifecycle (synchronized create/close)
 * - Pending ICE candidate queue with remote-description guard
 * - Audio track verification after connection establishment
 * - SDP failure logging for diagnostics
 */
class WebRtcClient @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val socketManager: WebSocketManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    // Media
    private val eglBase = EglBase.create()
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var isCameraRunning = false

    var remoteVideoTrack: VideoTrack? = null
        private set

    // Remote audio track — tracked for verification and re-enabling
    private var remoteAudioTrack: org.webrtc.AudioTrack? = null

    var currentMatchId: String? = null

    var onRemoteVideoTrackReceived: ((VideoTrack) -> Unit)? = null
    var onConnectionStateChanged: ((PeerConnection.PeerConnectionState) -> Unit)? = null

    // ICE Candidate Queueing — buffer candidates received before remote description is set
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var hasRemoteDescription = false
    private var hasHandledOffer = false // Offer-once guard: prevents duplicate offer processing

    // Remote renderer reference — stored so onTrack can always find the active sink
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var audioDeviceModule: JavaAudioDeviceModule? = null

    // ICE recovery timer
    private var iceRecoveryJob: Job? = null

    // PeerConnection lifecycle lock
    private val pcLock = Any()

    // Flag to indicate PeerConnection is fully created and ready for signaling
    @Volatile
    var isPeerConnectionReady = false
        private set

    // LOCAL TRACK READINESS: CompletableDeferred signals when camera+audio are initialized.
    // Awaited by ViewModel before createPeerConnection() to guarantee non-null tracks.
    private var localTracksDeferred = CompletableDeferred<Boolean>()

    @Volatile
    var areLocalTracksInitialized = false
        private set

    // Guard against stale PeerConnection callbacks after close
    private val isPeerConnectionActive = AtomicBoolean(false)

    companion object {
        private const val TAG = "WebRtcClient"
        private const val ICE_RECOVERY_TIMEOUT_MS = 8000L
        private const val AUDIO_VERIFY_DELAY_MS = 2000L
        private const val ICE_RESTART_MAX_ATTEMPTS = 3
    }

    init {
        initializeFactory()
    }

    private fun initializeFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        // ADM (Audio Device Module) setup for robust speakerphone support
        audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setAudioDeviceModule(audioDeviceModule)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
    }

    // Store local surface reference for deferred sink attachment
    private var localSurface: SurfaceViewRenderer? = null

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        try {
            surface.init(eglBase.eglBaseContext, null)
        } catch (e: Exception) {
            // Already initialized
        }
        surface.setMirror(true)
        localSurface = surface

        if (isCameraRunning && localVideoTrack != null) {
            localVideoTrack?.addSink(surface)
            return
        }

        // Move ALL heavy WebRTC init to background thread — prevents ANR
        scope.launch {
            try {
                videoCapturer = createCameraCapturer(context) ?: run {
                    android.util.Log.e(TAG, "Failed to create camera capturer")
                    if (!localTracksDeferred.isCompleted) localTracksDeferred.complete(false)
                    return@launch
                }

                val videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
                videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
                videoCapturer!!.startCapture(640, 480, 30)

                localVideoTrack = factory.createVideoTrack("100", videoSource)
                localSurface?.let { localVideoTrack?.addSink(it) }

                // Audio setup with echo cancellation and noise suppression
                val audioConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                }
                val audioSource = factory.createAudioSource(audioConstraints)
                localAudioTrack = factory.createAudioTrack("101", audioSource)
                localAudioTrack?.setEnabled(true)

                enableSpeakerphone()

                isCameraRunning = true
                areLocalTracksInitialized = true
                if (!localTracksDeferred.isCompleted) localTracksDeferred.complete(true)
                android.util.Log.d(TAG, "Camera + Audio initialized. Local tracks ready for PeerConnection.")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error initializing camera on background thread", e)
                if (!localTracksDeferred.isCompleted) localTracksDeferred.complete(false)
            }
        }
    }

    private fun enableSpeakerphone() {
        scope.launch {
            repeat(8) { attempt ->
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

                    // Check for Headset/Bluetooth
                    val isHeadsetConnected = audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
                    if (isHeadsetConnected) {
                        android.util.Log.d(TAG, "Headset connected. Skipping forced speakerphone.")
                        return@launch
                    }

                    // Modern Android (API 31+) Audio Management
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val devices = audioManager.availableCommunicationDevices
                        val speaker = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        if (speaker != null) {
                            val result = audioManager.setCommunicationDevice(speaker)
                            android.util.Log.d(TAG, "setCommunicationDevice (Speaker) result: $result")
                        }
                    }

                    // Fallback / Global Enforcement
                    audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                    audioManager.isSpeakerphoneOn = true

                    android.util.Log.d(TAG, "Attempt ${attempt + 1}: Loudspeaker route ensured.")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error in speakerphone loop", e)
                }
                delay(1000)
            }
        }
    }

    fun switchCamera() {
        if (!isCameraRunning) return
        try {
            (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initRemoteSurface(surface: SurfaceViewRenderer) {
        // ALWAYS store renderer first — so onTrack can find it even if init throws
        remoteRenderer = surface
        try {
            surface.init(eglBase.eglBaseContext, null)
            surface.setEnableHardwareScaler(true)
            surface.setMirror(false)
            surface.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            android.util.Log.d(TAG, "initRemoteSurface: Surface initialized successfully")
        } catch (e: IllegalStateException) {
            // Already initialized — this is OK
            android.util.Log.w(TAG, "initRemoteSurface: Surface already initialized (OK)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error initializing remote surface", e)
        }
        // If track already arrived before UI was ready, bind immediately
        attachSinkToRemoteTrack()
    }

    /**
     * Thread-safe helper: attaches remoteRenderer to remoteVideoTrack if both are available.
     * Can be called from any thread — safe for onTrack (WebRTC thread) and initRemoteSurface (main thread).
     */
    @Synchronized
    private fun attachSinkToRemoteTrack() {
        val track = remoteVideoTrack
        val renderer = remoteRenderer
        if (track != null && renderer != null) {
            android.util.Log.d(TAG, "attachSinkToRemoteTrack: Binding track to renderer")
            try {
                track.addSink(renderer)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "attachSinkToRemoteTrack: Failed", e)
            }
        } else {
            android.util.Log.d(TAG, "attachSinkToRemoteTrack: Not ready yet (track=$track, renderer=$renderer)")
        }
    }

    /**
     * Build ICE server list from BuildConfig TURN credentials + Google STUN servers.
     * Uses multiple transport protocols for maximum NAT traversal.
     */
    private fun buildIceServers(): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()

        // Multiple STUN servers for redundancy
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer())

        // TURN servers from BuildConfig — critical for users behind strict NATs/firewalls
        val turnUrl = BuildConfig.TURN_URL
        val turnUser = BuildConfig.TURN_USERNAME
        val turnPass = BuildConfig.TURN_PASSWORD

        if (turnUrl.isNotEmpty() && turnUser.isNotEmpty()) {
            // UDP TURN
            servers.add(
                PeerConnection.IceServer.builder(turnUrl)
                    .setUsername(turnUser)
                    .setPassword(turnPass)
                    .createIceServer()
            )
            // TCP TURN (for firewall-restricted networks)
            val tcpUrl = if (turnUrl.contains("?")) turnUrl else "$turnUrl?transport=tcp"
            servers.add(
                PeerConnection.IceServer.builder(tcpUrl)
                    .setUsername(turnUser)
                    .setPassword(turnPass)
                    .createIceServer()
            )
            // TURNS (TLS-wrapped TURN for maximum compatibility)
            val turnsUrl = turnUrl.replace("turn:", "turns:")
            servers.add(
                PeerConnection.IceServer.builder(turnsUrl)
                    .setUsername(turnUser)
                    .setPassword(turnPass)
                    .createIceServer()
            )
            android.util.Log.d(TAG, "TURN servers configured from BuildConfig: $turnUrl")
        } else {
            android.util.Log.w(TAG, "No TURN credentials in BuildConfig — NAT traversal may fail for restricted networks")
        }

        return servers
    }

    /**
     * Creates a new PeerConnection. MUST be called synchronously (not in a coroutine)
     * to guarantee the PC is ready before any signaling occurs.
     *
     * Caller is responsible for calling closePeerConnection() BEFORE this.
     */
    fun createPeerConnection() {
        synchronized(pcLock) {
            isPeerConnectionReady = false
            hasRemoteDescription = false
            hasHandledOffer = false
            pendingIceCandidates.clear()
            iceRecoveryJob?.cancel()
            remoteAudioTrack = null

            // Safety: dispose leftover PC if caller forgot to close
            peerConnection?.let {
                android.util.Log.w(TAG, "createPeerConnection: Disposing leftover PeerConnection")
                try { it.dispose() } catch (e: Exception) { /* ignore */ }
                peerConnection = null
            }

            val iceServers = buildIceServers()

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                iceTransportsType = PeerConnection.IceTransportsType.ALL
                // ICE candidate pool — pre-allocate for faster connection
                iceCandidatePoolSize = 2
            }

            peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        val payload = JSONObject().apply {
                            put("sdpMid", it.sdpMid)
                            put("sdpMLineIndex", it.sdpMLineIndex)
                            put("candidate", it.sdp)
                        }
                        currentMatchId?.let { id ->
                            socketManager.sendSignal(id, "ice-candidate", payload.toString())
                        }
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    // Guard against stale callbacks from disposed PeerConnection
                    if (!isPeerConnectionActive.get()) {
                        android.util.Log.w(TAG, "ICE Connection State: $newState (IGNORED — PC inactive)")
                        return
                    }
                    android.util.Log.d(TAG, "ICE Connection State: $newState")
                    when (newState) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            android.util.Log.i(TAG, "ICE connected/completed — cancelling recovery timer")
                            iceRecoveryJob?.cancel()
                            // Verify audio tracks after connection is established
                            verifyAudioTracks()
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            android.util.Log.w(TAG, "ICE DISCONNECTED — starting ${ICE_RECOVERY_TIMEOUT_MS}ms recovery timer")
                            startIceRecovery()
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            android.util.Log.e(TAG, "ICE FAILED — attempting ICE restart")
                            iceRecoveryJob?.cancel()
                            try {
                                synchronized(pcLock) {
                                    peerConnection?.restartIce()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e(TAG, "ICE restart failed", e)
                            }
                        }
                        else -> {}
                    }
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    android.util.Log.d(TAG, "ICE Gathering State: $p0")
                }

                override fun onAddStream(p0: MediaStream?) {}
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                    android.util.Log.d(TAG, "Signaling State: $p0")
                }

                override fun onRenegotiationNeeded() {}

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    // Guard against stale callbacks from disposed PeerConnection
                    if (!isPeerConnectionActive.get()) {
                        android.util.Log.w(TAG, "PeerConnection State: $newState (IGNORED — PC inactive)")
                        return
                    }
                    android.util.Log.d(TAG, "PeerConnection State: $newState")
                    newState?.let {
                        onConnectionStateChanged?.invoke(it)
                        if (it == PeerConnection.PeerConnectionState.CONNECTED) {
                            android.util.Log.d(TAG, "Connection established. Reinforcing Loudspeaker...")
                            enableSpeakerphone()
                        }
                    }
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    if (!isPeerConnectionActive.get()) {
                        android.util.Log.w(TAG, "onTrack: IGNORED (PC inactive)")
                        return
                    }
                    transceiver?.receiver?.track()?.let { track ->
                        when (track.kind()) {
                            "video" -> {
                                remoteVideoTrack = track as VideoTrack
                                android.util.Log.d(TAG, "onTrack: Remote VIDEO track received (enabled=${track.enabled()}). Renderer=${remoteRenderer != null}")
                                attachSinkToRemoteTrack()
                                onRemoteVideoTrackReceived?.invoke(remoteVideoTrack!!)
                            }
                            "audio" -> {
                                remoteAudioTrack = track as org.webrtc.AudioTrack
                                android.util.Log.d(TAG, "onTrack: Remote AUDIO track received (enabled=${track.enabled()})")
                                // Ensure remote audio is enabled
                                if (!track.enabled()) {
                                    android.util.Log.w(TAG, "Remote audio track was disabled — enabling it")
                                    track.setEnabled(true)
                                }
                            }
                        }
                    }
                }
            })

            // Add Local Tracks — guaranteed non-null if awaitLocalTracks() was called first
            addLocalTracksToPeerConnection()

            isPeerConnectionActive.set(true)
            isPeerConnectionReady = true
            android.util.Log.d(TAG, "PeerConnection created and ready. Local tracks: video=${localVideoTrack != null}, audio=${localAudioTrack != null}")
        }
    }

    /**
     * Add local video and audio tracks to the active PeerConnection.
     * Logs warnings if tracks are null (indicates startLocalVideo hasn't finished).
     */
    private fun addLocalTracksToPeerConnection() {
        val pc = peerConnection ?: return
        localVideoTrack?.let {
            pc.addTrack(it, listOf("stream1"))
            android.util.Log.d(TAG, "Added local VIDEO track to PeerConnection")
        } ?: android.util.Log.w(TAG, "localVideoTrack is NULL — video won't be sent to partner!")
        localAudioTrack?.let {
            pc.addTrack(it, listOf("stream1"))
            android.util.Log.d(TAG, "Added local AUDIO track to PeerConnection")
        } ?: android.util.Log.w(TAG, "localAudioTrack is NULL — audio won't be sent to partner!")
    }

    /**
     * Suspend until local video + audio tracks are initialized.
     * Must be called before createPeerConnection() to guarantee tracks are non-null.
     */
    suspend fun awaitLocalTracks(): Boolean {
        return localTracksDeferred.await()
    }

    /**
     * Public wrapper for ICE restart — called by MatchViewModel when connection
     * enters DISCONNECTED or FAILED state. Thread-safe via pcLock.
     */
    fun restartIce() {
        synchronized(pcLock) {
            try {
                if (peerConnection != null && isPeerConnectionActive.get()) {
                    android.util.Log.d(TAG, "restartIce() called — restarting ICE on active PeerConnection")
                    peerConnection?.restartIce()
                } else {
                    android.util.Log.w(TAG, "restartIce() called but PeerConnection is null or inactive")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "restartIce() failed", e)
            }
        }
    }

    /**
     * Start a timed ICE recovery: waits [ICE_RECOVERY_TIMEOUT_MS] then restarts ICE.
     * Cancelled if ICE recovers on its own (CONNECTED/COMPLETED).
     */
    private fun startIceRecovery() {
        iceRecoveryJob?.cancel()
        iceRecoveryJob = scope.launch {
            delay(ICE_RECOVERY_TIMEOUT_MS)
            android.util.Log.w(TAG, "ICE recovery timeout — attempting ICE restart")
            try {
                synchronized(pcLock) {
                    peerConnection?.restartIce()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "ICE restart after recovery timeout failed", e)
            }
        }
    }

    /**
     * Verify that audio tracks are properly enabled after connection.
     * Runs after a short delay to allow media negotiation to complete.
     */
    private fun verifyAudioTracks() {
        scope.launch {
            delay(AUDIO_VERIFY_DELAY_MS)
            val localAudio = localAudioTrack
            val remoteAudio = remoteAudioTrack

            android.util.Log.d(TAG, "Audio verification: local=${localAudio?.enabled()}, remote=${remoteAudio?.enabled()}")

            if (localAudio != null && !localAudio.enabled()) {
                android.util.Log.w(TAG, "Local audio track disabled — re-enabling")
                localAudio.setEnabled(true)
            }
            if (remoteAudio != null && !remoteAudio.enabled()) {
                android.util.Log.w(TAG, "Remote audio track disabled — re-enabling")
                remoteAudio.setEnabled(true)
            }
        }
    }

    fun createOffer() {
        if (!isPeerConnectionReady) {
            android.util.Log.e(TAG, "createOffer: PeerConnection not ready!")
            return
        }
        peerConnection?.createOffer(object : SdpObserverAdapter("createOffer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter("setLocalDesc-offer") {}, desc)
                val payload = JSONObject().apply {
                    put("type", desc?.type?.canonicalForm())
                    put("sdp", desc?.description)
                }
                android.util.Log.d(TAG, "Generated SDP (${desc?.type}): ${desc?.description?.take(80)}...")
                currentMatchId?.let { id ->
                    socketManager.sendSignal(id, "offer", payload.toString())
                }
            }
        }, MediaConstraints())
    }

    fun handleOffer(sdp: String) {
        // OFFER-ONCE GUARD: Reject duplicate offers for the same PeerConnection
        if (hasHandledOffer) {
            android.util.Log.w(TAG, "handleOffer: REJECTED (already handled an offer for this PC)")
            return
        }
        if (!isPeerConnectionReady) {
            android.util.Log.e(TAG, "handleOffer: PeerConnection not ready — dropping offer")
            return
        }
        hasHandledOffer = true

        peerConnection?.setRemoteDescription(object : SdpObserverAdapter("setRemoteDesc-offer") {
            override fun onSetSuccess() {
                hasRemoteDescription = true
                flushPendingIceCandidates()
                createAnswer()
            }
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserverAdapter("createAnswer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter("setLocalDesc-answer") {}, desc)
                val payload = JSONObject().apply {
                    put("type", desc?.type?.canonicalForm())
                    put("sdp", desc?.description)
                }
                currentMatchId?.let { id ->
                    socketManager.sendSignal(id, "answer", payload.toString())
                }
            }
        }, MediaConstraints())
    }

    fun handleAnswer(sdp: String) {
        if (!isPeerConnectionReady) {
            android.util.Log.e(TAG, "handleAnswer: PeerConnection not ready — dropping answer")
            return
        }
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter("setRemoteDesc-answer") {
            override fun onSetSuccess() {
                hasRemoteDescription = true
                flushPendingIceCandidates()
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun handleIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        if (hasRemoteDescription) {
            peerConnection?.addIceCandidate(iceCandidate)
        } else {
            android.util.Log.d(TAG, "Queueing ICE candidate (remote desc not set yet)")
            synchronized(pendingIceCandidates) {
                pendingIceCandidates.add(iceCandidate)
            }
        }
    }

    private fun flushPendingIceCandidates() {
        synchronized(pendingIceCandidates) {
            if (pendingIceCandidates.isNotEmpty()) {
                android.util.Log.d(TAG, "Flushing ${pendingIceCandidates.size} pending ICE candidates")
                pendingIceCandidates.forEach { candidate ->
                    peerConnection?.addIceCandidate(candidate)
                }
                pendingIceCandidates.clear()
            }
        }
    }


    fun closePeerConnection() {
        synchronized(pcLock) {
            try {
                android.util.Log.d(TAG, "closePeerConnection: Disposing...")
                // Mark inactive FIRST to prevent stale callbacks
                isPeerConnectionActive.set(false)
                isPeerConnectionReady = false
                iceRecoveryJob?.cancel()
                iceRecoveryJob = null

                // Detach remote tracks from renderer
                remoteVideoTrack?.let { track ->
                    try {
                        track.setEnabled(false)
                        remoteRenderer?.let { track.removeSink(it) }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Error detaching remote video track", e)
                    }
                }

                remoteAudioTrack?.let { track ->
                    try { track.setEnabled(false) } catch (e: Exception) { /* ignore */ }
                }

                // Dispose PC — but do NOT dispose local tracks!
                // Local tracks (video + audio) are created once by startLocalVideo()
                // and reused across all PeerConnections for the entire session.
                peerConnection?.dispose()
                peerConnection = null
                remoteVideoTrack = null
                remoteAudioTrack = null
                // CRITICAL: DO NOT set remoteRenderer = null here.
                // It is provided once by the UI and must survive match transitions.
                // CRITICAL: DO NOT null out localVideoTrack or localAudioTrack.
                // They must survive across match transitions.

                hasRemoteDescription = false
                hasHandledOffer = false
                synchronized(pendingIceCandidates) {
                    pendingIceCandidates.clear()
                }

                android.util.Log.d(TAG, "closePeerConnection: Done. Local tracks preserved: video=${localVideoTrack != null}, audio=${localAudioTrack != null}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in closePeerConnection", e)
            }
        }
    }

    fun clearRemoteVideoTrack(renderer: SurfaceViewRenderer?) {
        try {
            val r = renderer ?: remoteRenderer
            android.util.Log.d(TAG, "clearRemoteVideoTrack: renderer=$r")

            r?.let {
                remoteVideoTrack?.removeSink(it)
            }
            onRemoteVideoTrackReceived = null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in clearRemoteVideoTrack", e)
        }
    }

    fun stopCamera(surface: SurfaceViewRenderer? = null) {
        try {
            android.util.Log.d(TAG, "stopCamera: surface=$surface")
            surface?.let { localVideoTrack?.removeSink(it) }

            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            localVideoTrack?.setEnabled(false)
            localVideoTrack?.dispose()
            localVideoTrack = null

            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            localAudioTrack = null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in stopCamera", e)
        } finally {
            isCameraRunning = false
        }
    }

    fun cleanup() {
        try {
            closePeerConnection()
            stopCamera()
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            // Reset Audio Mode
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }

            audioManager.mode = android.media.AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false

            audioDeviceModule?.release()
            audioDeviceModule = null

            eglBase.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createCameraCapturer(context: Context): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        // Try front facing first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Fallback to back
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    /**
     * SDP Observer with proper failure logging.
     * The [label] helps identify which operation failed in logs.
     */
    open class SdpObserverAdapter(private val label: String = "SDP") : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(reason: String?) {
            android.util.Log.e("WebRtcClient", "SDP CREATE FAILURE [$label]: $reason")
        }
        override fun onSetFailure(reason: String?) {
            android.util.Log.e("WebRtcClient", "SDP SET FAILURE [$label]: $reason")
        }
    }
}
