package com.example.habitpower.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.TargetOperator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SeasonInsight(
    val seasonStart: LocalDate,
    val seasonEnd: LocalDate,
    val activeHabitCount: Int,
    val overallPercent: Int,
    val anchorHabit: String?,
    val mostConsistentHabit: String?,
    val graduateCandidate: String?,
    val retireCandidate: String?
)

data class SeasonReviewUiState(
    val insight: SeasonInsight? = null,
    val lastReviewEpochDay: Long? = null,
    val isDue: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonReviewViewModel(
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _insight = MutableStateFlow<SeasonInsight?>(null)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<SeasonReviewUiState> = combine(
        _insight,
        prefsRepository.lastSeasonReviewEpochDay,
        _isLoading
    ) { insight, lastReviewEpochDay, isLoading ->
        val today = LocalDate.now().toEpochDay()
        val isDue = lastReviewEpochDay == null || (today - lastReviewEpochDay) >= 90
        SeasonReviewUiState(insight, lastReviewEpochDay, isDue, isLoading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeasonReviewUiState())

    init {
        viewModelScope.launch {
            prefsRepository.activeUserId.flatMapLatest { userId ->
                if (userId == null) flowOf(null) else repository.getResolvedActiveUser()
            }.collect { user ->
                if (user != null) {
                    computeInsight(user.id)
                } else {
                    _insight.value = null
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun computeInsight(userId: Long) {
        _isLoading.value = true
        val today = LocalDate.now()
        val seasonStart = today.minusDays(89)

        val habits = repository.getAssignedHabitsForUser(userId).first()
        val entries = repository.getEntriesForUserInRange(userId, seasonStart, today).first()
        val dates = (0L..89L).map { seasonStart.plusDays(it) }

        val entriesByKey = entries.associateBy { it.date to it.habitId }

        data class HabitStat(val name: String, val completed: Int, val ratio: Float)

        val stats = habits.map { habit ->
            val completed = dates.count { date ->
                isEntrySuccessful(entriesByKey[date to habit.id], habit)
            }
            val ratio = if (dates.isEmpty()) 0f else completed.toFloat() / dates.size.toFloat()
            HabitStat(habit.name, completed, ratio)
        }

        val totalCompleted = stats.sumOf { it.completed }
        val totalPossible = habits.size * dates.size
        val overallPercent = if (totalPossible == 0) 0 else (totalCompleted * 100 / totalPossible)

        val anchorHabit = stats.maxByOrNull { it.completed }?.name
        val mostConsistentHabit = stats.maxByOrNull { it.ratio }
            ?.takeIf { it.ratio >= 0.6f }?.name
        val graduateCandidate = stats.filter { it.ratio >= 0.85f }
            .maxByOrNull { it.ratio }?.name
        val retireCandidate = stats.filter { it.ratio < 0.20f && it.completed > 0 }
            .minByOrNull { it.ratio }?.name

        _insight.value = SeasonInsight(
            seasonStart = seasonStart,
            seasonEnd = today,
            activeHabitCount = habits.size,
            overallPercent = overallPercent,
            anchorHabit = anchorHabit,
            mostConsistentHabit = mostConsistentHabit,
            graduateCandidate = graduateCandidate,
            retireCandidate = retireCandidate
        )
        _isLoading.value = false
    }

    fun completeReview() {
        viewModelScope.launch {
            prefsRepository.saveLastSeasonReviewEpochDay(LocalDate.now().toEpochDay())
        }
    }

    private fun isEntrySuccessful(
        entry: com.example.habitpower.data.model.DailyHabitEntry?,
        habit: HabitDefinition
    ): Boolean {
        if (entry == null) return false
        return when (habit.type) {
            HabitType.BOOLEAN, HabitType.ROUTINE -> entry.booleanValue == true
            HabitType.TEXT -> !entry.textValue.isNullOrBlank()
            else -> {
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
