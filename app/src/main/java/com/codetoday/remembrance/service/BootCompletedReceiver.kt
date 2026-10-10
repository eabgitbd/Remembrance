package com.codetoday.remembrance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Check if the user previously enabled the service and permissions are still valid
            val wasRunning = RemembranceForegroundService.isServiceRunning(context)
            val canDrawOverlays = Settings.canDrawOverlays(context)

            if (wasRunning && canDrawOverlays) {
                RemembranceForegroundService.start(context)
            }
        }
    }
}
