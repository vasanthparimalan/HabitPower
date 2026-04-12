package com.example.habitpower.gamification

import com.example.habitpower.data.dao.HabitTrackingDao
import com.example.habitpower.data.dao.LifeAreaDao
import com.example.habitpower.data.dao.UserStatsDao
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class GamificationRepository(
    private val userStatsDao: UserStatsDao,
    private val habitTrackingDao: HabitTrackingDao,
    private val lifeAreaDao: LifeAreaDao
) {

    fun observeStats(userId: Long): Flow<UserStats> =
        userStatsDao.observeStats(userId).map { it ?: UserStats(userId = userId) }

    suspend fun getStats(userId: Long): UserStats =
        userStatsDao.getStats(userId) ?: UserStats(userId = userId)

    /**
     * Called after the user saves their daily check-in.
     * Reads today's entries, recomputes the streak/XP/level, persists the result,
     * and returns any newly earned badges so the UI can celebrate them.
     *
     * Returns a [CheckInResult] describing XP gained, level-up if any, and new badges.
     */
    suspend fun onDayCheckedIn(userId: Long, date: LocalDate): CheckInResult {
        val habits = habitTrackingDao.getDailyHabitItems(userId, date)
            .map { list -> list.filter { it.isScheduledOn(date) } }
            .first()

        val completed = habits.count { it.isCompleted }
        val total = habits.size
        val isDayPerfect = total > 0 && completed == total

        val existing = getStats(userId)

        // Idempotency: if the same date is saved again, remove prior contribution for that
        // date first, then recompute from current entries. This prevents XP inflation when
        // users uncheck/recheck and save again.
        val baseStats = if (existing.lastCheckInDate == date) {
            existing.copy(
                currentStreak = existing.lastCheckInStreakBefore,
                longestStreak = existing.lastCheckInLongestBefore,
                totalXp = (existing.totalXp - existing.lastCheckInXpAwarded).coerceAtLeast(0),
                totalHabitsCompleted = (existing.totalHabitsCompleted - existing.lastCheckInCompletedCount).coerceAtLeast(0),
                totalDaysPerfect = (existing.totalDaysPerfect - if (existing.lastCheckInWasPerfect) 1 else 0).coerceAtLeast(0)
            )
        } else {
            existing
        }

        val streakBeforeThisCheckIn = baseStats.currentStreak
        val longestBeforeThisCheckIn = baseStats.longestStreak

        val (computedStats, newBadgesMask) = GamificationEngine.computeUpdatedStats(
            existing = baseStats,
            habitsCompleted = completed,
            totalHabits = total,
            isDayPerfect = isDayPerfect
        )
        val xpGained = computedStats.totalXp - baseStats.totalXp
        val didLevelUp = computedStats.level > baseStats.level

        val newStats = computedStats.copy(
            lastPerfectDate = if (isDayPerfect) date else computedStats.lastPerfectDate,
            lastCheckInDate = date,
            lastCheckInCompletedCount = completed,
            lastCheckInWasPerfect = isDayPerfect,
            lastCheckInXpAwarded = xpGained,
            lastCheckInStreakBefore = streakBeforeThisCheckIn,
            lastCheckInLongestBefore = longestBeforeThisCheckIn
        )

        userStatsDao.upsertStats(newStats)

        val newBadges = GamificationEngine.Badge.ALL.filter { (newBadgesMask and it.id) != 0L }

        return CheckInResult(
            stats = newStats,
            xpGained = xpGained,
            didLevelUp = didLevelUp,
            previousLevel = baseStats.level,
            isDayPerfect = isDayPerfect,
            newBadges = newBadges,
            completedCount = completed,
            totalCount = total
        )
    }

    /**
     * Prepare data for the morning brief notification.
     * Looks at yesterday's entries and the last 7 days.
     */
    suspend fun getMorningBriefData(userId: Long): MorningBriefData {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val yesterdayHabits = habitTrackingDao.getDailyHabitItems(userId, yesterday)
            .map { list -> list.filter { it.isScheduledOn(yesterday) } }
            .first()
        val yesterdayCompleted = yesterdayHabits.count { it.isCompleted }
        val yesterdayTotal = yesterdayHabits.size

        // Weekly score: count perfect days in last 7 days
        var perfectDaysThisWeek = 0
        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            val dayHabits = habitTrackingDao.getDailyHabitItems(userId, d)
                .map { list -> list.filter { it.isScheduledOn(d) } }
                .first()
            if (dayHabits.isNotEmpty() && dayHabits.all { it.isCompleted }) perfectDaysThisWeek++
        }

        // Weakest life area: area with lowest completion % over last 14 days
        val areaNames: Map<Long, String> = lifeAreaDao.getAllLifeAreas()
            .map { areas -> areas.associate { it.id to it.name } }
            .first()

        val areaStats = mutableMapOf<Long?, Pair<Int, Int>>() // areaId -> (completed, total)
        for (i in 0..13) {
            val d = today.minusDays(i.toLong())
            val dayHabits = habitTrackingDao.getDailyHabitItems(userId, d)
                .map { list -> list.filter { it.isScheduledOn(d) } }
                .first()
            for (habit in dayHabits) {
                val areaId = habit.lifeAreaId
                val prev = areaStats.getOrDefault(areaId, Pair(0, 0))
                areaStats[areaId] = Pair(
                    prev.first + if (habit.isCompleted) 1 else 0,
                    prev.second + 1
                )
            }
        }

        val weakestEntry: Map.Entry<Long?, Pair<Int, Int>>? = areaStats
            .filter { entry -> entry.value.second > 0 }
            .minByOrNull { entry -> entry.value.first.toDouble() / entry.value.second }
        val weakestAreaName: String? = weakestEntry?.key?.let { id -> areaNames[id] }

        val stats = getStats(userId)
        val todayHabits = habitTrackingDao.getDailyHabitItems(userId, today)
            .map { list -> list.filter { it.isScheduledOn(today) } }
            .first()

        return MorningBriefData(
            stats = stats,
            yesterdayCompleted = yesterdayCompleted,
            yesterdayTotal = yesterdayTotal,
            perfectDaysThisWeek = perfectDaysThisWeek,
            weakestAreaName = weakestAreaName,
            todayHabitCount = todayHabits.size
        )
    }

    /**
     * Prepare data for the evening summary notification.
     */
    suspend fun getEveningData(userId: Long): EveningData {
        val today = LocalDate.now()
        val habits = habitTrackingDao.getDailyHabitItems(userId, today)
            .map { list -> list.filter { it.isScheduledOn(today) } }
            .first()
        val completed = habits.count { it.isCompleted }
        val total = habits.size
        val pending = habits.filter { !it.isCompleted }.map { it.name }
        val stats = getStats(userId)
        return EveningData(
            completedCount = completed,
            totalCount = total,
            pendingHabitNames = pending,
            stats = stats
        )
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    data class CheckInResult(
        val stats: UserStats,
        val xpGained: Int,
        val didLevelUp: Boolean,
        val previousLevel: Int,
        val isDayPerfect: Boolean,
        val newBadges: List<GamificationEngine.Badge.Metadata>,
        val completedCount: Int,
        val totalCount: Int
    )

    data class MorningBriefData(
        val stats: UserStats,
        val yesterdayCompleted: Int,
        val yesterdayTotal: Int,
        val perfectDaysThisWeek: Int,
        val weakestAreaName: String?,
        val todayHabitCount: Int
    )

    data class EveningData(
        val completedCount: Int,
        val totalCount: Int,
        val pendingHabitNames: List<String>,
        val stats: UserStats
    )
}

// ── Extension: DailyHabitItem completion logic ───────────────────────────────
val DailyHabitItem.isCompleted: Boolean
    get() = when (type) {
        HabitType.BOOLEAN -> entryBooleanValue == true
        HabitType.TEXT -> !entryTextValue.isNullOrBlank()
        else -> entryNumericValue != null
    }
