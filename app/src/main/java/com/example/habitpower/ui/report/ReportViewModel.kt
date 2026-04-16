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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class ReportPeriod(val label: String, val days: Int) {
    DAYS_7("7 days", 7),
    DAYS_30("30 days", 30),
    DAYS_90("90 days", 90),
    CUSTOM("Custom", -1)
}

data class ReportKpi(
    val label: String,
    val value: String,
    val supportingText: String
)

data class WeekTrend(
    val weekLabel: String,
    val completionRatio: Float
)

data class HabitConsistency(
    val habitName: String,
    val completedCount: Int,
    val totalDays: Int,
    val consistencyRatio: Float
)

data class LifeAreaGauge(
    val name: String,
    val emoji: String?,
    val completedCount: Int,
    val totalCount: Int,
    val completionRatio: Float,
    val encouragement: String
)

data class ReportUiState(
    val isLoading: Boolean = false,
    val activeUser: UserProfile? = null,
    val selectedPeriod: ReportPeriod = ReportPeriod.DAYS_30,
    val startDate: LocalDate = LocalDate.now().minusDays(29),
    val endDate: LocalDate = LocalDate.now(),
    val headline: String = "Stay on the path",
    val subheadline: String = "Your long-term identity is built one completed day at a time.",
    val kpis: List<ReportKpi> = emptyList(),
    val weeklyTrend: List<WeekTrend> = emptyList(),
    val lifeAreaGauges: List<LifeAreaGauge> = emptyList(),
    val habitConsistency: List<HabitConsistency> = emptyList(),
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

    fun selectPeriod(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM) {
            uiState = uiState.copy(selectedPeriod = period)
            return
        }
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays((period.days - 1).toLong())
        uiState = uiState.copy(selectedPeriod = period, startDate = startDate, endDate = endDate)
        refreshReport()
    }

    fun updateStartDate(date: LocalDate) {
        val adjustedEnd = if (date.isAfter(uiState.endDate)) date else uiState.endDate
        uiState = uiState.copy(selectedPeriod = ReportPeriod.CUSTOM, startDate = date, endDate = adjustedEnd)
        refreshReport()
    }

    fun updateEndDate(date: LocalDate) {
        val adjustedStart = if (date.isBefore(uiState.startDate)) date else uiState.startDate
        uiState = uiState.copy(selectedPeriod = ReportPeriod.CUSTOM, startDate = adjustedStart, endDate = date)
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
                    weeklyTrend = emptyList(),
                    lifeAreaGauges = emptyList(),
                    habitConsistency = emptyList(),
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
                    weeklyTrend = emptyList(),
                    lifeAreaGauges = emptyList(),
                    habitConsistency = emptyList(),
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

            uiState = report.copy(
                isLoading = false,
                activeUser = currentUser,
                startDate = startDate,
                endDate = endDate,
                selectedPeriod = uiState.selectedPeriod
            )
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
            val completed = habits.count { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
            if (habits.isEmpty()) 0f else completed.toFloat() / habits.size.toFloat()
        }

        val totalTasks = habits.size * dates.size
        val totalCompletedTasks = dates.sumOf { date ->
            habits.count { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
        }
        val overallRatio = if (totalTasks == 0) 0f else totalCompletedTasks.toFloat() / totalTasks.toFloat()

        val showUpDays = dates.count { date ->
            habits.any { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
        }
        val showUpRate = if (dates.isEmpty()) 0f else showUpDays.toFloat() / dates.size.toFloat()

        val bestDayRatio = dayRatios.maxOrNull() ?: 0f

        val weeklyTrend = buildWeeklyTrend(dates, habits, entriesByKey)
        val lifeAreaGauges = buildLifeAreaGauges(dates, habits, entriesByKey, lifeAreas)
        val habitConsistency = buildHabitConsistency(dates, habits, entriesByKey)
        val weakestArea = lifeAreaGauges.minByOrNull { it.completionRatio }
        val strongestArea = lifeAreaGauges.maxByOrNull { it.completionRatio }

        val headline = when {
            overallRatio >= 0.85f -> "You are compounding excellence"
            overallRatio >= 0.60f -> "Momentum is on your side"
            overallRatio >= 0.35f -> "You are still in the game"
            else -> "Every comeback starts with one strong day"
        }

        val subheadline = when {
            showUpRate >= 0.9f -> "${user.name}, showing up every single day is the whole game. You're winning it."
            overallRatio >= 0.60f -> "Your consistency is real. Tighten the weakest habit and the next level opens up."
            else -> "This report is not judgment. It is a map. Use it to choose your next small win."
        }

        return ReportUiState(
            headline = headline,
            subheadline = subheadline,
            kpis = listOf(
                ReportKpi("Show-Up Rate", percentText(showUpRate), "$showUpDays of ${dates.size} days active"),
                ReportKpi("Consistency", percentText(overallRatio), "of all habit-days honored"),
                ReportKpi("Best Day", percentText(bestDayRatio), "highest single-day completion")
            ),
            weeklyTrend = weeklyTrend,
            lifeAreaGauges = lifeAreaGauges,
            habitConsistency = habitConsistency,
            weakestAreaMessage = weakestArea?.let {
                "Most attention needed: ${it.name} is at ${percentText(it.completionRatio)}. A few repeated wins here will change the whole story."
            },
            strongestAreaMessage = strongestArea?.let {
                "Strongest lane: ${it.name} is running at ${percentText(it.completionRatio)}. Protect this momentum."
            },
            emptyMessage = null
        )
    }

    private fun buildWeeklyTrend(
        dates: List<LocalDate>,
        habits: List<HabitDefinition>,
        entriesByKey: Map<Pair<LocalDate, Long>, DailyHabitEntry>
    ): List<WeekTrend> {
        if (dates.isEmpty() || habits.isEmpty()) return emptyList()
        // Short periods: one bar per day; longer: one bar per week
        val chunkSize = if (dates.size <= 14) 1 else 7
        val fmtWeek = DateTimeFormatter.ofPattern("MMM d")
        return dates.chunked(chunkSize).map { chunk ->
            val avg = chunk.map { date ->
                val completed = habits.count { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
                completed.toFloat() / habits.size.toFloat()
            }.average().toFloat()
            val label = if (chunkSize == 1) chunk.first().dayOfMonth.toString()
                        else chunk.first().format(fmtWeek)
            WeekTrend(weekLabel = label, completionRatio = avg)
        }
    }

    private fun buildHabitConsistency(
        dates: List<LocalDate>,
        habits: List<HabitDefinition>,
        entriesByKey: Map<Pair<LocalDate, Long>, DailyHabitEntry>
    ): List<HabitConsistency> {
        return habits.map { habit ->
            val completed = dates.count { date -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
            val ratio = if (dates.isEmpty()) 0f else completed.toFloat() / dates.size.toFloat()
            HabitConsistency(
                habitName = habit.name,
                completedCount = completed,
                totalDays = dates.size,
                consistencyRatio = ratio
            )
        }.sortedBy { it.consistencyRatio } // worst first — most attention needed at top
    }

    private fun buildLifeAreaGauges(
        dates: List<LocalDate>,
        habits: List<HabitDefinition>,
        entriesByKey: Map<Pair<LocalDate, Long>, DailyHabitEntry>,
        lifeAreas: List<LifeArea>
    ): List<LifeAreaGauge> {
        val grouped = habits.groupBy { it.lifeAreaId }
        val areaById = lifeAreas.associateBy { it.id }

        return grouped.map { (lifeAreaId, areaHabits) ->
            val area = lifeAreaId?.let { areaById[it] }
            val name = area?.name ?: "Other"
            val emoji = area?.emoji
            val totalCount = areaHabits.size * dates.size
            val completedCount = dates.sumOf { date ->
                areaHabits.count { habit -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
            }
            val ratio = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

            LifeAreaGauge(
                name = name,
                emoji = emoji,
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
                val target = habit.targetValue ?: return true
                when (habit.operator) {
                    TargetOperator.LESS_THAN_OR_EQUAL -> value <= target
                    TargetOperator.GREATER_THAN_OR_EQUAL -> value >= target
                    TargetOperator.EQUAL -> value == target
                }
            }
            HabitType.TIME -> {
                val value = entry.numericValue ?: return false
                val target = habit.targetValue ?: return true
                when (habit.operator) {
                    TargetOperator.LESS_THAN_OR_EQUAL -> value <= target
                    TargetOperator.GREATER_THAN_OR_EQUAL -> value >= target
                    TargetOperator.EQUAL -> value == target
                }
            }
            HabitType.TEXT -> !entry.textValue.isNullOrBlank()
            HabitType.ROUTINE -> entry.booleanValue == true
        }
    }
}
