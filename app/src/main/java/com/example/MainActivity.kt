package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.notification.NotificationHelper
import com.example.data.repository.EventRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.presentation.calendar.CalendarScreen
import com.example.presentation.calendar.CalendarViewModel
import com.example.presentation.create_edit.CreateEditScreen
import com.example.presentation.create_edit.CreateEditViewModel
import com.example.presentation.detail.EventDetailScreen
import com.example.presentation.detail.EventDetailViewModel
import com.example.presentation.events.EventsScreen
import com.example.presentation.events.EventsViewModel
import com.example.presentation.home.HomeScreen
import com.example.presentation.home.HomeViewModel
import com.example.presentation.navigation.Screen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.ui.theme.RemindMeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialEventId = intent?.getStringExtra(NotificationHelper.EXTRA_EVENT_ID)

        setContent {
            RemindMeTheme {
                MainAppContent(initialEventId = initialEventId)
            }
        }
    }
}

@Composable
fun MainAppContent(initialEventId: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Dependency Repositories
    val eventRepository = remember { EventRepositoryImpl(context) }
    val userRepository = remember { UserRepositoryImpl(context) }

    // ViewModels
    val homeViewModel = remember { HomeViewModel(eventRepository, userRepository) }
    val calendarViewModel = remember { CalendarViewModel(eventRepository) }
    val eventsViewModel = remember { EventsViewModel(eventRepository) }
    val createEditViewModel = remember { CreateEditViewModel(eventRepository, userRepository) }
    val eventDetailViewModel = remember { EventDetailViewModel(eventRepository) }
    val settingsViewModel = remember { SettingsViewModel(userRepository, eventRepository) }

    // Request Notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Direct navigation if launched from notification
    LaunchedEffect(initialEventId) {
        if (!initialEventId.isNullOrBlank()) {
            navController.navigate(Screen.EventDetail.createRoute(initialEventId))
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelDestination,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(text = screen.title) },
                            modifier = Modifier.testTag("nav_${screen.route}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateEdit.createRoute())
                    },
                    onNavigateToDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onNavigateToCreate = { dateMillis ->
                        navController.navigate(Screen.CreateEdit.createRoute(initialDateMillis = dateMillis))
                    },
                    onNavigateToDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }

            composable(Screen.Events.route) {
                EventsScreen(
                    viewModel = eventsViewModel,
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateEdit.createRoute())
                    },
                    onNavigateToDetail = { eventId ->
                        navController.navigate(Screen.EventDetail.createRoute(eventId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable(
                route = Screen.CreateEdit.route,
                arguments = listOf(
                    navArgument("eventId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("initialDateMillis") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                val initialDateMillis = backStackEntry.arguments?.getLong("initialDateMillis")
                CreateEditScreen(
                    viewModel = createEditViewModel,
                    eventId = if (eventId.isNullOrBlank()) null else eventId,
                    initialDateMillis = if (initialDateMillis == null || initialDateMillis <= 0L) null else initialDateMillis,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EventDetail.route,
                arguments = listOf(
                    navArgument("eventId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                EventDetailScreen(
                    viewModel = eventDetailViewModel,
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { editId ->
                        navController.navigate(Screen.CreateEdit.createRoute(eventId = editId))
                    }
                )
            }
        }
    }
}
