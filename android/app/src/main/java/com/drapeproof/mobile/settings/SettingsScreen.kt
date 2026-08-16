package com.drapeproof.mobile.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.mobile.data.DrapeRecordRepository
import com.drapeproof.mobile.network.DrapeProofApiClient
import com.drapeproof.mobile.ui.ScreenHeader
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.Moss
import com.drapeproof.mobile.youcam.YouCamLabStore

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var recordCount by remember { mutableStateOf(DrapeRecordRepository.all(context).size) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val api = remember { DrapeProofApiClient() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings & Privacy", evidence = "ON-DEVICE", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text("Product & Privacy Controls", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "DrapeIt is built around transparent evidence. Manage your local data, inspect privacy boundaries, and control cloud integration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))

            // App Identity & Principles
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("DrapeIt — Production v1.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Color evidence, not color rules.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "DrapeIt helps shoppers compare real fabric against their captured appearance, select their desired contrast, compare exact product variants, and optionally preview apparel with YouCam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Local Data Management
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Local Data Management", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Saved Drape Records", style = MaterialTheme.typography.bodyMedium)
                        Text("$recordCount record${if (recordCount == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium, color = Moss)
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "DrapeIt evidence export")
                                putExtra(Intent.EXTRA_TEXT, DrapeRecordRepository.exportJson(context))
                            }
                            context.startActivity(Intent.createChooser(share, "Export Drape Records"))
                        },
                        enabled = recordCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export Records (Share JSON)") }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                    ) {
                        Text("Delete All Local Data", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Privacy & Security Invariants
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Privacy & Security Invariants", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    PrivacyBullet("On-Device Frame Processing", "Live camera frames are processed in volatile memory and never written to disk.")
                    PrivacyBullet("Zero Beauty Retouching", "No automatic smoothing, face reshaping, or artificial color grading is applied.")
                    PrivacyBullet("Explicit Cloud Consent", "Selecting an image locally never uploads it. Cloud execution requires explicit consent and an explicit action.")
                    PrivacyBullet("Separation of VTO", "Generated virtual-try-on pixels are kept strictly separate from physical measurement evidence.")
                    PrivacyBullet("Server-Side Secrets", "The YouCam API key stays on the secure Worker and is never bundled into the app.")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cloud Session State
            var diagnosticResult by remember { mutableStateOf<String?>(null) }
            var isRunningDiagnostic by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Cloud Backend Connection", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("Server Host: ${api.serviceHost}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (api.cloudConfigured) "Status: Configured for secure cloud features" else "Status: Offline sentinel build",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (api.cloudConfigured) Moss else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isRunningDiagnostic = true
                                scope.launch {
                                    val start = System.currentTimeMillis()
                                    val res = withContext(Dispatchers.IO) {
                                        runCatching { api.health() }
                                    }
                                    val elapsed = System.currentTimeMillis() - start
                                    isRunningDiagnostic = false
                                    diagnosticResult = res.fold(
                                        onSuccess = { h ->
                                            "Ping: ${elapsed}ms • Status: ${if (h.ready) "ok" else "degraded"} • VTO: ${h.vtoProvider}\nAccess Gate: ${if (h.accessGateConfigured) "OK" else "Missing"} • Ledger DO: ${if (h.paidLedgerConfigured) "OK" else "Missing"}"
                                        },
                                        onFailure = { err ->
                                            "Diagnostic failed: ${err.localizedMessage ?: "Could not reach server"}"
                                        },
                                    )
                                }
                            },
                            enabled = !isRunningDiagnostic,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(if (isRunningDiagnostic) "Pinging…" else "Run Health Ping ⚡")
                        }
                        OutlinedButton(
                            onClick = {
                                api.clearSession()
                                YouCamLabStore.clearFacialOperationId(context)
                                YouCamLabStore.clearTryOnOperationId(context)
                                statusMessage = "Cloud session token and active task states cleared."
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Reset Session") }
                    }

                    diagnosticResult?.let { diag ->
                        Spacer(Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        ) {
                            Text(
                                diag,
                                style = MaterialTheme.typography.bodySmall,
                                color = Moss,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = Moss,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(Modifier.height(36.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Local Data?") },
            text = {
                Text("This action will permanently remove all saved drape records, skin profiles, stored task identifiers, and downloaded VTO result images from this device. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        YouCamLabStore.deleteAllLocalData(context)
                        com.drapeproof.mobile.data.DrapeSnapRepository.list(context).forEach { com.drapeproof.mobile.data.DrapeSnapRepository.delete(context, it.id) }
                        com.drapeproof.mobile.data.WardrobeRepository.clear(context)
                        com.drapeproof.mobile.data.SuitedColorsRepository.clear(context)
                        com.drapeproof.mobile.data.SkinProfileRepository.clear(context)
                        com.drapeproof.mobile.data.DrapeRecordRepository.deleteAll(context)
                        com.drapeproof.mobile.avatar.PhotoAvatarStore.deleteAll(context)
                        com.drapeproof.mobile.silhouette.UserProfileStore.clear(context)
                        recordCount = 0
                        showDeleteDialog = false
                        statusMessage = "All local data was permanently deleted."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PrivacyBullet(title: String, description: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
    }
}
