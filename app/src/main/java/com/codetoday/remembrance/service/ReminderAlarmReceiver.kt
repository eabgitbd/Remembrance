package com.codetoday.remembrance.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == ACTION_TRIGGER_REMINDER) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Remembrance::AlarmWakeLock"
            )
            try {
                wakeLock?.acquire(4000L) // Brief CPU lock while dispatching event
            } catch (e: Exception) {
                // ignore
            }

            val serviceIntent = Intent(context, RemembranceForegroundService::class.java).apply {
                this.action = RemembranceForegroundService.ACTION_ALARM_DISPATCH
                putExtra(EXTRA_REMINDER_ID, intent.getLongExtra(EXTRA_REMINDER_ID, -1L))
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.codetoday.remembrance.action.TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
