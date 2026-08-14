package com.drapeproof.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Editorial Warm Luxury Tokens (from DrapeIt Design System)
val EditorialInk = Color(0xFF181512)
val EditorialWarmBlack = Color(0xFF26211C)
val EditorialCream = Color(0xFFF7F2EA)
val EditorialSand = Color(0xFFE8DED0)
val EditorialStone = Color(0xFFC9BBAA)
val EditorialMuted = Color(0xFF8B7E70)
val EditorialSienna = Color(0xFF8F5945)
val EditorialPositive = Color(0xFF3F765A)
val EditorialWarning = Color(0xFFB07C31)
val EditorialNegative = Color(0xFF9B554A)

// Backward-compatibility aliases
val LuxuryCanvas = EditorialCream
val LuxurySurface = Color(0xFFFFFFFF)
val LuxuryTextPrimary = EditorialInk
val LuxuryTextSecondary = EditorialMuted
val LuxuryBorder = EditorialStone.copy(alpha = 0.50f)

val DrapeCoral = EditorialSienna
val GoldAccent = EditorialWarning
val Moss = EditorialPositive
val ClashingRed = EditorialNegative
val Cobalt = Color(0xFF2563EB)
val Plum = Color(0xFF7C3AED)
val Ink = EditorialInk
val Canvas = EditorialCream
val Paper = LuxurySurface
val QuietGray = EditorialMuted

private val EditorialLuxuryColorScheme = lightColorScheme(
    primary = EditorialInk,
    onPrimary = EditorialCream,
    secondary = EditorialSienna,
    onSecondary = Color.White,
    tertiary = EditorialPositive,
    background = EditorialCream,
    onBackground = EditorialInk,
    surface = Color.White,
    onSurface = EditorialInk,
    surfaceVariant = EditorialSand.copy(alpha = 0.55f),
    onSurfaceVariant = EditorialMuted,
    outline = EditorialStone.copy(alpha = 0.60f),
    outlineVariant = EditorialSand.copy(alpha = 0.40f),
)

@Composable
fun DrapeProofTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = EditorialLuxuryColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = DrapeTypography, content = content)
}
