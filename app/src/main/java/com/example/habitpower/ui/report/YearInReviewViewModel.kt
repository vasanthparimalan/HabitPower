package com.example.habitpower.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.gamification.GamificationEngine
import com.example.habitpower.gamification.GamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class YearInReviewState(
    val isLoading: Boolean = true,
    val year: Int = LocalDate.now().year,
    val userName: String = "",
    val totalHabitsCompleted: Int = 0,
    val activeDays: Int = 0,
    val totalDaysInPeriod: Int = 0,
    val perfectDays: Int = 0,
    val longestStreakInYear: Int = 0,
    val bestLifeAreaName: String? = null,
    val bestLifeAreaPercent: Int = 0,
    val currentLevel: Int = 0,
    val currentLevelName: String = "",
    val estimatedXpGained: Int = 0,
    val headline: String = "",
    val hasData: Boolean = false,
    val noDataMessage: String? = null
)

class YearInReviewViewModel(
    private val repository: HabitPowerRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(YearInReviewState())
    val state: StateFlow<YearInReviewState> = _state.asStateFlow()

    // Offer the two most relevant years: default to the most recently completed year
    // (or the current year if it has meaningful data).
    val availableYears: List<Int> = run {
        val now = LocalDate.now()
        val current = now.year
        // Show previous year first in Jan–Mar so the "just finished year" is default
        if (now.monthValue <= 3) listOf(current - 1, current) else listOf(current, current - 1)
    }

    init {
        _state.value = _state.value.copy(year = availableYears.first())
        loadReview()
    }

    fun selectYear(year: Int) {
        if (year == _state.value.year) return
        _state.value = _state.value.copy(year = year)
        loadReview()
    }

    private fun loadReview() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasData = false, noDataMessage = null)

            val user = repository.getResolvedActiveUser().first()
            if (user == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    noDataMessage = "No active user. Select a user from the Dashboard to view your Year in Review."
                )
                return@launch
            }

            val year = _state.value.year
            val startDate = LocalDate.of(year, 1, 1)
            val endDate = minOf(LocalDate.of(year, 12, 31), LocalDate.now())
            val totalDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()

            val assignedHabits = repository.getAssignedHabitsForUser(user.id).first()
            val entries = repository.getEntriesForUserInRange(user.id, startDate, endDate).first()
            val stats = gamificationRepository.getStats(user.id)

            if (entries.isEmpty() || assignedHabits.isEmpty()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    userName = user.name,
                    year = year,
                    hasData = false,
                    noDataMessage = "No habit data found for $year. Keep tracking and check back later."
                )
                return@launch
            }

            val habitById = assignedHabits.associateBy { it.id }
            val habitIdSet = habitById.keys
            val entriesByDate = entries
                .filter { it.habitId in habitIdSet }
                .groupBy { it.date }

            // Build date range and compute per-day stats
            val dateRange = (0 until totalDays).map { startDate.plusDays(it.toLong()) }

            var activeDays = 0
            var perfectDays = 0
            var currentStreak = 0
            var longestStreak = 0
            var totalCompleted = 0
            var estimatedXp = 0

            val areaCompleted = mutableMapOf<Long?, Int>()
            val areaTotal = mutableMapOf<Long?, Int>()

            for (date in dateRange) {
                val dayEntries = entriesByDate[date] ?: emptyList()
                val total = assignedHabits.size
                val completed = dayEntries.count { entry ->
                    val habit = habitById[entry.habitId] ?: return@count false
                    isEntrySuccessful(entry, habit)
                }

                totalCompleted += completed
                val isActive = completed > 0
                val isPerfect = total > 0 && completed == total

                if (isActive) activeDays++
                if (isPerfect) perfectDays++

                currentStreak = if (isActive) currentStreak + 1 else 0
                if (currentStreak > longestStreak) longestStreak = currentStreak

                if (isActive) {
                    estimatedXp += GamificationEngine.computeXpGain(completed, isPerfect, currentStreak)
                }

                // Per life area
                assignedHabits.forEach { habit ->
                    val areaId = habit.lifeAreaId
                    areaTotal[areaId] = (areaTotal[areaId] ?: 0) + 1
                    val entry = dayEntries.firstOrNull { it.habitId == habit.id }
                    if (entry != null && isEntrySuccessful(entry, habit)) {
                        areaCompleted[areaId] = (areaCompleted[areaId] ?: 0) + 1
                    }
                }
            }

            // Best life area
            val lifeAreas = repository.getActiveLifeAreas().first()
            val areaNameById = lifeAreas.associateBy({ it.id }, { it.name })
            val bestEntry = areaTotal
                .filter { (_, total) -> total > 0 }
                .maxByOrNull { (id, total) ->
                    (areaCompleted[id] ?: 0).toDouble() / total
                }
            val bestAreaName = bestEntry?.key?.let { areaNameById[it] }
            val bestAreaPct = bestEntry?.let { (id, total) ->
                val comp = areaCompleted[id] ?: 0
                (comp * 100 / total)
            } ?: 0

            val currentLevel = GamificationEngine.levelForXp(stats.totalXp)
            val currentLevelName = GamificationEngine.levelName(currentLevel)

            val consistencyPct = if (totalDays > 0) activeDays * 100 / totalDays else 0
            val headline = buildHeadline(user.name, activeDays, totalDays, consistencyPct, longestStreak, year)

            _state.value = YearInReviewState(
                isLoading = false,
                year = year,
                userName = user.name,
                totalHabitsCompleted = totalCompleted,
                activeDays = activeDays,
                totalDaysInPeriod = totalDays,
                perfectDays = perfectDays,
                longestStreakInYear = longestStreak,
                bestLifeAreaName = bestAreaName,
                bestLifeAreaPercent = bestAreaPct,
                currentLevel = currentLevel,
                currentLevelName = currentLevelName,
                estimatedXpGained = estimatedXp,
                headline = headline,
                hasData = true
            )
        }
    }

    fun buildShareText(): String {
        val s = _state.value
        return buildString {
            appendLine("🏆 ${s.userName}'s ${s.year} Year in Review — HabitPower")
            appendLine()
            appendLine("✅  ${s.totalHabitsCompleted} habits completed")
            appendLine("📅  ${s.activeDays} / ${s.totalDaysInPeriod} days active")
            appendLine("⭐  ${s.perfectDays} perfect days")
            appendLine("🔥  ${s.longestStreakInYear}-day best streak")
            if (s.bestLifeAreaName != null) {
                appendLine("💪  Best life area: ${s.bestLifeAreaName} (${s.bestLifeAreaPercent}%)")
            }
            appendLine("🎮  Level: ${s.currentLevel} — ${s.currentLevelName}")
            appendLine()
            append(s.headline)
        }
    }

    private fun buildHeadline(
        name: String,
        activeDays: Int,
        totalDays: Int,
        consistencyPct: Int,
        longestStreak: Int,
        year: Int
    ): String {
        val displayName = name.ifBlank { "You" }
        return when {
            consistencyPct >= 90 ->
                "$displayName showed up $activeDays days out of $totalDays in $year.\nThat kind of consistency doesn't happen by accident. That's who you are now."
            consistencyPct >= 70 ->
                "$displayName showed up $activeDays out of $totalDays days in $year.\nA $longestStreak-day streak was the peak. The foundation is solid — build higher in ${year + 1}."
            consistencyPct >= 50 ->
                "In $year, $displayName was active $activeDays days. Longest streak: $longestStreak days.\nThe commitment is real. The next step is protecting more of your days."
            activeDays >= 10 ->
                "In $year, $displayName showed up $activeDays days. Longest streak: $longestStreak days.\nEvery streak starts small. This year was the beginning."
            else ->
                "In $year, every day you opened this app was a vote for the person you want to become.\nThe data is young. The habit is real."
        }
    }

    private fun isEntrySuccessful(entry: DailyHabitEntry, habit: HabitDefinition): Boolean {
        return when (habit.type) {
            HabitType.BOOLEAN -> entry.booleanValue == true
            HabitType.ROUTINE -> entry.booleanValue == true
            HabitType.TEXT -> !entry.textValue.isNullOrBlank()
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER, HabitType.TIME -> {
                val value = entry.numericValue ?: return false
                val target = habit.targetValue ?: return true
                when (habit.operator) {
                    TargetOperator.LESS_THAN_OR_EQUAL -> value <= target
                    TargetOperator.GREATER_THAN_OR_EQUAL -> value >= target
                    TargetOperator.EQUAL -> value == target
                }
            }
        }
    }
}
