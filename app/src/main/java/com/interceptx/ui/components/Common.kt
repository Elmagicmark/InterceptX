package com.interceptx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.ui.theme.*

fun colorForStatus(code: Int?): Color = when (code) {
    null -> TextDisabled
    in 200..299 -> Status2xx
    in 300..399 -> Status3xx
    in 400..499 -> Status4xx
    in 500..599 -> Status5xx
    else -> TextSecondary
}

fun colorForMethod(method: String): Color = when (method.uppercase()) {
    "GET" -> MethodGet
    "POST" -> MethodPost
    "PUT" -> MethodPut
    "DELETE" -> MethodDelete
    "PATCH" -> MethodPatch
    else -> MethodOther
}

@Composable
fun StatusBadge(code: Int?, modifier: Modifier = Modifier) {
    val color = colorForStatus(code)
    Box(
        modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = code?.toString() ?: "—",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val color = colorForMethod(method)
    Box(
        modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(method.uppercase(), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    accent: Color = ElectricCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    GlowCard(modifier = modifier, accent = accent) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
