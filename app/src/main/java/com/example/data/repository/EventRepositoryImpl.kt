package com.example.data.repository

import android.content.Context
import com.example.core.scheduling.ReminderScheduler
import com.example.core.scheduling.SyncWorker
import com.example.data.local.AppDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.dto.EventDto
import com.example.data.remote.dto.SyncRequest
import com.example.domain.model.Event
import com.example.domain.model.SyncStatus
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepositoryImpl(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    private val scheduler: ReminderScheduler = ReminderScheduler(context)
) : EventRepository {

    private val dao = db.eventDao()
    private val userRepo = UserRepositoryImpl(context)

    override fun observeAllEvents(): Flow<List<Event>> {
        return dao.observeAllActive().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeEventById(id: String): Flow<Event?> {
        return dao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getEventById(id: String): Event? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun saveEvent(event: Event) {
        val eventToSave = event.copy(
            syncStatus = SyncStatus.PENDING_CREATE,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertOrReplace(eventToSave.toEntity())
        scheduler.scheduleRemindersForEvent(eventToSave)
        SyncWorker.triggerImmediateSync(context)
    }

    override suspend fun updateEvent(event: Event) {
        val eventToUpdate = event.copy(
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertOrReplace(eventToUpdate.toEntity())
        scheduler.scheduleRemindersForEvent(eventToUpdate)
        SyncWorker.triggerImmediateSync(context)
    }

    override suspend fun deleteEvent(id: String) {
        val existing = dao.getById(id)
        if (existing != null) {
            scheduler.cancelRemindersForEvent(existing.toDomain())
            dao.markDeleted(id, SyncStatus.PENDING_DELETE, System.currentTimeMillis())
            SyncWorker.triggerImmediateSync(context)
        }
    }

    override suspend fun toggleEventCompletion(id: String, completed: Boolean) {
        dao.updateCompletion(id, completed, SyncStatus.PENDING_UPDATE, System.currentTimeMillis())
        val updated = dao.getById(id)?.toDomain()
        if (updated != null) {
            if (completed) {
                scheduler.cancelRemindersForEvent(updated)
            } else {
                scheduler.scheduleRemindersForEvent(updated)
            }
        }
        SyncWorker.triggerImmediateSync(context)
    }

    override suspend fun syncWithServer(): Result<Int> {
        return try {
            val pending = dao.getPendingSyncEvents()
            val dtos = pending.map { entity ->
                val d = entity.toDomain()
                EventDto(
                    id = d.id,
                    userId = d.userId,
                    title = d.title,
                    description = d.description,
                    startDateTime = d.startDateTime,
                    endDateTime = d.endDateTime,
                    location = d.location,
                    categoryId = d.category.id,
                    recurrenceRule = d.recurrence.id,
                    colorHex = d.colorHex,
                    reminderOffsets = d.reminderOffsets.map { it.minutesBefore },
                    notificationEnabled = d.notificationEnabled,
                    emailEnabled = d.emailEnabled,
                    isCompleted = d.isCompleted,
                    isDeleted = d.isDeleted,
                    updatedAt = d.updatedAt
                )
            }

            val user = userRepo.getCurrentUser()
            val lastSync = user?.lastSyncTime ?: 0L

            val response = ApiClient.getService().syncEvents(
                SyncRequest(lastSyncTimestamp = lastSync, events = dtos)
            )

            if (response.isSuccessful && response.body() != null) {
                val syncData = response.body()!!
                // Mark locally pending as synced or remove permanently if deleted
                for (item in pending) {
                    if (item.isDeleted) {
                        dao.deletePermanently(item.id)
                    } else {
                        dao.updateSyncStatus(item.id, SyncStatus.SYNCED)
                    }
                }
                userRepo.updateLastSyncTime(syncData.serverTimestamp)
                Result.success(dtos.size)
            } else {
                Result.failure(Exception("Sync failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markSynced(id: String) {
        dao.updateSyncStatus(id, SyncStatus.SYNCED)
    }
}
