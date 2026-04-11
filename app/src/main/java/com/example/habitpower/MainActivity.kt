package com.example.habitpower

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.habitpower.ui.admin.AdminAssignmentsScreen
import com.example.habitpower.ui.admin.AdminHabitsScreen
import com.example.habitpower.ui.admin.AdminHomeScreen
import com.example.habitpower.ui.admin.AdminLifeAreasScreen
import com.example.habitpower.ui.admin.AdminUsersScreen
import com.example.habitpower.ui.dashboard.DashboardScreen
import com.example.habitpower.ui.execution.WorkoutRunnerScreen
import com.example.habitpower.ui.exercises.AddEditExerciseScreen
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.routines.AddEditRoutineScreen
import com.example.habitpower.ui.routines.RoutinesSection
import com.example.habitpower.ui.routines.RoutinesScreen
import com.example.habitpower.ui.theme.HabitPowerTheme
import com.example.habitpower.reminder.HabitReminderScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as HabitPowerApp).container.habitPowerRepository
        HabitReminderScheduler.createReminderChannel(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        lifecycleScope.launch {
            repository.seedHabitTrackingIfNeeded()
            repository.prepopulateRoutinesIfNeeded()
            repository.syncHabitReminders()
        }

        setContent {
            HabitPowerTheme {
                HabitPowerAppContent()
            }
        }
    }
}

@Composable
fun HabitPowerAppContent() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Handle deep link from widget
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val screen = activity?.intent?.getStringExtra("screen")
        val userId = activity?.intent
            ?.getLongExtra("userId", -1L)
            ?.takeIf { it >= 0L }
        if (screen != null) {
            val route = if (screen == Screen.DailyCheckIn.baseRoute && userId != null) {
                Screen.DailyCheckIn.createRoute(userId)
            } else if (screen == Screen.DailyCheckIn.baseRoute) {
                Screen.DailyCheckIn.createRoute()
            } else {
                screen
            }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }

    val items = listOf(
        Screen.Dashboard,
        Screen.Routines,
        Screen.Focus,
        Screen.Report
    )

    Scaffold(
        bottomBar = {
            // Only show bottom bar on top level screens
            if (currentDestination?.route in items.map { it.route }) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    Screen.Dashboard -> Icon(Icons.Default.DateRange, contentDescription = "Dashboard")
                                    Screen.Routines -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Routines")
                                    Screen.Focus -> Icon(Icons.Default.PlayArrow, contentDescription = "Focus")
                                    Screen.Report -> Icon(Icons.Default.Insights, contentDescription = "Analytics")
                                    else -> {}
                                }
                            },
                            label = {
                                Text(
                                    when (screen) {
                                        Screen.Dashboard -> "Dashboard"
                                        Screen.Routines -> "Routines"
                                        Screen.Focus -> "Focus"
                                        Screen.Report -> "Analytics"
                                        else -> screen.route.replaceFirstChar { it.uppercase() }
                                    }
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                val targetRoute = when (screen) {
                                    Screen.Focus -> Screen.Focus.createRoute()
                                    else -> screen.route
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(Screen.Routines.route) {
                RoutinesScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(Screen.Exercises.route) {
                RoutinesScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    initialSection = RoutinesSection.EXERCISES
                )
            }
            composable(Screen.Focus.route) {
                com.example.habitpower.ui.pomodoro.PomodoroScreen()
            }
            composable(Screen.AddEditExercise.route) {
                AddEditExerciseScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AddEditRoutine.route) {
                AddEditRoutineScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.ExecuteRoutine.route) {
                WorkoutRunnerScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.DailyCheckIn.route) {
                com.example.habitpower.ui.daily.DailyCheckInScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.Report.route) {
                com.example.habitpower.ui.report.ReportScreen()
            }
            composable(Screen.AdminHome.route) {
                AdminHomeScreen(
                    navigateBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.AdminUsers.route) {
                AdminUsersScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminHabits.route) {
                AdminHabitsScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminLifeAreas.route) {
                AdminLifeAreasScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminNotificationTone.route) {
                com.example.habitpower.ui.admin.AdminNotificationToneScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminAssignments.route) {
                AdminAssignmentsScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminQuotes.route) {
                com.example.habitpower.ui.admin.AdminQuotesScreen(navigateBack = { navController.popBackStack() })
            }
        }
    }
}
