package com.drapeproof.mobile.wardrobe

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.mobile.avatar.AvatarLighting
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.avatar.SavedAvatar
import com.drapeproof.mobile.data.DrapeRecordRepository
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.SavedWishlistItem
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.network.DrapeProofApiClient
import com.drapeproof.mobile.silhouette.BodyShape
import com.drapeproof.mobile.silhouette.UserBodyProfile
import com.drapeproof.mobile.silhouette.UserProfileStore
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.GoldAccent
import com.drapeproof.mobile.ui.theme.Moss
import com.drapeproof.mobile.youcam.YouCamLabStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class WardrobeFilter(val title: String, val icon: String) {
    ALL("All Items", "✨"),
    OUTFITS("Hanger Outfits", "🪝"),
    COLORS("Suited Colors", "🎨"),
    WISHLIST("Shopping Wishlist", "🛍️"),
    PROFILE("Profile & Avatars", "👤"),
}

@Composable
fun WardrobeScreen(
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, cutName: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { DrapeProofApiClient() }

    var selectedFilter by remember { mutableStateOf(WardrobeFilter.ALL) }

    // Data lists
    var suitedColors by remember { mutableStateOf(SuitedColorsRepository.list(context)) }
    var outfits by remember { mutableStateOf(WardrobeRepository.listOutfits(context)) }
    var wishlist by remember { mutableStateOf(WardrobeRepository.listWishlist(context)) }
    var avatars by remember { mutableStateOf(PhotoAvatarStore.listAvatars(context)) }
    var profile by remember { mutableStateOf(UserProfileStore.load(context)) }

    // Dialogs & Status
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var diagnosticResult by remember { mutableStateOf<String?>(null) }
    var isRunningDiagnostic by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val saved = PhotoAvatarStore.saveAvatarFromUri(
                context = context,
                sourceUri = it,
                name = "Daylight Photo (${avatars.size + 1})",
                lighting = AvatarLighting.DAYLIGHT,
                skinHex = SkinProfileRepository.load(context)?.skinHex,
            )
            if (saved != null) {
                avatars = PhotoAvatarStore.listAvatars(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        // Hero Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("My Wardrobe & Closet", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DrapeCoral.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("${outfits.size + suitedColors.size + wishlist.size} SAVED", style = MaterialTheme.typography.labelSmall, color = DrapeCoral, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your centralized lookbook, suited colors, and personal body profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Category Filter Chips Row (Clean vertical filter with spring bounce)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WardrobeFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                val scale by animateFloatAsState(if (isSelected) 1.04f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text("${filter.icon} ${filter.title}") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.scale(scale),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrapeCoral.copy(alpha = 0.15f),
                        selectedLabelColor = DrapeCoral,
                    ),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // SECTION 1: Hanger Outfits (Generated AI Looks)
        if (selectedFilter == WardrobeFilter.ALL || selectedFilter == WardrobeFilter.OUTFITS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🪝 OUTFITS ON THE CLOSET RACK", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                Text("${outfits.size} saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))

            if (outfits.isEmpty()) {
                if (selectedFilter == WardrobeFilter.OUTFITS) {
                    EmptyCard("No outfits saved yet", "Generate any look in the Try-On tab to hang outfits in your wardrobe.")
                }
            } else {
                outfits.forEach { out ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🪝", style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(8.dp))
                                    Text(out.title, style = MaterialTheme.typography.titleLarge)
                                }
                                TextButton(onClick = {
                                    WardrobeRepository.removeOutfit(context, out.id)
                                    outfits = WardrobeRepository.listOutfits(context)
                                }) {
                                    Text("✕", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Topwear: ${out.topwearCut} (${out.fabricName}) • Bottomwear: ${out.bottomwearCut}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onNavigateToShop(
                                        out.fabricName,
                                        "Matching Tone",
                                        out.colorHex,
                                        out.topwearCut,
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Find Where to Buy This Look  ↗", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }

        // SECTION 2: Suited Colors & Fabric Swatches
        if (selectedFilter == WardrobeFilter.ALL || selectedFilter == WardrobeFilter.COLORS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎨 SUITED COLOR & FABRIC PALETTE", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                Text("${suitedColors.size} swatches", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))

            if (suitedColors.isEmpty()) {
                if (selectedFilter == WardrobeFilter.COLORS) {
                    EmptyCard("No suited colors saved", "Star any color in the Studio tab to save it to your personal suited palette.")
                }
            } else {
                suitedColors.forEach { sc ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(sc.colorHex.asComposeColor()),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sc.colorName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${sc.fabricName ?: "All Fabrics"} • ${sc.contrastLabel} (${sc.matchScorePercent}% Match)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Moss,
                                )
                            }
                            Row {
                                TextButton(onClick = {
                                    onNavigateToShop(
                                        sc.fabricName ?: "Cotton",
                                        sc.colorName,
                                        sc.colorHex,
                                        "Relaxed Fit",
                                    )
                                }) {
                                    Text("Shop 🛒")
                                }
                                TextButton(onClick = {
                                    SuitedColorsRepository.remove(context, sc.id)
                                    suitedColors = SuitedColorsRepository.list(context)
                                }) {
                                    Text("✕", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }

        // SECTION 3: Saved Shopping Wishlist
        if (selectedFilter == WardrobeFilter.ALL || selectedFilter == WardrobeFilter.WISHLIST) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🛍️ SAVED SHOPPING WISHLIST", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                Text("${wishlist.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(8.dp))

            if (wishlist.isEmpty()) {
                if (selectedFilter == WardrobeFilter.WISHLIST) {
                    EmptyCard("Wishlist is empty", "Tap 'Wishlist' on any item in the Shop tab to save products here.")
                }
            } else {
                wishlist.forEach { wish ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(wish.retailer, style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                                Text(wish.price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(wish.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wish.shoppingUrl))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                                ) {
                                    Text("Open Store Page ↗", color = Color.White)
                                }
                                TextButton(onClick = {
                                    WardrobeRepository.removeWishlistItem(context, wish.id)
                                    wishlist = WardrobeRepository.listWishlist(context)
                                }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }

        // SECTION 4: Body Silhouette & Avatars Profile
        if (selectedFilter == WardrobeFilter.ALL || selectedFilter == WardrobeFilter.PROFILE) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("👤 MY BODY SILHOUETTE & PROPORTIONS", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                    Spacer(Modifier.height(10.dp))

                    Text("Shopper Name: ${profile.name}", style = MaterialTheme.typography.titleMedium)
                    Text("Height: ${profile.heightFormatted} (${profile.heightCategory.displayName})", style = MaterialTheme.typography.bodyMedium)
                    Text("Silhouette: ${profile.bodyShape.displayName} (${profile.bodyShape.description})", style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(14.dp))
                    Text("Quick Edit Shape:", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BodyShape.values().forEach { sh ->
                            FilterChip(
                                selected = profile.bodyShape == sh,
                                onClick = {
                                    profile = profile.copy(bodyShape = sh)
                                    UserProfileStore.save(context, profile)
                                },
                                label = { Text("${sh.icon} ${sh.displayName}") },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Photo Avatars Gallery
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("SAVED PHOTO AVATARS", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                        TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                            Text("➕ Add Photo")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (avatars.isEmpty()) {
                        Text("No custom photos saved. Tap '+ Add Photo' to save a daylight or indoor photo.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        avatars.forEach { av ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(av.lighting.icon, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(av.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(av.lighting.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                                TextButton(onClick = {
                                    PhotoAvatarStore.deleteAvatar(context, av.id)
                                    avatars = PhotoAvatarStore.listAvatars(context)
                                }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Privacy & Cloud Diagnostics Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("PRIVACY & DATA CONTROLS", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            isRunningDiagnostic = true
                            scope.launch {
                                val start = System.currentTimeMillis()
                                val res = withContext(Dispatchers.IO) { runCatching { api.health() } }
                                val elapsed = System.currentTimeMillis() - start
                                isRunningDiagnostic = false
                                diagnosticResult = res.fold(
                                    onSuccess = { h -> "Ping: ${elapsed}ms • Cloud Server: OK • VTO: ${h.vtoProvider}" },
                                    onFailure = { err -> "Diagnostic: ${err.localizedMessage ?: "Offline"}" },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (isRunningDiagnostic) "Pinging Server…" else "Check Cloud Server Health ⚡")
                    }

                    diagnosticResult?.let { diag ->
                        Spacer(Modifier.height(8.dp))
                        Text(diag, style = MaterialTheme.typography.bodySmall, color = Moss)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                    ) {
                        Text("Delete All Local Data", color = Color.White)
                    }
                }
            }
        }

        // Delete Confirmation Alert
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete All Local Data?") },
                text = { Text("This will erase all saved avatars, suited colors, lookbooks, and profile data from this device.") },
                confirmButton = {
                    Button(
                        onClick = {
                            PhotoAvatarStore.deleteAll(context)
                            SuitedColorsRepository.clear(context)
                            WardrobeRepository.clear(context)
                            UserProfileStore.clear(context)
                            SkinProfileRepository.clear(context)
                            DrapeRecordRepository.deleteAll(context)
                            com.drapeproof.mobile.data.DrapeSnapRepository.list(context).forEach { com.drapeproof.mobile.data.DrapeSnapRepository.delete(context, it.id) }
                            YouCamLabStore.deleteAllLocalData(context)

                            avatars = emptyList()
                            suitedColors = emptyList()
                            outfits = emptyList()
                            wishlist = emptyList()
                            profile = UserProfileStore.load(context)
                            showDeleteConfirm = false
                            statusMessage = "All local data successfully erased."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                    ) { Text("Confirm Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                },
            )
        }

        statusMessage?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, color = Moss)
        }

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun EmptyCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📭", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
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
