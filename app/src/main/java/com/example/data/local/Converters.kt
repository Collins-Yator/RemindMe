package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.EventCategory
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.ReminderOffset
import com.example.domain.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromCategory(category: EventCategory?): String {
        return category?.id ?: EventCategory.OTHER.id
    }

    @TypeConverter
    fun toCategory(id: String?): EventCategory {
        return EventCategory.fromId(id)
    }

    @TypeConverter
    fun fromRecurrence(rule: RecurrenceRule?): String {
        return rule?.id ?: RecurrenceRule.NONE.id
    }

    @TypeConverter
    fun toRecurrence(id: String?): RecurrenceRule {
        return RecurrenceRule.fromId(id)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String {
        return status?.name ?: SyncStatus.PENDING_CREATE.name
    }

    @TypeConverter
    fun toSyncStatus(name: String?): SyncStatus {
        return try {
            if (name != null) SyncStatus.valueOf(name) else SyncStatus.PENDING_CREATE
        } catch (_: Exception) {
            SyncStatus.PENDING_CREATE
        }
    }

    @TypeConverter
    fun fromReminderOffsets(offsets: List<ReminderOffset>?): String {
        if (offsets.isNullOrEmpty()) return ""
        return offsets.joinToString(",") { it.minutesBefore.toString() }
    }

    @TypeConverter
    fun toReminderOffsets(data: String?): List<ReminderOffset> {
        if (data.isNullOrBlank()) return listOf(ReminderOffset.MINUTES_15)
        return data.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .map { ReminderOffset.fromMinutes(it) }
    }
}
