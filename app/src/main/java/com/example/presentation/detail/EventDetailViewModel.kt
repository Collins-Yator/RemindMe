package com.example.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Event
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Success(val event: Event) : EventDetailUiState
    data object Deleted : EventDetailUiState
    data class Error(val message: String) : EventDetailUiState
}

class EventDetailViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private var currentEventId: String? = null

    fun loadEvent(eventId: String) {
        currentEventId = eventId
        viewModelScope.launch {
            eventRepository.observeEventById(eventId).collect { event ->
                if (event == null || event.isDeleted) {
                    _uiState.value = EventDetailUiState.Deleted
                } else {
                    _uiState.value = EventDetailUiState.Success(event)
                }
            }
        }
    }

    fun toggleComplete() {
        val current = (_uiState.value as? EventDetailUiState.Success)?.event ?: return
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(current.id, !current.isCompleted)
        }
    }

    fun deleteEvent() {
        val id = currentEventId ?: return
        viewModelScope.launch {
            eventRepository.deleteEvent(id)
            _uiState.value = EventDetailUiState.Deleted
        }
    }
}
