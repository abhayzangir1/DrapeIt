package com.drapeproof.mobile.shop

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SavedWishlistItem
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.GoldAccent
import com.drapeproof.mobile.ui.theme.Moss

private enum class ShopMode {
    SHOP_SUITED_COLORS,
    SEARCH_BY_IMAGE,
}

@Composable
fun ShopScreen(
    initialFabric: String? = null,
    initialColorName: String? = null,
    initialColorHex: String? = null,
    initialCut: String? = null,
) {
    val context = LocalContext.current
    var shopMode by remember { mutableStateOf(ShopMode.SHOP_SUITED_COLORS) }
    val suitedColors = remember { SuitedColorsRepository.list(context) }
    val profile = SkinProfileRepository.load(context)
    val fallbackColor = profile?.bestColors?.firstOrNull()
    val primaryColorName = "Your Best Color"
    val primaryColorHex = fallbackColor ?: profile?.skinHex ?: "#B85F45"

    var selectedColorName by remember { mutableStateOf(initialColorName ?: suitedColors.firstOrNull()?.colorName ?: primaryColorName) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex ?: suitedColors.firstOrNull()?.colorHex ?: primaryColorHex) }
    var selectedFabricName by remember { mutableStateOf(initialFabric ?: suitedColors.firstOrNull()?.fabricName ?: "Organic Cotton") }
    var selectedCutName by remember { mutableStateOf(initialCut ?: "Relaxed Casual") }

    var uploadedImageName by remember { mutableStateOf<String?>(null) }
    var wishlist by remember { mutableStateOf(WardrobeRepository.listWishlist(context)) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadedImageName = "Uploaded Garment Photo"
            shopMode = ShopMode.SEARCH_BY_IMAGE
        }
    }

    val products = remember(shopMode, selectedFabricName, selectedColorName, selectedColorHex, selectedCutName, uploadedImageName) {
        if (shopMode == ShopMode.SHOP_SUITED_COLORS) {
            ShoppingSearchEngine.generateStyleSuggestions(
                fabricName = selectedFabricName,
                colorName = selectedColorName,
                colorHex = selectedColorHex,
                cutName = selectedCutName,
            )
        } else {
            ShoppingSearchEngine.generateVisualSearchProducts(uploadedImageName ?: "Designer Apparel")
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
                    Text("Smart Shopping Hub", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("4 STORES LIVE", style = MaterialTheme.typography.labelSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Find and buy exact or matching clothes in your most flattering colors and cuts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2-Choice Action Cards with Scale Feedback
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val isSuited = shopMode == ShopMode.SHOP_SUITED_COLORS
            val scaleSuited by animateFloatAsState(if (isSuited) 1.02f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

            Card(
                onClick = { shopMode = ShopMode.SHOP_SUITED_COLORS },
                modifier = Modifier
                    .weight(1f)
                    .scale(scaleSuited),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSuited) 4.dp else 2.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("★", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Shop My Style",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSuited) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "$selectedFabricName • $selectedColorName",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuited) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
            }

            val isVisual = shopMode == ShopMode.SEARCH_BY_IMAGE
            val scaleVisual by animateFloatAsState(if (isVisual) 1.02f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

            Card(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .scale(scaleVisual),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVisual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isVisual) 4.dp else 2.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("🔍", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Find by Cloth Image",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isVisual) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Upload any dress photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isVisual) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Product Cards Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (shopMode == ShopMode.SHOP_SUITED_COLORS) "CURATED IN $selectedColorName ($selectedFabricName)" else "MATCHED MARKETPLACE RESULTS",
                style = MaterialTheme.typography.labelSmall,
                color = DrapeCoral,
            )
            Text("${products.size} Stores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        Spacer(Modifier.height(10.dp))

        // Product Cards List
        products.forEach { prod ->
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Moss.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(prod.tag, style = MaterialTheme.typography.labelSmall, color = Moss, fontWeight = FontWeight.Bold)
                        }
                        Text(prod.priceEstimate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(prod.title, style = MaterialTheme.typography.titleLarge)
                    Text(prod.retailer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Direct Buy Link
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(prod.shoppingUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                        ) {
                            Text("Open in Store  ↗", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }

                        // Wishlist Button
                        val isSaved = wishlist.any { it.shoppingUrl == prod.shoppingUrl }
                        OutlinedButton(
                            onClick = {
                                if (!isSaved) {
                                    WardrobeRepository.addWishlistItem(
                                        context,
                                        SavedWishlistItem(
                                            title = prod.title,
                                            retailer = prod.retailer,
                                            price = prod.priceEstimate,
                                            fabricName = prod.fabric,
                                            colorHex = prod.colorHex,
                                            shoppingUrl = prod.shoppingUrl,
                                        ),
                                    )
                                    wishlist = WardrobeRepository.listWishlist(context)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(if (isSaved) "★ Saved" else "☆ Wishlist")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}
