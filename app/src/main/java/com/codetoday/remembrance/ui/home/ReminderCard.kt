package com.codetoday.remembrance.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codetoday.remembrance.data.model.OverlayPosition
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.data.model.SupportedLanguage
import com.codetoday.remembrance.domain.typography.FontCatalog
import com.codetoday.remembrance.ui.components.SquirclePreview
import com.codetoday.remembrance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: ReminderItem,
    onUpdate: (ReminderItem) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFontDropdown by remember { mutableStateOf(false) }
    var expandedPositionDropdown by remember { mutableStateOf(false) }
    var showStyleCustomizer by remember { mutableStateOf(false) }

    val fontsList = remember(reminder.language) {
        FontCatalog.getFontsForLanguage(reminder.language)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glassmorphic(cornerRadius = 20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark.copy(alpha = 0.70f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Position badge and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LimeGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${reminder.language.displayName} • ${reminder.position.label}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = LimeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Reminder",
                        tint = AccentRed.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text input box (Unicode friendly)
            OutlinedTextField(
                value = reminder.text,
                onValueChange = { onUpdate(reminder.copy(text = it)) },
                label = { Text("Reminder Text (${reminder.language.displayName})") },
                placeholder = {
                    Text(
                        when (reminder.language) {
                            SupportedLanguage.BENGALI -> "যেমন: সুবহানাল্লাহ"
                            SupportedLanguage.ENGLISH -> "e.g., Stay mindful & grateful"
                            SupportedLanguage.ARABIC -> "مثال: سبحان الله وبحمده"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LimeGreen,
                    unfocusedBorderColor = GlassBorderDark,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Font selection dropdown
            ExposedDropdownMenuBox(
                expanded = expandedFontDropdown,
                onExpandedChange = { expandedFontDropdown = !expandedFontDropdown }
            ) {
                OutlinedTextField(
                    value = reminder.fontName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Font Style (Top 10 Google Fonts)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFontDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimeGreen,
                        unfocusedBorderColor = GlassBorderDark,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedFontDropdown,
                    onDismissRequest = { expandedFontDropdown = false }
                ) {
                    fontsList.forEach { fontName ->
                        DropdownMenuItem(
                            text = { Text(fontName) },
                            onClick = {
                                onUpdate(reminder.copy(fontName = fontName))
                                expandedFontDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration: Slider + Input Box (Seconds)
            Text(
                text = "Display Duration: ${reminder.durationSeconds} seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = reminder.durationSeconds.toFloat(),
                    onValueChange = { onUpdate(reminder.copy(durationSeconds = it.toInt())) },
                    valueRange = 1f..60f,
                    steps = 58,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = LimeGreen, activeTrackColor = LimeGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = reminder.durationSeconds.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull()?.coerceIn(1, 120) ?: 1
                        onUpdate(reminder.copy(durationSeconds = v))
                    },
                    modifier = Modifier.width(72.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frequency: Slider + Input Box (Minutes)
            Text(
                text = "Frequency (Interval): ${reminder.frequencyMinutes} minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = reminder.frequencyMinutes.toFloat(),
                    onValueChange = { onUpdate(reminder.copy(frequencyMinutes = it.toInt())) },
                    valueRange = 1f..180f,
                    steps = 178,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = LimeGreen, activeTrackColor = LimeGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = reminder.frequencyMinutes.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull()?.coerceIn(1, 360) ?: 1
                        onUpdate(reminder.copy(frequencyMinutes = v))
                    },
                    modifier = Modifier.width(72.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Position selector dropdown
            ExposedDropdownMenuBox(
                expanded = expandedPositionDropdown,
                onExpandedChange = { expandedPositionDropdown = !expandedPositionDropdown }
            ) {
                OutlinedTextField(
                    value = reminder.position.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Screen Position") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPositionDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LimeGreen,
                        unfocusedBorderColor = GlassBorderDark,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedPositionDropdown,
                    onDismissRequest = { expandedPositionDropdown = false }
                ) {
                    OverlayPosition.values().forEach { pos ->
                        DropdownMenuItem(
                            text = { Text(pos.label) },
                            onClick = {
                                onUpdate(reminder.copy(position = pos))
                                expandedPositionDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Preview of the Squircle overlay
            Text(
                text = "Live Squircle Preview:",
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite.copy(alpha = 0.6f)
            )
            SquirclePreview(reminder = reminder)

            // Customize style accordion toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showStyleCustomizer = !showStyleCustomizer }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showStyleCustomizer) "Hide Style Options" else "Customize Squircle Style",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LimeGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (showStyleCustomizer) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LimeGreen
                )
            }

            AnimatedVisibility(visible = showStyleCustomizer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Opacity slider
                    Text(
                        text = "Background Opacity: ${(reminder.backgroundOpacity * 100).toInt()}% (Default: 65%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.7f)
                    )
                    Slider(
                        value = reminder.backgroundOpacity,
                        onValueChange = { onUpdate(reminder.copy(backgroundOpacity = it)) },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = LimeGreen, activeTrackColor = LimeGreen)
                    )

                    // Corner radius slider (Squircle roundness)
                    Text(
                        text = "Squircle Corner Radius: ${reminder.cornerRadiusDp.toInt()} dp",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.7f)
                    )
                    Slider(
                        value = reminder.cornerRadiusDp,
                        onValueChange = { onUpdate(reminder.copy(cornerRadiusDp = it)) },
                        valueRange = 8f..40f,
                        colors = SliderDefaults.colors(thumbColor = LimeGreen, activeTrackColor = LimeGreen)
                    )

                    // Reset to default button
                    TextButton(
                        onClick = {
                            onUpdate(
                                reminder.copy(
                                    backgroundColorHex = "#32CD32",
                                    backgroundOpacity = 0.65f,
                                    borderColorHex = "#000000",
                                    borderWidthDp = 1.0f,
                                    cornerRadiusDp = 24.0f,
                                    textColorHex = "#000000",
                                    fontSizeSp = 18.0f
                                )
                            )
                        }
                    ) {
                        Text("Reset to Default (Lime Green 65% + Black Text)", color = PureWhite.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
