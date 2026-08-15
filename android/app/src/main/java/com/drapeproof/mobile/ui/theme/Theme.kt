package com.drapeproof.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.drapeproof.mobile.data.AppSettingsRepository
import com.drapeproof.mobile.data.AppThemeMode

// Minimal Luxury Tokens (Clean, Crisp, Lighter Radiant Crimson & Warm Gold)
val EditorialInk = Color(0xFF111111)
val EditorialWarmBlack = Color(0xFF18181B)
val EditorialCream = Color(0xFFFCFCFC) // Pure, airy, minimalist off-white canvas
val EditorialSand = Color(0xFFF4F4F5)  // Neutral stone surface variant
val EditorialStone = Color(0xFFE4E4E7) // Delicate hairline border
val EditorialMuted = Color(0xFF71717A) // Neutral stone subtext
val EditorialSienna = Color(0xFFC23B5A) // Lighter radiant couture crimson
val EditorialGold = Color(0xFFE2C475)   // Luxury warm metallic gold
val EditorialPositive = Color(0xFF16A34A)
val EditorialWarning = Color(0xFFD97706)
val EditorialNegative = Color(0xFFE11D48)

// Dark Luxury Theme Palette — Aligned with Haute Couture Reference Design
val DarkCanvas = Color(0xFF09080C)          // Deep obsidian noir
val DarkSurface = Color(0xFF14131A)         // Velvety dark graphite card surface
val DarkSurfaceVariant = Color(0xFF1D1B24)  // Subdued secondary dark container
val DarkBorder = Color(0xFF2E2A38)          // Fine hairline charcoal border
val DarkTextPrimary = Color(0xFFF5F5F7)     // High-contrast clean ivory
val DarkTextSecondary = Color(0xFF9A96A2)   // Warm muted pewter subtext
val DarkGoldAccent = Color(0xFFE2C475)      // Champagne radiant gold badge
val DarkCrimsonGlow = Color(0xFFC23B5A)     // Glowing luxury crimson

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
val Cobalt = Color(0xFF2563EB)
val Plum = EditorialSienna
val Ink = EditorialInk
val Canvas = EditorialCream
val Paper = LuxurySurface
val QuietGray = EditorialMuted

private val EditorialLuxuryColorScheme = lightColorScheme(
    primary = EditorialInk,
    onPrimary = Color.White,
    secondary = EditorialSienna,
    onSecondary = Color.White,
    tertiary = EditorialGold,
    background = EditorialCream,
    onBackground = EditorialInk,
    surface = Color.White,
    onSurface = EditorialInk,
    surfaceVariant = EditorialSand,
    onSurfaceVariant = EditorialMuted,
    outline = EditorialStone,
    outlineVariant = EditorialStone.copy(alpha = 0.50f),
)

private val EditorialDarkLuxuryColorScheme = darkColorScheme(
    primary = DarkGoldAccent,
    onPrimary = Color.Black,
    secondary = DarkCrimsonGlow,
    onSecondary = Color.White,
    tertiary = DarkGoldAccent,
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.70f),
)

@Composable
fun DrapeProofTheme(
    themeMode: AppThemeMode? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val effectiveMode = themeMode ?: AppSettingsRepository.getThemeMode(context)
    val isDark = when (effectiveMode) {
        AppThemeMode.SYSTEM -> darkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colors = if (isDark) EditorialDarkLuxuryColorScheme else EditorialLuxuryColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = DrapeTypography, content = content)
}
