package com.drapeproof.mobile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.GoldAccent
import kotlin.math.sin

@Composable
fun FabricWaveDrapeView(
    modifier: Modifier = Modifier,
    primaryColor: Color = DrapeCoral,
    accentColor: Color = GoldAccent,
) {
    val transition = rememberInfiniteTransition(label = "SilkClothWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "WavePhase",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Layer 1: Deep fluid silk wave (Back layer)
        val path1 = Path().apply {
            moveTo(0f, height * 0.45f)
            val waveSegments = 30
            for (i in 0..waveSegments) {
                val x = (i.toFloat() / waveSegments) * width
                val angle = (i.toFloat() / waveSegments) * (2 * Math.PI).toFloat() + phase
                val y = (height * 0.45f) + (sin(angle.toDouble()) * (height * 0.15f)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = path1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    accentColor.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
            ),
        )

        // Layer 2: Shimmering Mulberry Silk Fold (Middle layer with reverse motion)
        val path2 = Path().apply {
            moveTo(0f, height * 0.52f)
            val waveSegments = 30
            for (i in 0..waveSegments) {
                val x = (i.toFloat() / waveSegments) * width
                val angle = (i.toFloat() / waveSegments) * (2.4 * Math.PI).toFloat() - phase * 0.8f
                val y = (height * 0.52f) + (sin(angle.toDouble()) * (height * 0.18f)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = path2,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.28f),
                    primaryColor.copy(alpha = 0.25f),
                    Color(0xFFE2B774).copy(alpha = 0.18f),
                ),
            ),
        )

        // Layer 3: Organic crisp drape crest (Foreground highlights)
        val path3 = Path().apply {
            moveTo(0f, height * 0.60f)
            val waveSegments = 30
            for (i in 0..waveSegments) {
                val x = (i.toFloat() / waveSegments) * width
                val angle = (i.toFloat() / waveSegments) * (3 * Math.PI).toFloat() + phase * 1.2f
                val y = (height * 0.60f) + (sin(angle.toDouble()) * (height * 0.12f)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = path3,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.35f),
                    primaryColor.copy(alpha = 0.05f),
                ),
            ),
        )
    }
}
