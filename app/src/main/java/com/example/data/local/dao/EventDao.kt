package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.EventEntity
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE isDeleted = 0 ORDER BY startDateTime ASC")
    fun observeAllActive(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE isDeleted = 0")
    suspend fun getAllActiveList(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET isDeleted = 1, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("UPDATE events SET isCompleted = :completed, syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCompletion(
        id: String,
        completed: Boolean,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM events WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncEvents(): List<EventEntity>

    @Query("UPDATE events SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)
}
