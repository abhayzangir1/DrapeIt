package com.drapeproof.mobile.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning

@Composable
fun ProfileScreen(
    onRecalibrate: () -> Unit,
    onOpenYouCamLab: () -> Unit,
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(SkinProfileRepository.load(context)) }
    val effectiveProfile = profile ?: SkinProfileRepository.deriveProfileFromSkinHex("#D8B498", "default_preset")

    var showLabInfoDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val cx = (bmp.width * 0.5f).toInt().coerceIn(0, bmp.width - 1)
                        val cy = (bmp.height * 0.5f).toInt().coerceIn(0, bmp.height - 1)
                        val pixel = bmp.getPixel(cx, cy)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val hex = String.format("#%02X%02X%02X", r, g, b)
                        val newProfile = SkinProfileRepository.deriveProfileFromSkinHex(hex, "photo_upload")
                        SkinProfileRepository.save(context, newProfile)
                        profile = newProfile
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))

            Text(
                "Color Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Text(
                "Your locked undertone & personalized seasonal style guide",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(16.dp))

            // 1. HERO COLOR PROFILE CARD
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(effectiveProfile.skinHex.asComposeColor())
                                .border(3.dp, EditorialSand, CircleShape),
                        )

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    effectiveProfile.season,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialInk,
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EditorialPositive.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text("LOCKED ✓", color = EditorialPositive, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Undertone: ${effectiveProfile.undertone}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EditorialSienna,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        effectiveProfile.seasonDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorialInk.copy(alpha = 0.85f),
                        lineHeight = 20.sp,
                    )

                    Spacer(Modifier.height(16.dp))

                    // JEWELRY & METALS RECOMMENDATION
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EditorialSand.copy(alpha = 0.45f))
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💍", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Best Jewelry & Metals", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialMuted)
                                Text(effectiveProfile.bestMetals, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. BEST COLORS FOR YOU
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "🌟 Best Colors For You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Text(
                        "These shades highlight your complexion and create natural contrast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        effectiveProfile.bestColors.forEach { hex ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(52.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor()),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hex,
                                    fontSize = 10.sp,
                                    color = EditorialMuted,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. COLORS TO AVOID
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "⚠️ Colors to Avoid or Use as Accents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Text(
                        "These tones can clash with your undertone or wash out your features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        effectiveProfile.worstColors.forEach { hex ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EditorialSand.copy(alpha = 0.40f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor()),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(hex, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EditorialInk)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 4. SCAN FACE & PHOTO RECALIBRATION
            Text(
                "Update Color Profile",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onRecalibrate,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("📸 Scan Face with Camera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("📁 Upload Photo", color = EditorialInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            // 5. YOUCAM API LAB & DEVELOPER DIAGNOSTICS CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            Text("🔬", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "YouCam API Lab & Diagnostics",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                        }
                        Text(
                            "Why is this here?",
                            style = MaterialTheme.typography.labelSmall,
                            color = EditorialSienna,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showLabInfoDialog = true },
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Cloud diagnostic bench for hackathon judges to test live Clothes V3 tasks, verify upload latency, and check remaining credits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenYouCamLab,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open API Lab Bench →", color = EditorialInk, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showLabInfoDialog) {
            AlertDialog(
                onDismissRequest = { showLabInfoDialog = false },
                title = {
                    Text("Why YouCam API Lab is Included", fontWeight = FontWeight.Bold, color = EditorialInk)
                },
                text = {
                    Text(
                        "The YouCam API Lab & Diagnostics bench is built into DrapeIt so hackathon judges and engineers can:\n\n" +
                            "1. Verify real Cloudflare Worker authentication and Perfect Corp API health in real time.\n" +
                            "2. Check live credit status (718 units available) and verify that 2 units are deducted per generation.\n" +
                            "3. Inspect raw JSON responses and latency from YouCam Clothes V3.\n\n" +
                            "Regular shoppers enjoy the seamless Drape and Try-On experiences without needing to use the API Lab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialInk,
                        lineHeight = 18.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showLabInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text("Got it", color = Color.White)
                    }
                },
            )
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
