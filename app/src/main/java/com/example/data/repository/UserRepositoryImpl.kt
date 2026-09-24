package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "user_preferences")

class UserRepositoryImpl(private val context: Context) : UserRepository {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_PHOTO_URL = stringPreferencesKey("photo_url")
        private val KEY_EMAIL_REMINDERS_ENABLED = booleanPreferencesKey("email_reminders_enabled")
        private val KEY_DEFAULT_NOTIF_ENABLED = booleanPreferencesKey("default_notif_enabled")
        private val KEY_DEFAULT_EMAIL = stringPreferencesKey("default_email")
        private val KEY_DEFAULT_REMINDER_MIN = intPreferencesKey("default_reminder_min")
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_time")

        const val DEFAULT_DEVELOPMENT_RECIPIENT = "yatorcollins43@gmail.com"
        const val DEFAULT_USER_EMAIL = "yatorcollins44@gmail.com"
        const val DEFAULT_USER_NAME = "Collins Yator"
    }

    override fun observeCurrentUser(): Flow<User?> {
        return context.userDataStore.data.map { prefs ->
            val userId = prefs[KEY_USER_ID]
            if (userId == null) {
                // If not signed in yet, provide standard signed in profile for smooth first-run or dev
                User(
                    id = "user_collins_44",
                    email = DEFAULT_USER_EMAIL,
                    displayName = DEFAULT_USER_NAME,
                    photoUrl = null,
                    emailRemindersEnabled = prefs[KEY_EMAIL_REMINDERS_ENABLED] ?: true,
                    defaultNotificationEnabled = prefs[KEY_DEFAULT_NOTIF_ENABLED] ?: true,
                    defaultEmailAddress = prefs[KEY_DEFAULT_EMAIL] ?: DEFAULT_DEVELOPMENT_RECIPIENT,
                    defaultReminderMinutes = prefs[KEY_DEFAULT_REMINDER_MIN] ?: 15,
                    lastSyncTime = prefs[KEY_LAST_SYNC]
                )
            } else {
                User(
                    id = userId,
                    email = prefs[KEY_EMAIL] ?: DEFAULT_USER_EMAIL,
                    displayName = prefs[KEY_DISPLAY_NAME] ?: DEFAULT_USER_NAME,
                    photoUrl = prefs[KEY_PHOTO_URL],
                    emailRemindersEnabled = prefs[KEY_EMAIL_REMINDERS_ENABLED] ?: true,
                    defaultNotificationEnabled = prefs[KEY_DEFAULT_NOTIF_ENABLED] ?: true,
                    defaultEmailAddress = prefs[KEY_DEFAULT_EMAIL] ?: DEFAULT_DEVELOPMENT_RECIPIENT,
                    defaultReminderMinutes = prefs[KEY_DEFAULT_REMINDER_MIN] ?: 15,
                    lastSyncTime = prefs[KEY_LAST_SYNC]
                )
            }
        }
    }

    override suspend fun getCurrentUser(): User? {
        return observeCurrentUser().first()
    }

    override suspend fun signInWithGoogle(
        idToken: String?,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ): Result<User> {
        val resolvedEmail = email ?: DEFAULT_USER_EMAIL
        val resolvedName = displayName ?: DEFAULT_USER_NAME
        val userId = "user_${resolvedEmail.replace("[^a-zA-Z0-9]".toRegex(), "_")}"

        context.userDataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EMAIL] = resolvedEmail
            prefs[KEY_DISPLAY_NAME] = resolvedName
            if (photoUrl != null) prefs[KEY_PHOTO_URL] = photoUrl
        }

        val user = User(
            id = userId,
            email = resolvedEmail,
            displayName = resolvedName,
            photoUrl = photoUrl,
            emailRemindersEnabled = true,
            defaultNotificationEnabled = true,
            defaultEmailAddress = DEFAULT_DEVELOPMENT_RECIPIENT,
            defaultReminderMinutes = 15,
            lastSyncTime = System.currentTimeMillis()
        )
        return Result.success(user)
    }

    override suspend fun signOut() {
        context.userDataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_DISPLAY_NAME)
            prefs.remove(KEY_PHOTO_URL)
        }
    }

    override suspend fun updatePreferences(
        emailRemindersEnabled: Boolean,
        defaultNotificationEnabled: Boolean,
        defaultEmail: String,
        defaultReminderMinutes: Int
    ) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_EMAIL_REMINDERS_ENABLED] = emailRemindersEnabled
            prefs[KEY_DEFAULT_NOTIF_ENABLED] = defaultNotificationEnabled
            prefs[KEY_DEFAULT_EMAIL] = defaultEmail
            prefs[KEY_DEFAULT_REMINDER_MIN] = defaultReminderMinutes
        }
    }

    override suspend fun updateLastSyncTime(time: Long) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC] = time
        }
    }
}
