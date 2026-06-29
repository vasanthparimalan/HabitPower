package com.example.habitpower.ui.navigation

import java.time.LocalDate

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Routines : Screen("routines")
    object Exercises : Screen("exercises")
    object AddEditExercise : Screen("exercise/edit?exerciseId={exerciseId}") {
        fun createRoute(exerciseId: Long? = null) = "exercise/edit?exerciseId=${exerciseId ?: -1L}"
    }
    object AddEditRoutine : Screen("routine/edit?routineId={routineId}") {
        fun createRoute(routineId: Long? = null) = "routine/edit?routineId=${routineId ?: -1L}"
    }
    object ExecuteRoutine : Screen("routine/execute/{routineId}") {
        fun createRoute(routineId: Long) = "routine/execute/$routineId"
    }
    object DailyCheckIn : Screen("daily_check_in/{userId}/{date}") {
        const val baseRoute = "daily_check_in"

        fun createRoute(userId: Long? = null, date: LocalDate? = null): String {
            val resolvedDate = date ?: LocalDate.now()
            return "$baseRoute/${userId ?: -1L}/$resolvedDate"
        }
    }
    object Report : Screen("report")
    object Help : Screen("help")
    object AdminHome : Screen("admin")
    object AdminUsers : Screen("admin/users")
    object AdminHabits : Screen("admin/habits?editHabitId={editHabitId}") {
        fun createRoute(editHabitId: Long? = null): String =
            "admin/habits?editHabitId=${editHabitId ?: -1L}"
    }
    object AdminLifeAreas : Screen("admin/life-areas")
    object AdminAssignments : Screen("admin/assignments")
    object AdminNotificationTone : Screen("admin/notification-tone")
    object AdminNotificationChannels : Screen("admin/notification-channels")
    object Focus : Screen("focus_launcher")
    object FocusPomodoro : Screen("focus?habitId={habitId}") {
        const val baseRoute = "focus"

        fun createRoute(habitId: Long? = null): String {
            return "$baseRoute?habitId=${habitId ?: -1L}"
        }
    }
    object AdminQuotes : Screen("admin/quotes")
    object AdminExport : Screen("admin/export")
    object AdminImport : Screen("admin/import")
    object YearInReview : Screen("report/year-in-review")
    object LibraryBrowse : Screen("exercises/library")
    object ImportPack : Screen("exercises/import")
    object MissedDayWelcome : Screen("missed_day_welcome/{daysAbsent}") {
        fun createRoute(daysAbsent: Long) = "missed_day_welcome/$daysAbsent"
    }
    object StepBack : Screen("step_back")
    object Meditation : Screen("focus/meditation")
    object Chant : Screen("focus/chant")
    object Tasks : Screen("tasks")
    object AdminDriveSync : Screen("admin/drive-sync")
    object SeasonReview : Screen("report/season-review")
    object HabitInventory : Screen("report/habit-inventory")
    object SelfStandup : Screen("standup")
    object HabitLibrary : Screen("habits/library")
}
