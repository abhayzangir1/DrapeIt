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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.drapeproof.mobile.data.AppThemeMode
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialWarning

@Composable
fun ProfileScreen(
    onOpenYouCamLab: () -> Unit,
    onOpenTutorial: () -> Unit = {},
    onRestartInteractiveGuide: () -> Unit = {},
    onThemeChanged: (AppThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(SkinProfileRepository.load(context)) }

    var selectedPickerBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLiveScannerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Separate independent state for Compatible vs Caution hex reveal
    var tappedCompatibleHex by remember { mutableStateOf<String?>(null) }
    var tappedCautionHex by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        selectedPickerBitmap = bmp
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))

            // HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Calibrated colorimetry & seasonal archetype",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                        .clickable { isSettingsOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(22.dp))

            // 1. SPACIOUS INSTAGRAM BIO-STYLE HERO CARD
            if (profile == null) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Skin Profile Not Calibrated",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { isLiveScannerOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        ) {
                            Text("Run Live Skin Scan", color = Color.White)
                        }
                    }
                }
            } else {
                val effectiveProfile = profile!!
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                    ) {
                        // TOP BIO ROW: COLORTONE HALO (LEFT) + ARCHETYPE TITLE & METALS (RIGHT)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // TOP-LEFT: COLORTONE HALO CIRCLE
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(3.dp, EditorialGold.copy(alpha = 0.85f), CircleShape)
                                    .padding(5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(effectiveProfile.skinHex.asComposeColor())
                                        .border(1.5.dp, Color.White, CircleShape),
                                )
                            }

                            Spacer(Modifier.width(18.dp))

                            // TOP-RIGHT: ARCHETYPE DETAILS
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    effectiveProfile.season,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${effectiveProfile.undertone} • ${effectiveProfile.skinHex.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EditorialSienna,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text("✨", fontSize = 11.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        effectiveProfile.bestMetals,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            effectiveProfile.seasonDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )

                        Spacer(Modifier.height(20.dp))

                        // ACTION BUTTONS ROW (SPACIOUS & UNIFIED LUXURY STYLING)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // LIVE SCAN BUTTON
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.2.dp, EditorialSienna.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                                    .clickable { isLiveScannerOpen = true },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text("📸", fontSize = 17.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Live Scan",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                    )
                                }
                            }

                            // PHOTO PICK BUTTON
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.2.dp, EditorialGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                                    .clickable { photoPickerLauncher.launch("image/*") },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text("🖼️", fontSize = 17.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Photo Pick",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }

                        if (updateSuccessMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                updateSuccessMessage!!,
                                color = EditorialPositive,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. COMPATIBLE PALETTE CARD (WITH GENEROUS PADDING)
            if (profile != null) {
                val effectiveProfile = profile!!
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "COMPATIBLE PALETTE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialSienna,
                                letterSpacing = 1.2.sp,
                            )

                            if (tappedCompatibleHex != null) {
                                Text(
                                    tappedCompatibleHex!!.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialSienna,
                                )
                            } else {
                                Text(
                                    "Tap swatch for #HEX",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            effectiveProfile.bestColors.forEach { hex ->
                                val isTapped = tappedCompatibleHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(
                                            width = if (isTapped) 2.5.dp else 1.dp,
                                            color = if (isTapped) EditorialSienna else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            tappedCompatibleHex = if (isTapped) null else hex
                                        },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 3. CONTRAST CAUTION CARD (WITH GENEROUS PADDING)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "CONTRAST CAUTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialWarning,
                                letterSpacing = 1.2.sp,
                            )

                            if (tappedCautionHex != null) {
                                Text(
                                    tappedCautionHex!!.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialWarning,
                                )
                            } else {
                                Text(
                                    "Tap swatch for #HEX",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            effectiveProfile.worstColors.forEach { hex ->
                                val isTapped = tappedCautionHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(
                                            width = if (isTapped) 2.5.dp else 1.5.dp,
                                            color = if (isTapped) EditorialWarning else EditorialWarning.copy(alpha = 0.5f),
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            tappedCautionHex = if (isTapped) null else hex
                                        },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // 4. YOUCAM API DIAGNOSTICS CARD
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔬", fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "YouCam AI Engine",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Neural verification & lab diagnostics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Button(
                        onClick = onOpenYouCamLab,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp),
                    ) {
                        Text("Diagnostics", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // SETTINGS MODAL
        if (isSettingsOpen) {
            ProfileSettingsModal(
                onDismiss = { isSettingsOpen = false },
                onRestartInteractiveGuide = {
                    isSettingsOpen = false
                    onRestartInteractiveGuide()
                },
                onThemeChanged = onThemeChanged,
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
                    updateSuccessMessage = "Colortone calibrated to $hex"
                },
            )
        }

        // DEDICATED LIVE FACIAL SCANNER MODAL
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
