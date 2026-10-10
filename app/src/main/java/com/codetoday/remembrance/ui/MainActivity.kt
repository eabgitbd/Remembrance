package com.codetoday.remembrance.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.codetoday.remembrance.domain.localization.AppStrings
import com.codetoday.remembrance.ui.home.HomeScreen
import com.codetoday.remembrance.ui.home.RemembranceViewModel
import com.codetoday.remembrance.ui.permission.PermissionGateDialog
import com.codetoday.remembrance.ui.theme.RemembranceTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RemembranceViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            RemembranceTheme {
                val selectedLanguage by viewModel.selectedLanguage.collectAsState()
                val strings = remember(selectedLanguage) {
                    AppStrings.forLanguage(selectedLanguage)
                }

                var hasOverlayPermission by remember {
                    mutableStateOf(Settings.canDrawOverlays(this@MainActivity))
                }
                var showPermissionDialog by remember {
                    mutableStateOf(!hasOverlayPermission)
                }

                HomeScreen(viewModel = viewModel)

                if (showPermissionDialog) {
                    PermissionGateDialog(
                        hasOverlayPermission = hasOverlayPermission,
                        strings = strings,
                        onRefreshPermissions = {
                            hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity)
                            if (hasOverlayPermission) {
                                showPermissionDialog = false
                            }
                        },
                        onDismiss = {
                            showPermissionDialog = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            // Permission verified
        }
    }
}
