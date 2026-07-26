package com.interceptx.ui.screens.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.ui.components.GlowCard
import com.interceptx.ui.components.SectionHeader
import com.interceptx.ui.components.StatusBadge
import com.interceptx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

private val DEFAULT_RAW_REQUEST = """
GET /api/status HTTP/1.1
Host: example.com
User-Agent: InterceptX/1.0
Accept: */*
Connection: close

""".trimIndent()

/**
 * Low-level raw HTTP packet builder: the user types the exact bytes of the request
 * (request line + headers + blank line + optional body) and InterceptX sends it
 * verbatim over a raw socket — useful for crafting malformed/edge-case requests
 * that a structured form (like Repeater) would normalize away.
 */
@Composable
fun ComposerScreen() {
    var host by remember { mutableStateOf("example.com") }
    var port by remember { mutableStateOf("443") }
    var useTls by remember { mutableStateOf(true) }
    var rawRequest by remember { mutableStateOf(DEFAULT_RAW_REQUEST) }
    var rawResponse by remember { mutableStateOf<String?>(null) }
    var statusCode by remember { mutableStateOf<Int?>(null) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScopeCompat()

    Column(Modifier.fillMaxSize().background(VoidBlack).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Composer", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text(
            "Raw HTTP packet builder — bytes are sent exactly as written.",
            color = TextSecondary, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        GlowCard(Modifier.fillMaxWidth(), accent = ElectricCyan) {
            Row {
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.width(90.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = useTls, onCheckedChange = { useTls = it })
                Spacer(Modifier.width(8.dp))
                Text(if (useTls) "TLS (HTTPS)" else "Plaintext (HTTP)", color = TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader("Raw Request")
        OutlinedTextField(
            value = rawRequest,
            onValueChange = { rawRequest = it },
            modifier = Modifier.fillMaxWidth().height(220.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                sending = true
                scope.launchIo {
                    val result = sendRawRequest(host, port.toIntOrNull() ?: if (useTls) 443 else 80, useTls, rawRequest)
                    rawResponse = result.second
                    statusCode = result.first
                    sending = false
                }
            },
            enabled = !sending,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = VoidBlack)
        ) {
            Text(if (sending) "Sending…" else "Send Raw Packet", fontWeight = FontWeight.Bold)
        }

        rawResponse?.let { resp ->
            Spacer(Modifier.height(16.dp))
            SectionHeader("Raw Response")
            GlowCard(Modifier.fillMaxWidth(), accent = WarningCrimson) {
                StatusBadge(statusCode)
                Spacer(Modifier.height(8.dp))
                Text(resp, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun sendRawRequest(host: String, port: Int, useTls: Boolean, raw: String): Pair<Int?, String> = try {
    val socket: Socket = if (useTls) SSLSocketFactory.getDefault().createSocket(host, port) else Socket(host, port)
    socket.soTimeout = 15_000
    socket.getOutputStream().apply {
        write(raw.replace("\n", "\r\n").toByteArray(Charsets.UTF_8))
        flush()
    }
    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    val text = reader.readText()
    socket.close()
    val status = Regex("HTTP/\\d\\.\\d (\\d{3})").find(text)?.groupValues?.get(1)?.toIntOrNull()
    status to text
} catch (e: Exception) {
    null to "Failed to send raw packet: ${e.message}"
}

// --- tiny local coroutine helper so this file has no ViewModel dependency ---
private class ComposerScope(private val scope: kotlinx.coroutines.CoroutineScope) {
    fun launchIo(block: suspend () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { block() }
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat(): ComposerScope {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return remember { ComposerScope(scope) }
}
