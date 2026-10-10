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

fun Modifier.glassmorphic(
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = GlassSurfaceDark.copy(alpha = 0.65f),
    borderColor: Color = GlassBorderDark,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.80f),
                    backgroundColor.copy(alpha = 0.55f)
                )
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = 0.40f),
                    borderColor.copy(alpha = 0.10f)
                )
            ),
            shape = shape
        )
}

fun Modifier.squircleOverlay(
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color = LimeGreenOverlay,
    borderColor: Color = PureBlack,
    borderWidth: Dp = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
}
