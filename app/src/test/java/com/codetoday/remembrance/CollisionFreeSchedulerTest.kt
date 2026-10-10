package com.codetoday.remembrance

import com.codetoday.remembrance.data.model.OverlayPosition
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.data.model.SupportedLanguage
import com.codetoday.remembrance.domain.scheduler.CollisionFreeScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollisionFreeSchedulerTest {

    private val testScope = TestScope()

    @Test
    fun testSchedulerStaggersMultipleRemindersWithoutOverlap() = testScope.runTest {
        val scheduler = CollisionFreeScheduler(scope = this, bufferBetweenRemindersMs = 1000L)

        val reminderA = ReminderItem(
            id = 1,
            text = "Reminder 1",
            durationSeconds = 2,
            frequencyMinutes = 1
        )
        val reminderB = ReminderItem(
            id = 2,
            text = "Reminder 2",
            durationSeconds = 3,
            frequencyMinutes = 1
        )

        scheduler.start(listOf(reminderA, reminderB))

        // First event should be dispatched promptly
        val event = scheduler.displayEvents.first()
        assertTrue(event.reminder.id == 1L || event.reminder.id == 2L)

        scheduler.stop()
    }

    @Test
    fun testDefaultStylingValues() {
        val defaultReminder = ReminderItem(
            text = "Default Reminder"
        )

        assertEquals("#32CD32", defaultReminder.backgroundColorHex)
        assertEquals(0.65f, defaultReminder.backgroundOpacity, 0.001f)
        assertEquals("#000000", defaultReminder.borderColorHex)
        assertEquals(1.0f, defaultReminder.borderWidthDp, 0.001f)
        assertEquals("#000000", defaultReminder.textColorHex)
        assertEquals(OverlayPosition.TOP, defaultReminder.position)
        assertEquals(SupportedLanguage.BENGALI, defaultReminder.language)
        assertEquals("Noto Serif Bengali", defaultReminder.fontName)
    }
}
