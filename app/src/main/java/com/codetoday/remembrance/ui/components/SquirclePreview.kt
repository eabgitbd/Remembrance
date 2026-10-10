package com.codetoday.remembrance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.domain.typography.FontCatalog

@Composable
fun SquirclePreview(
    reminder: ReminderItem,
    modifier: Modifier = Modifier
) {
    val backgroundColor = parseHexColor(reminder.backgroundColorHex, Color(0xFF32CD32))
        .copy(alpha = reminder.backgroundOpacity.coerceIn(0f, 1f))

    val borderColor = parseHexColor(reminder.borderColorHex, Color(0xFF000000))
    val textColor = parseHexColor(reminder.textColorHex, Color(0xFF000000))
    val fontFamily = FontCatalog.getFontFamily(reminder.fontName, reminder.language)

    val shape = RoundedCornerShape(reminder.cornerRadiusDp.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .shadow(8.dp, shape, clip = false)
                .clip(shape)
                .background(backgroundColor)
                .border(reminder.borderWidthDp.dp, borderColor, shape)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (reminder.text.isBlank()) "سُبْحَانَ اللَّهِ • সুবহানাল্লাহ • Peace" else reminder.text,
                fontFamily = fontFamily,
                fontSize = reminder.fontSizeSp.sp,
                lineHeight = (reminder.fontSizeSp * 1.45f).sp,
                color = textColor,
                textAlign = TextAlign.Center,
                softWrap = true
            )
        }
    }
}

fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val cleanHex = hex.replace("#", "")
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}
