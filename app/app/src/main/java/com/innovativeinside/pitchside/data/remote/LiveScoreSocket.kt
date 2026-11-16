package com.innovativeinside.pitchside.data.remote

import com.innovativeinside.pitchside.data.remote.ApiFootballParse.objects
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * apifootball.com livescore push (wss://wss.apifootball.com/livescore?APIkey=...&timezone=+00:00). Each message is a JSON
 * array of changed events in the get_events shape; they are handed to [ApiFootballComApi.applyLivePush] and the changed
 * match ids are emitted on [updates] so live screens refresh at once instead of waiting for their 30 s poll.
 *
 * Runs only between [start] and [stop] (the activity's onStart/onStop), reconnects with exponential backoff (2 s up to
 * 60 s), and the screens' polling stays in place as the fallback when the socket is down.
 */
class LiveScoreSocket(
    private val apiKey: String,
    private val api: ApiFootballComApi,
    client: OkHttpClient = HttpFootballApi.defaultClient(),
    private val url: String = "wss://wss.apifootball.com/livescore",
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val client = client.newBuilder().pingInterval(30, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).callTimeout(0, TimeUnit.MILLISECONDS).build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _updates = MutableSharedFlow<Set<String>>(extraBufferCapacity = 16)
    /** Ids of matches that just changed. */
    val updates: SharedFlow<Set<String>> = _updates.asSharedFlow()

    @Volatile private var running = false
    /** Set when the server says the plan has no WebSocket access; then polling alone is used for this process. */
    @Volatile var planGated = false
        private set
    @Volatile private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var attempt = 0

    @Synchronized
    fun start() {
        if (running || apiKey.isBlank() || planGated) return
        running = true
        attempt = 0
        connect()
    }

    @Synchronized
    fun stop() {
        running = false
        reconnectJob?.cancel()
        socket?.close(1000, "background")
        socket = null
    }

    private fun connect() {
        // "+" must be escaped in the query; timezone matches the REST calls (UTC).
        val req = Request.Builder().url("$url?APIkey=$apiKey&timezone=%2B00:00").build()
        socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { attempt = 0 }
            override fun onMessage(webSocket: WebSocket, text: String) { handle(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { scheduleReconnect() }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { scheduleReconnect() }
        })
    }

    internal fun handle(text: String) {
        val trimmed = text.trimStart()
        val parsed = if (trimmed.startsWith("[") || trimmed.startsWith("{")) runCatching { json.parseToJsonElement(trimmed) }.getOrNull() else null
        if (parsed == null) {
            // Plain-text notice, e.g. "Your plan is expired or you don't have access to websocket!": stop retrying.
            val t = text.lowercase()
            if ("access" in t || "expired" in t || "authentification" in t) {
                planGated = true
                stop()
            }
            return
        }
        val rows: List<JsonObject> = parsed.let { el ->
            (el as? JsonObject)?.let { listOf(it) }?.filter { it["match_id"] != null } ?: el.objects()
        }
        if (rows.isEmpty()) return
        val changed = api.applyLivePush(rows)
        if (changed.isNotEmpty()) _updates.tryEmit(changed)
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (!running || planGated) return
        socket = null
        reconnectJob?.cancel()
        val wait = (2_000L shl attempt.coerceAtMost(5)).coerceAtMost(60_000L)
        attempt++
        reconnectJob = scope.launch {
            delay(wait)
            synchronized(this@LiveScoreSocket) { if (running && socket == null) connect() }
        }
    }
}
