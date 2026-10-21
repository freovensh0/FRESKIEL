package dev.scdeal.freskiel.corenet

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WebSocketTransportIntegrationTest {
    @Test
    fun `connect streams inbound and outbound websocket messages`() =
        runBlocking {
            val server = MockWebServer()
            val serverReceivedMessages = Channel<String>(Channel.UNLIMITED)

            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            webSocket.send("server-hello")
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            serverReceivedMessages.trySend(text)
                        }
                    },
                ),
            )
            server.start()

            val transport = WebSocketTransport(client = OkHttpClient())
            val (inboundMessages, collectorJob) = collectInbound(transport)

            try {
                transport.connect(targetFor(server))

                awaitState(transport, ConnectionState.CONNECTED)
                assertEquals("server-hello", withTimeout(TIMEOUT_MS) { inboundMessages.receive() })

                transport.send("client-hello")
                assertEquals("client-hello", withTimeout(TIMEOUT_MS) { serverReceivedMessages.receive() })

                transport.disconnect()
                assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
            } finally {
                collectorJob.cancel()
                transport.disconnect()
                closeServer(server)
            }
        }

    @Test
    fun `send fails when outbound queue is full`() =
        runBlocking {
            val transport = WebSocketTransport(client = OkHttpClient())

            repeat(256) { index ->
                transport.send("queued-$index")
            }

            assertFailsWith<IllegalStateException> {
                transport.send("overflow")
            }

            transport.disconnect()
        }

    @Test
    fun `disconnect clears queued outbound messages`() {
        runBlocking {
            val server = MockWebServer()
            val receivedMessages = Channel<String>(Channel.UNLIMITED)

            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            receivedMessages.trySend(text)
                        }
                    },
                ),
            )
            server.start()

            val transport = WebSocketTransport(client = OkHttpClient())

            try {
                transport.send("stale-before-connect")
                transport.disconnect()
                transport.connect(targetFor(server))

                awaitState(transport, ConnectionState.CONNECTED)
                assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
                    withTimeout(250) { receivedMessages.receive() }
                }
            } finally {
                transport.disconnect()
                closeServer(server)
            }
        }
    }

    @Test
    fun `reconnect flushes queued outbound message after socket drop`() =
        runBlocking {
            val server = MockWebServer()
            val firstSocket = CompletableDeferred<WebSocket>()
            val secondConnectionReceived = Channel<String>(Channel.UNLIMITED)

            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            firstSocket.complete(webSocket)
                            webSocket.send("server-first")
                        }
                    },
                ),
            )
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            webSocket.send("server-second")
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            secondConnectionReceived.trySend(text)
                        }
                    },
                ),
            )
            server.start()

            val transport = WebSocketTransport(client = OkHttpClient())
            val (inboundMessages, collectorJob) = collectInbound(transport)

            try {
                transport.connect(targetFor(server, reconnectInitialDelayMs = 500))

                awaitState(transport, ConnectionState.CONNECTED)
                assertEquals("server-first", withTimeout(TIMEOUT_MS) { inboundMessages.receive() })

                firstSocket.await().close(1012, "bridge restart")
                awaitState(transport, ConnectionState.RECONNECTING)

                transport.send("queued-during-reconnect")

                assertEquals("server-second", withTimeout(TIMEOUT_MS) { inboundMessages.receive() })
                awaitState(transport, ConnectionState.CONNECTED)
                assertEquals("queued-during-reconnect", withTimeout(TIMEOUT_MS) { secondConnectionReceived.receive() })
            } finally {
                collectorJob.cancel()
                transport.disconnect()
                closeServer(server)
            }
        }

    private fun CoroutineScope.collectInbound(transport: WebSocketTransport): Pair<Channel<String>, Job> {
        val messages = Channel<String>(Channel.UNLIMITED)
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                transport.inboundMessages.collect { messages.send(it) }
            }
        return messages to job
    }

    /** The mock server waits for the socket to go away; the client may still be closing it. */
    private suspend fun closeServer(server: MockWebServer) {
        delay(CLOSE_GRACE_MS)
        runCatching { server.close() }
    }

    private fun targetFor(
        server: MockWebServer,
        reconnectInitialDelayMs: Long = 25,
    ): WebSocketTarget =
        WebSocketTarget(
            url = server.url("/ws").toString(),
            reconnectInitialDelayMs = reconnectInitialDelayMs,
            reconnectMaxDelayMs = reconnectInitialDelayMs * 2,
        )

    private suspend fun awaitState(
        transport: WebSocketTransport,
        expected: ConnectionState,
    ) {
        withTimeout(TIMEOUT_MS) {
            transport.connectionState.first { state -> state == expected }
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
        private const val CLOSE_GRACE_MS = 200L
    }
}
