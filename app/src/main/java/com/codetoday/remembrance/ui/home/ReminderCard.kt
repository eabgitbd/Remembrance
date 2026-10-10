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
import com.codetoday.remembrance.domain.localization.AppStrings
import com.codetoday.remembrance.domain.typography.FontCatalog
import com.codetoday.remembrance.ui.components.SquirclePreview
import com.codetoday.remembrance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminder: ReminderItem,
    strings: AppStrings,
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
            .padding(vertical = 10.dp)
            .premiumGlassmorphic(cornerRadius = 22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Language badge, position badge, and delete icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldPrimary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = reminder.language.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PureWhite.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = strings.positionNames[reminder.position] ?: reminder.position.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLight.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = strings.deleteConfirm,
                        tint = AccentStopRed.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reminder Text Input
            OutlinedTextField(
                value = reminder.text,
                onValueChange = { onUpdate(reminder.copy(text = it)) },
                label = { Text(strings.reminderTextLabel) },
                placeholder = { Text(strings.reminderPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceGlassBorder,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedLabelColor = EmeraldPrimary,
                    unfocusedLabelColor = TextMuted
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Font Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedFontDropdown,
                onExpandedChange = { expandedFontDropdown = !expandedFontDropdown }
            ) {
                OutlinedTextField(
                    value = reminder.fontName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.fontStyleLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFontDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceGlassBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextMuted
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedFontDropdown,
                    onDismissRequest = { expandedFontDropdown = false },
                    modifier = Modifier.background(SurfaceGlassDark)
                ) {
                    fontsList.forEach { fontName ->
                        DropdownMenuItem(
                            text = { Text(fontName, color = TextLight) },
                            onClick = {
                                onUpdate(reminder.copy(fontName = fontName))
                                expandedFontDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration: Slider + Numeric Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.durationLabel}: ${reminder.durationSeconds} ${strings.secondsUnit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
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
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = SurfaceGlassBorder
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = reminder.durationSeconds.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull()?.coerceIn(1, 120) ?: 1
                        onUpdate(reminder.copy(durationSeconds = v))
                    },
                    modifier = Modifier.width(72.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceGlassBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency: Slider + Numeric Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.frequencyLabel}: ${reminder.frequencyMinutes} ${strings.minutesUnit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
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
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = SurfaceGlassBorder
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = reminder.frequencyMinutes.toString(),
                    onValueChange = {
                        val v = it.toIntOrNull()?.coerceIn(1, 360) ?: 1
                        onUpdate(reminder.copy(frequencyMinutes = v))
                    },
                    modifier = Modifier.width(72.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceGlassBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Screen Position Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedPositionDropdown,
                onExpandedChange = { expandedPositionDropdown = !expandedPositionDropdown }
            ) {
                OutlinedTextField(
                    value = strings.positionNames[reminder.position] ?: reminder.position.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.positionLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPositionDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceGlassBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextMuted
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedPositionDropdown,
                    onDismissRequest = { expandedPositionDropdown = false },
                    modifier = Modifier.background(SurfaceGlassDark)
                ) {
                    OverlayPosition.values().forEach { pos ->
                        DropdownMenuItem(
                            text = { Text(strings.positionNames[pos] ?: pos.label, color = TextLight) },
                            onClick = {
                                onUpdate(reminder.copy(position = pos))
                                expandedPositionDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Squircle Preview
            Text(
                text = strings.livePreviewLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
            SquirclePreview(reminder = reminder)

            // Customize style accordion toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showStyleCustomizer = !showStyleCustomizer }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showStyleCustomizer) strings.hideStyleLabel else strings.customizeStyleLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (showStyleCustomizer) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
            }

            AnimatedVisibility(visible = showStyleCustomizer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "${strings.opacityLabel}: ${(reminder.backgroundOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Slider(
                        value = reminder.backgroundOpacity,
                        onValueChange = { onUpdate(reminder.copy(backgroundOpacity = it)) },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = SurfaceGlassBorder
                        )
                    )

                    Text(
                        text = "${strings.cornerRadiusLabel}: ${reminder.cornerRadiusDp.toInt()} dp",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Slider(
                        value = reminder.cornerRadiusDp,
                        onValueChange = { onUpdate(reminder.copy(cornerRadiusDp = it)) },
                        valueRange = 8f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = SurfaceGlassBorder
                        )
                    )

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
                        Text(strings.resetToDefault, color = TextMuted)
                    }
                }
            }
        }
    }
}
