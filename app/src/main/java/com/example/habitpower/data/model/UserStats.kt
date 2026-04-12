package com.example.habitpower.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Persisted gamification state per user.
 * Updated whenever a day is marked complete or habits are checked in.
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val userId: Long,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXp: Int = 0,
    val level: Int = 1,
    val totalHabitsCompleted: Int = 0,
    val totalDaysPerfect: Int = 0,
    val lastPerfectDate: LocalDate? = null,
    /** Bitmask of earned badge IDs from GamificationEngine.Badge */
    val earnedBadgesMask: Long = 0L
)
