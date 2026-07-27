package com.interceptx.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.ui.components.*
import com.interceptx.ui.nav.Screen
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigate: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().background(VoidBlack).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DashboardHeader(state.proxyRunning) { viewModel.toggleProxy(8080) } }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total Requests", state.totalRequests.toString(), ElectricCyan, Modifier.weight(1f))
                StatCard("Intercepted", state.interceptedRequests.toString(), WarningCrimson, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Active Conns", state.activeConnections.toString(), NeonGreen, Modifier.weight(1f))
                StatCard("Bandwidth", formatBytes(state.bandwidthBytes), AmberAlert, Modifier.weight(1f))
            }
        }

        item {
            SectionHeader("Quick Actions")
            QuickActionsRow(onNavigate)
        }

        item { SectionHeader("Recent Traffic") }
        if (state.recentTraffic.isEmpty()) {
            item { EmptyState("No traffic yet — start the proxy and route device traffic through it.") }
        } else {
            items(state.recentTraffic) { tx ->
                TrafficRow(method = tx.method, url = tx.url, status = tx.responseStatusCode, timeMs = tx.responseTimeMs)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DashboardHeader(running: Boolean, onToggle: () -> Unit) {
    GlowCard(accent = if (running) NeonGreen else WarningCrimson) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Local Proxy", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp)
                            .background(if (running) NeonGreen else WarningCrimson, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (running) "RUNNING · 127.0.0.1:8080" else "STOPPED",
                        color = if (running) NeonGreen else TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) WarningCrimson else NeonGreen,
                    contentColor = VoidBlack
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (running) "STOP" else "START", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickActionsRow(onNavigate: (String) -> Unit) {
    val actions = listOf(
        Triple("Intercept", Icons.Filled.Security, Screen.Intercept.route),
        Triple("History", Icons.Filled.History, Screen.History.route),
        Triple("Repeater", Icons.Filled.Repeat, Screen.Repeater.route),
        Triple("Certificates", Icons.Filled.VerifiedUser, Screen.Certificates.route)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.forEach { (label, icon, route) ->
            GlowCard(
                modifier = Modifier.weight(1f).clickableAction { onNavigate(route) },
                accent = ElectricCyanDim
            ) {
                Icon(icon, contentDescription = label, tint = ElectricCyan)
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun TrafficRow(method: String, url: String, status: Int?, timeMs: Long?) {
    GlowCard(Modifier.fillMaxWidth(), accent = StrokeSlate) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MethodBadge(method)
            Spacer(Modifier.width(8.dp))
            Text(
                url, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            StatusBadge(status)
            Spacer(Modifier.width(8.dp))
            Text(
                timeMs?.let { "${it}ms" } ?: "…",
                color = TextDisabled,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

// Small helper to make GlowCard clickable.
private fun Modifier.clickableAction(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
