package com.example.core.scheduling

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.entity.toDomain
import com.example.data.remote.ApiClient
import com.example.data.remote.dto.EventDto
import com.example.data.remote.dto.SyncRequest
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.model.SyncStatus
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val pendingEntities = db.eventDao().getPendingSyncEvents()

            val dtos = pendingEntities.map { entity ->
                val domain = entity.toDomain()
                EventDto(
                    id = domain.id,
                    userId = domain.userId,
                    title = domain.title,
                    description = domain.description,
                    startDateTime = domain.startDateTime,
                    endDateTime = domain.endDateTime,
                    location = domain.location,
                    categoryId = domain.category.id,
                    recurrenceRule = domain.recurrence.id,
                    colorHex = domain.colorHex,
                    reminderOffsets = domain.reminderOffsets.map { it.minutesBefore },
                    notificationEnabled = domain.notificationEnabled,
                    emailEnabled = domain.emailEnabled,
                    isCompleted = domain.isCompleted,
                    isDeleted = domain.isDeleted,
                    updatedAt = domain.updatedAt
                )
            }

            val userRepo = UserRepositoryImpl(applicationContext)
            val user = userRepo.getCurrentUser()
            val lastSync = user?.lastSyncTime ?: 0L

            val response = ApiClient.getService().syncEvents(
                SyncRequest(
                    lastSyncTimestamp = lastSync,
                    events = dtos
                )
            )

            if (response.isSuccessful) {
                // Mark pending local events as SYNCED
                for (entity in pendingEntities) {
                    if (entity.isDeleted) {
                        db.eventDao().deletePermanently(entity.id)
                    } else {
                        db.eventDao().updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    }
                }
                userRepo.updateLastSyncTime(System.currentTimeMillis())
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "remindme_periodic_sync"
        private const val ONE_TIME_SYNC_NAME = "remindme_one_time_sync"

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            } catch (e: Exception) {
                android.util.Log.w("SyncWorker", "WorkManager schedule deferred: ${e.message}")
            }
        }

        fun triggerImmediateSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME_SYNC_NAME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
            } catch (e: Exception) {
                android.util.Log.w("SyncWorker", "WorkManager trigger deferred: ${e.message}")
            }
        }
    }
}
