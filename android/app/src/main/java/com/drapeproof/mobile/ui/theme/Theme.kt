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
import androidx.compose.ui.platform.LocalView

val Ink = Color(0xFF181817)
val Canvas = Color(0xFFF5F1E9)
val Paper = Color(0xFFFFFCF5)
val DrapeCoral = Color(0xFFE6674D)
val Moss = Color(0xFF527466)
val Cobalt = Color(0xFF3559B7)
val Plum = Color(0xFF6F456D)
val QuietGray = Color(0xFF77736B)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = DrapeCoral,
    onSecondary = Paper,
    tertiary = Moss,
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    outline = Color(0xFFC8C2B7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2EDE3),
    onPrimary = Ink,
    secondary = Color(0xFFFF9B83),
    onSecondary = Ink,
    tertiary = Color(0xFFA8CDBC),
    background = Color(0xFF10100F),
    onBackground = Color(0xFFF2EDE3),
    surface = Color(0xFF1A1A18),
    onSurface = Color(0xFFF2EDE3),
    outline = Color(0xFF625F59),
)

@Composable
fun DrapeProofTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
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
