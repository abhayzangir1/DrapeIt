package com.drapeproof.mobile.records

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.mobile.data.DrapeRecordRepository
import com.drapeproof.mobile.data.LocalDrapeRecord
import com.drapeproof.mobile.data.displayName
import com.drapeproof.mobile.ui.ScreenHeader
import com.drapeproof.mobile.ui.theme.Moss
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val records = remember { DrapeRecordRepository.all(context) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Drape Records", evidence = "LOCAL ONLY", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text("An evidence trail you can inspect.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(7.dp))
            Text(
                "Records contain sampled colors, measurement versions and limitations—not face images. They stay in this app's private storage unless you explicitly share the JSON.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(18.dp))

            if (records.isEmpty()) {
                EmptyRecordsCard()
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${records.size} SAVED", style = MaterialTheme.typography.labelSmall, color = Moss)
                    OutlinedButton(
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "DrapeProof evidence export")
                                putExtra(Intent.EXTRA_TEXT, DrapeRecordRepository.exportJson(context))
                            }
                            context.startActivity(Intent.createChooser(share, "Share Drape Records"))
                        },
                    ) { Text("Share JSON") }
                }
                Spacer(Modifier.height(10.dp))
                records.forEach { record ->
                    RecordCard(record)
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun EmptyRecordsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("No saved evidence yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(7.dp))
            Text(
                "Complete a photo comparison or exact-color catalog rank, then choose Save evidence. A record is never fabricated from an unfinished scan.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RecordCard(record: LocalDrapeRecord) {
    val date = remember(record.createdAtEpochMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(record.createdAtEpochMillis))
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(record.fabricHex.asComposeColor(), RoundedCornerShape(14.dp)),
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(record.variantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${record.sku} • ${record.fabricHex}", style = MaterialTheme.typography.bodySmall)
                }
                record.intent?.let {
                    Text(it.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("SEPARATION", String.format(Locale.US, "%.1f ΔE00", record.separationDeltaE00), Modifier.weight(1f))
                Metric("LIGHTNESS", String.format(Locale.US, "%+.1f ΔL*", record.deltaLStar), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(record.evidenceTier.displayName().uppercase(), style = MaterialTheme.typography.labelSmall, color = Moss)
            Text(record.source, style = MaterialTheme.typography.bodySmall)
            Text("$date • ${record.scoringVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            if (record.limitations.isNotEmpty()) {
                Spacer(Modifier.height(11.dp))
                Text("LIMITATIONS KEPT WITH THIS RECORD", style = MaterialTheme.typography.labelSmall)
                record.limitations.forEach { limitation ->
                    Text("• $limitation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(MaterialTheme.colorScheme.background, RoundedCornerShape(14.dp)).padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun String.asComposeColor(): Color = runCatching {
    val value = removePrefix("#").toLong(16)
    Color(
        red = ((value shr 16) and 0xFF).toInt(),
        green = ((value shr 8) and 0xFF).toInt(),
        blue = (value and 0xFF).toInt(),
    )
}.getOrDefault(Color.Gray)
