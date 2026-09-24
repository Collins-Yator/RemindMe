package com.example.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Event
import com.example.domain.model.EventCategory
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed")
}

data class EventsUiState(
    val searchQuery: String = "",
    val selectedCategory: EventCategory? = null,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sortAscending: Boolean = true,
    val filteredEvents: List<Event> = emptyList(),
    val totalCount: Int = 0
)

class EventsViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<EventCategory?>(null)
    private val statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val sortAscending = MutableStateFlow(true)

    val uiState: StateFlow<EventsUiState> = combine(
        searchQuery,
        selectedCategory,
        statusFilter,
        sortAscending,
        eventRepository.observeAllEvents()
    ) { query, category, status, asc, allEvents ->
        var list = allEvents

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
            }
        }

        if (category != null) {
            list = list.filter { it.category == category }
        }

        when (status) {
            StatusFilter.ALL -> {}
            StatusFilter.UPCOMING -> list = list.filter { !it.isCompleted }
            StatusFilter.COMPLETED -> list = list.filter { it.isCompleted }
        }

        list = if (asc) {
            list.sortedBy { it.startDateTime }
        } else {
            list.sortedByDescending { it.startDateTime }
        }

        EventsUiState(
            searchQuery = query,
            selectedCategory = category,
            statusFilter = status,
            sortAscending = asc,
            filteredEvents = list,
            totalCount = allEvents.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onCategoryFilterSelected(category: EventCategory?) {
        selectedCategory.value = category
    }

    fun onStatusFilterSelected(status: StatusFilter) {
        statusFilter.value = status
    }

    fun toggleSortOrder() {
        sortAscending.value = !sortAscending.value
    }

    fun toggleEventCompletion(eventId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(eventId, !isCompleted)
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }
}
