package com.example.domain.usecase

import com.example.domain.model.Event
import com.example.domain.model.EventCategory
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaveEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(event: Event): Result<Unit> {
        if (event.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Event title cannot be empty."))
        }
        if (event.endDateTime != null && event.endDateTime < event.startDateTime) {
            return Result.failure(IllegalArgumentException("End time must be after start time."))
        }

        val existing = repository.getEventById(event.id)
        if (existing == null) {
            repository.saveEvent(event)
        } else {
            repository.updateEvent(event)
        }
        return Result.success(Unit)
    }
}

class DeleteEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteEvent(id)
    }
}

class ToggleCompleteUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: String, completed: Boolean) {
        repository.toggleEventCompletion(id, completed)
    }
}

class SearchEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(
        query: String = "",
        category: EventCategory? = null,
        onlyUpcoming: Boolean = false,
        onlyCompleted: Boolean = false,
        sortAscending: Boolean = true
    ): Flow<List<Event>> {
        return repository.observeAllEvents().map { events ->
            var list = events

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

            if (onlyUpcoming) {
                list = list.filter { !it.isCompleted }
            } else if (onlyCompleted) {
                list = list.filter { it.isCompleted }
            }

            if (sortAscending) {
                list.sortedBy { it.startDateTime }
            } else {
                list.sortedByDescending { it.startDateTime }
            }
        }
    }
}
