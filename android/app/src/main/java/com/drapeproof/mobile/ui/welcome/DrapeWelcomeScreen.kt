package com.drapeproof.mobile.ui.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.R
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import kotlinx.coroutines.delay

@Composable
fun DrapeWelcomeScreen(
    onWelcomeFinished: () -> Unit,
) {
    val scaleAnim = remember { Animatable(1.05f) }
    val contentAlpha = remember { Animatable(0f) }
    val whiteTransitionAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmerTransition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    LaunchedEffect(Unit) {
        // Fade in content & gentle zoom out across 3.4 seconds
        contentAlpha.animateTo(1f, tween(600, easing = LinearEasing))
        scaleAnim.animateTo(1.00f, tween(3000, easing = FastOutSlowInEasing))

        // Wait to complete 3.4 seconds total
        delay(400)

        // White out transition
        whiteTransitionAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        onWelcomeFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                // Allow instant tap to transition
                onWelcomeFinished()
            },
        contentAlignment = Alignment.Center,
    ) {
        // FULL-SCREEN WELCOME SPLASH IMAGE
        Image(
            painter = painterResource(id = R.drawable.welcome_splash),
            contentDescription = "Drape It Welcome",
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleAnim.value)
                .alpha(contentAlpha.value),
            contentScale = ContentScale.Crop,
        )

        // SUBTLE COUTURE TAGLINE AT THE BOTTOM
        Text(
            text = "AI Photorealistic Colorimetry & Virtual Drape",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color = EditorialInk.copy(alpha = 0.70f * pulseGlow),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(contentAlpha.value),
        )

        // WHITE DISSOLVE TRANSITION LAYER
        if (whiteTransitionAlpha.value > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = whiteTransitionAlpha.value)),
            )
        }
    }
}
