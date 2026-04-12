package com.example.habitpower.gamification

import com.example.habitpower.data.model.UserStats

/**
 * Pure, side-effect-free gamification logic.
 *
 * No Android dependencies — safe to unit test.
 */
object GamificationEngine {

    // ── XP values ────────────────────────────────────────────────────────────
    private const val XP_PER_HABIT = 10
    private const val XP_DAY_COMPLETE_BONUS = 20
    private const val XP_STREAK_BONUS_PER_DAY = 5
    private const val XP_STREAK_BONUS_CAP = 50

    // ── Level thresholds (index = level - 1) ─────────────────────────────────
    private val LEVEL_XP_THRESHOLDS = intArrayOf(
        0,       // Level 1
        100,     // Level 2
        250,     // Level 3
        500,     // Level 4
        800,     // Level 5
        1_200,   // Level 6
        1_700,   // Level 7
        2_300,   // Level 8
        3_000,   // Level 9
        4_000,   // Level 10
        5_200,   // Level 11
        6_600,   // Level 12
        8_200,   // Level 13
        10_000,  // Level 14
        12_000,  // Level 15
        14_500,  // Level 16
        17_500,  // Level 17
        21_000,  // Level 18
        25_000,  // Level 19
        30_000   // Level 20 (max)
    )

    val MAX_LEVEL = LEVEL_XP_THRESHOLDS.size

    private val LEVEL_NAMES = arrayOf(
        "Seeker",        // 1
        "Starter",       // 2
        "Builder",       // 3
        "Achiever",      // 4
        "Warrior",       // 5
        "Contender",     // 6
        "Champion",      // 7
        "Trailblazer",   // 8
        "Master",        // 9
        "Legend",        // 10
        "Titan",         // 11
        "Vanguard",      // 12
        "Sage",          // 13
        "Visionary",     // 14
        "Luminary",      // 15
        "Ascendant",     // 16
        "Transcendent",  // 17
        "Paragon",       // 18
        "Sovereign",     // 19
        "Enlightened"    // 20
    )

    // ── Badge bitmask constants ───────────────────────────────────────────────
    object Badge {
        const val FIRST_STEP: Long      = 1L shl 0  // first habit ever checked
        const val STREAK_3: Long        = 1L shl 1
        const val STREAK_7: Long        = 1L shl 2
        const val STREAK_14: Long       = 1L shl 3
        const val STREAK_30: Long       = 1L shl 4
        const val STREAK_100: Long      = 1L shl 5
        const val CENTURY: Long         = 1L shl 6  // 100 total habits done
        const val LEVEL_5: Long         = 1L shl 7
        const val LEVEL_10: Long        = 1L shl 8
        const val LEVEL_MAX: Long       = 1L shl 9  // reached level 20
        const val PERFECT_WEEK: Long    = 1L shl 10 // 7 consecutive perfect days

        data class Metadata(val id: Long, val emoji: String, val name: String, val description: String)

        val ALL: List<Metadata> = listOf(
            Metadata(FIRST_STEP,   "🌱", "First Step",      "Completed your first habit check-in."),
            Metadata(STREAK_3,     "🔥", "On Fire",         "3-day streak — the momentum has started!"),
            Metadata(STREAK_7,     "⚡", "Weekly Warrior",  "7 consecutive perfect days!"),
            Metadata(STREAK_14,    "💪", "Fortnight Force", "14 days strong — you're a force of nature."),
            Metadata(STREAK_30,    "🏆", "Monthly Master",  "30 days straight. Pure discipline."),
            Metadata(STREAK_100,   "👑", "Century King",    "100-day streak. Legendary."),
            Metadata(CENTURY,      "💯", "Centurion",       "Checked off 100 individual habits total."),
            Metadata(LEVEL_5,      "⭐", "Rising Star",     "Reached Level 5 — Warrior!"),
            Metadata(LEVEL_10,     "🌟", "Decade Legend",   "Reached Level 10 — Legend!"),
            Metadata(LEVEL_MAX,    "🔮", "Enlightened",     "Maximum level reached. Truly enlightened."),
            Metadata(PERFECT_WEEK, "🎯", "Perfect Week",    "7 consecutive days of completing every habit.")
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun levelForXp(xp: Int): Int {
        for (i in LEVEL_XP_THRESHOLDS.indices.reversed()) {
            if (xp >= LEVEL_XP_THRESHOLDS[i]) return i + 1
        }
        return 1
    }

    fun levelName(level: Int): String =
        LEVEL_NAMES.getOrElse(level - 1) { LEVEL_NAMES.last() }

    /** XP required to reach [level] (0-based start of that level). */
    fun xpForLevel(level: Int): Int =
        LEVEL_XP_THRESHOLDS.getOrElse(level - 1) { LEVEL_XP_THRESHOLDS.last() }

    /** XP required to reach the next level after [level]. Returns null at max level. */
    fun xpForNextLevel(level: Int): Int? =
        if (level >= MAX_LEVEL) null else LEVEL_XP_THRESHOLDS.getOrNull(level)

    /**
     * Fraction (0f–1f) of progress within the current level band.
     */
    fun levelProgress(xp: Int): Float {
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return 1f
        val current = xpForLevel(level)
        val next = xpForNextLevel(level) ?: return 1f
        return ((xp - current).toFloat() / (next - current).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Compute the XP earned for completing [habitsCompletedToday] habits on a day
     * where the streak is [currentStreak].
     *
     * [isDayPerfect] = all assigned habits were completed.
     */
    fun computeXpGain(habitsCompletedToday: Int, isDayPerfect: Boolean, currentStreak: Int): Int {
        var xp = habitsCompletedToday * XP_PER_HABIT
        if (isDayPerfect) {
            xp += XP_DAY_COMPLETE_BONUS
            val streakBonus = (currentStreak * XP_STREAK_BONUS_PER_DAY).coerceAtMost(XP_STREAK_BONUS_CAP)
            xp += streakBonus
        }
        return xp
    }

    /**
     * Determine which new badges are earned given the updated stats.
     * Returns only *newly* earned badges (not already in [existingMask]).
     */
    fun computeNewBadgesMask(stats: UserStats): Long {
        var newMask = 0L
        fun check(bit: Long, condition: Boolean) {
            if (condition && (stats.earnedBadgesMask and bit) == 0L) newMask = newMask or bit
        }
        check(Badge.FIRST_STEP,   stats.totalHabitsCompleted >= 1)
        check(Badge.STREAK_3,     stats.currentStreak >= 3)
        check(Badge.STREAK_7,     stats.currentStreak >= 7)
        check(Badge.STREAK_14,    stats.currentStreak >= 14)
        check(Badge.STREAK_30,    stats.currentStreak >= 30)
        check(Badge.STREAK_100,   stats.currentStreak >= 100)
        check(Badge.CENTURY,      stats.totalHabitsCompleted >= 100)
        check(Badge.LEVEL_5,      stats.level >= 5)
        check(Badge.LEVEL_10,     stats.level >= 10)
        check(Badge.LEVEL_MAX,    stats.level >= MAX_LEVEL)
        check(Badge.PERFECT_WEEK, stats.currentStreak >= 7)
        return newMask
    }

    /**
     * Compute updated [UserStats] after a day's check-in.
     *
     * @param existing      Current persisted stats (or default for first time).
     * @param habitsCompleted Number of habits completed today.
     * @param totalHabits   Total assigned habits for today.
     * @param isDayPerfect  True when every habit is checked off.
     */
    fun computeUpdatedStats(
        existing: UserStats,
        habitsCompleted: Int,
        totalHabits: Int,
        isDayPerfect: Boolean
    ): Pair<UserStats, Long> { // returns (newStats, newBadgesMask)
        val hasAssignedHabits = totalHabits > 0
        val perfectDay = isDayPerfect && hasAssignedHabits
        val newStreak = if (perfectDay) existing.currentStreak + 1 else 0
        val newLongest = maxOf(existing.longestStreak, newStreak)
        val xpGain = computeXpGain(habitsCompleted, perfectDay, newStreak)
        val newXp = existing.totalXp + xpGain
        val newLevel = levelForXp(newXp)
        val newTotalHabits = existing.totalHabitsCompleted + habitsCompleted
        val newTotalDays = if (perfectDay) existing.totalDaysPerfect + 1 else existing.totalDaysPerfect

        val draft = existing.copy(
            currentStreak = newStreak,
            longestStreak = newLongest,
            totalXp = newXp,
            level = newLevel,
            totalHabitsCompleted = newTotalHabits,
            totalDaysPerfect = newTotalDays
        )
        val newBadges = computeNewBadgesMask(draft)
        val finalStats = draft.copy(earnedBadgesMask = draft.earnedBadgesMask or newBadges)
        return Pair(finalStats, newBadges)
    }

    /** Human-readable summary line for a streak, e.g. "🔥 12-day streak". */
    fun streakLabel(streak: Int): String = when {
        streak <= 0 -> "No streak yet"
        streak == 1 -> "🔥 1-day streak — keep going!"
        streak < 7  -> "🔥 $streak-day streak"
        streak < 14 -> "⚡ $streak-day STREAK!"
        streak < 30 -> "💪 $streak-day POWER STREAK!"
        streak < 100 -> "🏆 $streak-day LEGENDARY STREAK!!"
        else        -> "👑 $streak-day CENTURY STREAK — UNSTOPPABLE!!"
    }

    /** Short label for XP progress, e.g. "1,200 / 1,700 XP". */
    fun xpLabel(xp: Int): String {
        val level = levelForXp(xp)
        val next = xpForNextLevel(level)
        return if (next == null) "$xp XP (MAX)" else "$xp / $next XP"
    }
}
