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
    object AdminHabits : Screen("admin/habits")
    object AdminLifeAreas : Screen("admin/life-areas")
    object AdminAssignments : Screen("admin/assignments")
    object AdminNotificationTone : Screen("admin/notification-tone")
    object Focus : Screen("focus?habitId={habitId}") {
        const val baseRoute = "focus"

        fun createRoute(habitId: Long? = null): String {
            return "$baseRoute?habitId=${habitId ?: -1L}"
        }
    }
    object AdminQuotes : Screen("admin/quotes")
}
