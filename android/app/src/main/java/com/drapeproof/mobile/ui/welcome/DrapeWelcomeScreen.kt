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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.R
import com.drapeproof.mobile.ui.theme.EditorialInk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DrapeWelcomeScreen(
    onWelcomeFinished: () -> Unit,
) {
    val scaleAnim = remember { Animatable(1.06f) }
    val contentAlpha = remember { Animatable(0f) }
    val whiteTransitionAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmerTransition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    LaunchedEffect(Unit) {
        // Guaranteed 3.2s luxury brand presentation (non-skippable on touch)
        launch {
            contentAlpha.animateTo(1f, tween(500, easing = LinearEasing))
        }
        launch {
            scaleAnim.animateTo(1.00f, tween(3200, easing = FastOutSlowInEasing))
        }

        // Wait a guaranteed 3.2 seconds
        delay(3200)

        // Smooth white dissolve transition
        whiteTransitionAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        onWelcomeFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
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
