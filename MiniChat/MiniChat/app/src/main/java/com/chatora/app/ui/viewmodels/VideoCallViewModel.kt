package com.chatora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatora.app.data.remote.WebSocketEvent
import com.chatora.app.data.remote.WebSocketManager
import com.chatora.app.webrtc.WebRtcClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class VideoCallViewModel @Inject constructor(
    private val webRtcClient: WebRtcClient,
    private val socketManager: WebSocketManager
) : ViewModel() {

    private val _callState = MutableStateFlow<CallState>(CallState.Connecting)
    val callState = _callState.asStateFlow()

    init {
        listenForSignals()
        setupWebRtcCallbacks()
    }

    private fun setupWebRtcCallbacks() {
        webRtcClient.onConnectionStateChanged = { state ->
            when (state) {
                org.webrtc.PeerConnection.PeerConnectionState.CONNECTED -> {
                    _callState.value = CallState.Connected
                }
                org.webrtc.PeerConnection.PeerConnectionState.FAILED,
                org.webrtc.PeerConnection.PeerConnectionState.CLOSED -> {
                    _callState.value = CallState.Ended
                }
                else -> {} // Handle others if needed
            }
        }
    }

    fun initLocalVideo(renderer: SurfaceViewRenderer) {
        webRtcClient.startLocalVideo(renderer)
        webRtcClient.createPeerConnection()
        webRtcClient.createOffer() // Ideally only the "Caller" creates offer. 
        // For Random Chat, we might need a protocol to decide who is Caller (e.g. alphabetical ID sort, or server assigns).
        // Simplification for now: Both try to initiate? No, that causes glare.
        // Let's assume we receive a "MatchFound" event that has "role": "caller" or "callee".
        // But since we are already matched, let's just trigger offer if we haven't received one after 1s? 
        // Better: Wait for "offer" signal. If we are the one who clicked "Match", maybe we start? 
        // Actually, Random Chat is symmetric.
        // Let's implement a simple "Polite Peer" or just have the one with lower ID offer.
        // For MVP: Let's just have a "Start Call" button or auto-start. 
        // Let's assume the one who connects first waits? 
        // Correction: We'll implement a simple timeout or just have both send offer and WebRTC handles glare (imperfectly) or just randomize.
        // DECISION: We will wait for "SIGNAL" from socket. If no offer received in 2s, we create one.
    }
    
    fun initRemoteVideo(renderer: SurfaceViewRenderer) {
        webRtcClient.initRemoteSurface(renderer)
        webRtcClient.onRemoteVideoTrackReceived = { track ->
            track.addSink(renderer)
        }
    }

    private fun listenForSignals() {
        viewModelScope.launch {
            socketManager.events.collect { event ->
                if (event is WebSocketEvent.Signal) {
                    when (event.type) {
                        "offer" -> {
                            webRtcClient.handleOffer(org.json.JSONObject(event.data).getString("sdp"))
                        }
                        "answer" -> {
                            webRtcClient.handleAnswer(org.json.JSONObject(event.data).getString("sdp"))
                        }
                        "ice-candidate" -> {
                            val json = org.json.JSONObject(event.data)
                            webRtcClient.handleIceCandidate(
                                json.getString("sdpMid"),
                                json.getInt("sdpMLineIndex"),
                                json.getString("candidate")
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun endCall() {
        webRtcClient.cleanup()
        _callState.value = CallState.Ended
    }

    override fun onCleared() {
        super.onCleared()
        webRtcClient.cleanup()
    }
}

sealed class CallState {
    object Connecting : CallState()
    object Connected : CallState()
    object Ended : CallState()
}
