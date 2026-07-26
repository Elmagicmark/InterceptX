package com.interceptx.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.interceptx.ui.components.*
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.HistoryViewModel
import com.interceptx.viewmodel.RepeaterViewModel
import com.interceptx.viewmodel.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel, repeaterViewModel: RepeaterViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selected by viewModel.selectedIds.collectAsState()
    var exportedJson by remember { mutableStateOf<String?>(null) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(VoidBlack).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Row {
                IconButton(onClick = {
                    // Export: in a full build this writes to a SAF-picked file; here we surface the JSON inline.
                    coroutineScope.launch { exportedJson = viewModel.exportToJson() }
                }) { Icon(Icons.Filled.FileUpload, contentDescription = "Export JSON", tint = ElectricCyan) }
                if (selected.isNotEmpty()) {
                    IconButton(onClick = { viewModel.deleteSelected() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = WarningCrimson)
                    }
                }
            }
        }

        OutlinedTextField(
            value = filter.searchText,
            onValueChange = { viewModel.updateSearch(it) },
            label = { Text("Search URL / query") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("", "GET", "POST", "PUT", "DELETE", "PATCH").forEach { m ->
                FilterChip(
                    selected = filter.methodFilter == m,
                    onClick = { viewModel.updateMethod(m) },
                    label = { Text(if (m.isEmpty()) "All" else m) }
                )
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("" to "All", "2xx" to "2xx", "3xx" to "3xx", "4xx" to "4xx", "5xx" to "5xx").forEach { (cls, label) ->
                FilterChip(selected = filter.statusClass == cls, onClick = { viewModel.updateStatusClass(cls) }, label = { Text(label) })
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = filter.sortMode == mode,
                    onClick = { viewModel.updateSort(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            EmptyState("No matching transactions.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { tx ->
                    val isSelected = tx.id in selected
                    GlowCard(
                        Modifier.fillMaxWidth(),
                        accent = if (isSelected) ElectricCyan else StrokeSlate
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleSelection(tx.id) })
                            MethodBadge(tx.method)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.url, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Text(
                                    java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(tx.timestamp)),
                                    color = TextDisabled, style = MaterialTheme.typography.labelSmall
                                )
                            }
                            StatusBadge(tx.responseStatusCode)
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = {
                                repeaterViewModel.sendToRepeater(tx.method, tx.url, tx.requestHeaders, tx.requestBody)
                            }) {
                                Icon(Icons.Filled.Repeat, contentDescription = "Send to Repeater", tint = NeonGreen)
                            }
                        }
                    }
                }
            }
        }

        exportedJson?.let { json ->
            AlertDialog(
                onDismissRequest = { exportedJson = null },
                confirmButton = { TextButton(onClick = { exportedJson = null }) { Text("Close") } },
                title = { Text("Exported JSON") },
                text = {
                    Text(
                        json.take(2000) + if (json.length > 2000) "\n… (truncated preview)" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }
    }
}
