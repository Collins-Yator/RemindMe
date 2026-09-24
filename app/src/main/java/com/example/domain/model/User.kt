package com.example.domain.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val emailRemindersEnabled: Boolean = true,
    val defaultNotificationEnabled: Boolean = true,
    val defaultEmailAddress: String = email,
    val defaultReminderMinutes: Int = 15,
    val lastSyncTime: Long? = null
)
