package com.drapeproof.mobile.profile

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.mobile.data.SkinProfileRepository
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
) {
    val context = LocalContext.current
    val storedProfile = remember { SkinProfileRepository.load(context) }
    val effectiveSkinHex = storedProfile?.skinHex ?: "#D8B498"
    val skinLab = remember(effectiveSkinHex) { ColorConversions.hexToLab(effectiveSkinHex) }

    val undertoneLabel = if (skinLab.b > 11.0) "Warm Golden Undertone" else "Cool Crisp Undertone"

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
                "Your Color Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Text(
                "Measurement calibration & privacy settings",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(18.dp))

            // COLOR PROFILE CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(effectiveSkinHex.asComposeColor())
                                .border(2.dp, EditorialStone, CircleShape),
                        )

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                undertoneLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                            Text(
                                "Base Skin Tone: $effectiveSkinHex",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialMuted,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Coordinates pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EditorialSand.copy(alpha = 0.5f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("L* ${skinLab.l.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EditorialSand.copy(alpha = 0.5f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("a* ${skinLab.a.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EditorialSand.copy(alpha = 0.5f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("b* ${skinLab.b.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // YOUCAM AI CALIBRATION BANNER
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EditorialPositive.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✓", fontSize = 24.sp, color = EditorialPositive, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Calibrated with Perfect Corp AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                        Text(
                            "Dermatology-grade undertone & lighting reference active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // RECALIBRATE ACTION
            Button(
                onClick = onRecalibrate,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("📷 Re-Calibrate Color Profile", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))

            // PRIVACY TRANSPARENCY CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔒 Privacy & On-Device Processing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Live camera frames and facial color calculations run 100% locally on your device using MediaPipe. Photos are only transmitted to Perfect Corp's YouCam API when you explicitly tap 'AI Try-On'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
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
