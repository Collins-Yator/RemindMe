package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.core.notification.NotificationHelper
import com.example.core.scheduling.ReminderScheduler
import com.example.core.scheduling.SyncWorker
import com.example.data.local.AppDatabase
import com.example.data.local.entity.toEntity
import com.example.domain.model.Event
import com.example.domain.model.EventCategory
import com.example.domain.model.RecurrenceRule
import com.example.domain.model.ReminderOffset
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class RemindMeApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        SyncWorker.schedulePeriodicSync(this)
        seedInitialEventsIfEmpty()
    }

    private fun seedInitialEventsIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@RemindMeApp)
            val dao = db.eventDao()
            val existing = dao.getAllActiveList()
            if (existing.isEmpty()) {
                val nowCal = Calendar.getInstance()
                val currentYear = nowCal.get(Calendar.YEAR)
                val currentMonth = nowCal.get(Calendar.MONTH)
                val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)

                // Event 1: Today 10:00 AM - Android Development Class
                val cal1 = Calendar.getInstance().apply {
                    set(currentYear, currentMonth, currentDay, 10, 0, 0)
                }
                // Event 2: Today 2:00 PM - Team Project Meeting
                val cal2 = Calendar.getInstance().apply {
                    set(currentYear, currentMonth, currentDay, 14, 0, 0)
                }
                // Event 3: Today 6:00 PM - Gym & Fitness Session
                val cal3 = Calendar.getInstance().apply {
                    set(currentYear, currentMonth, currentDay, 18, 0, 0)
                }
                // Event 4: Tomorrow 11:30 AM - Submit Assignment
                val cal4 = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 11)
                    set(Calendar.MINUTE, 30)
                }
                // Event 5: In 3 days - Family Dinner
                val cal5 = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 3)
                    set(Calendar.HOUR_OF_DAY, 19)
                    set(Calendar.MINUTE, 0)
                }

                val sampleEvents = listOf(
                    Event(
                        id = UUID.randomUUID().toString(),
                        userId = "user_collins_44",
                        title = "Android Development Class",
                        description = "Lecture on Jetpack Compose, Room database, and WorkManager background architecture.",
                        startDateTime = cal1.timeInMillis,
                        endDateTime = cal1.timeInMillis + (90 * 60 * 1000),
                        location = "Engineering Block 3B",
                        category = EventCategory.SCHOOL,
                        recurrence = RecurrenceRule.WEEKLY,
                        colorHex = EventCategory.SCHOOL.defaultColorHex,
                        reminderOffsets = listOf(ReminderOffset.HOURS_1, ReminderOffset.MINUTES_15),
                        notificationEnabled = true,
                        emailEnabled = true,
                        isCompleted = false,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Event(
                        id = UUID.randomUUID().toString(),
                        userId = "user_collins_44",
                        title = "Team Project Meeting",
                        description = "Discuss sprint deliverables, PostgreSQL schema migration, and notification flows.",
                        startDateTime = cal2.timeInMillis,
                        endDateTime = cal2.timeInMillis + (60 * 60 * 1000),
                        location = "Conference Room Alpha",
                        category = EventCategory.MEETING,
                        recurrence = RecurrenceRule.NONE,
                        colorHex = EventCategory.MEETING.defaultColorHex,
                        reminderOffsets = listOf(ReminderOffset.MINUTES_30, ReminderOffset.AT_TIME),
                        notificationEnabled = true,
                        emailEnabled = true,
                        isCompleted = false,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Event(
                        id = UUID.randomUUID().toString(),
                        userId = "user_collins_44",
                        title = "Gym & Workout Session",
                        description = "Upper body training and 25-minute cardio routine.",
                        startDateTime = cal3.timeInMillis,
                        endDateTime = cal3.timeInMillis + (60 * 60 * 1000),
                        location = "Campus Fitness Hub",
                        category = EventCategory.EXERCISE,
                        recurrence = RecurrenceRule.WEEKLY,
                        colorHex = EventCategory.EXERCISE.defaultColorHex,
                        reminderOffsets = listOf(ReminderOffset.MINUTES_15),
                        notificationEnabled = true,
                        emailEnabled = false,
                        isCompleted = false,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Event(
                        id = UUID.randomUUID().toString(),
                        userId = "user_collins_44",
                        title = "Submit Mobile Architecture Assignment",
                        description = "Upload repository link, technical design document, and APK release artifact.",
                        startDateTime = cal4.timeInMillis,
                        endDateTime = cal4.timeInMillis + (30 * 60 * 1000),
                        location = "Online Portal",
                        category = EventCategory.DEADLINE,
                        recurrence = RecurrenceRule.NONE,
                        colorHex = EventCategory.DEADLINE.defaultColorHex,
                        reminderOffsets = listOf(ReminderOffset.HOURS_24, ReminderOffset.HOURS_1),
                        notificationEnabled = true,
                        emailEnabled = true,
                        isCompleted = false,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Event(
                        id = UUID.randomUUID().toString(),
                        userId = "user_collins_44",
                        title = "Family Dinner & Gathering",
                        description = "Weekend family dinner at Bistro Garden.",
                        startDateTime = cal5.timeInMillis,
                        endDateTime = cal5.timeInMillis + (120 * 60 * 1000),
                        location = "Bistro Garden",
                        category = EventCategory.PERSONAL,
                        recurrence = RecurrenceRule.NONE,
                        colorHex = EventCategory.PERSONAL.defaultColorHex,
                        reminderOffsets = listOf(ReminderOffset.HOURS_24),
                        notificationEnabled = true,
                        emailEnabled = false,
                        isCompleted = false,
                        syncStatus = SyncStatus.SYNCED
                    )
                )

                dao.insertAll(sampleEvents.map { it.toEntity() })
                val scheduler = ReminderScheduler(this@RemindMeApp)
                for (event in sampleEvents) {
                    scheduler.scheduleRemindersForEvent(event)
                }
            }
        }
    }
}
