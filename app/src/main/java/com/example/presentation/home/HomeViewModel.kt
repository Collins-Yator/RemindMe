package com.example.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.User
import com.example.domain.repository.EventRepository
import com.example.domain.repository.UserRepository
import com.example.domain.usecase.DashboardData
import com.example.domain.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    getDashboardDataUseCase: GetDashboardDataUseCase = GetDashboardDataUseCase(eventRepository)
) : ViewModel() {

    val dashboardState: StateFlow<DashboardData> = getDashboardDataUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardData(
                greeting = "Welcome",
                todayTotalCount = 0,
                todayCompletedCount = 0,
                todayUpcomingCount = 0,
                nextEvent = null,
                nextEventTimeText = null,
                nextEventStartsInText = null,
                todayTimelineEvents = emptyList(),
                upcomingEvents = emptyList()
            )
        )

    val currentUser: StateFlow<User?> = userRepository.observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleEventCompletion(eventId: String, isCurrentlyCompleted: Boolean) {
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(eventId, !isCurrentlyCompleted)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            eventRepository.syncWithServer()
        }
    }
}
