package com.example.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Event
import com.example.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarUiState(
    val displayedYear: Int,
    val displayedMonth: Int, // 0-indexed (0 = Jan, 8 = Sep)
    val selectedYear: Int,
    val selectedMonth: Int,
    val selectedDay: Int,
    val eventsForSelectedDay: List<Event> = emptyList(),
    val eventDatesInMonth: Map<Int, List<String>> = emptyMap() // dayOfMonth -> List of category color hexes
)

class CalendarViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val nowCal = Calendar.getInstance()
    private val displayedYearMonth = MutableStateFlow(
        Pair(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))
    )
    private val selectedDate = MutableStateFlow(
        Triple(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH))
    )

    val uiState: StateFlow<CalendarUiState> = combine(
        displayedYearMonth,
        selectedDate,
        eventRepository.observeAllEvents()
    ) { (dispYear, dispMonth), (selYear, selMonth, selDay), allEvents ->
        // Events for selected day
        val selectedDayEvents = allEvents.filter { event ->
            event.recurrence.occursOnDate(event.startDateTime, selYear, selMonth, selDay)
        }.sortedBy { it.startDateTime }

        // Find days in currently displayed month that have events
        val daysInMonthMap = mutableMapOf<Int, MutableList<String>>()
        val cal = Calendar.getInstance().apply {
            set(dispYear, dispMonth, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..maxDays) {
            val eventsOnDay = allEvents.filter { event ->
                event.recurrence.occursOnDate(event.startDateTime, dispYear, dispMonth, day)
            }
            if (eventsOnDay.isNotEmpty()) {
                daysInMonthMap[day] = eventsOnDay.map { it.colorHex }.distinct().take(3).toMutableList()
            }
        }

        CalendarUiState(
            displayedYear = dispYear,
            displayedMonth = dispMonth,
            selectedYear = selYear,
            selectedMonth = selMonth,
            selectedDay = selDay,
            eventsForSelectedDay = selectedDayEvents,
            eventDatesInMonth = daysInMonthMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(
            displayedYear = nowCal.get(Calendar.YEAR),
            displayedMonth = nowCal.get(Calendar.MONTH),
            selectedYear = nowCal.get(Calendar.YEAR),
            selectedMonth = nowCal.get(Calendar.MONTH),
            selectedDay = nowCal.get(Calendar.DAY_OF_MONTH)
        )
    )

    fun onDateSelected(year: Int, month: Int, day: Int) {
        selectedDate.value = Triple(year, month, day)
    }

    fun onPreviousMonth() {
        val (year, month) = displayedYearMonth.value
        val cal = Calendar.getInstance().apply {
            set(year, month, 1)
            add(Calendar.MONTH, -1)
        }
        displayedYearMonth.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun onNextMonth() {
        val (year, month) = displayedYearMonth.value
        val cal = Calendar.getInstance().apply {
            set(year, month, 1)
            add(Calendar.MONTH, 1)
        }
        displayedYearMonth.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun onJumpToToday() {
        val today = Calendar.getInstance()
        val y = today.get(Calendar.YEAR)
        val m = today.get(Calendar.MONTH)
        val d = today.get(Calendar.DAY_OF_MONTH)
        displayedYearMonth.value = Pair(y, m)
        selectedDate.value = Triple(y, m, d)
    }

    fun toggleEventCompletion(eventId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(eventId, !isCompleted)
        }
    }
}
