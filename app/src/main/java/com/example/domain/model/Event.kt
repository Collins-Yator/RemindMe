package com.example.domain.model

data class Event(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val startDateTime: Long, // Epoch milliseconds in UTC
    val endDateTime: Long? = null,
    val location: String = "",
    val category: EventCategory = EventCategory.PERSONAL,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val colorHex: String = category.defaultColorHex,
    val reminderOffsets: List<ReminderOffset> = listOf(ReminderOffset.MINUTES_15),
    val notificationEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE
)
