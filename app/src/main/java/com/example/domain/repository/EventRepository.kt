package com.example.domain.repository

import com.example.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeAllEvents(): Flow<List<Event>>
    fun observeEventById(id: String): Flow<Event?>
    suspend fun getEventById(id: String): Event?
    suspend fun saveEvent(event: Event)
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(id: String)
    suspend fun toggleEventCompletion(id: String, completed: Boolean)
    suspend fun syncWithServer(): Result<Int>
    suspend fun markSynced(id: String)
}
