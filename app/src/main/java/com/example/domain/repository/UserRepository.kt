package com.example.domain.repository

import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun signInWithGoogle(idToken: String?, email: String?, displayName: String?, photoUrl: String?): Result<User>
    suspend fun signOut()
    suspend fun updatePreferences(
        emailRemindersEnabled: Boolean,
        defaultNotificationEnabled: Boolean,
        defaultEmail: String,
        defaultReminderMinutes: Int
    )
    suspend fun updateLastSyncTime(time: Long)
}
