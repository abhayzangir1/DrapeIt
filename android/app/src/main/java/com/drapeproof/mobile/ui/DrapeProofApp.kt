package com.drapeproof.mobile.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
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
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

enum class AppTab(val title: String, val icon: String) {
    DRAPE("Drape", "🪞"),
    EXPLORE("Explore", "🔍"),
    LOOKS("Looks", "✨"),
    PROFILE("Profile", "👤"),
}

private enum class SubFlow {
    NONE,
    COMPARE,
    TRY_ON,
}

@Composable
fun DrapeProofApp(
    sharedImageUri: Uri?,
    onSharedImageConsumed: () -> Unit,
) {
    val context = LocalContext.current
    var isOnboarded by remember { mutableStateOf(UserProfileStore.isOnboarded(context)) }
    var currentTab by remember { mutableStateOf(AppTab.DRAPE) }
    var subFlow by remember { mutableStateOf(SubFlow.NONE) }

    // Inter-screen styling parameters for Try-On
    var tryOnFabricId by remember { mutableStateOf("silk") }
    var tryOnColorHex by remember { mutableStateOf("#831843") }

    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            currentTab = AppTab.LOOKS
        }
    }

    if (!isOnboarded) {
        OnboardingLoginScreen(onComplete = { isOnboarded = true })
        return
    }

    BackHandler(enabled = subFlow != SubFlow.NONE || currentTab != AppTab.DRAPE) {
        if (subFlow != SubFlow.NONE) {
            subFlow = SubFlow.NONE
        } else {
            currentTab = AppTab.DRAPE
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
                            subFlow = SubFlow.TRY_ON
                        },
                    )
                }

                SubFlow.TRY_ON -> {
                    TryOnScreen(
                        initialFabricId = tryOnFabricId,
                        initialColorHex = tryOnColorHex,
                        initialCutName = "Relaxed Tailored",
                        onNavigateToShop = { _, _, _, _ ->
                            subFlow = SubFlow.NONE
                            currentTab = AppTab.LOOKS
                        },
                    )
                }

                SubFlow.NONE -> {
                    when (currentTab) {
                        AppTab.DRAPE -> {
                            DrapeCaptureScreen(
                                onBack = { currentTab = AppTab.EXPLORE },
                                onNavigateToCompare = { subFlow = SubFlow.COMPARE },
                                onNavigateToTryOn = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    subFlow = SubFlow.TRY_ON
                                },
                            )
                        }

                        AppTab.EXPLORE -> {
                            ExploreScreen(
                                onNavigateToDrape = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    currentTab = AppTab.DRAPE
                                },
                                onNavigateToTryOn = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    subFlow = SubFlow.TRY_ON
                                },
                            )
                        }

                        AppTab.LOOKS -> {
                            LooksScreen(
                                onNavigateToTryOn = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    subFlow = SubFlow.TRY_ON
                                },
                                onNavigateToDrape = { fabricId, colorHex ->
                                    tryOnFabricId = fabricId
                                    tryOnColorHex = colorHex
                                    currentTab = AppTab.DRAPE
                                },
                            )
                        }

                        AppTab.PROFILE -> {
                            ProfileScreen(
                                onRecalibrate = { currentTab = AppTab.DRAPE },
                            )
                        }
                    }
                }
            }
        }
    }
}
