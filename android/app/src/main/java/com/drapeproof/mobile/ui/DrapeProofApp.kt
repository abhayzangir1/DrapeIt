package com.drapeproof.mobile.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.camera.DrapeCaptureScreen
import com.drapeproof.mobile.compare.CompareScreen
import com.drapeproof.mobile.data.TutorialRepository
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
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.tutorial.DrapeTutorialModal
import com.drapeproof.mobile.ui.welcome.DrapeWelcomeScreen
import com.drapeproof.mobile.youcam.YouCamLabScreen

enum class AppTab(val title: String, val icon: String) {
    EXPLORE("Explore", "🔍"),
    DRAPE("Drape", "🪞"),
    TRY_ON("Try-On", "👗"),
    LOOKS("Looks", "🖼️"),
    PROFILE("Profile", "👤"),
}

private enum class SubFlow {
    NONE,
    COMPARE,
    YOUCAM_LAB,
}

@Composable
fun DrapeProofApp(
    sharedImageUri: Uri?,
    onSharedImageConsumed: () -> Unit,
) {
    val context = LocalContext.current
    var showWelcome by remember { mutableStateOf(true) }
    val mainEntranceWhiteAlpha = remember { Animatable(1f) }

    var isOnboarded by remember { mutableStateOf(UserProfileStore.isOnboarded(context)) }
    var currentTab by remember { mutableStateOf(AppTab.EXPLORE) }
    var subFlow by remember { mutableStateOf(SubFlow.NONE) }
    var showTutorial by remember { mutableStateOf(!TutorialRepository.isTutorialCompleted(context)) }

    // Inter-screen parameters for Drape
    var drapeInitialFabricId by remember { mutableStateOf<String?>("silk") }
    var drapeInitialColorHex by remember { mutableStateOf<String?>("#831843") }

    // Inter-screen styling parameters for Try-On
    var tryOnFabricId by remember { mutableStateOf("silk") }
    var tryOnColorHex by remember { mutableStateOf("#831843") }
    var tryOnGarmentUri by remember { mutableStateOf<Uri?>(null) }

    // Inter-screen parameters for Compare
    var compareSelectedIds by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            tryOnGarmentUri = sharedImageUri
            currentTab = AppTab.TRY_ON
            onSharedImageConsumed()
        }
    }

    if (showWelcome) {
        DrapeWelcomeScreen(
            onWelcomeFinished = {
                showWelcome = false
            },
        )
        return
    }

    LaunchedEffect(showWelcome) {
        if (!showWelcome) {
            mainEntranceWhiteAlpha.snapTo(1f)
            mainEntranceWhiteAlpha.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
    }

    if (!isOnboarded) {
        OnboardingLoginScreen(onComplete = { isOnboarded = true })
        return
    }

    BackHandler(enabled = subFlow != SubFlow.NONE || currentTab != AppTab.EXPLORE) {
        if (subFlow != SubFlow.NONE) {
            subFlow = SubFlow.NONE
            compareSelectedIds = emptyList()
        } else {
            currentTab = AppTab.EXPLORE
        }
    }

    val glossyMaroonBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFA82643), // Lustrous silky top highlight
                Color(0xFF7A1C30), // Rich couture maroon
                Color(0xFF50101D), // Deep velvet maroon base
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (subFlow == SubFlow.NONE) {
                    Surface(
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.75.dp,
                                color = EditorialStone.copy(alpha = 0.60f),
                                shape = androidx.compose.ui.graphics.RectangleShape,
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                val tabScale by animateFloatAsState(
                                    targetValue = if (selected) 1.18f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                                    label = "TabZoom_${tab.name}",
                                )

                                Box(
                                    modifier = Modifier
                                        .scale(tabScale)
                                        .clip(RoundedCornerShape(14.dp))
                                        .then(
                                            if (selected) {
                                                Modifier
                                                    .background(glossyMaroonBrush)
                                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            } else {
                                                Modifier
                                                    .clickable { currentTab = tab }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            tab.icon,
                                            fontSize = if (selected) 18.sp else 19.sp,
                                        )
                                        if (selected) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                tab.title,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }
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
                            initialSelectedIds = compareSelectedIds,
                            onBack = {
                                subFlow = SubFlow.NONE
                                compareSelectedIds = emptyList()
                            },
                            onSelectLookForTryOn = { fabricId, colorHex ->
                                tryOnFabricId = fabricId
                                tryOnColorHex = colorHex
                                tryOnGarmentUri = null
                                subFlow = SubFlow.NONE
                                currentTab = AppTab.TRY_ON
                                compareSelectedIds = emptyList()
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
                                        currentTab = AppTab.TRY_ON
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
                                    onNavigateToTryOn = { fabricId, colorHex ->
                                        tryOnFabricId = fabricId
                                        tryOnColorHex = colorHex
                                        tryOnGarmentUri = null
                                        currentTab = AppTab.TRY_ON
                                    },
                                )
                            }

                            AppTab.TRY_ON -> {
                                TryOnScreen(
                                    initialFabricId = tryOnFabricId,
                                    initialColorHex = tryOnColorHex,
                                    initialGarmentUri = tryOnGarmentUri,
                                    onNavigateToLooks = {
                                        currentTab = AppTab.LOOKS
                                    },
                                    onNavigateToShop = { _, _, _, _ ->
                                        currentTab = AppTab.LOOKS
                                    },
                                )
                            }

                            AppTab.LOOKS -> {
                                LooksScreen(
                                    onNavigateToTryOn = { fabricId, colorHex, garmentUri ->
                                        tryOnFabricId = fabricId
                                        tryOnColorHex = colorHex
                                        tryOnGarmentUri = garmentUri
                                        currentTab = AppTab.TRY_ON
                                    },
                                    onNavigateToDrape = { fabricId, colorHex ->
                                        drapeInitialFabricId = fabricId
                                        drapeInitialColorHex = colorHex
                                        currentTab = AppTab.DRAPE
                                    },
                                    onNavigateToCompare = { selectedIds ->
                                        compareSelectedIds = selectedIds
                                        subFlow = SubFlow.COMPARE
                                    },
                                )
                            }

                            AppTab.PROFILE -> {
                                ProfileScreen(
                                    onRecalibrate = { currentTab = AppTab.DRAPE },
                                    onOpenYouCamLab = { subFlow = SubFlow.YOUCAM_LAB },
                                    onOpenTutorial = { showTutorial = true },
                                )
                            }
                        }
                    }
                }
            }
        }

        // WHITE DISSOLVE ENTRANCE TRANSITION OVERLAY
        if (mainEntranceWhiteAlpha.value > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = mainEntranceWhiteAlpha.value)),
            )
        }

        // FIRST TIME COMPLETE APP TUTORIAL MODAL
        if (showTutorial) {
            DrapeTutorialModal(
                onDismiss = {
                    showTutorial = false
                    TutorialRepository.setTutorialCompleted(context, true)
                },
            )
        }
    }
}
