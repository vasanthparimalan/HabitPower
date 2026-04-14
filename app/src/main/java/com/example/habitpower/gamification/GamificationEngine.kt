package com.example.habitpower.gamification

import com.example.habitpower.data.model.UserStats

/**
 * Pure, side-effect-free gamification logic.
 *
 * No Android dependencies — safe to unit test.
 */
object GamificationEngine {

    // ── XP values ────────────────────────────────────────────────────────────
    private const val XP_FOR_SHOWING_UP = 12
    private const val XP_EXTRA_HABIT_BONUS = 2
    private const val XP_EXTRA_HABIT_BONUS_CAP = 4
    private const val XP_DAY_COMPLETE_BONUS = 8
    private const val XP_STREAK_BONUS_PER_DAY = 6
    private const val XP_STREAK_BONUS_CAP = 90

    // ── Level thresholds (index = level - 1, covers levels 1–20) ────────────
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
        30_000   // Level 20 — Enlightened
    )

    // Levels 21+ use a linear formula: each level needs XP_PER_POST_LEVEL more XP than the last.
    private const val LEGACY_MAX_LEVEL = 20
    private const val XP_PER_POST_LEVEL = 8_000

    // No hard cap — levels extend indefinitely for users who stay for years.
    val MAX_LEVEL = Int.MAX_VALUE

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

    // Distinct titles for levels 21–50. After 50, levelName() returns "Level N".
    private val POST_LEVEL_NAMES = arrayOf(
        "Philosopher",   // 21
        "Virtuoso",      // 22
        "Pathfinder",    // 23
        "Alchemist",     // 24
        "Mastermind",    // 25 — mastery milestone
        "Sentinel",      // 26
        "Crusader",      // 27
        "Harbinger",     // 28
        "Archon",        // 29
        "Eternal",       // 30 — mastery milestone
        "Oracle",        // 31
        "Warlord",       // 32
        "Artisan",       // 33
        "Exemplar",      // 34
        "Apex",          // 35
        "Pinnacle",      // 36
        "Celestial",     // 37
        "Immutable",     // 38
        "Boundless",     // 39
        "Mythic",        // 40 — mastery milestone
        "Infinite",      // 41
        "Timeless",      // 42
        "Cosmic",        // 43
        "Absolute",      // 44
        "Invincible",    // 45
        "Immortal",      // 46
        "Undaunted",     // 47
        "Omnipotent",    // 48
        "Supreme",       // 49
        "Undying"        // 50 — mastery milestone
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
        const val LEVEL_MAX: Long       = 1L shl 9  // reached level 20 — Enlightened
        const val PERFECT_WEEK: Long    = 1L shl 10 // 7 consecutive perfect days
        // Mastery milestones — earned once, never lost
        const val LEVEL_25: Long        = 1L shl 11 // Mastermind
        const val LEVEL_30: Long        = 1L shl 12 // Eternal
        const val LEVEL_40: Long        = 1L shl 13 // Mythic
        const val LEVEL_50: Long        = 1L shl 14 // Undying

        data class Metadata(val id: Long, val emoji: String, val name: String, val description: String)

        val ALL: List<Metadata> = listOf(
            Metadata(FIRST_STEP,   "🌱", "First Step",        "Completed your first habit check-in."),
            Metadata(STREAK_3,     "🔥", "On Fire",           "3-day streak — the momentum has started!"),
            Metadata(STREAK_7,     "⚡", "Weekly Warrior",    "7 consecutive perfect days!"),
            Metadata(STREAK_14,    "💪", "Fortnight Force",   "14 days strong — you're a force of nature."),
            Metadata(STREAK_30,    "🏆", "Monthly Master",    "30 days straight. Pure discipline."),
            Metadata(STREAK_100,   "👑", "Century King",      "100-day streak. Legendary."),
            Metadata(CENTURY,      "💯", "Centurion",         "Checked off 100 individual habits total."),
            Metadata(LEVEL_5,      "⭐", "Rising Star",       "Reached Level 5 — Warrior!"),
            Metadata(LEVEL_10,     "🌟", "Decade Legend",     "Reached Level 10 — Legend!"),
            Metadata(LEVEL_MAX,    "🔮", "Enlightened",       "Reached Level 20. The first pinnacle of mastery."),
            Metadata(PERFECT_WEEK, "🎯", "Perfect Week",      "7 consecutive days of completing every habit."),
            Metadata(LEVEL_25,     "💠", "Mastermind",        "Reached Level 25. Your habits are part of who you are."),
            Metadata(LEVEL_30,     "🌌", "Eternal",           "Reached Level 30. Discipline without end."),
            Metadata(LEVEL_40,     "⚜️", "Mythic",            "Reached Level 40. You are a living legend."),
            Metadata(LEVEL_50,     "🏵️", "Undying",           "Reached Level 50. Commitment beyond measure.")
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun levelForXp(xp: Int): Int {
        if (xp >= LEVEL_XP_THRESHOLDS.last()) {
            return LEGACY_MAX_LEVEL + (xp - LEVEL_XP_THRESHOLDS.last()) / XP_PER_POST_LEVEL
        }
        for (i in LEVEL_XP_THRESHOLDS.indices.reversed()) {
            if (xp >= LEVEL_XP_THRESHOLDS[i]) return i + 1
        }
        return 1
    }

    fun levelName(level: Int): String = when {
        level <= LEGACY_MAX_LEVEL -> LEVEL_NAMES.getOrElse(level - 1) { LEVEL_NAMES.last() }
        level <= LEGACY_MAX_LEVEL + POST_LEVEL_NAMES.size ->
            POST_LEVEL_NAMES[level - LEGACY_MAX_LEVEL - 1]
        else -> "Level $level"
    }

    /** XP required to reach [level] (start threshold of that level). */
    fun xpForLevel(level: Int): Int = when {
        level <= LEGACY_MAX_LEVEL -> LEVEL_XP_THRESHOLDS.getOrElse(level - 1) { LEVEL_XP_THRESHOLDS.last() }
        else -> LEVEL_XP_THRESHOLDS.last() + (level - LEGACY_MAX_LEVEL) * XP_PER_POST_LEVEL
    }

    /** XP required to reach the next level after [level]. Never returns null — levels have no cap. */
    fun xpForNextLevel(level: Int): Int = xpForLevel(level + 1)

    /**
     * Fraction (0f–1f) of progress within the current level band.
     */
    fun levelProgress(xp: Int): Float {
        val level = levelForXp(xp)
        val current = xpForLevel(level)
        val next = xpForNextLevel(level)
        return ((xp - current).toFloat() / (next - current).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Compute the XP earned for completing [habitsCompletedToday] habits on a day
     * where the streak is [currentStreak].
     *
     * [isDayPerfect] = all assigned habits were completed.
     */
    fun computeXpGain(habitsCompletedToday: Int, isDayPerfect: Boolean, currentStreak: Int): Int {
        if (habitsCompletedToday <= 0) return 0

        // Reward daily consistency first, with diminishing returns for high daily volume.
        var xp = XP_FOR_SHOWING_UP
        val extraHabits = (habitsCompletedToday - 1).coerceAtLeast(0)
        xp += (extraHabits * XP_EXTRA_HABIT_BONUS).coerceAtMost(XP_EXTRA_HABIT_BONUS_CAP)

        val streakBonus = (currentStreak * XP_STREAK_BONUS_PER_DAY).coerceAtMost(XP_STREAK_BONUS_CAP)
        xp += streakBonus

        if (isDayPerfect) {
            xp += XP_DAY_COMPLETE_BONUS
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
        check(Badge.LEVEL_MAX,    stats.level >= LEGACY_MAX_LEVEL)
        check(Badge.PERFECT_WEEK, stats.currentStreak >= 7)
        check(Badge.LEVEL_25,     stats.level >= 25)
        check(Badge.LEVEL_30,     stats.level >= 30)
        check(Badge.LEVEL_40,     stats.level >= 40)
        check(Badge.LEVEL_50,     stats.level >= 50)
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

        // Streak represents daily consistency (at least one completed habit),
        // not strict "all habits done" behavior.
        val activeDay = habitsCompleted > 0
        val newStreak = if (activeDay) existing.currentStreak + 1 else 0
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

    /**
     * Returns the next streak milestone the user is working towards.
     * Milestones: 3, 7, 14, 21, 30, 50, 60, 90, 100, then every 100.
     */
    fun nextStreakMilestone(streak: Int): Int {
        val fixed = intArrayOf(3, 7, 14, 21, 30, 50, 60, 90, 100)
        for (m in fixed) {
            if (streak < m) return m
        }
        return ((streak / 100) + 1) * 100
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
        return "$xp / $next XP"
    }
}
