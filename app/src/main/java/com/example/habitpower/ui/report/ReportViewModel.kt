package com.example.habitpower.ui.report

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class ReportKpi(
    val label: String,
    val value: String,
    val supportingText: String
)

data class TreeRingSegment(
    val label: String,
    val completionRatio: Float
)

data class LifeAreaGauge(
    val name: String,
    val completedCount: Int,
    val totalCount: Int,
    val completionRatio: Float,
    val encouragement: String
)

data class ReportUiState(
    val isLoading: Boolean = false,
    val activeUser: UserProfile? = null,
    val startDate: LocalDate = LocalDate.now().minusDays(29),
    val endDate: LocalDate = LocalDate.now(),
    val headline: String = "Stay on the path",
    val subheadline: String = "Your long-term identity is built one completed day at a time.",
    val kpis: List<ReportKpi> = emptyList(),
    val treeRings: List<TreeRingSegment> = emptyList(),
    val lifeAreaGauges: List<LifeAreaGauge> = emptyList(),
    val weakestAreaMessage: String? = null,
    val strongestAreaMessage: String? = null,
    val emptyMessage: String? = null
)

class ReportViewModel(private val repository: HabitPowerRepository) : ViewModel() {
    var uiState by mutableStateOf(ReportUiState())
        private set

    init {
        observeActiveUser()
    }

    fun updateStartDate(date: LocalDate) {
        val adjustedEnd = if (date.isAfter(uiState.endDate)) date else uiState.endDate
        uiState = uiState.copy(startDate = date, endDate = adjustedEnd)
        refreshReport()
    }

    fun updateEndDate(date: LocalDate) {
        val adjustedStart = if (date.isBefore(uiState.startDate)) date else uiState.startDate
        uiState = uiState.copy(startDate = adjustedStart, endDate = date)
        refreshReport()
    }

    private fun observeActiveUser() {
        viewModelScope.launch {
            repository.getResolvedActiveUser().collect { user ->
                uiState = uiState.copy(activeUser = user)
                refreshReport()
            }
        }
    }

    private fun refreshReport() {
        viewModelScope.launch {
            val currentUser = uiState.activeUser
            if (currentUser == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    kpis = emptyList(),
                    treeRings = emptyList(),
                    lifeAreaGauges = emptyList(),
                    emptyMessage = "No active user selected yet. Pick a user to unlock analytics."
                )
                return@launch
            }

            uiState = uiState.copy(isLoading = true, emptyMessage = null)

            val startDate = uiState.startDate
            val endDate = uiState.endDate
            val assignedLifeAreaIds = repository.getAssignedLifeAreaIdsForUser(currentUser.id).first().toSet()
            val assignedHabits = filterHabitsByAssignedAreas(
                habits = repository.getAssignedHabitsForUser(currentUser.id).first(),
                assignedAreaIds = assignedLifeAreaIds
            )
            val entries = repository.getEntriesForUserInRange(currentUser.id, startDate, endDate).first()
            val lifeAreas = if (assignedLifeAreaIds.isEmpty()) {
                repository.getActiveLifeAreas().first()
            } else {
                repository.getAssignedLifeAreasForUser(currentUser.id).first()
            }

            if (assignedHabits.isEmpty()) {
                uiState = uiState.copy(
                    isLoading = false,
                    kpis = emptyList(),
                    treeRings = emptyList(),
                    lifeAreaGauges = emptyList(),
                    emptyMessage = "Assign habits and life areas to ${currentUser.name} to start building a meaningful report."
                )
                return@launch
            }

            val report = buildReport(
                user = currentUser,
                startDate = startDate,
                endDate = endDate,
                habits = assignedHabits,
                entries = entries,
                lifeAreas = lifeAreas
            )

            uiState = report.copy(isLoading = false, activeUser = currentUser, startDate = startDate, endDate = endDate)
        }
    }

    private fun buildReport(
        user: UserProfile,
        startDate: LocalDate,
        endDate: LocalDate,
        habits: List<HabitDefinition>,
        entries: List<DailyHabitEntry>,
        lifeAreas: List<LifeArea>
    ): ReportUiState {
        val dates = generateDateRange(startDate, endDate)
        val entriesByKey = entries.associateBy { it.date to it.habitId }

        val dayRatios = dates.map { date ->
            val completed = habits.count { habit ->
                isEntrySuccessful(entriesByKey[date to habit.id], habit)
            }
            if (habits.isEmpty()) 0f else completed.toFloat() / habits.size.toFloat()
        }

        val totalTasks = habits.size * dates.size
        val totalCompletedTasks = dates.sumOf { date ->
            habits.count { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
        }
        val overallRatio = if (totalTasks == 0) 0f else totalCompletedTasks.toFloat() / totalTasks.toFloat()
        val currentStreak = dates.asReversed().takeWhile { date ->
            habits.all { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
        }.count()
        val bestDayRatio = dayRatios.maxOrNull() ?: 0f

        val trunkSegments = buildTreeRingSegments(dates, dayRatios)
        val lifeAreaGauges = buildLifeAreaGauges(dates, habits, entriesByKey, lifeAreas)
        val weakestArea = lifeAreaGauges.minByOrNull { it.completionRatio }
        val strongestArea = lifeAreaGauges.maxByOrNull { it.completionRatio }

        val headline = when {
            overallRatio >= 0.85f -> "You are compounding excellence"
            overallRatio >= 0.60f -> "Momentum is on your side"
            overallRatio >= 0.35f -> "You are still in the game"
            else -> "Every comeback starts with one strong day"
        }

        val subheadline = when {
            currentStreak >= 14 -> "${user.name}, your systems are becoming part of your identity. Keep protecting the streak."
            overallRatio >= 0.60f -> "Your consistency is real. Tighten the weakest area and the next level opens up."
            else -> "This report is not judgment. It is a map. Use it to choose your next small win."
        }

        return ReportUiState(
            headline = headline,
            subheadline = subheadline,
            kpis = listOf(
                ReportKpi("Streak", currentStreak.toString(), "fully completed days ending ${endDate}"),
                ReportKpi("Consistency", percentText(overallRatio), "completed tasks across the selected range"),
                ReportKpi("Best Day", percentText(bestDayRatio), "highest single-day completion rate")
            ),
            treeRings = trunkSegments,
            lifeAreaGauges = lifeAreaGauges,
            weakestAreaMessage = weakestArea?.let {
                "Most attention needed: ${it.name} is at ${percentText(it.completionRatio)}. A few repeated wins here will change the whole story."
            },
            strongestAreaMessage = strongestArea?.let {
                "Strongest lane: ${it.name} is running at ${percentText(it.completionRatio)}. Protect this momentum."
            },
            emptyMessage = null
        )
    }

    private fun buildTreeRingSegments(
        dates: List<LocalDate>,
        dayRatios: List<Float>
    ): List<TreeRingSegment> {
        if (dates.isEmpty()) return emptyList()

        val bucketCount = minOf(dates.size, 48)
        val bucketSize = ceil(dates.size / bucketCount.toFloat()).toInt().coerceAtLeast(1)

        return dates.zip(dayRatios)
            .chunked(bucketSize)
            .map { bucket ->
                val start = bucket.first().first
                val end = bucket.last().first
                val averageRatio = bucket.map { it.second }.average().toFloat()
                val label = if (start == end) start.toString() else "$start → $end"
                TreeRingSegment(label = label, completionRatio = averageRatio)
            }
    }

    private fun buildLifeAreaGauges(
        dates: List<LocalDate>,
        habits: List<HabitDefinition>,
        entriesByKey: Map<Pair<LocalDate, Long>, DailyHabitEntry>,
        lifeAreas: List<LifeArea>
    ): List<LifeAreaGauge> {
        val grouped = habits.groupBy { it.lifeAreaId }
        val areaNames = lifeAreas.associateBy({ it.id }, { it.name })

        return grouped.map { (lifeAreaId, areaHabits) ->
            val name = lifeAreaId?.let { areaNames[it] } ?: "Other"
            val totalCount = areaHabits.size * dates.size
            val completedCount = dates.sumOf { date ->
                areaHabits.count { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
            }
            val ratio = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

            LifeAreaGauge(
                name = name,
                completedCount = completedCount,
                totalCount = totalCount,
                completionRatio = ratio,
                encouragement = when {
                    ratio >= 0.8f -> "This area is thriving. Keep raising the standard."
                    ratio >= 0.5f -> "Solid progress. A little more consistency will turn this into a strength."
                    else -> "This area needs care. Shrink the goal, win today, repeat tomorrow."
                }
            )
        }.sortedByDescending { it.completionRatio }
    }

    private fun generateDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        return (0..days).map { startDate.plusDays(it.toLong()) }
    }

    private fun percentText(ratio: Float): String = "${(ratio * 100).toInt()}%"

    private fun filterHabitsByAssignedAreas(
        habits: List<HabitDefinition>,
        assignedAreaIds: Set<Long>
    ): List<HabitDefinition> {
        if (assignedAreaIds.isEmpty()) return habits
        return habits.filter { habit -> habit.lifeAreaId != null && assignedAreaIds.contains(habit.lifeAreaId) }
    }

    private fun isEntrySuccessful(entry: DailyHabitEntry?, habit: HabitDefinition): Boolean {
        if (entry == null) return false

        return when (habit.type) {
            HabitType.BOOLEAN -> entry.booleanValue == true
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> {
                val value = entry.numericValue ?: return false
                val target = habit.targetValue
                if (target == null) {
                    true
                } else {
                    when (habit.operator) {
                        TargetOperator.LESS_THAN_OR_EQUAL -> value <= target
                        TargetOperator.GREATER_THAN_OR_EQUAL -> value >= target
                        TargetOperator.EQUAL -> value == target
                    }
                }
            }

            HabitType.TIME -> {
                val value = entry.numericValue ?: return false
                val target = habit.targetValue
                if (target == null) {
                    true
                } else {
                    when (habit.operator) {
                        TargetOperator.LESS_THAN_OR_EQUAL -> value <= target
                        TargetOperator.GREATER_THAN_OR_EQUAL -> value >= target
                        TargetOperator.EQUAL -> value == target
                    }
                }
            }
            HabitType.TEXT -> !entry.textValue.isNullOrBlank()
        }
    }
}
