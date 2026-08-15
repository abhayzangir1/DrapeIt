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

// Minimal Luxury Tokens (Clean, Crisp, Airy)
val EditorialInk = Color(0xFF111111)
val EditorialWarmBlack = Color(0xFF1F1F1F)
val EditorialCream = Color(0xFFFCFCFC) // Pure, airy, minimalist off-white canvas
val EditorialSand = Color(0xFFF5F5F4)  // Neutral stone surface variant
val EditorialStone = Color(0xFFE7E5E4) // Delicate hairline border
val EditorialMuted = Color(0xFF78716C) // Neutral stone subtext
val EditorialSienna = Color(0xFF7A1C30) // Subtle couture wine accent from Logo
val EditorialGold = Color(0xFFB48B57)   // Subtle champagne gold
val EditorialPositive = Color(0xFF15803D)
val EditorialWarning = Color(0xFFB45309)
val EditorialNegative = Color(0xFFBE123C)

// Backward-compatibility aliases
val LuxuryCanvas = EditorialCream
val LuxurySurface = Color(0xFFFFFFFF)
val LuxuryTextPrimary = EditorialInk
val LuxuryTextSecondary = EditorialMuted
val LuxuryBorder = EditorialStone.copy(alpha = 0.50f)

val DrapeCoral = EditorialSienna
val GoldAccent = EditorialGold
val Moss = EditorialPositive
val ClashingRed = EditorialNegative
val Cobalt = Color(0xFF1E3A8A)
val Plum = Color(0xFF7A1C30)
val Ink = EditorialInk
val Canvas = EditorialCream
val Paper = LuxurySurface
val QuietGray = EditorialMuted

private val EditorialLuxuryColorScheme = lightColorScheme(
    primary = EditorialInk,
    onPrimary = Color.White,
    secondary = EditorialSienna,
    onSecondary = Color.White,
    tertiary = EditorialPositive,
    background = EditorialCream,
    onBackground = EditorialInk,
    surface = Color.White,
    onSurface = EditorialInk,
    surfaceVariant = EditorialSand,
    onSurfaceVariant = EditorialMuted,
    outline = EditorialStone,
    outlineVariant = EditorialStone.copy(alpha = 0.50f),
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
