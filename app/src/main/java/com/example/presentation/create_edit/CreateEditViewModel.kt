package com.example.presentation.create_edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Event
import com.example.domain.model.EventCategory
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.ReminderOffset
import com.example.domain.model.SyncStatus
import com.example.domain.repository.EventRepository
import com.example.domain.repository.UserRepository
import com.example.domain.usecase.SaveEventUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class CreateEditUiState(
    val eventId: String? = null,
    val title: String = "",
    val description: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val hasEndTime: Boolean = false,
    val endHour: Int = 10,
    val endMinute: Int = 0,
    val location: String = "",
    val category: EventCategory = EventCategory.PERSONAL,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val colorHex: String = EventCategory.PERSONAL.defaultColorHex,
    val selectedReminders: Set<ReminderOffset> = setOf(ReminderOffset.MINUTES_15),
    val notificationEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val isCompleted: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CreateEditViewModel(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val saveEventUseCase: SaveEventUseCase = SaveEventUseCase(eventRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    fun initialize(eventId: String?, initialDateMillis: Long?) {
        if (!eventId.isNullOrBlank()) {
            viewModelScope.launch {
                val existing = eventRepository.getEventById(eventId)
                if (existing != null) {
                    val startCal = Calendar.getInstance().apply { timeInMillis = existing.startDateTime }
                    val endCal = if (existing.endDateTime != null) {
                        Calendar.getInstance().apply { timeInMillis = existing.endDateTime }
                    } else null

                    _uiState.value = CreateEditUiState(
                        eventId = existing.id,
                        title = existing.title,
                        description = existing.description,
                        year = startCal.get(Calendar.YEAR),
                        month = startCal.get(Calendar.MONTH),
                        day = startCal.get(Calendar.DAY_OF_MONTH),
                        startHour = startCal.get(Calendar.HOUR_OF_DAY),
                        startMinute = startCal.get(Calendar.MINUTE),
                        hasEndTime = existing.endDateTime != null,
                        endHour = endCal?.get(Calendar.HOUR_OF_DAY) ?: (startCal.get(Calendar.HOUR_OF_DAY) + 1),
                        endMinute = endCal?.get(Calendar.MINUTE) ?: startCal.get(Calendar.MINUTE),
                        location = existing.location,
                        category = existing.category,
                        recurrence = existing.recurrence,
                        colorHex = existing.colorHex,
                        selectedReminders = existing.reminderOffsets.toSet(),
                        notificationEnabled = existing.notificationEnabled,
                        emailEnabled = existing.emailEnabled,
                        isCompleted = existing.isCompleted
                    )
                }
            }
        } else {
            val baseCal = Calendar.getInstance()
            if (initialDateMillis != null && initialDateMillis > 0L) {
                baseCal.timeInMillis = initialDateMillis
            }
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val defaultStartHour = if (currentHour < 23) currentHour + 1 else 9

            _uiState.value = CreateEditUiState(
                year = baseCal.get(Calendar.YEAR),
                month = baseCal.get(Calendar.MONTH),
                day = baseCal.get(Calendar.DAY_OF_MONTH),
                startHour = defaultStartHour,
                startMinute = 0,
                hasEndTime = false,
                endHour = if (defaultStartHour < 23) defaultStartHour + 1 else 10,
                endMinute = 0
            )
        }
    }

    fun onTitleChanged(value: String) {
        _uiState.value = _uiState.value.copy(title = value, errorMessage = null)
    }

    fun onDescriptionChanged(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onDateChanged(year: Int, month: Int, day: Int) {
        _uiState.value = _uiState.value.copy(year = year, month = month, day = day)
    }

    fun onStartTimeChanged(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(startHour = hour, startMinute = minute)
    }

    fun onEndTimeChanged(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(endHour = hour, endMinute = minute, hasEndTime = true)
    }

    fun onHasEndTimeToggled(hasEnd: Boolean) {
        _uiState.value = _uiState.value.copy(hasEndTime = hasEnd)
    }

    fun onLocationChanged(value: String) {
        _uiState.value = _uiState.value.copy(location = value)
    }

    fun onCategorySelected(category: EventCategory) {
        _uiState.value = _uiState.value.copy(
            category = category,
            colorHex = category.defaultColorHex
        )
    }

    fun onRecurrenceSelected(recurrence: RecurrenceRule) {
        _uiState.value = _uiState.value.copy(recurrence = recurrence)
    }

    fun onColorSelected(colorHex: String) {
        _uiState.value = _uiState.value.copy(colorHex = colorHex)
    }

    fun toggleReminder(offset: ReminderOffset) {
        val current = _uiState.value.selectedReminders.toMutableSet()
        if (current.contains(offset)) {
            if (current.size > 1) { // Keep at least one
                current.remove(offset)
            }
        } else {
            current.add(offset)
        }
        _uiState.value = _uiState.value.copy(selectedReminders = current)
    }

    fun onNotificationToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationEnabled = enabled)
    }

    fun onEmailToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(emailEnabled = enabled)
    }

    fun saveEvent() {
        val s = _uiState.value
        if (s.title.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Please enter an event title.")
            return
        }

        val startCal = Calendar.getInstance().apply {
            set(s.year, s.month, s.day, s.startHour, s.startMinute, 0)
        }
        val startMillis = startCal.timeInMillis

        val endMillis = if (s.hasEndTime) {
            val endCal = Calendar.getInstance().apply {
                set(s.year, s.month, s.day, s.endHour, s.endMinute, 0)
            }
            if (endCal.timeInMillis <= startMillis) {
                // If end time is earlier, assume it ends on following day or invalid
                endCal.add(Calendar.DAY_OF_MONTH, 1)
            }
            endCal.timeInMillis
        } else null

        _uiState.value = s.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            val userId = user?.id ?: "user_collins_44"

            val event = Event(
                id = s.eventId ?: UUID.randomUUID().toString(),
                userId = userId,
                title = s.title.trim(),
                description = s.description.trim(),
                startDateTime = startMillis,
                endDateTime = endMillis,
                location = s.location.trim(),
                category = s.category,
                recurrence = s.recurrence,
                colorHex = s.colorHex,
                reminderOffsets = s.selectedReminders.toList(),
                notificationEnabled = s.notificationEnabled,
                emailEnabled = s.emailEnabled,
                isCompleted = s.isCompleted,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_CREATE
            )

            val result = saveEventUseCase(event)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to save event."
                )
            }
        }
    }
}
