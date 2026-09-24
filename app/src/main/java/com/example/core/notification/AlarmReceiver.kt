package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.remote.ApiClient
import com.example.data.remote.dto.SendEmailReminderRequest
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.model.EventCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val entity = db.eventDao().getById(eventId)
                if (entity != null && !entity.isDeleted && !entity.isCompleted) {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val timeFormatted = timeFormat.format(Date(entity.startDateTime))

                    val startsInText = when (offsetMinutes) {
                        0 -> "Starts now"
                        60 -> "Starts in 1 hour"
                        1440 -> "Starts tomorrow"
                        else -> "Starts in $offsetMinutes minutes"
                    }

                    if (entity.notificationEnabled) {
                        NotificationHelper.showEventNotification(
                            context = context,
                            eventId = entity.id,
                            title = entity.title,
                            startsInText = startsInText,
                            timeFormatted = timeFormatted,
                            location = entity.location,
                            isImportant = entity.category == EventCategory.IMPORTANT || entity.category == EventCategory.DEADLINE
                        )
                    }

                    if (entity.emailEnabled) {
                        val userRepo = UserRepositoryImpl(context)
                        val currentUser = userRepo.getCurrentUser()
                        val recipient = currentUser?.defaultEmailAddress ?: UserRepositoryImpl.DEFAULT_DEVELOPMENT_RECIPIENT

                        try {
                            ApiClient.getService().sendEmailReminder(
                                SendEmailReminderRequest(
                                    eventId = entity.id,
                                    recipientEmail = recipient,
                                    eventTitle = entity.title,
                                    eventTimeFormatted = timeFormatted,
                                    location = entity.location,
                                    startsInText = startsInText
                                )
                            )
                        } catch (_: Exception) {
                            // Handled gracefully
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "com.example.remindme.EXTRA_EVENT_ID"
        const val EXTRA_OFFSET_MINUTES = "com.example.remindme.EXTRA_OFFSET_MINUTES"
    }
}
