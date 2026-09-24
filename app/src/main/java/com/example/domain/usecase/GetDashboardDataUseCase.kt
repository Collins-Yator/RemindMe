package com.example.domain.usecase

import com.example.domain.model.Event
import com.example.domain.model.RecurrenceRule
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class DashboardData(
    val greeting: String,
    val todayTotalCount: Int,
    val todayCompletedCount: Int,
    val todayUpcomingCount: Int,
    val nextEvent: Event?,
    val nextEventTimeText: String?,
    val nextEventStartsInText: String?,
    val todayTimelineEvents: List<Event>,
    val upcomingEvents: List<Event>
)

class GetDashboardDataUseCase(private val eventRepository: EventRepository) {

    operator fun invoke(): Flow<DashboardData> {
        return eventRepository.observeAllEvents().map { allEvents ->
            val nowCal = Calendar.getInstance()
            val nowMillis = nowCal.timeInMillis
            val todayYear = nowCal.get(Calendar.YEAR)
            val todayMonth = nowCal.get(Calendar.MONTH)
            val todayDay = nowCal.get(Calendar.DAY_OF_MONTH)

            val greeting = when (nowCal.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }

            // Events occurring today
            val todayEvents = allEvents.filter { event ->
                event.recurrence.occursOnDate(event.startDateTime, todayYear, todayMonth, todayDay)
            }.sortedBy { it.startDateTime }

            val completedCount = todayEvents.count { it.isCompleted }
            val upcomingCount = todayEvents.size - completedCount

            // Next event: nearest upcoming incomplete event (either today later, or future day)
            val upcomingIncomplete = allEvents.filter { !it.isCompleted }.mapNotNull { event ->
                val nextOccurrence = if (event.recurrence != RecurrenceRule.NONE) {
                    event.recurrence.nextOccurrenceAfter(nowMillis, event.startDateTime)
                } else {
                    event.startDateTime
                }
                if (nextOccurrence >= nowMillis - 60_000L) {
                    Pair(event, nextOccurrence)
                } else null
            }.sortedBy { it.second }

            val nextItem = upcomingIncomplete.firstOrNull()
            val nextEvent = nextItem?.first
            val nextTimeMillis = nextItem?.second

            val (timeText, startsInText) = if (nextEvent != null && nextTimeMillis != null) {
                val diffMinutes = ((nextTimeMillis - nowMillis) / (60 * 1000)).toInt()
                val text = when {
                    diffMinutes <= 0 -> "Starts now"
                    diffMinutes == 1 -> "Starts in 1 minute"
                    diffMinutes < 60 -> "Starts in $diffMinutes minutes"
                    diffMinutes < 1440 -> "Starts in ${diffMinutes / 60} hour(s)"
                    else -> "Starts in ${diffMinutes / 1440} day(s)"
                }
                val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                Pair(format.format(java.util.Date(nextTimeMillis)), text)
            } else {
                Pair(null, null)
            }

            // Upcoming days events (occurring after today)
            val endOfTodayCal = Calendar.getInstance().apply {
                set(todayYear, todayMonth, todayDay, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val upcomingList = allEvents.filter { event ->
                val nextOccur = if (event.recurrence != RecurrenceRule.NONE) {
                    event.recurrence.nextOccurrenceAfter(endOfTodayCal.timeInMillis, event.startDateTime)
                } else {
                    event.startDateTime
                }
                nextOccur > endOfTodayCal.timeInMillis && !event.isCompleted
            }.sortedBy { it.startDateTime }.take(10)

            DashboardData(
                greeting = greeting,
                todayTotalCount = todayEvents.size,
                todayCompletedCount = completedCount,
                todayUpcomingCount = upcomingCount,
                nextEvent = nextEvent,
                nextEventTimeText = timeText,
                nextEventStartsInText = startsInText,
                todayTimelineEvents = todayEvents,
                upcomingEvents = upcomingList
            )
        }
    }
}
