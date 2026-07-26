package com.interceptx.proxy

import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.model.TransactionState
import com.interceptx.data.repository.InterceptXRepository
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.SSLSocket

/**
 * Local intercepting HTTP/HTTPS proxy.
 *
 * - Plain HTTP requests are parsed, optionally paused for interception, then
 *   forwarded to the origin server and the response streamed back.
 * - HTTPS traffic arrives as a CONNECT tunnel; InterceptX terminates the client's
 *   TLS connection itself (presenting a per-host leaf cert signed by the local
 *   root CA — see [CertificateAuthority]), decrypts, applies the same intercept
 *   pipeline, then opens its own outbound TLS connection to the real origin.
 *   This requires the InterceptX root CA to be trusted on the device (Certificates screen).
 *
 * The engine is intentionally dependency-light (raw sockets, no Netty) so it has
 * no third-party runtime beyond BouncyCastle for certificate generation.
 */
class ProxyEngine(
    private val repository: InterceptXRepository,
    private val certificateAuthority: CertificateAuthority,
    private val interceptQueue: InterceptQueue,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    @Volatile var isRunning = false
        private set

    var interceptEnabled = false
    var projectId: Long = 1
    private var nextTransactionId = 1L

    fun start(port: Int) {
        if (isRunning) return
        isRunning = true
        scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress("127.0.0.1", port))
                serverSocket = server
                while (isRunning) {
                    val client = server.accept()
                    launch(Dispatchers.IO) { handleClient(client) }
                }
            } catch (e: IOException) {
                // Socket closed on stop() — expected.
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 20_000
            val input = BufferedInputStream(client.getInputStream())
            val requestLine = readLine(input) ?: return

            if (requestLine.startsWith("CONNECT")) {
                handleConnectTunnel(client, input, requestLine)
            } else {
                handlePlainHttp(client, input, requestLine)
            }
        } catch (e: Exception) {
            // Best-effort proxy; log and drop the connection rather than crash the engine.
        } finally {
            runCatching { client.close() }
        }
    }

    // ---- Plain HTTP path ----
    private fun handlePlainHttp(client: Socket, input: InputStream, requestLine: String) {
        val (method, url) = parseRequestLine(requestLine) ?: return
        val headers = readHeaders(input)
        val body = readBody(input, headers)
        val host = headers["host"] ?: extractHostFromUrl(url) ?: return

        val decision = maybeIntercept(method, url, headers, body)
        if (decision is InterceptDecision.Drop) return

        val finalRequest = (decision as? InterceptDecision.Forward)?.edited
        val outMethod = finalRequest?.method ?: method
        val outUrl = finalRequest?.url ?: url
        val outHeaders = finalRequest?.headers ?: headers
        val outBody = finalRequest?.body ?: body

        val start = System.currentTimeMillis()
        val upstream = Socket(host, 80)
        upstream.getOutputStream().apply {
            write(buildRawRequest(outMethod, outUrl, outHeaders, outBody))
            flush()
        }

        val responseBytes = upstream.getInputStream().readBytes()
        client.getOutputStream().write(responseBytes)
        upstream.close()

        recordTransaction(outMethod, "http://$host$outUrl", host, "http", outHeaders, outBody, responseBytes, start)
    }

    // ---- HTTPS via CONNECT + local TLS termination ----
    private fun handleConnectTunnel(client: Socket, input: InputStream, requestLine: String) {
        val target = requestLine.substringAfter(" ").substringBefore(" ")
        val host = target.substringBefore(":")
        // Drain remaining CONNECT headers.
        readHeaders(input)

        client.getOutputStream().apply {
            write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
            flush()
        }

        val sslContext = certificateAuthority.sslContextFor(host)
        val clientTls = sslContext.socketFactory.createSocket(
            client, host, client.port, true
        ) as SSLSocket
        clientTls.useClientMode = false
        clientTls.startHandshake()

        val tlsInput = BufferedInputStream(clientTls.getInputStream())
        val requestLine2 = readLine(tlsInput) ?: return
        val (method, url) = parseRequestLine(requestLine2) ?: return
        val headers = readHeaders(tlsInput)
        val body = readBody(tlsInput, headers)

        val decision = maybeIntercept(method, url, headers, body)
        if (decision is InterceptDecision.Drop) return
        val finalRequest = (decision as? InterceptDecision.Forward)?.edited
        val outMethod = finalRequest?.method ?: method
        val outUrl = finalRequest?.url ?: url
        val outHeaders = finalRequest?.headers ?: headers
        val outBody = finalRequest?.body ?: body

        val start = System.currentTimeMillis()
        val upstreamSocket = javax.net.ssl.SSLSocketFactory.getDefault().createSocket(host, 443) as SSLSocket
        upstreamSocket.getOutputStream().apply {
            write(buildRawRequest(outMethod, outUrl, outHeaders, outBody))
            flush()
        }
        val responseBytes = upstreamSocket.getInputStream().readBytes()
        clientTls.getOutputStream().write(responseBytes)
        upstreamSocket.close()

        recordTransaction(outMethod, "https://$host$outUrl", host, "https", outHeaders, outBody, responseBytes, start)
    }

    private fun maybeIntercept(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): InterceptDecision {
        if (!interceptEnabled) return InterceptDecision.Forward(
            PendingRequest(0, method, url, headers.toMutableMap(), body)
        )
        val id = nextTransactionId++
        scope.launch {
            repository.insertTransaction(
                HttpTransaction(
                    projectId = projectId, method = method, url = url,
                    host = headers["host"] ?: "", scheme = if (url.startsWith("https")) "https" else "http",
                    requestHeaders = JSONObject(headers as Map<*, *>).toString(),
                    requestBody = body, responseStatusCode = null, responseHeaders = null,
                    responseBody = null, responseTimeMs = null, sizeBytes = 0,
                    timestamp = System.currentTimeMillis(), state = TransactionState.INTERCEPTED, inScope = true
                )
            )
        }
        val pending = PendingRequest(id, method, url, headers.toMutableMap(), body)
        return interceptQueue.await(pending)
    }

    private fun recordTransaction(
        method: String, fullUrl: String, host: String, scheme: String,
        headers: Map<String, String>, body: String?, responseRaw: ByteArray, startTime: Long
    ) {
        val elapsed = System.currentTimeMillis() - startTime
        val (statusCode, respHeaders, respBody) = parseResponse(responseRaw)
        scope.launch {
            repository.insertTransaction(
                HttpTransaction(
                    projectId = projectId, method = method, url = fullUrl, host = host, scheme = scheme,
                    requestHeaders = JSONObject(headers as Map<*, *>).toString(), requestBody = body,
                    responseStatusCode = statusCode,
                    responseHeaders = JSONObject(respHeaders as Map<*, *>).toString(),
                    responseBody = respBody, responseTimeMs = elapsed, sizeBytes = responseRaw.size.toLong(),
                    timestamp = System.currentTimeMillis(), state = TransactionState.COMPLETED, inScope = true
                )
            )
        }
    }

    // ---- Raw HTTP parsing helpers ----
    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (prev == '\r'.code && b == '\n'.code) { sb.setLength(sb.length - 1); return sb.toString() }
            sb.append(b.toChar())
            prev = b
        }
    }

    private fun parseRequestLine(line: String): Pair<String, String>? {
        val parts = line.split(" ")
        if (parts.size < 2) return null
        return parts[0] to parts[1]
    }

    private fun readHeaders(input: InputStream): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val idx = line.indexOf(":")
            if (idx > 0) headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
        }
        return headers
    }

    private fun readBody(input: InputStream, headers: Map<String, String>): String? {
        val length = headers["content-length"]?.toIntOrNull() ?: return null
        if (length <= 0) return null
        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(buf, read, length - read)
            if (n == -1) break
            read += n
        }
        return String(buf, 0, read, Charsets.UTF_8)
    }

    private fun extractHostFromUrl(url: String): String? =
        Regex("https?://([^/]+)").find(url)?.groupValues?.get(1)

    private fun buildRawRequest(method: String, path: String, headers: Map<String, String>, body: String?): ByteArray {
        val sb = StringBuilder()
        sb.append("$method $path HTTP/1.1\r\n")
        headers.forEach { (k, v) -> sb.append("$k: $v\r\n") }
        if (body != null && !headers.containsKey("content-length")) {
            sb.append("Content-Length: ${body.toByteArray().size}\r\n")
        }
        sb.append("Connection: close\r\n\r\n")
        if (body != null) sb.append(body)
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun parseResponse(raw: ByteArray): Triple<Int?, Map<String, String>, String?> {
        val text = String(raw, Charsets.ISO_8859_1)
        val headerEnd = text.indexOf("\r\n\r\n")
        if (headerEnd == -1) return Triple(null, emptyMap(), null)
        val headerBlock = text.substring(0, headerEnd)
        val lines = headerBlock.split("\r\n")
        val statusCode = lines.firstOrNull()?.split(" ")?.getOrNull(1)?.toIntOrNull()
        val headers = mutableMapOf<String, String>()
        lines.drop(1).forEach {
            val idx = it.indexOf(":")
            if (idx > 0) headers[it.substring(0, idx).trim().lowercase()] = it.substring(idx + 1).trim()
        }
        val body = text.substring(minOf(headerEnd + 4, text.length))
        return Triple(statusCode, headers, body)
    }
}
