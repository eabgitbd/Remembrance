package com.codetoday.remembrance

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.codetoday.remembrance.data.db.AppDatabase
import com.codetoday.remembrance.data.repository.ReminderRepository

class RemembranceApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ReminderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        repository = ReminderRepository(database.reminderDao())

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "remembrance_service_channel"
        lateinit var instance: RemembranceApp
            private set
    }
}
