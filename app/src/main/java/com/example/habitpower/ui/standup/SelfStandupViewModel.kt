package com.example.habitpower.ui.standup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.gamification.AnchorHabitEngine
import com.example.habitpower.gamification.GrowthProjection
import com.example.habitpower.gamification.IdentitySentenceEngine
import com.example.habitpower.gamification.ReviewPromptEngine
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// ── Meeting framework metadata ────────────────────────────────────────────────

data class StandupMeeting(
    val type: String,
    val duration: String,
    val objective: String,
    val commitmentPrompt: String
)

private val WEEKLY_MEETING = StandupMeeting(
    type = "Weekly Review",
    duration = "10–15 min",
    objective = "See the week clearly. One pattern to carry forward, one to change.",
    commitmentPrompt = "One thing I will focus on next week:"
)

private val MONTHLY_MEETING = StandupMeeting(
    type = "Monthly Retrospective",
    duration = "20–30 min",
    objective = "Assess habit health. Spot what is thriving and what needs a decision.",
    commitmentPrompt = "One habit I'll protect. One I'll reconsider:"
)

private val QUARTERLY_MEETING = StandupMeeting(
    type = "Season Retrospective",
    duration = "30–45 min",
    objective = "Recalibrate the system. Graduate, retire, set one new intention for the next season.",
    commitmentPrompt = "The new practice that will define this next season:"
)

private val YEARLY_MEETING = StandupMeeting(
    type = "Annual Vision Review",
    duration = "60–90 min",
    objective = "Anchor the year's effort. See who you became. Set the identity for the year ahead.",
    commitmentPrompt = "The person I am becoming in the next year:"
)

val FIVEYEAR_MEETING = StandupMeeting(
    type = "5-Year Projection",
    duration = "15–20 min",
    objective = "Follow the math of your current practices forward. Decide if you like where you are going.",
    commitmentPrompt = "The practice I want to deepen most over the next 5 years:"
)

val TENYEAR_MEETING = StandupMeeting(
    type = "10-Year Vision",
    duration = "10–15 min",
    objective = "Connect today's habits to the person you are building across a decade.",
    commitmentPrompt = "The one word that describes who I want to be in 10 years:"
)

val DECISIONS_MEETING = StandupMeeting(
    type = "Decisions & Action Items",
    duration = "5 min",
    objective = "Translate insights into specific next actions. Not someday — this week.",
    commitmentPrompt = "The one decision I am making today:"
)

// ── State ─────────────────────────────────────────────────────────────────────

data class StandupTimeframe(
    val label: String,
    val icon: String,
    val days: Int,
    val hasData: Boolean,
    val consistencyPercent: Int,
    val topHabit: String?,
    val topLifeArea: String?,
    val insights: List<String>,
    val reflectionPrompt: String,
    val meeting: StandupMeeting,
    val daysUntilUnlock: Int? = null
)

data class SelfStandupUiState(
    val isLoading: Boolean = true,
    val hasHabits: Boolean = false,
    val userName: String = "",
    val dailyIntention: String = "",
    val weekly: StandupTimeframe? = null,
    val monthly: StandupTimeframe? = null,
    val quarterly: StandupTimeframe? = null,
    val yearly: StandupTimeframe? = null,
    val growthProjections: List<GrowthProjection> = emptyList(),
    val fiveYearInsights: List<String> = emptyList(),
    val fiveYearReflection: String = "",
    val tenYearReflection: String = "",
    val identityStatements: List<String> = emptyList(),
    val decisionsInsights: List<String> = emptyList(),
    val retireCandidates: List<String> = emptyList(),
    val graduateCandidates: List<String> = emptyList(),
    val overCommitted: Boolean = false,
    val commitments: Map<String, String> = emptyMap(),
    val smartExpandedKey: String = "weekly"
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class SelfStandupViewModel(
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelfStandupUiState())
    val uiState: StateFlow<SelfStandupUiState> = _uiState.asStateFlow()

    init { compute() }

    fun refresh() = compute()

    fun saveCommitment(cadence: String, text: String) {
        viewModelScope.launch {
            prefsRepository.saveStandupCommitment(cadence, text)
            val current = _uiState.value
            _uiState.value = current.copy(
                commitments = current.commitments + (cadence to text)
            )
        }
    }

    fun saveDailyIntention(text: String) {
        viewModelScope.launch {
            prefsRepository.saveDailyIntention(LocalDate.now().toString(), text)
            _uiState.value = _uiState.value.copy(dailyIntention = text)
            try { repository.updateWidgetState() } catch (_: Exception) {}
        }
    }

    private fun compute() {
        viewModelScope.launch {
            _uiState.value = SelfStandupUiState(isLoading = true)

            // Load saved commitments
            val cadences = listOf("weekly", "monthly", "quarterly", "yearly", "fiveyear", "tenyear", "decisions")
            val commitments = cadences.associateWith { prefsRepository.getStandupCommitment(it).first() }

            val user = repository.getResolvedActiveUser().first() ?: run {
                _uiState.value = SelfStandupUiState(isLoading = false, commitments = commitments)
                return@launch
            }

            val today = LocalDate.now()
            val dailyIntention = prefsRepository.getDailyIntention(today.toString()).first()
            val habits = repository.getAssignedHabitsForUser(user.id).first()
                .filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }

            if (habits.isEmpty()) {
                _uiState.value = SelfStandupUiState(
                    isLoading = false, userName = user.name,
                    dailyIntention = dailyIntention, commitments = commitments
                )
                return@launch
            }

            val maxLookback = 365L
            val earliestRange = today.minusDays(maxLookback)
            val allEntries = repository.getEntriesForUserInRange(user.id, earliestRange, today).first()
            val firstDataDay = allEntries.minOfOrNull { it.date } ?: today
            val daysOfData = ChronoUnit.DAYS.between(firstDataDay, today).toInt() + 1

            val lifeAreas = repository.getAssignedLifeAreasForUser(user.id).first()
            val lifeAreaById = lifeAreas.associateBy { it.id }

            // ── Weekly ────────────────────────────────────────────────────────
            val weekly = buildTimeframe(
                label = "This Week", icon = "📅", days = 7,
                minDays = 1, daysOfData = daysOfData,
                habits = habits, today = today, allEntries = allEntries,
                lifeAreaById = lifeAreaById.mapValues { it.value.name },
                meeting = WEEKLY_MEETING
            ) { consistency, topHabit, totalActive ->
                ReviewPromptEngine.weeklyInsights(consistency, topHabit, totalActive, 0) to
                ReviewPromptEngine.weeklyReflectionPrompt(consistency, topHabit)
            }

            // ── Monthly ───────────────────────────────────────────────────────
            val monthly = buildTimeframe(
                label = "This Month", icon = "📊", days = 30,
                minDays = 7, daysOfData = daysOfData,
                habits = habits, today = today, allEntries = allEntries,
                lifeAreaById = lifeAreaById.mapValues { it.value.name },
                meeting = MONTHLY_MEETING
            ) { consistency, _, totalActive ->
                val (thriving, steady, struggling, stale) = habitHealthDistribution(habits, today, 30, allEntries)
                val anchor = computeAnchor(habits, allEntries, today.minusDays(29), today)
                val topIdentity = computeTopIdentity(habits, allEntries, today.minusDays(29), today)
                ReviewPromptEngine.monthlyInsights(
                    consistency, thriving, steady, struggling, stale,
                    anchor, topIdentity, totalActive
                ) to ReviewPromptEngine.monthlyReflectionPrompt(stale, thriving, totalActive)
            }

            // ── Quarterly ─────────────────────────────────────────────────────
            val quarterly = buildTimeframe(
                label = "This Season (90 days)", icon = "🌿", days = 90,
                minDays = 21, daysOfData = daysOfData,
                habits = habits, today = today, allEntries = allEntries,
                lifeAreaById = lifeAreaById.mapValues { it.value.name },
                meeting = QUARTERLY_MEETING
            ) { _, _, _ ->
                val start90 = today.minusDays(89)
                val anchor90 = computeAnchor(habits, allEntries, start90, today)
                val stats = computeHabitStats(habits, today.minusDays(89), today, allEntries)
                val mostConsistent = stats.maxByOrNull { it.ratio }?.takeIf { it.ratio >= 0.6f }?.name
                val grad = stats.filter { it.ratio >= 0.85f }.maxByOrNull { it.ratio }?.name
                val retire = stats.filter { it.ratio < 0.20f && it.completed > 0 }.minByOrNull { it.ratio }?.name
                ReviewPromptEngine.quarterlyInsights(
                    (stats.map { it.ratio }.average() * 100).roundToInt(),
                    habits.size, anchor90, mostConsistent, grad, retire
                ) to ReviewPromptEngine.quarterlyReflectionPrompt(grad, retire)
            }

            // ── Yearly ────────────────────────────────────────────────────────
            val graduatedCount = repository.getGraduatedHabitsForUser(user.id).first().size
            val yearly = if (daysOfData >= 90) buildTimeframe(
                label = "Past Year", icon = "🗓️", days = minOf(365, daysOfData),
                minDays = 90, daysOfData = daysOfData,
                habits = habits, today = today, allEntries = allEntries,
                lifeAreaById = lifeAreaById.mapValues { it.value.name },
                meeting = YEARLY_MEETING
            ) { consistency, topHabit, _ ->
                val start = today.minusDays(minOf(364L, daysOfData.toLong() - 1))
                val stats = computeHabitStats(habits, start, today, allEntries)
                val totalCompletions = stats.sumOf { it.completed }
                val (thriving, _, _, _) = habitHealthDistribution(habits, today, minOf(365, daysOfData), allEntries)
                ReviewPromptEngine.yearlyInsights(consistency, totalCompletions, thriving, graduatedCount, habits.size) to
                ReviewPromptEngine.yearlyReflectionPrompt(consistency, topHabit)
            } else null

            // ── Identity + growth + decisions ─────────────────────────────────
            val identityStart = today.minusDays(29)
            val entriesByDate = allEntries.filter { !it.date.isBefore(identityStart) }.groupBy { it.date }
            val identitySentences = IdentitySentenceEngine.compute(habits, entriesByDate, identityStart, today).map { it.sentence }

            val stats30 = computeHabitStats(habits, today.minusDays(29), today, allEntries)
            val projections = habits.mapNotNull { habit ->
                val stat = stats30.find { it.name == habit.name } ?: return@mapNotNull null
                if (stat.scheduled < 5) return@mapNotNull null
                val rate = stat.completed.toFloat() / stat.scheduled.coerceAtLeast(1)
                val perYear = estimateScheduledDaysPerYear(habit)
                val fiveYear = (perYear * rate * 5).roundToInt()
                val tenYear = (perYear * rate * 10).roundToInt()
                if (fiveYear == 0) null
                else GrowthProjection(
                    habitName = habit.name,
                    completionPercent = (rate * 100).roundToInt().coerceIn(0, 100),
                    scheduledDaysPerYear = perYear,
                    fiveYearSessions = fiveYear,
                    tenYearSessions = tenYear
                )
            }.sortedByDescending { it.fiveYearSessions }

            val stats90 = computeHabitStats(habits, today.minusDays(89), today, allEntries)
            val retireCandidates = stats90.filter { it.ratio < 0.20f && it.completed > 0 }.map { it.name }
            val graduateCandidates = stats90.filter { it.ratio >= 0.85f }.map { it.name }
            val overallRatio30 = stats30.map { it.ratio }.average().toFloat()
            val overCommitted = habits.size >= 7 && overallRatio30 < 0.5f

            val smartExpandedKey = when {
                today.dayOfWeek == DayOfWeek.SUNDAY -> "weekly"
                today.dayOfMonth == 1 -> "monthly"
                daysOfData >= 90 -> "yearly"
                daysOfData >= 21 -> "quarterly"
                daysOfData >= 7 -> "monthly"
                else -> "weekly"
            }

            _uiState.value = SelfStandupUiState(
                isLoading = false,
                hasHabits = true,
                userName = user.name,
                dailyIntention = dailyIntention,
                weekly = weekly,
                monthly = monthly,
                quarterly = quarterly,
                yearly = yearly,
                growthProjections = projections.take(5),
                fiveYearInsights = ReviewPromptEngine.fiveYearInsight(projections, identitySentences),
                fiveYearReflection = ReviewPromptEngine.fiveYearReflectionPrompt(identitySentences.isNotEmpty()),
                tenYearReflection = ReviewPromptEngine.tenYearReflectionPrompt(),
                identityStatements = identitySentences,
                decisionsInsights = ReviewPromptEngine.decisionsInsight(retireCandidates, graduateCandidates, overCommitted),
                retireCandidates = retireCandidates,
                graduateCandidates = graduateCandidates,
                overCommitted = overCommitted,
                commitments = commitments,
                smartExpandedKey = smartExpandedKey
            )
        }
    }

    // ── Timeframe builder ─────────────────────────────────────────────────────

    private inline fun buildTimeframe(
        label: String,
        icon: String,
        days: Int,
        minDays: Int,
        daysOfData: Int,
        habits: List<HabitDefinition>,
        today: LocalDate,
        allEntries: List<DailyHabitEntry>,
        lifeAreaById: Map<Long?, String>,
        meeting: StandupMeeting,
        crossinline makeInsights: (Int, String?, Int) -> Pair<List<String>, String>
    ): StandupTimeframe {
        if (daysOfData < minDays) {
            return StandupTimeframe(
                label = label, icon = icon, days = days,
                hasData = false, consistencyPercent = 0,
                topHabit = null, topLifeArea = null,
                insights = listOf("Building data — check back after ${minDays - daysOfData} more day${if (minDays - daysOfData == 1) "" else "s"}."),
                reflectionPrompt = "Every day you show up adds to this picture.",
                meeting = meeting,
                daysUntilUnlock = minDays - daysOfData
            )
        }
        val start = today.minusDays((days - 1).toLong())
        val rangeEntries = allEntries.filter { !it.date.isBefore(start) }
        val entriesByKey = rangeEntries.associateBy { it.date to it.habitId }
        val dates = (0 until days).map { today.minusDays((days - 1 - it).toLong()) }

        val stats = computeHabitStatsFromKey(habits, dates, entriesByKey)
        val totalScheduled = stats.sumOf { it.scheduled }
        val totalCompleted = stats.sumOf { it.completed }
        val consistency = if (totalScheduled == 0) 0 else (totalCompleted * 100 / totalScheduled)

        val topHabit = stats.filter { it.scheduled > 0 }.maxByOrNull { it.ratio }
            ?.takeIf { it.ratio >= 0.5f }?.name
        val topLifeArea = computeTopLifeArea(habits, dates, entriesByKey, lifeAreaById)
        val (insights, reflection) = makeInsights(consistency, topHabit, habits.size)

        return StandupTimeframe(
            label = label, icon = icon, days = days,
            hasData = true, consistencyPercent = consistency,
            topHabit = topHabit, topLifeArea = topLifeArea,
            insights = insights, reflectionPrompt = reflection,
            meeting = meeting
        )
    }

    // ── Computation helpers ───────────────────────────────────────────────────

    private data class HabitStat(val name: String, val completed: Int, val scheduled: Int, val ratio: Float)

    private fun computeHabitStatsFromKey(
        habits: List<HabitDefinition>,
        dates: List<LocalDate>,
        entriesByKey: Map<Pair<LocalDate, Long>, DailyHabitEntry>
    ): List<HabitStat> = habits.map { habit ->
        val scheduledDates = dates.filter { HabitRecurrenceCalculator.isScheduledOn(it, habit) }
        val completed = scheduledDates.count { date -> isEntrySuccessful(entriesByKey[date to habit.id], habit) }
        val ratio = if (scheduledDates.isEmpty()) 0f else completed.toFloat() / scheduledDates.size
        HabitStat(habit.name, completed, scheduledDates.size, ratio)
    }

    private fun computeHabitStats(
        habits: List<HabitDefinition>,
        start: LocalDate,
        end: LocalDate,
        allEntries: List<DailyHabitEntry>
    ): List<HabitStat> {
        val rangeEntries = allEntries.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        val entriesByKey = rangeEntries.associateBy { it.date to it.habitId }
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val dates = (0 until days).map { start.plusDays(it.toLong()) }
        return computeHabitStatsFromKey(habits, dates, entriesByKey)
    }

    private fun computeTopLifeArea(
        habits: List<HabitDefinition>,
        dates: List<LocalDate>,
        entriesByKey: Map<Pair<LocalDate, Long>, DailyHabitEntry>,
        lifeAreaById: Map<Long?, String>
    ): String? {
        val grouped = habits.filter { it.lifeAreaId != null }.groupBy { it.lifeAreaId }
        return grouped.maxByOrNull { (_, areaHabits) ->
            val scheduled = areaHabits.sumOf { h -> dates.count { HabitRecurrenceCalculator.isScheduledOn(it, h) } }
            val completed = areaHabits.sumOf { h ->
                dates.count { d ->
                    HabitRecurrenceCalculator.isScheduledOn(d, h) && isEntrySuccessful(entriesByKey[d to h.id], h)
                }
            }
            if (scheduled == 0) 0f else completed.toFloat() / scheduled
        }?.let { (lifeAreaId, _) -> lifeAreaById[lifeAreaId] }
    }

    private fun computeAnchor(
        habits: List<HabitDefinition>,
        allEntries: List<DailyHabitEntry>,
        start: LocalDate,
        end: LocalDate
    ): String? {
        val rangeEntries = allEntries.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        return AnchorHabitEngine.compute(habits, rangeEntries.groupBy { it.date }, start, end)?.habitName
    }

    private fun computeTopIdentity(
        habits: List<HabitDefinition>,
        allEntries: List<DailyHabitEntry>,
        start: LocalDate,
        end: LocalDate
    ): String? {
        val rangeEntries = allEntries.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        return IdentitySentenceEngine.compute(habits, rangeEntries.groupBy { it.date }, start, end).firstOrNull()?.sentence
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun habitHealthDistribution(
        habits: List<HabitDefinition>,
        today: LocalDate,
        days: Int,
        allEntries: List<DailyHabitEntry>
    ): Quadruple<Int, Int, Int, Int> {
        val start = today.minusDays((days - 1).toLong())
        val stats = computeHabitStats(habits, start, today, allEntries)
        var thriving = 0; var steady = 0; var struggling = 0; var stale = 0
        stats.forEach { s ->
            when {
                s.ratio >= 0.80f -> thriving++
                s.ratio >= 0.50f -> steady++
                s.ratio >= 0.20f -> struggling++
                else -> stale++
            }
        }
        return Quadruple(thriving, steady, struggling, stale)
    }

    private fun estimateScheduledDaysPerYear(habit: HabitDefinition): Int = when (habit.recurrenceType) {
        HabitRecurrenceType.DAILY -> 365
        HabitRecurrenceType.WEEKLY_SELECTED_DAYS -> Integer.bitCount(habit.recurrenceDaysOfWeekMask) * 52
        HabitRecurrenceType.EVERY_N_DAYS -> (365.0 / habit.recurrenceInterval.coerceAtLeast(1)).roundToInt()
        HabitRecurrenceType.MONTHLY_BY_DATE, HabitRecurrenceType.MONTHLY_BY_NTH_WEEKDAY -> 12
        HabitRecurrenceType.YEARLY_BY_DATE, HabitRecurrenceType.YEARLY_MULTI_DATE ->
            if (habit.recurrenceYearlyDates.isBlank()) 1
            else habit.recurrenceYearlyDates.split(',').count { it.trim().length == 5 }
    }

    private fun isEntrySuccessful(entry: DailyHabitEntry?, habit: HabitDefinition): Boolean {
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
