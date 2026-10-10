package com.codetoday.remembrance.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
    private var remindersJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmManager: AlarmManager? = null

    override fun onCreate() {
        super.onCreate()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        overlayWindowManager = OverlayWindowManager(this)
        scheduler = CollisionFreeScheduler(serviceScope)

        overlayWindowManager.onDismissCallback = {
            scheduler.onOverlayDismissed()
        }

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Remembrance::ServiceLock")
        } catch (e: Exception) {
            // ignore
        }

        // Bridge scheduler to hardware RTC alarm driver
        scheduler.onScheduleAlarmRequested = { triggerAtMs, reminderId ->
            scheduleHardwareAlarm(triggerAtMs, reminderId)
        }

        // Listen for reminder display events and show floating overlays + heads-up alert
        serviceScope.launch {
            scheduler.displayEvents.collect { event ->
                try {
                    wakeLock?.acquire(4000L) // Brief CPU wake lock while dispatching
                } catch (e: Exception) {
                    // ignore
                }
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
            ACTION_ALARM_DISPATCH -> {
                val reminderId = intent?.getLongExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, -1L) ?: -1L
                scheduler.onAlarmTriggered(reminderId)
            }
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

        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(10000L) // Initial stabilization lock
            }
        } catch (e: Exception) {
            // ignore
        }

        // Continuously observe Room database for real-time reminder updates
        remindersJob?.cancel()
        var hasInitialized = false
        remindersJob = serviceScope.launch(Dispatchers.IO) {
            val repository = (application as RemembranceApp).repository
            repository.allReminders.collect { remindersList ->
                val activeReminders = remindersList.filter { it.isEnabled }
                withContext(Dispatchers.Main) {
                    if (!hasInitialized) {
                        hasInitialized = true
                        scheduler.start(activeReminders)
                    } else {
                        scheduler.updateReminders(activeReminders)
                    }
                }
            }
        }
    }

    private fun stopRemembranceService() {
        _isRunningState.value = false
        setSavedRunningState(this, false)

        cancelHardwareAlarm()
        remindersJob?.cancel()
        scheduler.stop()
        overlayWindowManager.dismissCurrentOverlay()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // ignore
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun scheduleHardwareAlarm(triggerAtMs: Long, reminderId: Long) {
        val am = alarmManager ?: return
        val intent = Intent(this, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_PENDING_INTENT_REQ,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            } catch (ignored: Exception) {}
        }
    }

    private fun cancelHardwareAlarm() {
        try {
            val intent = Intent(this, ReminderAlarmReceiver::class.java).apply {
                action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_PENDING_INTENT_REQ,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager?.cancel(pendingIntent)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isServiceRunning(this)) {
            // Reschedule quick self-healing resurrection if swiped from Recents
            val restartIntent = Intent(applicationContext, RemembranceForegroundService::class.java).apply {
                action = ACTION_START
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                9999,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1500L,
                restartPendingIntent
            )
        }
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
        cancelHardwareAlarm()
        remindersJob?.cancel()
        serviceScope.cancel()
        overlayWindowManager.dismissCurrentOverlay()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ALARM_PENDING_INTENT_REQ = 3001
        const val ACTION_START = "com.codetoday.remembrance.action.START"
        const val ACTION_STOP = "com.codetoday.remembrance.action.STOP"
        const val ACTION_ALARM_DISPATCH = "com.codetoday.remembrance.action.ALARM_DISPATCH"

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
