package com.chatora.app.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Build
import com.chatora.app.data.remote.WebSocketEvent
import com.chatora.app.data.remote.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import javax.inject.Inject
import javax.inject.Singleton

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
            .setAudioDeviceModule(audioDeviceModule) // IMPORTANT: Force proper audio driver
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

        // Move ALL heavy WebRTC init to background thread — prevents ANR from Object.wait() in native code
        scope.launch {
            try {
                videoCapturer = createCameraCapturer(context) ?: return@launch
                
                val videoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
                videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
                videoCapturer!!.startCapture(640, 480, 30)
                
                localVideoTrack = factory.createVideoTrack("100", videoSource)
                // addSink is thread-safe in WebRTC
                localSurface?.let { localVideoTrack?.addSink(it) }
                
                // AUDIO OPTIMIZATION
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
                android.util.Log.d("WebRtcClient", "Camera + Audio initialized on background thread")
            } catch (e: Exception) {
                android.util.Log.e("WebRtcClient", "Error initializing camera on background thread", e)
            }
        }
    }

    private fun enableSpeakerphone() {
        scope.launch {
            // Repeat to ensure it sticks (WebRTC internals might reset it)
            repeat(8) { attempt ->
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    
                    // 1. Check for Headset/Bluetooth
                    val isHeadsetConnected = audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
                    if (isHeadsetConnected) {
                         android.util.Log.d("WebRtcClient", "Headset connected. Skipping forced speakerphone.")
                         return@launch
                    }

                    // 2. Modern Android (API 31+) Audio Management
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val devices = audioManager.availableCommunicationDevices
                        val speaker = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        if (speaker != null) {
                            val result = audioManager.setCommunicationDevice(speaker)
                            android.util.Log.d("WebRtcClient", "setCommunicationDevice (Speaker) result: $result")
                        }
                    }

                    // 3. Fallback / Global Enforcement
                    audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
                    audioManager.isSpeakerphoneOn = true
                    
                    // 4. Volume Fix: Removed forced volume to allow user control.
                    android.util.Log.d("WebRtcClient", "Attempt ${attempt+1}: Loudspeaker route ensured.")
                } catch (e: Exception) {
                    android.util.Log.e("WebRtcClient", "Error in speakerphone loop", e)
                }
                kotlinx.coroutines.delay(1000)
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
            android.util.Log.d("WebRtcClient", "initRemoteSurface: Surface initialized successfully")
        } catch (e: IllegalStateException) {
            // Already initialized — this is OK
            android.util.Log.w("WebRtcClient", "initRemoteSurface: Surface already initialized (OK)")
        } catch (e: Exception) {
            android.util.Log.e("WebRtcClient", "Error initializing remote surface", e)
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
            android.util.Log.d("WebRtcClient", "attachSinkToRemoteTrack: Binding track to renderer")
            try {
                track.addSink(renderer)
            } catch (e: Exception) {
                android.util.Log.e("WebRtcClient", "attachSinkToRemoteTrack: Failed", e)
            }
        } else {
            android.util.Log.d("WebRtcClient", "attachSinkToRemoteTrack: Not ready yet (track=$track, renderer=$renderer)")
        }
    }

    fun createPeerConnection() {
        // NOTE: Caller is responsible for calling closePeerConnection() BEFORE this.
        // We do NOT call closePeerConnection() here to avoid double-clearing remoteRenderer.
        hasRemoteDescription = false
        hasHandledOffer = false // Reset offer guard for new PeerConnection
        pendingIceCandidates.clear()

        // Multiple STUN servers for redundancy and better NAT traversal
        // + TURN servers for relay when direct peer-to-peer fails (symmetric NAT, firewalls)
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
            // Free TURN servers (Open Relay Project) — critical for users behind strict NATs
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            // ICE restart support — allow ICE to be restarted on connectivity failures
            iceTransportsType = PeerConnection.IceTransportsType.ALL
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
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(p0: MediaStream?) {}
            
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            
            override fun onRenegotiationNeeded() {}

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
               newState?.let { 
                   onConnectionStateChanged?.invoke(it) 
                   if (it == PeerConnection.PeerConnectionState.CONNECTED) {
                       android.util.Log.d("WebRtcClient", "Connection established. Reinforcing Loudspeaker...")
                       enableSpeakerphone()
                   }
               }
            }
            
            override fun onTrack(transceiver: RtpTransceiver?) {
                transceiver?.receiver?.track()?.let { track ->
                    if (track.kind() == "video") {
                        remoteVideoTrack = track as VideoTrack
                        android.util.Log.d("WebRtcClient", "onTrack: Remote video track received. Renderer=${remoteRenderer != null}")
                        // Use synchronized helper to safely attach sink
                        attachSinkToRemoteTrack()
                        onRemoteVideoTrackReceived?.invoke(remoteVideoTrack!!)
                    }
                }
            }
        })
        
        // Add Local Tracks
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("stream1")) }
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("stream1")) }
    }

    fun createOffer() {
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, desc)
                val payload = JSONObject().apply {
                    put("type", desc?.type?.canonicalForm())
                    put("sdp", desc?.description)
                }
                android.util.Log.d("WebRtcClient", "Generated SDP (${desc?.type}): ${desc?.description?.take(50)}...")
                currentMatchId?.let { id ->
                    socketManager.sendSignal(id, "offer", payload.toString())
                }
            }
        }, MediaConstraints())
    }
    
    fun handleOffer(sdp: String) {
        // OFFER-ONCE GUARD: Reject duplicate offers for the same PeerConnection.
        // This prevents the catastrophic scenario where two offers from the initiator
        // (caused by duplicate MATCH_FOUND processing) corrupt the SDP state.
        if (hasHandledOffer) {
            android.util.Log.w("WebRtcClient", "handleOffer: REJECTED (already handled an offer for this PC)")
            return
        }
        hasHandledOffer = true
        
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                hasRemoteDescription = true
                flushPendingIceCandidates()
                createAnswer()
            }
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }
    
    fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, desc)
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
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
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
            // Queue candidate — will be flushed after remote description is set
            android.util.Log.d("WebRtcClient", "Queueing ICE candidate (remote desc not set yet)")
            pendingIceCandidates.add(iceCandidate)
        }
    }

    private fun flushPendingIceCandidates() {
        if (pendingIceCandidates.isNotEmpty()) {
            android.util.Log.d("WebRtcClient", "Flushing ${pendingIceCandidates.size} pending ICE candidates")
            pendingIceCandidates.forEach { candidate ->
                peerConnection?.addIceCandidate(candidate)
            }
            pendingIceCandidates.clear()
        }
    }

    /**
     * Restart ICE gathering to recover from failed/disconnected connections.
     * Called by ViewModel when connection state is DISCONNECTED.
     */
    fun restartIce() {
        android.util.Log.d("WebRtcClient", "Restarting ICE...")
        hasRemoteDescription = false
        pendingIceCandidates.clear()
        peerConnection?.restartIce()
    }

    fun closePeerConnection() {
        try {
            android.util.Log.d("WebRtcClient", "closePeerConnection: Disposing...")
            remoteVideoTrack?.let { track ->
                track.setEnabled(false)
                // Remove sink from persistent renderer before track disposal
                remoteRenderer?.let { track.removeSink(it) }
            }
            
            peerConnection?.dispose()
            peerConnection = null
            remoteVideoTrack = null
            // CRITICAL FIX: DO NOT set remoteRenderer = null here. 
            // It is provided once by the UI and must survive match transitions.
            
            hasRemoteDescription = false
            pendingIceCandidates.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearRemoteVideoTrack(renderer: SurfaceViewRenderer?) {
        try {
            val r = renderer ?: remoteRenderer
            android.util.Log.d("WebRtcClient", "clearRemoteVideoTrack: renderer=$r")
            
            r?.let { 
                remoteVideoTrack?.removeSink(it)
                // PRO-TIP: Force the renderer to clear its buffer by posting a null frame or just clearing it
                // Since SurfaceViewRenderer doesn't have a direct 'clear' we rely on track removal 
                // and UI placeholder logic.
            }
            onRemoteVideoTrackReceived = null 
        } catch (e: Exception) {
            android.util.Log.e("WebRtcClient", "Error in clearRemoteVideoTrack", e)
        }
    }

    fun stopCamera(surface: SurfaceViewRenderer? = null) {
        try {
            android.util.Log.d("WebRtcClient", "stopCamera: surface=$surface")
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
            android.util.Log.e("WebRtcClient", "Error in stopCamera", e)
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
            
            eglBase.release() // CRITICAL: Release EGL context to prevent native memory leak
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

    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
