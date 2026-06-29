package com.example.habitpower.data

data class WidgetState(
    val userName: String = "",
    /** Only pending (incomplete) habits for today. Completed ones are excluded from the list. */
    val habits: List<WidgetHabit> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val dailyIntention: String = ""
)

data class WidgetHabit(
    val habitId: Long = 0,
    val name: String,
    val streak: Int,
    val navigateTo: String = "daily_check_in",
    val isCompleted: Boolean = false,
    val isBoolean: Boolean = false
)
