package com.example.core.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.core.notification.AlarmReceiver
import com.example.domain.model.Event
import com.example.domain.model.RecurrenceRule

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleRemindersForEvent(event: Event) {
        if (event.isDeleted || event.isCompleted) {
            cancelRemindersForEvent(event)
            return
        }

        val now = System.currentTimeMillis()

        // Determine effective start timestamp: if recurring, find next occurrence
        val effectiveStartMillis = if (event.recurrence != RecurrenceRule.NONE) {
            event.recurrence.nextOccurrenceAfter(now, event.startDateTime)
        } else {
            event.startDateTime
        }

        // Cancel previous alarms for this event first
        cancelRemindersForEvent(event)

        if (alarmManager == null) return

        // For each reminder offset, schedule exact alarm
        for (offset in event.reminderOffsets) {
            val reminderTriggerTime = effectiveStartMillis - (offset.minutesBefore * 60 * 1000L)
            // Only schedule if in future
            if (reminderTriggerTime > now) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
                    putExtra(AlarmReceiver.EXTRA_OFFSET_MINUTES, offset.minutesBefore)
                }

                val requestCode = generateRequestCode(event.id, offset.minutesBefore)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTriggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTriggerTime,
                            pendingIntent
                        )
                    }
                    Log.d("ReminderScheduler", "Scheduled alarm for ${event.title} at $reminderTriggerTime (offset ${offset.minutesBefore}m)")
                } catch (e: SecurityException) {
                    Log.w("ReminderScheduler", "Exact alarm permission missing or restricted: ${e.message}")
                }
            }
        }
    }

    fun cancelRemindersForEvent(event: Event) {
        if (alarmManager == null) return
        for (offset in event.reminderOffsets) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = generateRequestCode(event.id, offset.minutesBefore)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun generateRequestCode(eventId: String, offsetMinutes: Int): Int {
        return (eventId.hashCode() * 31) + offsetMinutes
    }
}
