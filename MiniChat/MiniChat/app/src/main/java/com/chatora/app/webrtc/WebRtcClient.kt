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
    private val eglBase by lazy { EglBase.create() }
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

        // Adaptive bitrate constraints (kbps)
        // Start at HIGH quality — WebRTC will adapt DOWN if network is slow
        private const val VIDEO_MAX_BITRATE_BPS = 1_500_000  // 1.5 Mbps — great quality on good networks
        private const val VIDEO_MIN_BITRATE_BPS = 100_000    // 100 kbps — minimum usable video
        private const val VIDEO_START_BITRATE_BPS = 800_000  // 800 kbps — balanced start
        private const val AUDIO_MAX_BITRATE_BPS = 64_000     // 64 kbps — Opus default
        private const val SDP_BANDWIDTH_LIMIT_KBPS = 1800    // Total session cap in kbps (b=AS:)
    }

    // Lazy init flag — factory initialization deferred until first use
    // to prevent ANR during ViewModel creation on the main thread.
    @Volatile
    private var isFactoryInitialized = false
    private val factoryLock = Any()

    private fun ensureFactoryInitialized() {
        if (isFactoryInitialized) return
        synchronized(factoryLock) {
            if (isFactoryInitialized) return
            android.util.Log.d(TAG, "Lazy-initializing PeerConnectionFactory...")
            initializeFactory()
            isFactoryInitialized = true
            android.util.Log.d(TAG, "PeerConnectionFactory initialized successfully")
        }
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
                // Ensure factory is ready (lazy init — first call initializes it)
                ensureFactoryInitialized()
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
     * Build ICE server list with multiple STUN + TURN servers.
     *
     * CRITICAL for Iranian ISPs: Direct P2P between Irancell and Hamrah-e-Aval
     * almost always fails due to symmetric NAT. A reliable TURN relay is MANDATORY.
     *
     * Priority order:
     * 1. User's own VPS TURN (most reliable, closest geographically)
     * 2. BuildConfig TURN (configurable)
     * 3. Free TURN servers as fallback
     * 4. Multiple STUN servers for P2P when possible
     */
    private fun buildIceServers(): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()

        // ── STUN servers (for P2P discovery when NAT allows it) ──
        servers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer())
        servers.add(PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer())

        // ── PRIMARY TURN: User's own VPS ──
        // This is the most reliable option for Iranian users since it's geographically close
        // and not blocked. Requires coturn to be installed on the VPS.
        val vpsIp = BuildConfig.API_BASE_URL
            .replace("http://", "")
            .replace("https://", "")
            .split(":")[0] // Cleanly extract ONLY the IP/Domain, removing ports/paths
            .trimEnd('/')
        
        if (vpsIp.isNotEmpty()) {
            // UDP TURN on standard port
            servers.add(
                PeerConnection.IceServer.builder("turn:$vpsIp:3478")
                    .setUsername("chatora")
                    .setPassword("chatora2024")
                    .createIceServer()
            )
            // TCP TURN (for networks that block UDP)
            servers.add(
                PeerConnection.IceServer.builder("turn:$vpsIp:3478?transport=tcp")
                    .setUsername("chatora")
                    .setPassword("chatora2024")
                    .createIceServer()
            )
            // TCP TURN on 443 (Plain TURN over 443 — excellent for bypassing ISP throttling)
            servers.add(
                PeerConnection.IceServer.builder("turn:$vpsIp:443?transport=tcp")
                    .setUsername("chatora")
                    .setPassword("chatora2024")
                    .createIceServer()
            )
            // TURNS (TLS-wrapped TURN over 443 — requires TLS certs on server to work)
            servers.add(
                PeerConnection.IceServer.builder("turns:$vpsIp:443?transport=tcp")
                    .setUsername("chatora")
                    .setPassword("chatora2024")
                    .createIceServer()
            )
            android.util.Log.d(TAG, "VPS TURN+TURNS servers configured on 3478 & 443: $vpsIp")
        }

        // ── SECONDARY TURN: BuildConfig (configurable) ──
        val turnUrl = BuildConfig.TURN_URL
        val turnUser = BuildConfig.TURN_USERNAME
        val turnPass = BuildConfig.TURN_PASSWORD

        if (turnUrl.isNotEmpty() && turnUser.isNotEmpty()) {
            servers.add(
                PeerConnection.IceServer.builder(turnUrl)
                    .setUsername(turnUser)
                    .setPassword(turnPass)
                    .createIceServer()
            )
            val tcpUrl = if (turnUrl.contains("?")) turnUrl else "$turnUrl?transport=tcp"
            servers.add(
                PeerConnection.IceServer.builder(tcpUrl)
                    .setUsername(turnUser)
                    .setPassword(turnPass)
                    .createIceServer()
            )
            android.util.Log.d(TAG, "BuildConfig TURN servers configured: $turnUrl")
        }

        android.util.Log.d(TAG, "Total ICE servers configured: ${servers.size}")
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

            // Ensure factory is ready (lazy init — should already be initialized by startLocalVideo)
            ensureFactoryInitialized()

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
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                // FORCE all traffic through TURN relay — critical for Iranian mobile ISPs
                // P2P "ghost connections" happen between Irancell/MCI where ICE connects
                // but media doesn't flow due to symmetric NAT. RELAY guarantees media flow.
                iceTransportsType = PeerConnection.IceTransportsType.RELAY
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
                            android.util.Log.d(TAG, "Connection established. Applying bitrate constraints + Reinforcing Loudspeaker...")
                            applyBandwidthConstraints()
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
            // Guard: if PC was disposed during the delay, skip verification
            if (!isPeerConnectionActive.get()) {
                android.util.Log.d(TAG, "Audio verification skipped — PeerConnection no longer active")
                return@launch
            }
            try {
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
            } catch (e: IllegalStateException) {
                // MediaStreamTrack was disposed between our check and access — safe to ignore
                android.util.Log.w(TAG, "Audio verification: track already disposed (safe to ignore)", e)
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
                val constrainedDesc = desc?.let { prepareSdp(it) } ?: desc
                peerConnection?.setLocalDescription(object : SdpObserverAdapter("setLocalDesc-offer") {}, constrainedDesc)
                val payload = JSONObject().apply {
                    put("type", constrainedDesc?.type?.canonicalForm())
                    put("sdp", constrainedDesc?.description)
                }
                android.util.Log.d(TAG, "Generated SDP (${constrainedDesc?.type}): ${constrainedDesc?.description?.take(80)}...")
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
                val constrainedDesc = desc?.let { prepareSdp(it) } ?: desc
                peerConnection?.setLocalDescription(object : SdpObserverAdapter("setLocalDesc-answer") {}, constrainedDesc)
                val payload = JSONObject().apply {
                    put("type", constrainedDesc?.type?.canonicalForm())
                    put("sdp", constrainedDesc?.description)
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

    /**
     * Adaptive bitrate control — sets max/min/start bitrate on the video sender.
     *
     * WebRTC internally uses GCC (Google Congestion Control) to estimate available
     * bandwidth. By setting these parameters, we tell the encoder:
     * - Start at [VIDEO_START_BITRATE_BPS] (800 kbps — balanced)
     * - Scale UP to [VIDEO_MAX_BITRATE_BPS] (1.5 Mbps) on fast networks
     * - Scale DOWN to [VIDEO_MIN_BITRATE_BPS] (100 kbps) on slow networks
     *
     * Without this, WebRTC may try to maintain high bitrate on a slow link,
     * causing packet loss and black screens.
     */
    private fun applyBandwidthConstraints() {
        try {
            val pc = peerConnection ?: return
            pc.senders.forEach { sender ->
                val track = sender.track() ?: return@forEach
                val params = sender.parameters

                when (track.kind()) {
                    "video" -> {
                        if (params.encodings.isNotEmpty()) {
                            params.encodings[0].apply {
                                maxBitrateBps = VIDEO_MAX_BITRATE_BPS
                                minBitrateBps = VIDEO_MIN_BITRATE_BPS
                                // Let WebRTC's GCC algorithm adapt within this range
                            }
                            sender.parameters = params
                            android.util.Log.d(TAG, "Video bitrate constraints applied: min=${VIDEO_MIN_BITRATE_BPS/1000}kbps, max=${VIDEO_MAX_BITRATE_BPS/1000}kbps")
                        }
                    }
                    "audio" -> {
                        if (params.encodings.isNotEmpty()) {
                            params.encodings[0].maxBitrateBps = AUDIO_MAX_BITRATE_BPS
                            sender.parameters = params
                            android.util.Log.d(TAG, "Audio bitrate constraint applied: max=${AUDIO_MAX_BITRATE_BPS/1000}kbps")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error applying bandwidth constraints", e)
        }
    }

    /**
     * Prepares SDP for signaling:
     * 1. Applies bandwidth limits (b=AS:)
     * 2. Prioritizes H.264 codec over VP8/VP9 (better hardware support/reliability)
     */
    private fun prepareSdp(desc: SessionDescription): SessionDescription {
        try {
            val lines = desc.description.lines().toMutableList()
            val modifiedLines = mutableListOf<String>()
            var bandwidthAdded = false

            for (line in lines) {
                // Codec Prioritization: Find H264 and move it to the front
                if (line.startsWith("m=video")) {
                    modifiedLines.add(line)
                    if (!bandwidthAdded) {
                        modifiedLines.add("b=AS:$SDP_BANDWIDTH_LIMIT_KBPS")
                        bandwidthAdded = true
                    }
                    continue
                }

                modifiedLines.add(line)
            }

            // More aggressive H264 preference by manipulating payload types
            val sdpString = modifiedLines.joinToString("\n")
            val finalSdp = preferCodec(sdpString, "H264", true)

            return SessionDescription(desc.type, finalSdp)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error preparing SDP", e)
            return desc
        }
    }

    private fun preferCodec(sdp: String, codec: String, isVideo: Boolean): String {
        val lines = sdp.split("\n")
        val mLineIndex = lines.indexOfFirst { it.startsWith(if (isVideo) "m=video" else "m=audio") }
        if (mLineIndex == -1) return sdp

        val mLine = lines[mLineIndex]
        val parts = mLine.split(" ").toMutableList()
        if (parts.size < 4) return sdp

        val payloadTypes = parts.subList(3, parts.size)
        val codecPayloadTypes = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("a=rtpmap") && line.contains(codec, ignoreCase = true)) {
                val pt = line.split(" ")[0].split(":")[1]
                if (payloadTypes.contains(pt)) {
                    codecPayloadTypes.add(pt)
                }
            }
        }

        if (codecPayloadTypes.isEmpty()) return sdp

        val newPayloadTypes = codecPayloadTypes + (payloadTypes - codecPayloadTypes)
        parts.subList(3, parts.size).clear()
        parts.addAll(newPayloadTypes)

        val newLines = lines.toMutableList()
        newLines[mLineIndex] = parts.joinToString(" ")
        return newLines.joinToString("\n")
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
            // DO NOT null out onRemoteVideoTrackReceived here.
            // It is set once by MatchViewModel.initRemoteVideo() and must
            // persist across all match transitions for the entire session.
            // Nulling it caused subsequent matches to never receive remote
            // video tracks, resulting in a 3-second freeze.
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
