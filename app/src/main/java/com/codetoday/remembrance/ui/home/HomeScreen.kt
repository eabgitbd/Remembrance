package com.codetoday.remembrance.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codetoday.remembrance.data.model.SupportedLanguage
import com.codetoday.remembrance.domain.localization.AppStrings
import com.codetoday.remembrance.ui.about.AboutDialog
import com.codetoday.remembrance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RemembranceViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.reminders.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    // Dynamically retrieve translated strings for the entire app UI
    val strings = remember(selectedLanguage) {
        AppStrings.forLanguage(selectedLanguage)
    }

    val buttonBrush = if (isRunning) {
        Brush.horizontalGradient(listOf(AccentStopRedDark, AccentStopRed))
    } else {
        Brush.horizontalGradient(listOf(EmeraldDark, EmeraldPrimary))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.appTitle,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextLight,
                            fontSize = 24.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = strings.appSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = strings.aboutTitle,
                            tint = TextLight.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            // Floating sticky action bar at the bottom with ambient glow
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = if (isRunning) AccentStopRed else EmeraldPrimary,
                            spotColor = if (isRunning) AccentStopRed else EmeraldPrimary
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(buttonBrush)
                        .clickable { viewModel.toggleService() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRunning) strings.stopService else strings.startService,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Top App Language Switcher Segmented Pills
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.selectLanguage,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumGlassmorphic(cornerRadius = 16.dp, elevation = 2.dp)
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SupportedLanguage.values().forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                                .clickable { viewModel.setLanguage(lang) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) PureBlack else TextLight,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Reminders Cards List
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    strings = strings,
                    onUpdate = { updated -> viewModel.updateReminder(updated) },
                    onDelete = { viewModel.deleteReminder(reminder) }
                )
            }

            // Add Reminder Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.addReminder(selectedLanguage) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceGlassDark.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .premiumGlassmorphic(cornerRadius = 16.dp, elevation = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.addReminder,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            strings = strings,
            onDismiss = { showAboutDialog = false }
        )
    }
}
