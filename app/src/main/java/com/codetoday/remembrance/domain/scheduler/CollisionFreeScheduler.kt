package com.codetoday.remembrance.domain.scheduler

import com.codetoday.remembrance.data.model.ReminderItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class CollisionFreeScheduler(
    private val scope: CoroutineScope,
    private val bufferBetweenRemindersMs: Long = 1500L
) {

    private val _displayEvents = MutableSharedFlow<ReminderDisplayEvent>(extraBufferCapacity = 32)
    val displayEvents: SharedFlow<ReminderDisplayEvent> = _displayEvents.asSharedFlow()

    private val currentReminders = CopyOnWriteArrayList<ReminderItem>()
    private val nextScheduledTimeMap = ConcurrentHashMap<Long, Long>()
    private val previousIntervalMap = ConcurrentHashMap<Long, Long>()
    private var schedulerJob: Job? = null

    /**
     * Callback invoked whenever a new earliest wake-up time is determined,
     * so that the host service can program AlarmManager.
     */
    var onScheduleAlarmRequested: ((triggerAtMs: Long, reminderId: Long) -> Unit)? = null

    @Volatile
    private var isOverlayCurrentlyActive = false

    @Volatile
    private var activeOverlayEndsAtMs = 0L

    fun start(reminders: List<ReminderItem>) {
        stop()
        if (reminders.isEmpty()) return

        val active = reminders.filter { it.isEnabled && it.text.isNotBlank() }
        currentReminders.clear()
        currentReminders.addAll(active)

        val now = System.currentTimeMillis()
        active.forEachIndexed { index, reminder ->
            val interval = reminder.intervalMs
            previousIntervalMap[reminder.id] = interval

            // First reminder triggers quickly (1.5s) for instant user feedback, subsequent staggered
            val initialDelay = 1500L + (index * (reminder.durationSeconds * 1000L + bufferBetweenRemindersMs))
            nextScheduledTimeMap[reminder.id] = now + initialDelay
        }

        requestNextAlarm()

        schedulerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                checkAndDispatchReminders()
                delay(1000L) // In-memory precision tick for active screen
            }
        }
    }

    fun updateReminders(reminders: List<ReminderItem>) {
        val active = reminders.filter { it.isEnabled && it.text.isNotBlank() }
        currentReminders.clear()
        currentReminders.addAll(active)

        val now = System.currentTimeMillis()
        active.forEachIndexed { _, reminder ->
            val newInterval = reminder.intervalMs
            val oldInterval = previousIntervalMap[reminder.id]

            if (oldInterval == null || oldInterval != newInterval || !nextScheduledTimeMap.containsKey(reminder.id)) {
                previousIntervalMap[reminder.id] = newInterval
                nextScheduledTimeMap[reminder.id] = now + newInterval
            }
        }

        // Clean up deleted reminders
        val activeIds = active.map { it.id }.toSet()
        nextScheduledTimeMap.keys.retainAll(activeIds)
        previousIntervalMap.keys.retainAll(activeIds)

        requestNextAlarm()
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
        currentReminders.clear()
        nextScheduledTimeMap.clear()
        previousIntervalMap.clear()
        isOverlayCurrentlyActive = false
        activeOverlayEndsAtMs = 0L
    }

    /**
     * Called directly when ReminderAlarmReceiver delivers an RTC hardware wakeup.
     */
    fun onAlarmTriggered(reminderId: Long) {
        val now = System.currentTimeMillis()
        if (isOverlayCurrentlyActive && now < activeOverlayEndsAtMs) {
            // Still displaying an active reminder; reschedule slightly later
            nextScheduledTimeMap[reminderId] = activeOverlayEndsAtMs + bufferBetweenRemindersMs
            requestNextAlarm()
            return
        }

        val reminder = currentReminders.find { it.id == reminderId }
            ?: currentReminders.minByOrNull { nextScheduledTimeMap[it.id] ?: Long.MAX_VALUE }

        if (reminder != null) {
            dispatchReminder(reminder)
        } else {
            requestNextAlarm()
        }
    }

    private suspend fun checkAndDispatchReminders() {
        val now = System.currentTimeMillis()

        if (isOverlayCurrentlyActive && now >= activeOverlayEndsAtMs) {
            isOverlayCurrentlyActive = false
        }

        if (isOverlayCurrentlyActive) {
            return
        }

        val dueReminders = currentReminders.filter { reminder ->
            val scheduled = nextScheduledTimeMap[reminder.id]
            scheduled != null && scheduled <= now
        }

        if (dueReminders.isEmpty()) return

        val selectedReminder = dueReminders.minByOrNull { nextScheduledTimeMap[it.id] ?: Long.MAX_VALUE }
            ?: return

        dispatchReminder(selectedReminder)
    }

    private fun dispatchReminder(reminder: ReminderItem) {
        val now = System.currentTimeMillis()
        val durationMs = (reminder.durationSeconds * 1000L).coerceAtLeast(2000L)
        isOverlayCurrentlyActive = true
        activeOverlayEndsAtMs = now + durationMs + bufferBetweenRemindersMs

        // Schedule next occurrence based on frequency interval
        nextScheduledTimeMap[reminder.id] = activeOverlayEndsAtMs + reminder.intervalMs

        // Stagger any other pending reminders to prevent visual collision
        currentReminders.filter { it.id != reminder.id }.forEachIndexed { idx, other ->
            val existing = nextScheduledTimeMap[other.id] ?: (now + bufferBetweenRemindersMs)
            if (existing <= activeOverlayEndsAtMs) {
                nextScheduledTimeMap[other.id] = activeOverlayEndsAtMs + ((idx + 1) * bufferBetweenRemindersMs)
            }
        }

        _displayEvents.tryEmit(
            ReminderDisplayEvent(
                reminder = reminder,
                durationMs = durationMs
            )
        )

        requestNextAlarm()

        // Safety watchdog coroutine
        scope.launch {
            delay(durationMs + bufferBetweenRemindersMs + 500L)
            if (System.currentTimeMillis() >= activeOverlayEndsAtMs) {
                isOverlayCurrentlyActive = false
            }
        }
    }

    private fun requestNextAlarm() {
        val earliest = nextScheduledTimeMap.entries
            .minByOrNull { it.value }
            ?: return

        onScheduleAlarmRequested?.invoke(earliest.value, earliest.key)
    }

    fun onOverlayDismissed() {
        isOverlayCurrentlyActive = false
        activeOverlayEndsAtMs = System.currentTimeMillis()
        requestNextAlarm()
    }
}

data class ReminderDisplayEvent(
    val reminder: ReminderItem,
    val durationMs: Long
)
