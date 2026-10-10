package com.codetoday.remembrance.domain.scheduler

import com.codetoday.remembrance.data.model.ReminderItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

class CollisionFreeScheduler(
    private val scope: CoroutineScope,
    private val bufferBetweenRemindersMs: Long = 2000L
) {

    private val _displayEvents = MutableSharedFlow<ReminderDisplayEvent>(extraBufferCapacity = 16)
    val displayEvents: SharedFlow<ReminderDisplayEvent> = _displayEvents.asSharedFlow()

    private val nextScheduledTimeMap = ConcurrentHashMap<Long, Long>()
    private var schedulerJob: Job? = null

    @Volatile
    private var isOverlayCurrentlyActive = false

    @Volatile
    private var activeOverlayEndsAtMs = 0L

    fun start(reminders: List<ReminderItem>) {
        stop()
        if (reminders.isEmpty()) return

        val now = System.currentTimeMillis()
        reminders.filter { it.isEnabled }.forEachIndexed { index, reminder ->
            // Stagger initial start times to avoid instant simultaneous collision on service launch
            val initialOffsetMs = (index * (reminder.durationSeconds * 1000L + bufferBetweenRemindersMs))
            val intervalMs = reminder.frequencyMinutes * 60 * 1000L
            nextScheduledTimeMap[reminder.id] = now + initialOffsetMs + 1000L
        }

        schedulerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                checkAndDispatchReminders(reminders)
                delay(1000L) // 1-second evaluation tick
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
        nextScheduledTimeMap.clear()
        isOverlayCurrentlyActive = false
        activeOverlayEndsAtMs = 0L
    }

    private suspend fun checkAndDispatchReminders(reminders: List<ReminderItem>) {
        val now = System.currentTimeMillis()

        // If an overlay is currently showing, wait until it finishes
        if (isOverlayCurrentlyActive && now < activeOverlayEndsAtMs) {
            return
        }

        // Find due reminders
        val dueReminders = reminders.filter { reminder ->
            reminder.isEnabled && (nextScheduledTimeMap[reminder.id]?.let { it <= now } ?: false)
        }

        if (dueReminders.isEmpty()) return

        // Pick the most overdue reminder
        val selectedReminder = dueReminders.minByOrNull { nextScheduledTimeMap[it.id] ?: Long.MAX_VALUE }
            ?: return

        // Calculate duration and end time
        val durationMs = selectedReminder.durationSeconds * 1000L
        isOverlayCurrentlyActive = true
        activeOverlayEndsAtMs = now + durationMs + bufferBetweenRemindersMs

        // Schedule its next repetition
        val intervalMs = selectedReminder.frequencyMinutes * 60 * 1000L
        nextScheduledTimeMap[selectedReminder.id] = activeOverlayEndsAtMs + intervalMs

        // If any other due reminders collided, reschedule their trigger time to after this one ends
        dueReminders.filter { it.id != selectedReminder.id }.forEachIndexed { idx, other ->
            val staggeredTime = activeOverlayEndsAtMs + (idx * bufferBetweenRemindersMs)
            nextScheduledTimeMap[other.id] = staggeredTime
        }

        // Dispatch display event
        _displayEvents.emit(
            ReminderDisplayEvent(
                reminder = selectedReminder,
                durationMs = durationMs
            )
        )

        // Reset overlay active state after duration + buffer completes
        scope.launch {
            delay(durationMs + bufferBetweenRemindersMs)
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
