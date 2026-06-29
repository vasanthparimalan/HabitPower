package com.example.habitpower.reminder

enum class NotificationChannelType(
    val displayName: String,
    val description: String,
    val isComingSoon: Boolean = false
) {
    HABIT_REMINDERS(
        "Habit Reminders",
        "Individual reminders at your commitment time for each habit."
    ),
    DAILY_BRIEF(
        "Morning Brief",
        "A daily morning summary of yesterday's practice and today's habits."
    ),
    PRACTICE_NUDGE(
        "Practice Nudge",
        "A midday and evening check-in when habits are still pending."
    ),
    WEEKLY_INSIGHT(
        "Weekly Insight",
        "A Sunday evening summary of your week's patterns."
    ),
    SEASON_REVIEW(
        "Season Review",
        "A reflection every 90 days on your growth and patterns.",
        isComingSoon = true
    )
}
