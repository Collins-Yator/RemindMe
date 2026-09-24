package com.example

import com.example.domain.model.Event
import com.example.domain.model.EventCategory
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.ReminderOffset
import com.example.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RemindMeDomainTest {

    @Test
    fun testRecurrenceRule_None() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 10, 0, 0)
        }
        val startMillis = cal.timeInMillis

        assertTrue(RecurrenceRule.NONE.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 24))
        assertFalse(RecurrenceRule.NONE.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 25))
    }

    @Test
    fun testRecurrenceRule_Weekly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 10, 0, 0) // Thursday
        }
        val startMillis = cal.timeInMillis

        // Same day
        assertTrue(RecurrenceRule.WEEKLY.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 24))
        // Next week Thursday (Oct 1)
        assertTrue(RecurrenceRule.WEEKLY.occursOnDate(startMillis, 2026, Calendar.OCTOBER, 1))
        // A Friday
        assertFalse(RecurrenceRule.WEEKLY.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 25))
    }

    @Test
    fun testRecurrenceRule_Monthly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 10, 0, 0)
        }
        val startMillis = cal.timeInMillis

        // 15th of current month
        assertTrue(RecurrenceRule.MONTHLY.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 15))
        // 15th of next month
        assertTrue(RecurrenceRule.MONTHLY.occursOnDate(startMillis, 2026, Calendar.OCTOBER, 15))
        // 16th of current month
        assertFalse(RecurrenceRule.MONTHLY.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 16))
    }

    @Test
    fun testRecurrenceRule_Yearly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 10, 0, 0)
        }
        val startMillis = cal.timeInMillis

        // Same date
        assertTrue(RecurrenceRule.YEARLY.occursOnDate(startMillis, 2026, Calendar.SEPTEMBER, 24))
        // Same date next year
        assertTrue(RecurrenceRule.YEARLY.occursOnDate(startMillis, 2027, Calendar.SEPTEMBER, 24))
        // Different date
        assertFalse(RecurrenceRule.YEARLY.occursOnDate(startMillis, 2027, Calendar.SEPTEMBER, 25))
    }

    @Test
    fun testEventCreationDefaults() {
        val event = Event(
            id = "test-id-1",
            userId = "user-1",
            title = "Android Lecture",
            description = "Deep dive into WorkManager",
            startDateTime = 1758700000000L,
            location = "Room 404",
            category = EventCategory.SCHOOL,
            recurrence = RecurrenceRule.WEEKLY,
            colorHex = EventCategory.SCHOOL.defaultColorHex,
            reminderOffsets = listOf(ReminderOffset.MINUTES_15, ReminderOffset.HOURS_1),
            notificationEnabled = true,
            emailEnabled = true,
            isCompleted = false,
            syncStatus = SyncStatus.PENDING_CREATE
        )

        assertEquals("Android Lecture", event.title)
        assertEquals(EventCategory.SCHOOL, event.category)
        assertTrue(event.emailEnabled)
        assertTrue(event.notificationEnabled)
        assertEquals(2, event.reminderOffsets.size)
    }
}
