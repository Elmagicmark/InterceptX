package com.interceptx.ui.screens.repeater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.data.model.RepeaterTab
import com.interceptx.ui.components.*
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.RepeaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeaterScreen(viewModel: RepeaterViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val sendingId by viewModel.sending.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(VoidBlack)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Repeater", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            IconButton(onClick = { viewModel.newTab() }) {
                Icon(Icons.Filled.Add, contentDescription = "New tab", tint = NeonGreen)
            }
        }

        if (tabs.isEmpty()) {
            EmptyState("No repeater tabs yet. Tap + to craft a request, or send one from History.")
            return@Column
        }

        val safeIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
        ScrollableTabRow(selectedTabIndex = safeIndex, containerColor = DarkSlate, contentColor = ElectricCyan) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = index == safeIndex, onClick = { selectedIndex = index }, text = { Text(tab.label) })
            }
        }

        val currentTab = tabs[safeIndex]
        RepeaterTabEditor(
            tab = currentTab,
            isSending = sendingId == currentTab.id,
            onChange = { viewModel.updateTab(it) },
            onSend = { viewModel.sendRequest(it) },
            onDelete = { viewModel.deleteTab(it); selectedIndex = 0 }
        )
    }
}

@Composable
private fun RepeaterTabEditor(
    tab: RepeaterTab,
    isSending: Boolean,
    onChange: (RepeaterTab) -> Unit,
    onSend: (RepeaterTab) -> Unit,
    onDelete: (RepeaterTab) -> Unit
) {
    var method by remember(tab.id) { mutableStateOf(tab.method) }
    var url by remember(tab.id) { mutableStateOf(tab.url) }
    var headers by remember(tab.id) { mutableStateOf(tab.headers) }
    var body by remember(tab.id) { mutableStateOf(tab.body) }
    var bodyType by remember(tab.id) { mutableStateOf(tab.bodyType) }
    var responseView by remember(tab.id) { mutableStateOf("Pretty") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        GlowCard(Modifier.fillMaxWidth(), accent = ElectricCyan) {
            Row {
                OutlinedTextField(
                    value = method, onValueChange = { method = it },
                    modifier = Modifier.width(110.dp), label = { Text("Method") }
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    modifier = Modifier.weight(1f), label = { Text("Target URL") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("JSON", "TEXT", "FORM_DATA").forEach { type ->
                    FilterChip(selected = bodyType == type, onClick = { bodyType = type }, label = { Text(type) })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = headers, onValueChange = { headers = it }, label = { Text("Headers (JSON)") },
                modifier = Modifier.fillMaxWidth().height(110.dp), textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = body, onValueChange = { body = it }, label = { Text("Request Body") },
                modifier = Modifier.fillMaxWidth().height(140.dp), textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val updated = tab.copy(method = method, url = url, headers = headers, body = body, bodyType = bodyType)
                        onChange(updated)
                        onSend(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = VoidBlack),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = VoidBlack, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isSending) "Sending…" else "Send Request", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { onChange(tab.copy(method = method, url = url, headers = headers, body = body, bodyType = bodyType)) }) {
                    Text("Save")
                }
                IconButton(onClick = { onDelete(tab) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete tab", tint = WarningCrimson)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (tab.lastStatusCode != null) {
            SectionHeader("Response Inspector")
            GlowCard(Modifier.fillMaxWidth(), accent = colorForStatusPublic(tab.lastStatusCode)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(tab.lastStatusCode)
                    Spacer(Modifier.width(10.dp))
                    Text("${tab.lastResponseTimeMs ?: 0} ms", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Pretty", "Raw", "Headers").forEach { v ->
                        FilterChip(selected = responseView == v, onClick = { responseView = v }, label = { Text(v) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                val displayText = when (responseView) {
                    "Headers" -> tab.lastResponseHeaders ?: ""
                    "Pretty" -> prettyPrintJsonSafely(tab.lastResponseBody ?: "")
                    else -> tab.lastResponseBody ?: ""
                }
                Text(
                    displayText,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun colorForStatusPublic(code: Int?) = com.interceptx.ui.components.colorForStatus(code)

private fun prettyPrintJsonSafely(text: String): String = try {
    if (text.trim().startsWith("{")) org.json.JSONObject(text).toString(2)
    else if (text.trim().startsWith("[")) org.json.JSONArray(text).toString(2)
    else text
} catch (e: Exception) {
    text
}
