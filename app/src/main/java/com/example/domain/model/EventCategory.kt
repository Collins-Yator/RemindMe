package com.example.domain.model

enum class EventCategory(
    val id: String,
    val displayName: String,
    val defaultColorHex: String
) {
    PERSONAL("personal", "Personal", "#6366F1"),
    WORK("work", "Work", "#0EA5E9"),
    SCHOOL("school", "School", "#8B5CF6"),
    MEETING("meeting", "Meeting", "#14B8A6"),
    APPOINTMENT("appointment", "Appointment", "#EC4899"),
    BIRTHDAY("birthday", "Birthday", "#F43F5E"),
    EXERCISE("exercise", "Exercise", "#10B981"),
    DEADLINE("deadline", "Deadline", "#F59E0B"),
    IMPORTANT("important", "Important", "#EF4444"),
    OTHER("other", "Other", "#64748B");

    companion object {
        fun fromId(id: String?): EventCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}
