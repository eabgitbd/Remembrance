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
    private val bufferBetweenRemindersMs: Long = 2000L
) {

    private val _displayEvents = MutableSharedFlow<ReminderDisplayEvent>(extraBufferCapacity = 32)
    val displayEvents: SharedFlow<ReminderDisplayEvent> = _displayEvents.asSharedFlow()

    private val currentReminders = CopyOnWriteArrayList<ReminderItem>()
    private val nextScheduledTimeMap = ConcurrentHashMap<Long, Long>()
    private var schedulerJob: Job? = null

    @Volatile
    private var isOverlayCurrentlyActive = false

    @Volatile
    private var activeOverlayEndsAtMs = 0L

    fun start(reminders: List<ReminderItem>) {
        stop()
        if (reminders.isEmpty()) return

        currentReminders.clear()
        currentReminders.addAll(reminders.filter { it.isEnabled && it.text.isNotBlank() })

        val now = System.currentTimeMillis()
        currentReminders.forEachIndexed { index, reminder ->
            // Initial staggered trigger: first reminder after 2s, subsequent after short offsets
            val initialOffsetMs = 2000L + (index * (reminder.durationSeconds * 1000L + bufferBetweenRemindersMs))
            nextScheduledTimeMap[reminder.id] = now + initialOffsetMs
        }

        schedulerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                checkAndDispatchReminders()
                delay(1000L) // 1-second check interval
            }
        }
    }

    fun updateReminders(reminders: List<ReminderItem>) {
        val filtered = reminders.filter { it.isEnabled && it.text.isNotBlank() }
        currentReminders.clear()
        currentReminders.addAll(filtered)

        val now = System.currentTimeMillis()
        // Ensure every active reminder has a schedule entry
        filtered.forEachIndexed { index, reminder ->
            if (!nextScheduledTimeMap.containsKey(reminder.id)) {
                val offset = 2000L + (index * 3000L)
                nextScheduledTimeMap[reminder.id] = now + offset
            }
        }
        // Remove deleted reminders
        val activeIds = filtered.map { it.id }.toSet()
        nextScheduledTimeMap.keys.retainAll(activeIds)
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
        currentReminders.clear()
        nextScheduledTimeMap.clear()
        isOverlayCurrentlyActive = false
        activeOverlayEndsAtMs = 0L
    }

    private suspend fun checkAndDispatchReminders() {
        val now = System.currentTimeMillis()

        // Failsafe auto-reset: if previous overlay window timed out, clear lock
        if (isOverlayCurrentlyActive && now >= activeOverlayEndsAtMs) {
            isOverlayCurrentlyActive = false
        }

        // If an overlay is legitimately currently active on screen, wait for it to finish
        if (isOverlayCurrentlyActive) {
            return
        }

        // Find due reminders
        val dueReminders = currentReminders.filter { reminder ->
            val scheduled = nextScheduledTimeMap[reminder.id]
            scheduled != null && scheduled <= now
        }

        if (dueReminders.isEmpty()) return

        // Pick the oldest due reminder
        val selectedReminder = dueReminders.minByOrNull { nextScheduledTimeMap[it.id] ?: Long.MAX_VALUE }
            ?: return

        val durationMs = selectedReminder.durationSeconds * 1000L
        isOverlayCurrentlyActive = true
        activeOverlayEndsAtMs = now + durationMs + bufferBetweenRemindersMs

        // Schedule next occurrence based on frequency in minutes
        val intervalMs = (selectedReminder.frequencyMinutes * 60 * 1000L).coerceAtLeast(10_000L)
        nextScheduledTimeMap[selectedReminder.id] = activeOverlayEndsAtMs + intervalMs

        // If other reminders were also due, stagger them so they do NOT collide
        dueReminders.filter { it.id != selectedReminder.id }.forEachIndexed { idx, other ->
            nextScheduledTimeMap[other.id] = activeOverlayEndsAtMs + ((idx + 1) * bufferBetweenRemindersMs)
        }

        // Emit display event
        _displayEvents.emit(
            ReminderDisplayEvent(
                reminder = selectedReminder,
                durationMs = durationMs
            )
        )

        // Safety timeout coroutine: forcibly release lock after duration + buffer
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
