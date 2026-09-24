package com.example.domain.model

enum class ReminderOffset(val minutesBefore: Int, val label: String) {
    AT_TIME(0, "At event time"),
    MINUTES_15(15, "15 minutes before"),
    MINUTES_30(30, "30 minutes before"),
    HOURS_1(60, "1 hour before"),
    HOURS_24(1440, "24 hours before");

    companion object {
        fun fromMinutes(minutes: Int): ReminderOffset {
            return entries.firstOrNull { it.minutesBefore == minutes } ?: AT_TIME
        }
    }
}
