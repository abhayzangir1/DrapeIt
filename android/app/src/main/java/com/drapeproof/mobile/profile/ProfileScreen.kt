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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EditorialInk,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EditorialPositive.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (effectiveProfile.isCalibrated) "CALIBRATED" else "DEFAULT",
                        color = EditorialPositive,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 1. HERO COLOR PROFILE CARD
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(effectiveProfile.skinHex.asComposeColor())
                            .border(3.dp, EditorialStone.copy(alpha = 0.5f), CircleShape),
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        effectiveProfile.season,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )

                    Text(
                        "${effectiveProfile.skinHex.uppercase()} • ${effectiveProfile.undertone} Undertone",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                        ) {
                            Text("Photo Pick", color = EditorialInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isLiveScannerOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                        ) {
                            Text("Live Scan", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
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

            Spacer(Modifier.height(14.dp))

            // 2. METALS ROW
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("💍", fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Best Metals", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialMuted)
                        Text(effectiveProfile.bestMetals, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3. BEST COLORS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Best Colors",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        effectiveProfile.bestColors.forEach { hex ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(48.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(1.dp, EditorialStone, CircleShape),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hex,
                                    fontSize = 9.sp,
                                    color = EditorialInk,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. COLORS TO AVOID
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Colors to Avoid",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        effectiveProfile.worstColors.forEach { hex ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(48.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(1.5.dp, EditorialWarning.copy(alpha = 0.5f), CircleShape),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    hex,
                                    fontSize = 9.sp,
                                    color = EditorialInk,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 5. YOUCAM API DIAGNOSTICS CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔬", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "YouCam AI Engine",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                            Text(
                                "Live neural diagnostics",
                                style = MaterialTheme.typography.labelSmall,
                                color = EditorialMuted,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenYouCamLab,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("Diagnostics", color = EditorialInk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // INTERACTIVE 3-POINT SKIN TONE PICKER MODAL
        if (selectedPickerBitmap != null) {
            SkinTonePickerModal(
                bitmap = selectedPickerBitmap!!,
                onDismiss = { selectedPickerBitmap = null },
                onSaved = { hex ->
                    profile = SkinProfileRepository.load(context)
                    selectedPickerBitmap = null
                    updateSuccessMessage = "Colortone calibrated to $hex"
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
                    updateSuccessMessage = "Live scan calibrated to $hex"
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
