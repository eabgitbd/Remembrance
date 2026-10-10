package com.codetoday.remembrance.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.premiumGlassmorphic(
    cornerRadius: Dp = 22.dp,
    backgroundColor: Color = SurfaceGlassDark.copy(alpha = 0.75f),
    borderColor: Color = SurfaceGlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.85f),
                    backgroundColor.copy(alpha = 0.65f)
                )
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    SurfaceGlassHighlight,
                    borderColor.copy(alpha = 0.12f)
                )
            ),
            shape = shape
        )
}

fun Modifier.squircleOverlay(
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color = LimeOverlayDefault,
    borderColor: Color = PureBlack,
    borderWidth: Dp = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
}
