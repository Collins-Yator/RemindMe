package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.scheduling.ReminderScheduler
import com.example.data.local.AppDatabase
import com.example.data.local.entity.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED) {

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val activeEntities = db.eventDao().getAllActiveList()
                    val scheduler = ReminderScheduler(context)
                    for (entity in activeEntities) {
                        scheduler.scheduleRemindersForEvent(entity.toDomain())
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
