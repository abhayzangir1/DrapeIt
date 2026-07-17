package com.drapeproof.mobile.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.mobile.camera.DrapeCaptureScreen
import com.drapeproof.mobile.catalog.CatalogScreen
import com.drapeproof.mobile.photo.PhotoAnalysisScreen
import com.drapeproof.mobile.records.RecordsScreen
import com.drapeproof.mobile.youcam.YouCamLabScreen
import com.drapeproof.mobile.ui.theme.Cobalt
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.Moss
import com.drapeproof.mobile.ui.theme.Plum

private enum class Destination { HOME, LIVE, PHOTO, CATALOG, RECORDS, YOUCAM }

@Composable
fun DrapeProofApp(
    sharedImageUri: Uri?,
    onSharedImageConsumed: () -> Unit,
) {
    var destination by remember { mutableStateOf(Destination.HOME) }
    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) destination = Destination.PHOTO
    }
    BackHandler(enabled = destination != Destination.HOME) { destination = Destination.HOME }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (destination) {
            Destination.HOME -> HomeScreen(onNavigate = { destination = it })
            Destination.LIVE -> DrapeCaptureScreen(onBack = { destination = Destination.HOME })
            Destination.PHOTO -> PhotoAnalysisScreen(
                initialFabricUri = sharedImageUri,
                onInitialUriConsumed = onSharedImageConsumed,
                onBack = { destination = Destination.HOME },
                onSeeCatalog = { destination = Destination.CATALOG },
            )
            Destination.CATALOG -> CatalogScreen(
                onBack = { destination = Destination.HOME },
                onOpenRecords = { destination = Destination.RECORDS },
            )
            Destination.RECORDS -> RecordsScreen(onBack = { destination = Destination.HOME })
            Destination.YOUCAM -> YouCamLabScreen(onBack = { destination = Destination.HOME })
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (Destination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("DRAPEPROOF", style = MaterialTheme.typography.labelSmall)
            EvidencePill("NO APP BEAUTY FILTER")
        }

        Text("Color evidence,\nnot color rules.", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Compare a real fabric beside your face under one locked camera session—then choose the exact colorway for the contrast you want.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onNavigate(Destination.LIVE) },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
        ) {
            Text("Start a real-cloth scan  →", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { onNavigate(Destination.PHOTO) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Check face + product photos") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { onNavigate(Destination.YOUCAM) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("YouCam Lab · facial colors + scarf try-on") }

        Spacer(Modifier.height(34.dp))
        Text("ONE DECISION. THREE SIGNALS.", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        SignalCard(Moss, "Cloth–skin separation", "How softly or strongly the two captured colors separate.")
        SignalCard(Cobalt, "Feature definition", "How eyes, brows and lips read against nearby skin—without calling it beauty.")
        SignalCard(Plum, "Apparent face shift", "A camera-recorded change shown only when the capture controls pass.")

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallRouteCard("6 colorways", "Explore catalog", Modifier.weight(1f)) { onNavigate(Destination.CATALOG) }
            SmallRouteCard("Evidence trail", "Drape Records", Modifier.weight(1f)) { onNavigate(Destination.RECORDS) }
        }

        Spacer(Modifier.height(30.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("What DrapeProof refuses to do", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    "No seasonal type. No attractiveness score. No medical inference. Weak lighting or an uncertain product image gets a clear downgrade—not fake confidence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun EvidencePill(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun SignalCard(color: Color, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 4.dp).size(14.dp).clip(CircleShape).background(color))
        Column(Modifier.padding(start = 13.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun SmallRouteCard(kicker: String, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("$title  →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ScreenHeader(title: String, evidence: String? = null, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp)) { Text("←") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 14.dp).weight(1f))
        if (evidence != null) EvidencePill(evidence)
    }
}
