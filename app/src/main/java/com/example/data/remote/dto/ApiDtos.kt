package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "startDateTime") val startDateTime: Long,
    @Json(name = "endDateTime") val endDateTime: Long? = null,
    @Json(name = "location") val location: String = "",
    @Json(name = "categoryId") val categoryId: String = "personal",
    @Json(name = "recurrenceRule") val recurrenceRule: String = "none",
    @Json(name = "colorHex") val colorHex: String = "#6366F1",
    @Json(name = "reminderOffsets") val reminderOffsets: List<Int> = listOf(15),
    @Json(name = "notificationEnabled") val notificationEnabled: Boolean = true,
    @Json(name = "emailEnabled") val emailEnabled: Boolean = false,
    @Json(name = "isCompleted") val isCompleted: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequest(
    @Json(name = "idToken") val idToken: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "photoUrl") val photoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "token") val token: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "email") val email: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "photoUrl") val photoUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncRequest(
    @Json(name = "lastSyncTimestamp") val lastSyncTimestamp: Long,
    @Json(name = "events") val events: List<EventDto>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "serverTimestamp") val serverTimestamp: Long,
    @Json(name = "updatedEvents") val updatedEvents: List<EventDto>,
    @Json(name = "deletedEventIds") val deletedEventIds: List<String> = emptyList(),
    @Json(name = "message") val message: String = "Sync successful"
)

@JsonClass(generateAdapter = true)
data class SendEmailReminderRequest(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "recipientEmail") val recipientEmail: String,
    @Json(name = "eventTitle") val eventTitle: String,
    @Json(name = "eventTimeFormatted") val eventTimeFormatted: String,
    @Json(name = "location") val location: String,
    @Json(name = "startsInText") val startsInText: String
)

@JsonClass(generateAdapter = true)
data class SendEmailResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
