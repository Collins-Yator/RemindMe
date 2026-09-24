package com.example.domain.model

import java.util.Calendar
import java.util.TimeZone

enum class RecurrenceRule(val id: String, val label: String) {
    NONE("none", "Does not repeat"),
    WEEKLY("weekly", "Every week"),
    MONTHLY("monthly", "Every month"),
    YEARLY("yearly", "Every year");

    companion object {
        fun fromId(id: String?): RecurrenceRule {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
        }
    }

    /**
     * Checks if this recurring event occurs on a specific day represented by targetCalendar.
     */
    fun occursOnDate(eventStartMillis: Long, targetYear: Int, targetMonth: Int, targetDay: Int): Boolean {
        val eventCal = Calendar.getInstance().apply { timeInMillis = eventStartMillis }
        val eYear = eventCal.get(Calendar.YEAR)
        val eMonth = eventCal.get(Calendar.MONTH)
        val eDay = eventCal.get(Calendar.DAY_OF_MONTH)

        // Event cannot occur before its initial start date
        val targetCal = Calendar.getInstance().apply {
            set(targetYear, targetMonth, targetDay, 23, 59, 59)
        }
        if (targetCal.timeInMillis < eventStartMillis) {
            // Only if target is strictly before the start day
            val startDayCal = Calendar.getInstance().apply {
                timeInMillis = eventStartMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (targetCal.before(startDayCal)) return false
        }

        return when (this) {
            NONE -> {
                eYear == targetYear && eMonth == targetMonth && eDay == targetDay
            }
            WEEKLY -> {
                val targetDayOfWeek = Calendar.getInstance().apply {
                    set(targetYear, targetMonth, targetDay)
                }.get(Calendar.DAY_OF_WEEK)
                val eventDayOfWeek = eventCal.get(Calendar.DAY_OF_WEEK)
                targetDayOfWeek == eventDayOfWeek
            }
            MONTHLY -> {
                // Occurs on the same day of the month
                targetDay == eDay
            }
            YEARLY -> {
                // Occurs on the same month and day of year
                targetMonth == eMonth && targetDay == eDay
            }
        }
    }

    /**
     * Calculates next occurrence after referenceMillis (or the start time if referenceMillis is before it).
     */
    fun nextOccurrenceAfter(referenceMillis: Long, eventStartMillis: Long): Long {
        if (referenceMillis <= eventStartMillis) return eventStartMillis
        if (this == NONE) return eventStartMillis

        val cal = Calendar.getInstance().apply { timeInMillis = eventStartMillis }

        while (cal.timeInMillis <= referenceMillis) {
            when (this) {
                WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                MONTHLY -> cal.add(Calendar.MONTH, 1)
                YEARLY -> cal.add(Calendar.YEAR, 1)
                NONE -> break
            }
        }
        return cal.timeInMillis
    }
}
