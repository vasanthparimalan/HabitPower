package com.example.habitpower.data.model

/**
 * Lifecycle state of a habit. Controls whether it appears in daily tracking,
 * reminders, and analytics — without deleting the habit or its history.
 */
enum class HabitLifecycleStatus {
    /** Normal active tracking. Shows in daily check-in and contributes to streak. */
    ACTIVE,

    /**
     * Temporarily suspended (travel, illness, life events).
     * Hidden from daily tracking. Reminders silenced.
     * Does NOT count as a miss — streak is unaffected.
     */
    PAUSED,

    /**
     * Discontinued habit. Excluded from daily tracking and analytics.
     * Full entry history is preserved for reporting and export.
     */
    RETIRED,

    /**
     * Habit is now automatic behavior — internalized, no longer needs tracking.
     * Excluded from daily tracking. Shown on the Identity Wall as a permanent
     * record of who the user has become.
     */
    GRADUATED;

    val label: String get() = when (this) {
        ACTIVE -> "Active"
        PAUSED -> "Paused"
        RETIRED -> "Retired"
        GRADUATED -> "Graduated"
    }

    val description: String get() = when (this) {
        ACTIVE -> "Tracked daily."
        PAUSED -> "Temporarily suspended. Streak unaffected."
        RETIRED -> "Discontinued. History preserved."
        GRADUATED -> "Internalized. Shown on Identity Wall."
    }

    /** Whether the habit should appear in daily tracking and count toward streak/analytics. */
    val isTracked: Boolean get() = this == ACTIVE
}
