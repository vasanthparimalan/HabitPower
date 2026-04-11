package com.example.habitpower.data

data class WidgetState(
    val userName: String = "",
    val habits: List<WidgetHabit> = emptyList()
)

data class WidgetHabit(
    val name: String,
    val isCompleted: Boolean,
    val streak: Int
)
