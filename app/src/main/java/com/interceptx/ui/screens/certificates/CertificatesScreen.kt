package com.interceptx.ui.screens.certificates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interceptx.ui.components.GlowCard
import com.interceptx.ui.components.SectionHeader
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.CertificatesViewModel

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    var showPem by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(VoidBlack).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = ElectricCyan)
            Spacer(Modifier.width(8.dp))
            Text("Certificates & Security", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(12.dp))

        GlowCard(Modifier.fillMaxWidth(), accent = ElectricCyan) {
            Text("InterceptX Root CA", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "This device-local CA signs a fresh leaf certificate per host so InterceptX can decrypt HTTPS traffic passed through the proxy. It never leaves this device and is unique per install.",
                color = TextSecondary, style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Text("SHA-256 Fingerprint", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                viewModel.fingerprintSha256,
                color = NeonGreen, style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPem = true }, colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = VoidBlack)) {
                    Text("Export Root CA (.pem)", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Install on Android")
        InstallStep(1, "Export the root CA", "Tap \"Export Root CA (.pem)\" above and save the file to your device storage.")
        InstallStep(2, "Open device certificate settings", "Settings → Security → Encryption & credentials → Install a certificate → CA certificate.")
        InstallStep(3, "Select the exported file", "Browse to the saved .pem file and confirm installation. Android will warn that a CA can monitor traffic — that's expected for local security testing.")
        InstallStep(4, "Verify with the fingerprint", "Compare the SHA-256 shown above against the certificate details in Settings to confirm you installed the right CA.")
        InstallStep(5, "Android 7+ apps", "Apps targeting API 24+ ignore user-installed CAs by default. For apps you control, add a network_security_config that trusts user certs, or test against apps that opt in to user CAs.")

        if (showPem) {
            AlertDialog(
                onDismissRequest = { showPem = false },
                confirmButton = { TextButton(onClick = { showPem = false }) { Text("Close") } },
                title = { Text("Root CA (PEM)") },
                text = { Text(viewModel.exportPem(), style = MaterialTheme.typography.bodySmall) }
            )
        }
    }
}

@Composable
private fun InstallStep(number: Int, title: String, description: String) {
    GlowCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), accent = StrokeSlate) {
        Row {
            Box(
                Modifier
                    .background(ElectricCyan.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
                    .size(28.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(number.toString(), color = ElectricCyan, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
