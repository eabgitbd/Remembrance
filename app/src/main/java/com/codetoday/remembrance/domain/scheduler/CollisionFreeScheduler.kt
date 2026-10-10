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

            // First reminder triggers quickly (1.5 seconds) so user gets immediate visual confirmation
            val initialDelay = 1500L + (index * (reminder.durationSeconds * 1000L + bufferBetweenRemindersMs))
            nextScheduledTimeMap[reminder.id] = now + initialDelay
        }

        schedulerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                checkAndDispatchReminders()
                delay(1000L) // 1-second precision tick
            }
        }
    }

    fun updateReminders(reminders: List<ReminderItem>) {
        val active = reminders.filter { it.isEnabled && it.text.isNotBlank() }
        currentReminders.clear()
        currentReminders.addAll(active)

        val now = System.currentTimeMillis()
        active.forEachIndexed { index, reminder ->
            val newInterval = reminder.intervalMs
            val oldInterval = previousIntervalMap[reminder.id]

            if (oldInterval == null || oldInterval != newInterval || !nextScheduledTimeMap.containsKey(reminder.id)) {
                previousIntervalMap[reminder.id] = newInterval
                // Reschedule with the user's newly updated frequency interval
                nextScheduledTimeMap[reminder.id] = now + newInterval
            }
        }

        // Clean up deleted reminders
        val activeIds = active.map { it.id }.toSet()
        nextScheduledTimeMap.keys.retainAll(activeIds)
        previousIntervalMap.keys.retainAll(activeIds)
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

    private suspend fun checkAndDispatchReminders() {
        val now = System.currentTimeMillis()

        // Failsafe watchdog: unlock if timeout has elapsed
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

        val durationMs = (selectedReminder.durationSeconds * 1000L).coerceAtLeast(2000L)
        isOverlayCurrentlyActive = true
        activeOverlayEndsAtMs = now + durationMs + bufferBetweenRemindersMs

        // Schedule next occurrence based on reminder interval
        nextScheduledTimeMap[selectedReminder.id] = activeOverlayEndsAtMs + selectedReminder.intervalMs

        // Prevent collisions: stagger any other due reminders
        dueReminders.filter { it.id != selectedReminder.id }.forEachIndexed { idx, other ->
            nextScheduledTimeMap[other.id] = activeOverlayEndsAtMs + ((idx + 1) * bufferBetweenRemindersMs)
        }

        _displayEvents.emit(
            ReminderDisplayEvent(
                reminder = selectedReminder,
                durationMs = durationMs
            )
        )

        // Safety watchdog coroutine
        scope.launch {
            delay(durationMs + bufferBetweenRemindersMs + 500L)
            if (System.currentTimeMillis() >= activeOverlayEndsAtMs) {
                isOverlayCurrentlyActive = false
            }
        }
    }

    fun onOverlayDismissed() {
        isOverlayCurrentlyActive = false
        activeOverlayEndsAtMs = System.currentTimeMillis()
    }
}

data class ReminderDisplayEvent(
    val reminder: ReminderItem,
    val durationMs: Long
)
