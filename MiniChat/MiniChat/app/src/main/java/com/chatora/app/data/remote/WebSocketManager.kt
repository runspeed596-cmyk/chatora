package com.chatora.app.data.remote

import android.util.Log
import com.chatora.app.data.remote.ApiConstants.WS_URL
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade WebSocket manager with:
 * - Exponential backoff auto-reconnect (1s → 2s → 4s → ... → 30s max)
 * - STOMP heartbeat handling (client and server)
 * - Signal queueing when STOMP is not connected
 * - Connection state tracking
 * - Graceful disconnect vs error disconnect differentiation
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient
) : WebSocketListener() {

    companion object {
        private const val TAG = "WebSocket"
        private const val MAX_RECONNECT_ATTEMPTS = 15
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30000L
        private const val HEARTBEAT_INTERVAL_MS = 10000L
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<WebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()
    private var authToken: String? = null
    private var isStompConnected = false

    // Connection state observable
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    // QUEUE: Store messages if socket is not ready
    private val pendingMessages = java.util.concurrent.CopyOnWriteArrayList<String>()

    // Reconnection state
    private var reconnectAttempts = 0
    private var isUserInitiatedDisconnect = false
    private var reconnectJob: Job? = null
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Heartbeat
    private var heartbeatJob: Job? = null
    private var serverHeartbeatIntervalMs: Long = HEARTBEAT_INTERVAL_MS

    fun connect(token: String) {
        if (webSocket != null && _connectionState.value != ConnectionState.DISCONNECTED) {
            Log.d(TAG, "Already connected or connecting, skipping")
            return
        }
        this.authToken = token
        isUserInitiatedDisconnect = false
        reconnectAttempts = 0
        _connectionState.value = ConnectionState.CONNECTING
        doConnect()
    }

    private fun doConnect() {
        val token = authToken ?: return
        Log.d(TAG, "Connecting to $WS_URL...")
        val request = Request.Builder().url(WS_URL).build()
        webSocket = client.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "OnOpen: WebSocket connected")
        reconnectAttempts = 0 // Reset backoff on successful connection

        // Send STOMP CONNECT frame with Auth header
        val connectFrame = buildString {
            append("CONNECT\n")
            append("accept-version:1.1,1.0\n")
            append("heart-beat:${HEARTBEAT_INTERVAL_MS},${HEARTBEAT_INTERVAL_MS}\n")
            authToken?.let { append("Authorization:Bearer $it\n") }
            append("\n\u0000")
        }
        webSocket.send(connectFrame)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // STOMP heartbeat (server sends a single newline as heartbeat)
        if (text == "\n" || text == "\r\n") {
            Log.v(TAG, "Heartbeat received from server")
            return
        }

        Log.d(TAG, "OnMessage: ${text.take(200)}")

        if (text.startsWith("CONNECTED")) {
            isStompConnected = true
            _connectionState.value = ConnectionState.CONNECTED

            // Parse server heartbeat interval from CONNECTED frame
            parseServerHeartbeat(text)

            // Subscribe to private queues
            subscribe("/user/queue/match")
            subscribe("/user/queue/call")
            _events.tryEmit(WebSocketEvent.Connected("connected"))

            // Start heartbeat timer
            startHeartbeat()

            // FLUSH QUEUE
            if (pendingMessages.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingMessages.size} pending messages")
                pendingMessages.forEach { msg ->
                    webSocket.send(msg)
                }
                pendingMessages.clear()
            }
        } else if (text.startsWith("MESSAGE")) {
            handleMessage(text)
        } else if (text.startsWith("ERROR")) {
            Log.e(TAG, "STOMP ERROR: $text")
        }
    }

    /**
     * Parse the server heartbeat interval from the CONNECTED frame.
     * Format: heart-beat:sx,sy where sx = server send interval, sy = server receive interval.
     */
    private fun parseServerHeartbeat(connectedFrame: String) {
        try {
            val lines = connectedFrame.lines()
            for (line in lines) {
                if (line.startsWith("heart-beat:")) {
                    val parts = line.removePrefix("heart-beat:").split(",")
                    if (parts.size >= 2) {
                        val serverSend = parts[0].trim().toLongOrNull() ?: 0L
                        if (serverSend > 0) {
                            serverHeartbeatIntervalMs = serverSend
                            Log.d(TAG, "Server heartbeat interval: ${serverHeartbeatIntervalMs}ms")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse server heartbeat", e)
        }
    }

    /**
     * Start sending periodic heartbeat frames to keep the STOMP connection alive.
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = reconnectScope.launch {
            while (isActive && isStompConnected) {
                delay(HEARTBEAT_INTERVAL_MS)
                try {
                    val sent = webSocket?.send("\n") ?: false
                    if (!sent) {
                        Log.w(TAG, "Heartbeat send failed — connection may be dead")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat send error", e)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun handleMessage(text: String) {
        try {
            val headerEnd = text.indexOf("\n\n")
            if (headerEnd == -1) return

            val headers = text.substring(0, headerEnd).lines().associate { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
            }

            val destination = headers["destination"] ?: ""
            Log.d(TAG, "Message Destination: $destination")

            val bodyStartIndex = headerEnd + 2
            val jsonBody = text.substring(bodyStartIndex).trim().replace("\u0000", "")

            if (jsonBody.isNotEmpty()) {
                val json = JSONObject(jsonBody)

                if (destination.contains("/queue/match")) {
                    _events.tryEmit(WebSocketEvent.MatchFound(json.toString()))
                } else if (destination.contains("/queue/call") || destination.contains("/topic/call/")) {
                    val type = json.optString("type")
                    val data = json.optString("data", json.toString())
                    _events.tryEmit(WebSocketEvent.Signal(type, data))
                } else if (destination.contains("/topic/chat/")) {
                    Log.d(TAG, "Chat message received: $jsonBody")
                    val sender = json.optString("sender")
                    val message = json.optString("message")
                    val mediaUrl = json.optString("mediaUrl", null)
                    val mediaType = json.optString("mediaType", null)
                    _events.tryEmit(WebSocketEvent.ChatMessage(sender, message, mediaUrl, mediaType))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
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
        Log.d(TAG, "Sending Match Request: $sendFrame")

        if (!isStompConnected) {
            Log.w(TAG, "STOMP not connected (socket=${webSocket != null}), queuing request")
            pendingMessages.add(sendFrame)
            if (webSocket == null) {
                authToken?.let { connect(it) }
            }
            return
        }

        val result = webSocket?.send(sendFrame) ?: false
        if (!result) {
            Log.w(TAG, "Send failed, queuing request")
            pendingMessages.add(sendFrame)
        }
        Log.d(TAG, "Match Request Sent Result: $result")
    }

    fun sendLeaveRequest(myCountry: String, targetGender: String) {
        val json = JSONObject()
            .put("myCountry", myCountry)
            .put("targetGender", targetGender.uppercase())
        val sendFrame = "SEND\ndestination:/app/match/leave\ncontent-type:application/json\n\n$json\u0000"
        webSocket?.send(sendFrame)
    }

    /**
     * Send a WebRTC signal. Queues the signal if STOMP is not connected,
     * ensuring no signals are silently dropped.
     */
    fun sendSignal(matchId: String, type: String, data: String) {
        val json = JSONObject().put("type", type).put("data", data)
        val sendFrame = "SEND\ndestination:/app/signal/$matchId\ncontent-type:application/json\n\n$json\u0000"
        Log.d(TAG, "Sending Signal [$type] to match $matchId")

        if (!isStompConnected) {
            Log.w(TAG, "Signal queued (STOMP not connected): type=$type")
            pendingMessages.add(sendFrame)
            return
        }

        val result = webSocket?.send(sendFrame) ?: false
        if (!result) {
            Log.w(TAG, "Signal send failed, queuing: type=$type")
            pendingMessages.add(sendFrame)
        }
    }

    fun sendMessage(matchId: String, message: String, mediaUrl: String? = null, mediaType: String? = null) {
        try {
            val json = JSONObject().put("message", message)
            mediaUrl?.let { json.put("mediaUrl", it) }
            mediaType?.let { json.put("mediaType", it) }

            val sendFrame = "SEND\ndestination:/app/chat/$matchId\ncontent-type:application/json\n\n$json\u0000"
            Log.d(TAG, "Sending Message: $sendFrame")
            val success = webSocket?.send(sendFrame) ?: false
            if (!success) {
                Log.e(TAG, "Failed to send message frame")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building send frame", e)
        }
    }

    /**
     * User-initiated disconnect. Does NOT trigger auto-reconnect.
     */
    fun disconnect() {
        isUserInitiatedDisconnect = true
        reconnectJob?.cancel()
        stopHeartbeat()
        webSocket?.close(1000, "User disconnect")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "Closed: code=$code reason=$reason")
        cleanupConnectionState()

        if (!isUserInitiatedDisconnect && code != 1000) {
            scheduleReconnect()
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Connection failure: ${t.message}", t)
        cleanupConnectionState()
        _events.tryEmit(WebSocketEvent.Error(t.message ?: "Unknown Error"))

        if (!isUserInitiatedDisconnect) {
            scheduleReconnect()
        }
    }

    private fun cleanupConnectionState() {
        this.webSocket = null
        this.isStompConnected = false
        stopHeartbeat()

        if (isUserInitiatedDisconnect) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Schedule a reconnection attempt with exponential backoff.
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached. Giving up.")
            _connectionState.value = ConnectionState.DISCONNECTED
            _events.tryEmit(WebSocketEvent.Error("Connection lost. Please restart the app."))
            return
        }

        _connectionState.value = ConnectionState.RECONNECTING
        val backoffMs = minOf(INITIAL_BACKOFF_MS * (1L shl reconnectAttempts), MAX_BACKOFF_MS)
        reconnectAttempts++

        Log.i(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${backoffMs}ms")

        reconnectJob?.cancel()
        reconnectJob = reconnectScope.launch {
            delay(backoffMs)
            Log.i(TAG, "Reconnect attempt $reconnectAttempts starting...")
            doConnect()
        }
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
