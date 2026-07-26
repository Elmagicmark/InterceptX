package com.interceptx.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interceptx.data.model.ScopeType
import com.interceptx.ui.components.EmptyState
import com.interceptx.ui.components.GlowCard
import com.interceptx.ui.components.SectionHeader
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.ProjectsViewModel

@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel) {
    val projects by viewModel.projects.collectAsState()
    val scopeRules by viewModel.scopeRules.collectAsState()
    var newProjectName by remember { mutableStateOf("") }
    var newScopePattern by remember { mutableStateOf("") }
    var newScopeType by remember { mutableStateOf(ScopeType.IN_SCOPE) }

    Column(Modifier.fillMaxSize().background(VoidBlack).padding(16.dp)) {
        Text("Projects & Scope", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        GlowCard(Modifier.fillMaxWidth(), accent = ElectricCyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newProjectName, onValueChange = { newProjectName = it },
                    label = { Text("New project name") }, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newProjectName.isNotBlank()) { viewModel.createProject(newProjectName); newProjectName = "" }
                }) { Icon(Icons.Filled.Add, contentDescription = "Create project", tint = NeonGreen) }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader("Workspaces")
        if (projects.isEmpty()) {
            EmptyState("No projects yet — create one above.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(projects) { project ->
                    GlowCard(
                        Modifier.fillMaxWidth(),
                        accent = if (project.isActive) NeonGreen else StrokeSlate
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(project.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            if (project.isActive) {
                                Text("ACTIVE", color = NeonGreen, style = MaterialTheme.typography.labelSmall)
                            } else {
                                TextButton(onClick = { viewModel.setActive(project.id) }) { Text("Switch") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Target Scope")
        GlowCard(Modifier.fillMaxWidth(), accent = WarningCrimson) {
            Row {
                FilterChip(selected = newScopeType == ScopeType.IN_SCOPE, onClick = { newScopeType = ScopeType.IN_SCOPE }, label = { Text("In-Scope") })
                Spacer(Modifier.width(6.dp))
                FilterChip(selected = newScopeType == ScopeType.OUT_OF_SCOPE, onClick = { newScopeType = ScopeType.OUT_OF_SCOPE }, label = { Text("Out-of-Scope") })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newScopePattern, onValueChange = { newScopePattern = it },
                    label = { Text("URL pattern e.g. *.example.com/api/*") }, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newScopePattern.isNotBlank()) { viewModel.addScopeRule(newScopePattern, newScopeType); newScopePattern = "" }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add rule", tint = ElectricCyan) }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(scopeRules) { rule ->
                GlowCard(Modifier.fillMaxWidth(), accent = if (rule.type == ScopeType.IN_SCOPE) NeonGreen else WarningCrimson) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(rule.pattern, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text(rule.type.name.replace("_", "-"), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.removeScopeRule(rule) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
