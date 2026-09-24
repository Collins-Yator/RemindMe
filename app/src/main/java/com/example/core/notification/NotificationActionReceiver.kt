package com.example.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(NotificationHelper.EXTRA_EVENT_ID) ?: return
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, eventId.hashCode())

        val notifManager = NotificationManagerCompat.from(context)
        notifManager.cancel(notifId)

        when (intent.action) {
            NotificationHelper.ACTION_MARK_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        db.eventDao().updateCompletion(eventId, true)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            NotificationHelper.ACTION_SNOOZE_10M -> {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val snoozeTimeMillis = System.currentTimeMillis() + (10 * 60 * 1000)

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(AlarmReceiver.EXTRA_EVENT_ID, eventId)
                    putExtra(AlarmReceiver.EXTRA_OFFSET_MINUTES, 10)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId.hashCode() + 9999,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTimeMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTimeMillis,
                            pendingIntent
                        )
                    }
                } catch (_: SecurityException) {
                    // Exact alarm permission not granted
                }
            }
        }
    }
}
