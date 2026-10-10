package com.codetoday.remembrance.ui.permission

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codetoday.remembrance.domain.localization.AppStrings
import com.codetoday.remembrance.ui.theme.*

@Composable
fun PermissionGateDialog(
    hasOverlayPermission: Boolean,
    hasBatteryOptimizationIgnored: Boolean,
    strings: AppStrings,
    onRefreshPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val isOemSpecialDevice = rememberIsOemSpecialDevice()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .premiumGlassmorphic(cornerRadius = 24.dp, elevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceGlassDark.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.permissionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = strings.permissionDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Overlay Permission Button
                Button(
                    onClick = {
                        requestOverlayPermission(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasOverlayPermission) SurfaceGlassHighlight else EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = if (hasOverlayPermission) TextLight else PureBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasOverlayPermission) strings.overlayGranted else strings.enableOverlayBtn,
                        color = if (hasOverlayPermission) TextLight else PureBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Battery Optimization Exemption Button
                Button(
                    onClick = {
                        requestIgnoreBatteryOptimizations(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasBatteryOptimizationIgnored) SurfaceGlassHighlight else EmeraldPrimary.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (hasBatteryOptimizationIgnored) TextLight else PureBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasBatteryOptimizationIgnored) strings.batteryGranted else strings.batteryOptimizationBtn,
                        color = if (hasBatteryOptimizationIgnored) TextLight else PureBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App Settings Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = TextLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.settingsBtn,
                        color = TextLight,
                        fontSize = 14.sp
                    )
                }

                // OEM Special Guidance Hint (Xiaomi, Vivo, Oppo, Realme)
                if (isOemSpecialDevice) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceGlassHighlight.copy(alpha = 0.08f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.oemGuidanceHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onRefreshPermissions()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TextLight),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = strings.permissionsDoneBtn,
                        color = PureBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

private fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}

@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(fallbackIntent)
            } catch (ignored: Exception) {}
        }
    }
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    } else {
        true
    }
}

private fun rememberIsOemSpecialDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return manufacturer.contains("xiaomi") ||
            manufacturer.contains("redmi") ||
            manufacturer.contains("poco") ||
            manufacturer.contains("oppo") ||
            manufacturer.contains("vivo") ||
            manufacturer.contains("realme") ||
            manufacturer.contains("huawei")
}
