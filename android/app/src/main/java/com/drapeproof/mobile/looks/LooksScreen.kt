package com.drapeproof.mobile.looks

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

private enum class LooksSubTab(val title: String) {
    PROVEN_LOOKS("Proven Looks"),
    TRY_ANYTHING("Try Anything"),
}

@Composable
fun LooksScreen(
    onNavigateToTryOn: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(LooksSubTab.PROVEN_LOOKS) }
    val savedColors: List<SavedSuitedColor> = remember { SuitedColorsRepository.list(context) }
    val savedOutfits: List<SavedTryOnOutfit> = remember { WardrobeRepository.listOutfits(context) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            Text(
                "Your Looks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Text(
                "Proven combinations & screenshot try-on",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = EditorialSand.copy(alpha = 0.50f),
                contentColor = EditorialInk,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                        color = EditorialSienna,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
            ) {
                LooksSubTab.values().forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == tab) EditorialInk else EditorialMuted,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (activeTab) {
                LooksSubTab.PROVEN_LOOKS -> {
                    if (savedColors.isEmpty() && savedOutfits.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("✨", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No Proven Looks Saved Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Bookmark combinations from the Live Drape Studio or Explore screen to build your personal lookbook.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            savedColors.forEach { (id, colorHex, colorName, fabricId, fabricName, matchScorePercent, contrastLabel, _) ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(colorHex.asComposeColor())
                                                        .border(1.dp, EditorialStone, CircleShape),
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        "$colorName ${fabricName ?: "Fabric"}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EditorialInk,
                                                    )
                                                    Text(
                                                        contrastLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EditorialMuted,
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(EditorialPositive.copy(alpha = 0.15f))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                            ) {
                                                Text(
                                                    "$matchScorePercent%",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = EditorialPositive,
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val query = Uri.encode("$colorName ${fabricName ?: ""} shirt")
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query&tbm=shop"))
                                                    context.startActivity(intent)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text("🔍 Find Similar", color = EditorialInk, style = MaterialTheme.typography.labelSmall)
                                            }

                                            Button(
                                                onClick = { onNavigateToTryOn(fabricId ?: "silk", colorHex) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text("📸 AI Try-On", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                LooksSubTab.TRY_ANYTHING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("📸", fontSize = 42.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Drop Any Product Screenshot",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialInk,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Upload screenshots from Zara, Amazon, Pinterest, or Instagram to test if that exact piece complements your complexion and try it on via YouCam Clothes V3.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EditorialMuted,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(18.dp))

                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Upload Screenshot / Photo", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                selectedImageUri?.let { uri ->
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        "✓ Loaded: ${uri.lastPathSegment?.take(24)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EditorialPositive,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { onNavigateToTryOn("cotton", "#831843") },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EditorialPositive),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("⚡ Analyze & Try-On via YouCam", color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

private fun String.asComposeColor(): Color {
    return runCatching {
        val value = removePrefix("#").toLong(16)
        Color(
            red = ((value shr 16) and 0xFF).toInt(),
            green = ((value shr 8) and 0xFF).toInt(),
            blue = (value and 0xFF).toInt(),
        )
    }.getOrDefault(Color.Gray)
}
