package com.example.habitpower

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
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
import com.example.habitpower.ui.admin.ExportScreen
import com.example.habitpower.ui.admin.ImportScreen
import com.example.habitpower.ui.dashboard.DashboardScreen
import com.example.habitpower.ui.execution.WorkoutRunnerScreen
import com.example.habitpower.ui.exercises.AddEditExerciseScreen
import com.example.habitpower.ui.exercises.ImportPackScreen
import com.example.habitpower.ui.exercises.LibraryBrowseScreen
import com.example.habitpower.ui.navigation.Screen
import com.example.habitpower.ui.routines.AddEditRoutineScreen
import com.example.habitpower.ui.routines.ExecuteRoutineScreen
import com.example.habitpower.ui.routines.RoutinesSection
import com.example.habitpower.ui.routines.RoutinesScreen
import com.example.habitpower.ui.theme.AppIconography
import com.example.habitpower.ui.theme.HabitPowerTheme
import com.example.habitpower.ui.welcome.MissedDayWelcomeScreen
import com.example.habitpower.reminder.HabitReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

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
            repository.autoResumeExpiredHolds()   // resume time-bound holds whose date passed
            repository.seedHabitTrackingIfNeeded()
            repository.prepopulateRoutinesIfNeeded()
            repository.patchExerciseInstructionsAndSeedYoga()
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

    // Handle deep link from widget and missed-day welcome on launch
    val prefsRepository = (context.applicationContext as HabitPowerApp).container.userPreferencesRepository
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val screen = activity?.intent?.getStringExtra("screen")
        val userId = activity?.intent
            ?.getLongExtra("userId", -1L)
            ?.takeIf { it >= 0L }

        // Widget deep link takes priority — skip welcome screen if navigating somewhere specific
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
        } else {
            // Check for missed-day welcome (2+ days since last open)
            val lastEpochDay = prefsRepository.lastOpenedEpochDay.first()
            val todayEpochDay = LocalDate.now().toEpochDay()
            if (lastEpochDay != null) {
                val daysAbsent = todayEpochDay - lastEpochDay
                if (daysAbsent >= 2) {
                    navController.navigate(Screen.MissedDayWelcome.createRoute(daysAbsent)) {
                        launchSingleTop = true
                    }
                }
            }
            // Save today's open date (whether welcome shown or not)
            prefsRepository.saveLastOpenedEpochDay(todayEpochDay)
        }
    }

    val items = listOf(
        Screen.Dashboard,
        Screen.Tasks,
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
                                    Screen.Dashboard -> Icon(AppIconography.Dashboard, contentDescription = "Dashboard")
                                    Screen.Tasks -> Icon(AppIconography.Tasks, contentDescription = "Tasks")
                                    Screen.Routines -> Icon(AppIconography.Routines, contentDescription = "Routines")
                                    Screen.Focus -> Icon(AppIconography.Focus, contentDescription = "Focus")
                                    Screen.Report -> Icon(AppIconography.Analytics, contentDescription = "Analytics")
                                    else -> {}
                                }
                            },
                            label = {
                                Text(
                                    when (screen) {
                                        Screen.Dashboard -> "Dashboard"
                                        Screen.Tasks -> "Tasks"
                                        Screen.Routines -> "Routines"
                                        Screen.Focus -> "Focus"
                                        Screen.Report -> "Analytics"
                                        else -> screen.route.replaceFirstChar { it.uppercase() }
                                    }
                                )
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(Screen.Tasks.route) {
                com.example.habitpower.ui.tasks.TasksScreen()
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
                com.example.habitpower.ui.focus.FocusLauncherScreen(
                    onStartPomodoro = {
                        navController.navigate(Screen.FocusPomodoro.createRoute())
                    },
                    onStartMeditation = {
                        navController.navigate(Screen.Meditation.route)
                    },
                    onStartChant = {
                        navController.navigate(Screen.Chant.route)
                    }
                )
            }
            composable(Screen.Meditation.route) {
                com.example.habitpower.ui.meditation.MeditationScreen(
                    navigateBack = { navController.popBackStack() },
                    onSessionComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Chant.route) {
                com.example.habitpower.ui.chant.ChantScreen(
                    navigateBack = { navController.popBackStack() },
                    onSessionComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.FocusPomodoro.route) {
                com.example.habitpower.ui.pomodoro.PomodoroScreen(
                    navigateBack = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.AddEditExercise.route) {
                AddEditExerciseScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AddEditRoutine.route) {
                AddEditRoutineScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.ExecuteRoutine.route) {
                ExecuteRoutineScreen(
                    navigateBack = { navController.popBackStack() },
                    onRoutineComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.DailyCheckIn.route) { backStackEntry ->
                val userIdArg = backStackEntry.arguments?.getString("userId")?.toLongOrNull()
                    ?: backStackEntry.arguments?.getLong("userId")
                com.example.habitpower.ui.daily.DailyCheckInScreen(
                    navigateBack = { navController.popBackStack() },
                    onJumpToToday = {
                        navController.navigate(Screen.DailyCheckIn.createRoute(userIdArg, java.time.LocalDate.now())) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Report.route) {
                com.example.habitpower.ui.report.ReportScreen(
                    onNavigateToYearInReview = { navController.navigate(Screen.YearInReview.route) },
                    onNavigateToSeasonReview = { navController.navigate(Screen.SeasonReview.route) },
                    onNavigateToHabitInventory = { navController.navigate(Screen.HabitInventory.route) },
                    onNavigateToStandup = { navController.navigate(Screen.SelfStandup.route) }
                )
            }
            composable(Screen.Help.route) {
                com.example.habitpower.ui.help.HelpGuideScreen(
                    navigateBack = { navController.popBackStack() }
                )
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
            composable(
                route = Screen.AdminHabits.route,
                arguments = listOf(
                    androidx.navigation.navArgument("editHabitId") {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val editHabitId = backStackEntry.arguments?.getLong("editHabitId")
                    ?.takeIf { it > 0 }
                AdminHabitsScreen(
                    navigateBack = { navController.popBackStack() },
                    editHabitId = editHabitId
                )
            }
            composable(Screen.AdminLifeAreas.route) {
                AdminLifeAreasScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminNotificationTone.route) {
                com.example.habitpower.ui.admin.AdminNotificationToneScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminNotificationChannels.route) {
                com.example.habitpower.ui.admin.AdminNotificationChannelsScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AdminAssignments.route) {
                AdminAssignmentsScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminQuotes.route) {
                com.example.habitpower.ui.admin.AdminQuotesScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminExport.route) {
                ExportScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.AdminImport.route) {
                ImportScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.YearInReview.route) {
                com.example.habitpower.ui.report.YearInReviewScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LibraryBrowse.route) {
                LibraryBrowseScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.ImportPack.route) {
                ImportPackScreen(navigateBack = { navController.popBackStack() })
            }
            composable(Screen.StepBack.route) {
                com.example.habitpower.ui.dashboard.StepBackScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MissedDayWelcome.route) {
                MissedDayWelcomeScreen(
                    navigateToDashboard = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.MissedDayWelcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.AdminDriveSync.route) {
                com.example.habitpower.ui.admin.DriveSyncScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SeasonReview.route) {
                com.example.habitpower.ui.report.SeasonReviewScreen(
                    navigateBack = { navController.popBackStack() },
                    onOpenTemplates = { navController.navigate(Screen.HabitLibrary.route) }
                )
            }
            composable(Screen.HabitInventory.route) {
                com.example.habitpower.ui.report.HabitInventoryScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SelfStandup.route) {
                com.example.habitpower.ui.standup.SelfStandupScreen(
                    navigateBack = { navController.popBackStack() },
                    onNavigateToAdminHabits = { navController.navigate(Screen.AdminHabits.createRoute()) }
                )
            }
            composable(Screen.HabitLibrary.route) {
                com.example.habitpower.ui.habits.HabitLibraryScreen(
                    navigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
