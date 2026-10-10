package com.codetoday.remembrance.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.codetoday.remembrance.R
import com.codetoday.remembrance.RemembranceApp
import com.codetoday.remembrance.domain.scheduler.CollisionFreeScheduler
import com.codetoday.remembrance.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemembranceForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var scheduler: CollisionFreeScheduler

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
        scheduler = CollisionFreeScheduler(serviceScope)
        overlayWindowManager.onDismissCallback = {
            scheduler.onOverlayDismissed()
        }

        // Listen for reminder display events and show floating overlays
        serviceScope.launch {
            scheduler.displayEvents.collect { event ->
                overlayWindowManager.showReminderOverlay(event.reminder) {
                    scheduler.onOverlayDismissed()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> startRemembranceService()
            ACTION_STOP -> stopRemembranceService()
        }

        return START_STICKY
    }

    private fun startRemembranceService() {
        _isRunningState.value = true
        setSavedRunningState(this, true)

        val notification = buildPersistentNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Load active reminders from database and feed to collision-free scheduler
        serviceScope.launch(Dispatchers.IO) {
            val repository = (application as RemembranceApp).repository
            val activeReminders = repository.getActiveReminders()
            withContext(Dispatchers.Main) {
                scheduler.start(activeReminders)
            }
        }
    }

    private fun stopRemembranceService() {
        _isRunningState.value = false
        setSavedRunningState(this, false)

        scheduler.stop()
        overlayWindowManager.dismissCurrentOverlay()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildPersistentNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, RemembranceForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, RemembranceApp.CHANNEL_SERVICE_ID)
            .setContentTitle(getString(R.string.notification_running_title))
            .setContentText(getString(R.string.notification_running_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunningState.value = false
        serviceScope.cancel()
        overlayWindowManager.dismissCurrentOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.codetoday.remembrance.action.START"
        const val ACTION_STOP = "com.codetoday.remembrance.action.STOP"

        private const val PREFS_NAME = "remembrance_service_prefs"
        private const val KEY_IS_RUNNING = "is_service_running"

        private val _isRunningState = MutableStateFlow(false)
        val isRunningState: StateFlow<Boolean> = _isRunningState.asStateFlow()

        fun isServiceRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_RUNNING, false)
        }

        fun setSavedRunningState(context: Context, running: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_RUNNING, running).apply()
            _isRunningState.value = running
        }

        fun start(context: Context) {
            val intent = Intent(context, RemembranceForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RemembranceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
