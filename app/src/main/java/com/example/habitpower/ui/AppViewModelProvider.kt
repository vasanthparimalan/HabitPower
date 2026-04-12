package com.example.habitpower.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.ui.admin.AdminAssignmentsViewModel
import com.example.habitpower.ui.admin.AdminHabitsViewModel
import com.example.habitpower.ui.admin.AdminLifeAreasViewModel
import com.example.habitpower.ui.admin.AdminUsersViewModel
import com.example.habitpower.ui.daily.DailyCheckInViewModel
import com.example.habitpower.ui.dashboard.DashboardViewModel
import com.example.habitpower.ui.execution.WorkoutRunnerViewModel
import com.example.habitpower.ui.exercises.AddEditExerciseViewModel
import com.example.habitpower.ui.exercises.ExercisesViewModel
import com.example.habitpower.ui.routines.AddEditRoutineViewModel
import com.example.habitpower.ui.routines.ExecuteRoutineRouterViewModel
import com.example.habitpower.ui.routines.RoutinesViewModel
import com.example.habitpower.ui.routines.TimedRoutineExecutorViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ExercisesViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            AddEditExerciseViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository
            )
        }
        initializer {
            RoutinesViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            AddEditRoutineViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository
            )
        }
        initializer {
            TimedRoutineExecutorViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository
            )
        }
        initializer {
            ExecuteRoutineRouterViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository
            )
        }
        initializer {
            WorkoutRunnerViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository
            )
        }
        initializer {
            DashboardViewModel(
                habitPowerApplication().container.habitPowerRepository,
                habitPowerApplication()
            )
        }
        initializer {
            DailyCheckInViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository
            )
        }
        initializer {
            com.example.habitpower.ui.report.ReportViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            AdminUsersViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            AdminLifeAreasViewModel(habitPowerApplication().container.lifeAreaRepository)
        }
        initializer {
            AdminHabitsViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            AdminAssignmentsViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            com.example.habitpower.ui.pomodoro.PomodoroViewModel(
                this.createSavedStateHandle(),
                habitPowerApplication().container.habitPowerRepository,
                habitPowerApplication().container.userPreferencesRepository
            )
        }
        initializer {
            com.example.habitpower.ui.admin.AdminQuotesViewModel(habitPowerApplication().container.habitPowerRepository)
        }
        initializer {
            com.example.habitpower.ui.admin.AdminNotificationToneViewModel(
                habitPowerApplication().container.userPreferencesRepository
            )
        }
        initializer {
            com.example.habitpower.ui.gamification.GamificationViewModel(
                habitPowerApplication().container.habitPowerRepository,
                habitPowerApplication().container.gamificationRepository
            )
        }
        // Add other ViewModels here as we create them
    }
}

fun CreationExtras.habitPowerApplication(): HabitPowerApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HabitPowerApp)
