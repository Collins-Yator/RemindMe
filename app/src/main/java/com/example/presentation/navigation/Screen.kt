package com.example.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Calendar : Screen(
        route = "calendar",
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )

    data object Events : Screen(
        route = "events",
        title = "Events",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object CreateEdit : Screen(
        route = "create_edit?eventId={eventId}&initialDateMillis={initialDateMillis}",
        title = "Create Event"
    ) {
        fun createRoute(eventId: String? = null, initialDateMillis: Long? = null): String {
            val idParam = eventId ?: ""
            val dateParam = initialDateMillis ?: -1L
            return "create_edit?eventId=$idParam&initialDateMillis=$dateParam"
        }
    }

    data object EventDetail : Screen(
        route = "event_detail/{eventId}",
        title = "Event Details"
    ) {
        fun createRoute(eventId: String): String = "event_detail/$eventId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Calendar, Events, Settings)
    }
}
