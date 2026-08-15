package com.drapeproof.mobile.profile

import android.graphics.Bitmap
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
import androidx.compose.runtime.LaunchedEffect
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
    onRecalibrate: () -> Unit = {},
    onOpenYouCamLab: () -> Unit,
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(SkinProfileRepository.load(context)) }

    LaunchedEffect(Unit) {
        profile = SkinProfileRepository.load(context)
    }

    val effectiveProfile = profile ?: SkinProfileRepository.deriveProfileFromSkinHex("#D8B498", "default_preset")

    var showUpdateModal by remember { mutableStateOf(false) }
    var showLabInfoDialog by remember { mutableStateOf(false) }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }

    var selectedPickerBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLiveScannerOpen by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        selectedPickerBitmap = bmp
                        showUpdateModal = false
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(14.dp))

            // 1. CENTERED HERO SKIN COLORTONE CIRCLE & DETAILS
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(26.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "COLOR PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(14.dp))

                    // LARGE CENTERED COLORTONE CIRCLE
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(effectiveProfile.skinHex.asComposeColor())
                            .border(4.dp, EditorialSand, CircleShape)
                            .border(6.dp, Color.White, CircleShape),
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        effectiveProfile.skinHex.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EditorialInk,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "${effectiveProfile.season} Season",
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

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Undertone: ${effectiveProfile.undertone}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EditorialSienna,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        effectiveProfile.seasonDescription,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = EditorialInk.copy(alpha = 0.85f),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    // UPDATE PROFILE BUTTON (DIRECTLY BELOW CIRCLE)
                    Button(
                        onClick = { showUpdateModal = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Text("🔄 Update Color Profile", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }

                    if (updateSuccessMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            updateSuccessMessage!!,
                            color = EditorialPositive,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. BEST JEWELRY & METALS
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("💍", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Best Jewelry & Metals", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialMuted)
                        Text(effectiveProfile.bestMetals, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. BEST COLOR SUGGESTIONS (HIGH VISUAL CONSISTENCY)
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
                        "🌟 Best Color Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Text(
                        "These shades highlight your natural facial contrast and ${effectiveProfile.undertone.lowercase()} undertone.",
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
                                modifier = Modifier.width(56.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(1.5.dp, EditorialSand, CircleShape),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hex,
                                    fontSize = 10.sp,
                                    color = EditorialInk,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. COLORS TO AVOID (DESIGNED IDENTICALLY TO BEST COLORS FOR VISUAL HARMONY)
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
                        "⚠️ Colors to Avoid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Text(
                        "These tones clash with your undertone and can wash out or overpower your natural complexion.",
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
                        effectiveProfile.worstColors.forEach { hex ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(56.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(1.5.dp, EditorialWarning.copy(alpha = 0.5f), CircleShape),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hex,
                                    fontSize = 10.sp,
                                    color = EditorialInk,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 5. YOUCAM API LAB & DEVELOPER DIAGNOSTICS CARD (BOTTOM-MOST)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔬", fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "YouCam API Lab Bench",
                                style = MaterialTheme.typography.titleMedium,
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
                        "Cloud diagnostic tool for Devpost hackathon judges to test live Clothes V3 tasks, check credit balance (718 units available), and verify neural endpoint latency.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                        lineHeight = 18.sp,
                    )

                    Spacer(Modifier.height(12.dp))

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

        // UPDATE PROFILE MODAL WITH 2 CHOICES
        if (showUpdateModal) {
            AlertDialog(
                onDismissRequest = { showUpdateModal = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text("Update Color Profile", fontWeight = FontWeight.Bold, color = EditorialInk, style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Choose how you want to calibrate your personal colortone:",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                        )
                        Spacer(Modifier.height(16.dp))

                        // OPTION 1: PHOTO SAMPLING
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = EditorialCream),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { photoPickerLauncher.launch("image/*") }
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🖼️", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Choose Image & Pick Colortone", fontWeight = FontWeight.Bold, color = EditorialInk, fontSize = 13.sp)
                                    Text("Point on forehead & cheeks to sample", style = MaterialTheme.typography.bodySmall, color = EditorialMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // OPTION 2: LIVE CAMERA SCAN
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = EditorialCream),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showUpdateModal = false
                                    isLiveScannerOpen = true
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("📷", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Live Camera Scan (KYC Mode)", fontWeight = FontWeight.Bold, color = EditorialInk, fontSize = 13.sp)
                                    Text("Biometric facial scan with auto-calibration", style = MaterialTheme.typography.bodySmall, color = EditorialMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { showUpdateModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSand),
                    ) {
                        Text("Cancel", color = EditorialInk, fontWeight = FontWeight.Bold)
                    }
                },
            )
        }

        // INTERACTIVE 3-POINT SKIN TONE PICKER MODAL
        if (selectedPickerBitmap != null) {
            SkinTonePickerModal(
                bitmap = selectedPickerBitmap!!,
                onDismiss = { selectedPickerBitmap = null },
                onSaved = { hex ->
                    profile = SkinProfileRepository.load(context)
                    selectedPickerBitmap = null
                    updateSuccessMessage = "Colortone updated to $hex successfully!"
                },
            )
        }

        // DEDICATED LIVE KYC-STYLE CAMERA COLORIMETRY SCANNER
        if (isLiveScannerOpen) {
            LiveSkinScanScreen(
                onDismiss = { isLiveScannerOpen = false },
                onScanSuccess = { hex ->
                    profile = SkinProfileRepository.load(context)
                    isLiveScannerOpen = false
                    updateSuccessMessage = "Live colortone scan calibrated to $hex!"
                },
            )
        }

        // LAB INFO DIALOG
        if (showLabInfoDialog) {
            AlertDialog(
                onDismissRequest = { showLabInfoDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text("Why is the API Lab Bench included?", fontWeight = FontWeight.Bold, color = EditorialInk)
                },
                text = {
                    Text(
                        "The YouCam API Lab Bench provides live developer diagnostics for hackathon evaluators.\n\n" +
                            "• Live verification of Cloudflare Worker proxy security & auth\n" +
                            "• Real-time YouCam Cloud credit quota monitoring\n" +
                            "• Raw JSON payload inspection and task polling diagnostics\n" +
                            "• Direct end-to-end neural rendering latency benchmarks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorialInk.copy(alpha = 0.85f),
                        lineHeight = 20.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showLabInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
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
