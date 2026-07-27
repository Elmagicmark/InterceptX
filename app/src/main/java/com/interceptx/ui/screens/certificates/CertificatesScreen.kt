package com.interceptx.ui.screens.certificates

import android.content.Intent
import android.security.KeyChain
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.interceptx.ui.components.GlowCard
import com.interceptx.ui.components.SectionHeader
import com.interceptx.ui.theme.*
import com.interceptx.viewmodel.CertificatesViewModel
import java.io.File

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val context = LocalContext.current
    var showPem by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Text(viewModel.fingerprintSha256, color = NeonGreen, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        try {
                            val intent = KeyChain.createInstallIntent()
                            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, viewModel.certificateBytes())
                            intent.putExtra(KeyChain.EXTRA_NAME, "InterceptX Root CA")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            errorMessage = "Couldn't open the system installer: ${e.message}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = VoidBlack)
                ) { Text("Install Certificate", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    try {
                        val uri = writeCertFileAndGetUri(context, viewModel.certificateBytes())
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/x-x509-ca-cert"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "InterceptX Root CA")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share / Save Root CA"))
                    } catch (e: Exception) {
                        errorMessage = "Couldn't share the certificate file: ${e.message}"
                    }
                }) { Text("Share / Save as File") }

                OutlinedButton(onClick = { showPem = true }) { Text("View Raw PEM") }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Install on Android")
        InstallStep(1, "Tap \"Install Certificate\"", "Opens Android's built-in certificate installer directly — no need to export a file manually.")
        InstallStep(2, "Name it and confirm", "Android may ask you to set a screen lock if you haven't already (required to store CA certs) and to name the certificate.")
        InstallStep(3, "Verify with the fingerprint", "Compare the SHA-256 above against the certificate details in Settings → Security → Trusted credentials → User.")
        InstallStep(4, "Alternative: Share / Save as File", "If the direct installer doesn't suit your case (e.g. installing on a different device, or via a browser download), use \"Share / Save as File\" to send the .crt through any app — browser, email, Drive, etc.")
        InstallStep(5, "Android 7+ apps", "Apps targeting API 24+ ignore user-installed CAs by default. For apps you control, add a network_security_config that trusts user certs, or test against apps that opt in to user CAs.")

        if (showPem) {
            AlertDialog(
                onDismissRequest = { showPem = false },
                confirmButton = { TextButton(onClick = { showPem = false }) { Text("Close") } },
                title = { Text("Root CA (PEM)") },
                text = { Text(viewModel.exportPem(), style = MaterialTheme.typography.bodySmall) }
            )
        }

        errorMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
                title = { Text("Something went wrong") },
                text = { Text(msg, style = MaterialTheme.typography.bodySmall) }
            )
        }
    }
}

/** Writes the DER-encoded cert to app cache and returns a shareable content:// URI via FileProvider. */
private fun writeCertFileAndGetUri(context: android.content.Context, certBytes: ByteArray): android.net.Uri {
    val certsDir = File(context.cacheDir, "certs").apply { mkdirs() }
    val file = File(certsDir, "interceptx_ca.crt")
    file.writeBytes(certBytes)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
