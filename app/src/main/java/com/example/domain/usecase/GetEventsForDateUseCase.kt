package com.example.domain.usecase

import com.example.domain.model.Event
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetEventsForDateUseCase(private val eventRepository: EventRepository) {

    operator fun invoke(year: Int, month: Int, dayOfMonth: Int): Flow<List<Event>> {
        return eventRepository.observeAllEvents().map { allEvents ->
            allEvents.filter { event ->
                event.recurrence.occursOnDate(event.startDateTime, year, month, dayOfMonth)
            }.sortedBy { it.startDateTime }
        }
    }
}
