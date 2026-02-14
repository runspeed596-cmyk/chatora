package com.chatora.app.data.remote

import android.util.Log
import com.chatora.app.data.local.UserEntity
import com.chatora.app.data.remote.ApiConstants.WS_URL
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient
) : WebSocketListener() {

    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<WebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()
    private var authToken: String? = null
    private var isStompConnected = false
    
    // QUEUE: Store messages if socket is not ready
    private val pendingMessages = java.util.concurrent.CopyOnWriteArrayList<String>()

    fun connect(token: String) {
        if (webSocket != null) return // Already connecting or connected
        this.authToken = token
        val request = Request.Builder().url(WS_URL).build()
        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("WebSocket", "OnOpen: Connected")
        // Send STOMP CONNECT frame with Auth header
        val connectFrame = buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.0\n")
            append("heart-beat:10000,10000\n")
            authToken?.let { append("Authorization:Bearer $it\n") }
            append("\n\u0000")
        }
        webSocket.send(connectFrame)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WebSocket", "OnMessage: $text")
        if (text.startsWith("CONNECTED")) {
            isStompConnected = true
            // Subscribe to private queues for match notifications and signaling
            subscribe("/user/queue/match")
            subscribe("/user/queue/call")  // Per-user signaling channel (no more per-match topic subscriptions)
            _events.tryEmit(WebSocketEvent.Connected("connected"))
            
            // FLUSH QUEUE
            if (pendingMessages.isNotEmpty()) {
                Log.d("WebSocket", "Flushing ${pendingMessages.size} pending messages")
                pendingMessages.forEach { msg -> 
                    webSocket.send(msg)
                }
                pendingMessages.clear()
            }
        } else if (text.startsWith("MESSAGE")) {
            handleMessage(text)
        }
    }

    private fun handleMessage(text: String) {
        Log.d("WebSocket", "Handling incoming STOMP message")
        try {
            // Parse Headers
            val headerEnd = text.indexOf("\n\n")
            if (headerEnd == -1) return
            
            val headers = text.substring(0, headerEnd).lines().associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
            }
            
            val destination = headers["destination"] ?: ""
            Log.d("WebSocket", "Message Destination: $destination")

            // Parse Body
            val bodyStartIndex = headerEnd + 2
            val jsonBody = text.substring(bodyStartIndex).trim().replace("\u0000", "")
            
            if (jsonBody.isNotEmpty()) {
                val json = JSONObject(jsonBody)
                
                // Robust Destination Check
                if (destination.contains("/queue/match")) {
                    _events.tryEmit(WebSocketEvent.MatchFound(json.toString()))
                } else if (destination.contains("/queue/call") || destination.contains("/topic/call/")) {
                    val type = json.optString("type")
                    val data = json.optString("data", json.toString())
                    _events.tryEmit(WebSocketEvent.Signal(type, data))
                } else if (destination.contains("/topic/chat/")) {
                    Log.d("WebSocket", "Chat message received: $jsonBody")
                    val sender = json.optString("sender")
                    val message = json.optString("message")
                    val mediaUrl = json.optString("mediaUrl", null)
                    val mediaType = json.optString("mediaType", null)
                    _events.tryEmit(WebSocketEvent.ChatMessage(sender, message, mediaUrl, mediaType))
                }
            }
        } catch (e: Exception) {
             Log.e("WebSocket", "Error parsing message", e)
        }
    }

    fun subscribe(topic: String, id: String = UUID.randomUUID().toString()): String {
        val subFrame = "SUBSCRIBE\nid:$id\ndestination:$topic\n\n\u0000"
        webSocket?.send(subFrame)
        return id
    }

    fun unsubscribe(id: String) {
        val unsubFrame = "UNSUBSCRIBE\nid:$id\n\n\u0000"
        webSocket?.send(unsubFrame)
    }

    fun sendMatchRequest(myCountry: String, targetCountry: String, targetGender: String) {
        val uppercaseGender = targetGender.uppercase()
        val json = JSONObject()
            .put("myCountry", myCountry)
            .put("targetCountry", targetCountry)
            .put("targetGender", uppercaseGender)
            
        val sendFrame = "SEND\ndestination:/app/match/join\ncontent-type:application/json\n\n$json\u0000"
        Log.d("WebSocket", "Sending Match Request: $sendFrame")
        
        if (!isStompConnected) {
            Log.w("WebSocket", "STOMP not connected (socket=${webSocket != null}), queuing request")
            pendingMessages.add(sendFrame)
            if (webSocket == null) {
                authToken?.let { connect(it) }
            }
            return
        }
        
        val result = webSocket?.send(sendFrame) ?: false
        if (!result) {
             Log.w("WebSocket", "Send failed, queuing request")
             pendingMessages.add(sendFrame)
        }
        Log.d("WebSocket", "Match Request Sent Result: $result")
    }

    fun sendLeaveRequest(myCountry: String, targetGender: String) {
        val json = JSONObject()
            .put("myCountry", myCountry)
            .put("targetGender", targetGender.uppercase())
        val sendFrame = "SEND\ndestination:/app/match/leave\ncontent-type:application/json\n\n$json\u0000"
        webSocket?.send(sendFrame)
    }
    
    fun sendSignal(matchId: String, type: String, data: String) {
         val json = JSONObject().put("type", type).put("data", data)
         val sendFrame = "SEND\ndestination:/app/signal/$matchId\ncontent-type:application/json\n\n$json\u0000"
         Log.d("WebSocket", "Sending Signal [$type] to match $matchId")
         webSocket?.send(sendFrame)
    }

    fun sendMessage(matchId: String, message: String, mediaUrl: String? = null, mediaType: String? = null) {
        try {
            val json = JSONObject().put("message", message)
            mediaUrl?.let { json.put("mediaUrl", it) }
            mediaType?.let { json.put("mediaType", it) }
            
            val sendFrame = "SEND\ndestination:/app/chat/$matchId\ncontent-type:application/json\n\n$json\u0000"
            Log.d("WebSocket", "Sending Message: $sendFrame")
            val success = webSocket?.send(sendFrame) ?: false
            if (!success) {
                Log.e("WebSocket", "Failed to send message frame")
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Error building send frame", e)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
    }
    
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i("WebSocket", "Closed: $reason")
        this.webSocket = null
        this.isStompConnected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WebSocket", "Error", t)
        this.webSocket = null
        this.isStompConnected = false
        _events.tryEmit(WebSocketEvent.Error(t.message ?: "Unknown Error"))
    }
}

sealed class WebSocketEvent {
    data class Connected(val sessionId: String) : WebSocketEvent()
    data class MatchFound(val data: String) : WebSocketEvent()
    data class Signal(val type: String, val data: String) : WebSocketEvent()
    data class ChatMessage(
        val sender: String, 
        val message: String, 
        val mediaUrl: String? = null, 
        val mediaType: String? = null
    ) : WebSocketEvent()
    data class Error(val reason: String) : WebSocketEvent()
}
