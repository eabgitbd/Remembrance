package com.codetoday.remembrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val language: SupportedLanguage = SupportedLanguage.BENGALI,
    val fontName: String = "Noto Serif Bengali",
    val durationSeconds: Int = 5,       // display duration in seconds (1..120)
    val frequencyMinutes: Int = 10,     // display frequency interval in minutes (1..360)
    val position: OverlayPosition = OverlayPosition.TOP,
    
    // Squircle styling customization
    val backgroundColorHex: String = "#32CD32", // Lime green
    val backgroundOpacity: Float = 0.65f,       // 65% transparency
    val borderColorHex: String = "#000000",     // Pure black thin border
    val borderWidthDp: Float = 1.0f,            // Thin border
    val cornerRadiusDp: Float = 24.0f,          // Squircle corner curvature
    val textColorHex: String = "#000000",       // Pure black font
    val fontSizeSp: Float = 18.0f,
    
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
