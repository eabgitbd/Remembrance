package com.codetoday.remembrance.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PremiumDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = MintAccent,
    tertiary = SageMuted,
    background = ObsidianBackground,
    surface = SurfaceGlassDark,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = TextLight,
    onSurface = TextLight
)

@Composable
fun RemembranceTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBackground.toArgb()
                window.navigationBarColor = ObsidianBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        content = content
    )
}
