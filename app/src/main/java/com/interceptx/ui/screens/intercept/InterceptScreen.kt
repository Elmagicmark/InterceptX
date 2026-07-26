package com.interceptx.ui.screens.intercept

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.data.model.HttpTransaction
import com.interceptx.ui.components.*
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.InterceptViewModel
import org.json.JSONObject

@Composable
fun InterceptScreen(viewModel: InterceptViewModel) {
    val queue by viewModel.interceptedQueue.collectAsState()
    var interceptOn by remember { mutableStateOf(viewModel.interceptOn) }
    var selected by remember { mutableStateOf<HttpTransaction?>(null) }

    Column(Modifier.fillMaxSize().background(VoidBlack).padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Intercept", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (interceptOn) "ON" else "OFF", color = if (interceptOn) NeonGreen else TextSecondary)
                Switch(
                    checked = interceptOn,
                    onCheckedChange = {
                        interceptOn = it
                        viewModel.setInterceptOn(it)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = NeonGreenDim, checkedThumbColor = NeonGreen)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.forwardAll() }) { Text("Forward All") }
        }
        Spacer(Modifier.height(12.dp))

        if (selected != null) {
            RequestEditorPanel(
                transaction = selected!!,
                onForward = { headers, body -> viewModel.forward(selected!!, headers, body); selected = null },
                onDrop = { viewModel.drop(selected!!); selected = null },
                onClose = { selected = null }
            )
        } else if (queue.isEmpty()) {
            EmptyState(
                if (interceptOn) "Waiting for traffic to intercept…"
                else "Turn Intercept ON to pause and inspect live requests."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(queue) { tx ->
                    GlowCard(Modifier.fillMaxWidth(), accent = WarningCrimson) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            MethodBadge(tx.method)
                            Spacer(Modifier.width(8.dp))
                            Text(tx.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { selected = tx }, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = VoidBlack)) {
                                Text("Inspect / Edit")
                            }
                            Button(onClick = { viewModel.forward(tx, JSONObject(tx.requestHeaders).toMap(), tx.requestBody) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = VoidBlack)) {
                                Text("Forward")
                            }
                            Button(onClick = { viewModel.drop(tx) },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningCrimson, contentColor = VoidBlack)) {
                                Text("Drop")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestEditorPanel(
    transaction: HttpTransaction,
    onForward: (Map<String, String>, String?) -> Unit,
    onDrop: () -> Unit,
    onClose: () -> Unit
) {
    var method by remember { mutableStateOf(transaction.method) }
    var url by remember { mutableStateOf(transaction.url) }
    var headersText by remember { mutableStateOf(JSONObject(transaction.requestHeaders).toString(2)) }
    var body by remember { mutableStateOf(transaction.requestBody ?: "") }

    GlowCard(Modifier.fillMaxWidth(), accent = ElectricCyan) {
        Text("Edit Request", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL / Path") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = headersText, onValueChange = { headersText = it }, label = { Text("Headers (JSON)") },
            modifier = Modifier.fillMaxWidth().height(120.dp), textStyle = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body, onValueChange = { body = it }, label = { Text("Request Body") },
            modifier = Modifier.fillMaxWidth().height(140.dp), textStyle = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val headerMap = runCatching { JSONObject(headersText).toMap() }.getOrDefault(emptyMap())
                    onForward(headerMap, body.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = VoidBlack)
            ) { Text("Forward", fontWeight = FontWeight.Bold) }
            Button(onClick = onDrop, colors = ButtonDefaults.buttonColors(containerColor = WarningCrimson, contentColor = VoidBlack)) {
                Text("Drop")
            }
            OutlinedButton(onClick = onClose) { Text("Cancel") }
        }
    }
}

private fun JSONObject.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    keys().forEach { map[it] = optString(it) }
    return map
}
