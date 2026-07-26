package com.interceptx.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.data.model.ProxySettings
import com.interceptx.ui.components.GlowCard
import com.interceptx.ui.components.SectionHeader
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val current = settings ?: ProxySettings()

    var host by remember(current) { mutableStateOf(current.proxyHost) }
    var port by remember(current) { mutableStateOf(current.proxyPort.toString()) }
    var upstreamEnabled by remember(current) { mutableStateOf(current.upstreamProxyEnabled) }
    var upstreamHost by remember(current) { mutableStateOf(current.upstreamHost) }
    var upstreamPort by remember(current) { mutableStateOf(current.upstreamPort.toString()) }
    var tlsBypass by remember(current) { mutableStateOf(current.tlsBypassEnabled) }
    var interceptReq by remember(current) { mutableStateOf(current.interceptRequests) }
    var interceptResp by remember(current) { mutableStateOf(current.interceptResponses) }

    Column(Modifier.fillMaxSize().background(VoidBlack).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        SectionHeader("Proxy Configuration")
        GlowCard(Modifier.fillMaxWidth(), accent = ElectricCyan) {
            Row {
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Proxy Host") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.width(100.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Upstream Proxy")
        GlowCard(Modifier.fillMaxWidth(), accent = NeonGreen) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = upstreamEnabled, onCheckedChange = { upstreamEnabled = it })
                Spacer(Modifier.width(8.dp))
                Text("Route via upstream proxy", color = TextPrimary)
            }
            if (upstreamEnabled) {
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(value = upstreamHost, onValueChange = { upstreamHost = it }, label = { Text("Upstream Host") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = upstreamPort, onValueChange = { upstreamPort = it }, label = { Text("Port") }, modifier = Modifier.width(100.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("SSL / TLS & Intercept Rules")
        GlowCard(Modifier.fillMaxWidth(), accent = WarningCrimson) {
            SettingsToggleRow("TLS bypass (decrypt HTTPS via local CA)", tlsBypass) { tlsBypass = it }
            SettingsToggleRow("Intercept requests", interceptReq) { interceptReq = it }
            SettingsToggleRow("Intercept responses", interceptResp) { interceptResp = it }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.update(
                    current.copy(
                        proxyHost = host,
                        proxyPort = port.toIntOrNull() ?: 8080,
                        upstreamProxyEnabled = upstreamEnabled,
                        upstreamHost = upstreamHost,
                        upstreamPort = upstreamPort.toIntOrNull() ?: 0,
                        tlsBypassEnabled = tlsBypass,
                        interceptRequests = interceptReq,
                        interceptResponses = interceptResp,
                        interceptEnabled = interceptReq || interceptResp
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = VoidBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
