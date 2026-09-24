package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Event
import com.example.domain.model.EventCategory
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.ReminderOffset
import com.example.domain.model.SyncStatus

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["startDateTime"]),
        Index(value = ["userId"]),
        Index(value = ["isDeleted"]),
        Index(value = ["syncStatus"])
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val startDateTime: Long,
    val endDateTime: Long?,
    val location: String,
    val category: EventCategory,
    val recurrence: RecurrenceRule,
    val colorHex: String,
    val reminderOffsets: List<ReminderOffset>,
    val notificationEnabled: Boolean,
    val emailEnabled: Boolean,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)

fun EventEntity.toDomain(): Event = Event(
    id = id,
    userId = userId,
    title = title,
    description = description,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    location = location,
    category = category,
    recurrence = recurrence,
    colorHex = colorHex,
    reminderOffsets = reminderOffsets,
    notificationEnabled = notificationEnabled,
    emailEnabled = emailEnabled,
    isCompleted = isCompleted,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    userId = userId,
    title = title,
    description = description,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    location = location,
    category = category,
    recurrence = recurrence,
    colorHex = colorHex,
    reminderOffsets = reminderOffsets,
    notificationEnabled = notificationEnabled,
    emailEnabled = emailEnabled,
    isCompleted = isCompleted,
    isDeleted = isDeleted,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)
