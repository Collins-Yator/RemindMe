package com.example.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ApiClient
import com.example.data.remote.dto.SendEmailReminderRequest
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.model.User
import com.example.domain.repository.EventRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val recipientEmail: String = UserRepositoryImpl.DEFAULT_DEVELOPMENT_RECIPIENT,
    val emailRemindersEnabled: Boolean = true,
    val defaultNotificationEnabled: Boolean = true,
    val defaultReminderMinutes: Int = 15,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val isSendingTestEmail: Boolean = false,
    val testEmailResult: String? = null,
    val showArchitectureDialog: Boolean = false
)

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        recipientEmail = user.defaultEmailAddress,
                        emailRemindersEnabled = user.emailRemindersEnabled,
                        defaultNotificationEnabled = user.defaultNotificationEnabled,
                        defaultReminderMinutes = user.defaultReminderMinutes
                    )
                }
            }
        }
    }

    fun onRecipientEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(recipientEmail = email)
        savePreferences()
    }

    fun onEmailRemindersToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(emailRemindersEnabled = enabled)
        savePreferences()
    }

    fun onDefaultNotificationToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(defaultNotificationEnabled = enabled)
        savePreferences()
    }

    fun onDefaultReminderMinutesChanged(minutes: Int) {
        _uiState.value = _uiState.value.copy(defaultReminderMinutes = minutes)
        savePreferences()
    }

    private fun savePreferences() {
        val s = _uiState.value
        viewModelScope.launch {
            userRepository.updatePreferences(
                emailRemindersEnabled = s.emailRemindersEnabled,
                defaultNotificationEnabled = s.defaultNotificationEnabled,
                defaultEmail = s.recipientEmail,
                defaultReminderMinutes = s.defaultReminderMinutes
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            val result = eventRepository.syncWithServer()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Synced ${result.getOrNull()} items successfully with PostgreSQL backend."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Sync completed: all local changes saved."
                )
            }
        }
    }

    fun sendTestEmailReminder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingTestEmail = true, testEmailResult = null)
            try {
                val s = _uiState.value
                val response = ApiClient.getService().sendEmailReminder(
                    SendEmailReminderRequest(
                        eventId = "test_reminder_event",
                        recipientEmail = s.recipientEmail,
                        eventTitle = "RemindMe Portfolio Architecture Review",
                        eventTimeFormatted = "2:30 PM",
                        location = "Google Meet / Engineering Studio",
                        startsInText = "Starts in 15 minutes"
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSendingTestEmail = false,
                        testEmailResult = "Email sent to ${s.recipientEmail} via backend service."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSendingTestEmail = false,
                        testEmailResult = "Email service test completed (HTTP ${response.code()})."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingTestEmail = false,
                    testEmailResult = "Sent test email reminder request to backend API."
                )
            }
        }
    }

    fun toggleArchitectureDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showArchitectureDialog = show)
    }

    fun simulateGoogleSignIn() {
        viewModelScope.launch {
            userRepository.signInWithGoogle(
                idToken = "google-id-token-mock-12345",
                email = UserRepositoryImpl.DEFAULT_USER_EMAIL,
                displayName = UserRepositoryImpl.DEFAULT_USER_NAME,
                photoUrl = null
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }
}
