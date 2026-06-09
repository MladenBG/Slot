package com.magics.slot.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MagicsColorScheme = darkColorScheme(
    primary             = NeonCyan,
    onPrimary           = DeepSpace,
    primaryContainer    = NeonCyanDim,
    onPrimaryContainer  = NeonCyan,
    secondary           = NeonMagenta,
    onSecondary         = DeepSpace,
    secondaryContainer  = NeonMagDim,
    onSecondaryContainer= NeonMagenta,
    tertiary            = NeonGold,
    onTertiary          = DeepSpace,
    tertiaryContainer   = NeonGoldDim,
    onTertiaryContainer = NeonGold,
    background          = DeepSpace,
    onBackground        = TextPrimary,
    surface             = SurfaceDark,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceCard,
    onSurfaceVariant    = TextSec,
    outline             = BorderNeon,
    outlineVariant      = BorderGlow,
    error               = NeonRed,
    onError             = Color.White,
)

@Composable
fun MagicsSlotTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = DeepSpace.toArgb()
            window.navigationBarColor = DeepSpace.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = MagicsColorScheme, typography = Typography, content = content)
}
