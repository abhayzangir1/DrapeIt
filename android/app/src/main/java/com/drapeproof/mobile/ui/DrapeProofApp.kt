package com.drapeproof.mobile.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.camera.DrapeCaptureScreen
import com.drapeproof.mobile.compare.CompareScreen
import com.drapeproof.mobile.explore.ExploreScreen
import com.drapeproof.mobile.looks.LooksScreen
import com.drapeproof.mobile.onboarding.OnboardingLoginScreen
import com.drapeproof.mobile.profile.ProfileScreen
import com.drapeproof.mobile.silhouette.UserProfileStore
import com.drapeproof.mobile.tryon.TryOnScreen
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.youcam.YouCamLabScreen

enum class AppTab(val title: String, val icon: String) {
    EXPLORE("Explore", "🔍"),
    DRAPE("Drape", "🪞"),
    LOOKS("Looks", "✨"),
    PROFILE("Profile", "👤"),
}

private enum class SubFlow {
    NONE,
    COMPARE,
    TRY_ON,
    YOUCAM_LAB,
}

@Composable
fun DrapeProofApp(
    sharedImageUri: Uri?,
    onSharedImageConsumed: () -> Unit,
) {
    val context = LocalContext.current
    var isOnboarded by remember { mutableStateOf(UserProfileStore.isOnboarded(context)) }
    var currentTab by remember { mutableStateOf(AppTab.EXPLORE) }
    var subFlow by remember { mutableStateOf(SubFlow.NONE) }

    // Inter-screen parameters for Drape
    var drapeInitialFabricId by remember { mutableStateOf<String?>("silk") }
    var drapeInitialColorHex by remember { mutableStateOf<String?>("#831843") }

    // Inter-screen styling parameters for Try-On
    var tryOnFabricId by remember { mutableStateOf("silk") }
    var tryOnColorHex by remember { mutableStateOf("#831843") }
    var tryOnGarmentUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            tryOnGarmentUri = sharedImageUri
            currentTab = AppTab.LOOKS
            subFlow = SubFlow.TRY_ON
            onSharedImageConsumed()
        }
    }

    if (!isOnboarded) {
        OnboardingLoginScreen(onComplete = { isOnboarded = true })
        return
    }

    BackHandler(enabled = subFlow != SubFlow.NONE || currentTab != AppTab.EXPLORE) {
        if (subFlow != SubFlow.NONE) {
            subFlow = SubFlow.NONE
        } else {
            currentTab = AppTab.EXPLORE
        }
    }

    Scaffold(
        bottomBar = {
            if (subFlow == SubFlow.NONE) {
                NavigationBar(
                    containerColor = EditorialSand.copy(alpha = 0.85f),
                    tonalElevation = 6.dp,
                ) {
                    AppTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Text(tab.icon, fontSize = 20.sp)
                            },
                            label = {
                                Text(
                                    tab.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) EditorialSienna else EditorialMuted,
                                    fontSize = 11.sp,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = EditorialCream,
                                selectedIconColor = EditorialSienna,
                                unselectedIconColor = EditorialMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (subFlow) {
                SubFlow.COMPARE -> {
                    CompareScreen(
                        onBack = { subFlow = SubFlow.NONE },
                        onSelectLookForTryOn = { fabricId, colorHex ->
                            tryOnFabricId = fabricId
                            tryOnColorHex = colorHex
                            tryOnGarmentUri = null
                            subFlow = SubFlow.TRY_ON
                        },
                    )
                }

                SubFlow.TRY_ON -> {
                    TryOnScreen(
                        initialFabricId = tryOnFabricId,
                        initialColorHex = tryOnColorHex,
                        initialCutName = "Relaxed Tailored",
                        initialGarmentUri = tryOnGarmentUri,
                        onNavigateToShop = { _, _, _, _ ->
                            subFlow = SubFlow.NONE
                            currentTab = AppTab.LOOKS
                        },
                    )
                }

                SubFlow.YOUCAM_LAB -> {
                    YouCamLabScreen(
                        onBack = { subFlow = SubFlow.NONE },
                    )
                }

                SubFlow.NONE -> {
                    when (currentTab) {
                        AppTab.EXPLORE -> {
                            ExploreScreen(
                                onNavigateToDrape = { fabricId, colorHex ->
                                    drapeInitialFabricId = fabricId
                                    drapeInitialColorHex = colorHex
                                    currentTab = AppTab.DRAPE
                                },
                                onNavigateToTryOn = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    tryOnGarmentUri = null
                                    subFlow = SubFlow.TRY_ON
                                },
                                onNavigateToProfile = {
                                    currentTab = AppTab.PROFILE
                                },
                            )
                        }

                        AppTab.DRAPE -> {
                            DrapeCaptureScreen(
                                initialFabricId = drapeInitialFabricId,
                                initialColorHex = drapeInitialColorHex,
                                onBack = { currentTab = AppTab.EXPLORE },
                                onNavigateToCompare = { subFlow = SubFlow.COMPARE },
                                onNavigateToTryOn = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    tryOnGarmentUri = null
                                    subFlow = SubFlow.TRY_ON
                                },
                            )
                        }

                        AppTab.LOOKS -> {
                            LooksScreen(
                                onNavigateToTryOn = { fabricId, colorHex, garmentUri ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    tryOnGarmentUri = garmentUri
                                    subFlow = SubFlow.TRY_ON
                                },
                                onNavigateToDrape = { fabricId, colorHex ->
                                    drapeInitialFabricId = fabricId
                                    drapeInitialColorHex = colorHex
                                    currentTab = AppTab.DRAPE
                                },
                                onNavigateToCompare = {
                                    subFlow = SubFlow.COMPARE
                                },
                            )
                        }

                        AppTab.PROFILE -> {
                            ProfileScreen(
                                onRecalibrate = { currentTab = AppTab.DRAPE },
                                onOpenYouCamLab = { subFlow = SubFlow.YOUCAM_LAB },
                            )
                        }
                    }
                }
            }
        }
    }
}
