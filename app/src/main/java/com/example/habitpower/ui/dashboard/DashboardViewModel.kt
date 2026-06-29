package com.example.habitpower.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.UserPreferencesRepository
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.gamification.AnchorHabitEngine
import com.example.habitpower.gamification.IdentitySentenceEngine
import com.example.habitpower.gamification.SadhanaScoreEngine
import com.example.habitpower.reminder.HabitRecurrenceCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

// Exposed so DashboardScreen can play sound/vibrate without knowing the pref keys
data class CompletionSoundPrefs(
    val soundEnabled: Boolean = true,
    val soundId: String = "positive",
    val vibrationEnabled: Boolean = true
)

data class LifeAreaCompletion(
    val lifeAreaId: Long,
    val lifeAreaName: String,
    val emoji: String?,
    val completedCount: Int,
    val totalCount: Int,
    val completionPercent: Float,
    val habits: List<DailyHabitItem> = emptyList()
)

data class CompletionFeedback(
    val habitName: String,
    val completedCount: Int,
    val totalCount: Int
)

data class StrugglingHabit(
    val habit: HabitDefinition,
    val scheduledCount: Int,
    val completedCount: Int
) {
    val completionPercent: Int get() = if (scheduledCount == 0) 0 else (completedCount * 100 / scheduledCount)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: HabitPowerRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _completionEvents = MutableSharedFlow<CompletionFeedback>(extraBufferCapacity = 1)
    val completionEvents: SharedFlow<CompletionFeedback> = _completionEvents.asSharedFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private val _quoteIndex = MutableStateFlow(0)
    private val _allQuotesList = MutableStateFlow<List<com.example.habitpower.data.model.Quote>>(emptyList())

    val atomicQuotes: StateFlow<List<com.example.habitpower.data.model.Quote>> = _allQuotesList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedQuotesIfNeeded()
        }

        viewModelScope.launch {
            repository.allQuotes.collect { list ->
                _allQuotesList.value = list
            }
        }
    }

    fun nextQuote() {
        _quoteIndex.update { it + 1 }
    }

    val weeklyStandupDoneThisWeek: StateFlow<Boolean> = prefsRepository.getStandupLastCompletedMs("weekly").map { ms ->
        if (ms == null) return@map false
        val completedDate = java.time.Instant.ofEpochMilli(ms)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        !completedDate.isBefore(weekStart)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val dailyQuote: StateFlow<String> = combine(_allQuotesList, _quoteIndex) { list, idx ->
        if (list.isEmpty()) "" else list[idx.mod(list.size)].text
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val users: StateFlow<List<UserProfile>> = repository.getActiveUsers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeUser: StateFlow<UserProfile?> = repository.getResolvedActiveUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val assignedLifeAreas: StateFlow<List<LifeArea>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getAssignedLifeAreasForUser(user.id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val assignedLifeAreaIds: StateFlow<Set<Long>> = assignedLifeAreas
        .map { areas -> areas.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    fun setActiveUser(userId: Long) {
        viewModelScope.launch {
            // saveActiveUserId already calls updateWidgetState() â†’ DataStore emits â†’ widget recomposes reactively
            repository.saveActiveUserId(userId)
        }
    }

    // Heatmap Calculation
    val heatmapData: StateFlow<Map<LocalDate, Pair<Float, Boolean>>> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptyMap())

        val start = LocalDate.now().minusDays(89)
        val end = LocalDate.now()

        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id),
            repository.getEntriesForUserInRange(user.id, start, end)
        ) { assignedHabits, assignedAreaIds, entries ->
            val scopedHabits = filterHabitsByAssignedAreas(assignedHabits, assignedAreaIds.toSet())
            DashboardMetrics.buildHeatmap(scopedHabits, entries.groupBy { it.date }, start, end)
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyMap())

    // Habits for selected date (supports backfilling)
    val selectedDateHabits: StateFlow<List<DailyHabitItem>> =
        combine(activeUser, selectedDate) { user, date -> user to date }
            .flatMapLatest { (user, date) ->
                if (user == null) flowOf(emptyList()) else repository.getFocusHabitItems(user.id, date)
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    val todayHabits: StateFlow<List<DailyHabitItem>> = activeUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getFocusHabitItems(user.id, LocalDate.now())
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    val lifeAreaCompletionForSelectedDate: StateFlow<List<LifeAreaCompletion>> =
        combine(activeUser, selectedDate, assignedLifeAreas, assignedLifeAreaIds) { user, date, areas, assignedAreaIds ->
            Quad(user, date, areas, assignedAreaIds)
        }.flatMapLatest { (user, date, areas, assignedAreaIds) ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                // getFocusHabitItems (not getDailyHabitItems) so TIMER habits are included:
                // TIMER habits have showInDailyCheckIn=false and are excluded by getDailyHabitItems,
                // but they are real habits that count toward life area and progress tracking.
                repository.getFocusHabitItems(user.id, date).map { habits ->
                    val scopedHabits = filterDailyItemsByAssignedAreas(habits, assignedAreaIds)
                    computeLifeAreaCompletion(areas = areas, habits = scopedHabits)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Identity Wall â€” habits the user has graduated (internalized)
    val graduatedHabits: StateFlow<List<HabitDefinition>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else repository.getGraduatedHabitsForUser(user.id)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    // Missed Yesterday
    val missedYesterdayHabitIds: StateFlow<Set<Long>> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptySet())
        val yesterday = LocalDate.now().minusDays(1)
        repository.getDailyHabitItems(user.id, yesterday).map { items ->
            items.filter { !it.isCompleted() }.map { it.habitId }.toSet()
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptySet())

    // Anchor Habit â€” keystone practice (requires 21+ days of data)
    val anchorHabit: StateFlow<AnchorHabitEngine.AnchorHabit?> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(null)
        val end = LocalDate.now().minusDays(1)
        val start = end.minusDays(89) // 90-day window for statistical strength
        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id),
            repository.getEntriesForUserInRange(user.id, start, end)
        ) { habits, assignedAreaIds, entries ->
            val scopedHabits = filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
            val entriesByDate = entries.groupBy { it.date }
            AnchorHabitEngine.compute(scopedHabits, entriesByDate, start, end)
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    // Identity Sentences â€” "You are someone who..."
    val identitySentences: StateFlow<List<IdentitySentenceEngine.IdentitySentence>> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptyList())
        val end = LocalDate.now().minusDays(1)
        val start = end.minusDays(29) // 30-day lookback window
        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id),
            repository.getEntriesForUserInRange(user.id, start, end)
        ) { habits, assignedAreaIds, entries ->
            val scopedHabits = filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
            val entriesByDate = entries.groupBy { it.date }
            IdentitySentenceEngine.compute(scopedHabits, entriesByDate, start, end)
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    // Yesterday's Sadhana Score
    val sadhanaScore: StateFlow<SadhanaScoreEngine.Score?> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(null)
        val yesterday = LocalDate.now().minusDays(1)
        combine(
            repository.getFocusHabitItems(user.id, yesterday),
            repository.getStatForDate(yesterday),
            repository.getMeditationSessionCountForDate(user.id, yesterday)
        ) { habits, stat, meditationCount ->
            if (habits.isEmpty()) null
            else SadhanaScoreEngine.compute(habits, stat, yesterday, meditationCount)
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    // 7-day Sadhana Score history for sparkline
    val sadhanaWeeklyScores: StateFlow<List<Pair<LocalDate, Int>>> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptyList())
        val end = LocalDate.now().minusDays(1)
        val start = end.minusDays(6)
        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id),
            repository.getEntriesForUserInRange(user.id, start, end),
            repository.getStatsForDateRange(start, end)
        ) { habits, assignedAreaIds, entries, stats ->
            val scopedHabits = filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
            val entriesByDate = entries.groupBy { it.date }
            val statsByDate = stats.associateBy { it.date }
            (0..6).map { offset ->
                val date = start.plusDays(offset.toLong())
                val scheduledIds = DashboardMetrics.scheduledHabitIdsForDate(scopedHabits, date)
                val completedIds = entriesByDate[date].orEmpty().map { it.habitId }.distinct().toSet()
                val completed = scheduledIds.count { it in completedIds }
                val stat = statsByDate[date]
                date to SadhanaScoreEngine.computeSimple(completed, scheduledIds.size, stat?.sleepHours)
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    // KPI: Current Streak
    val currentStreak: StateFlow<Int> = activeUser.flatMapLatest activeUserFlow@{ user ->
        if (user == null) return@activeUserFlow flowOf(0)

        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id)
        ) { habits, assignedAreaIds ->
            filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
        }.flatMapLatest habitsFlow@{ habits ->
            if (habits.isEmpty()) return@habitsFlow flowOf(0)

            val start = LocalDate.now().minusDays(89)
            val end = LocalDate.now()

            repository.getEntriesForUserInRange(user.id, start, end).map { entries ->
                DashboardMetrics.currentStreak(habits, entries.groupBy { it.date }, LocalDate.now())
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    // KPI: This Week Consistency %
    val thisWeekConsistency: StateFlow<Int> = activeUser.flatMapLatest activeUserFlow@{ user ->
        if (user == null) return@activeUserFlow flowOf(0)

        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id)
        ) { habits, assignedAreaIds ->
            filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
        }.flatMapLatest habitsFlow@{ habits ->
            if (habits.isEmpty()) return@habitsFlow flowOf(0)

            val today = LocalDate.now()
            val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())

            repository.getEntriesForUserInRange(user.id, startOfWeek, today).map { entries ->
                DashboardMetrics.consistencyPercentage(habits, entries.groupBy { it.date }, startOfWeek, today)
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    // KPI: Best Personal Record %
    val bestPersonalRecord: StateFlow<Int> = activeUser.flatMapLatest activeUserFlow@{ user ->
        if (user == null) return@activeUserFlow flowOf(0)

        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id)
        ) { habits, assignedAreaIds ->
            filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
        }.flatMapLatest habitsFlow@{ habits ->
            if (habits.isEmpty()) return@habitsFlow flowOf(0)

            val start = LocalDate.now().minusDays(89)
            val end = LocalDate.now()

            repository.getEntriesForUserInRange(user.id, start, end).map { entries ->
                DashboardMetrics.bestWeeklyPercentage(habits, entries.groupBy { it.date }, start, end)
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    // Completion sound + vibration prefs (for DashboardScreen to react to)
    val completionSoundPrefs: StateFlow<CompletionSoundPrefs> = combine(
        repository.getCompletionSoundEnabled(),
        repository.getCompletionSoundId(),
        repository.getCompletionVibrationEnabled()
    ) { soundEnabled, soundId, vibrationEnabled ->
        CompletionSoundPrefs(soundEnabled, soundId, vibrationEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CompletionSoundPrefs()
    )

    // Selected Date Routines
    val sessionsForSelectedDate: StateFlow<List<com.example.habitpower.data.model.WorkoutSession>> = _selectedDate
        .flatMapLatest { date -> repository.getSessionsForDate(date) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    // Habit Health — habits scheduled ≥4 times in last 14 days with <50% completion
    val strugglingHabits: StateFlow<List<StrugglingHabit>> = activeUser.flatMapLatest activeUserFlow@{ user ->
        if (user == null) return@activeUserFlow flowOf(emptyList())

        combine(
            repository.getAssignedHabitsForUser(user.id),
            repository.getAssignedLifeAreaIdsForUser(user.id)
        ) { habits, assignedAreaIds ->
            filterHabitsByAssignedAreas(habits, assignedAreaIds.toSet())
        }.flatMapLatest habitsFlow@{ habits ->
            val activeHabits = habits.filter { it.lifecycleStatus == HabitLifecycleStatus.ACTIVE }
            if (activeHabits.isEmpty()) return@habitsFlow flowOf(emptyList())

            val today = LocalDate.now()
            val end = today.minusDays(1)
            val start = today.minusDays(14)

            repository.getEntriesForUserInRange(user.id, start, end).map { entries ->
                val entriesByHabitAndDate = entries.groupBy { it.habitId to it.date }
                activeHabits.mapNotNull { habit ->
                    var scheduledCount = 0
                    var completedCount = 0
                    var date = start
                    while (!date.isAfter(end)) {
                        if (HabitRecurrenceCalculator.isScheduledOn(date, habit)) {
                            scheduledCount++
                            val entry = entriesByHabitAndDate[habit.id to date]?.firstOrNull()
                            if (entry != null && isEntryCompleted(entry, habit.type)) completedCount++
                        }
                        date = date.plusDays(1)
                    }
                    if (scheduledCount >= 4 && completedCount * 2 < scheduledCount) {
                        StrugglingHabit(habit, scheduledCount, completedCount)
                    } else null
                }
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteWorkoutSession(sessionId) }
    }

    fun toggleHabit(habitId: Long, date: LocalDate = selectedDate.value) {
        val user = activeUser.value ?: return
        val snapshot = todayHabits.value
        val habit = snapshot.firstOrNull { it.habitId == habitId }
        val wasCompleted = habit?.isCompleted() ?: true  // treat unknown as done â†’ no feedback

        viewModelScope.launch {
            repository.toggleBooleanHabit(user.id, habitId, date)

            // Only fire feedback when going incomplete â†’ complete, never for un-marking
            if (!wasCompleted && habit != null) {
                val doneNow = snapshot.count { it.isCompleted() } + 1
                val total = snapshot.size
                _completionEvents.tryEmit(
                    CompletionFeedback(
                        habitName = habit.name,
                        completedCount = doneNow,
                        totalCount = total
                    )
                )
            }
        }
    }

    fun updateHabitTime(habitId: Long, time: String?) {
        viewModelScope.launch {
            repository.updateHabitCommitmentTime(habitId, time?.takeIf { it.isNotBlank() })
        }
    }

    // Step-Back Mode state
    val stepBackActive: StateFlow<Boolean> = repository.getStepBackActive()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    val stepBackReturnEpochDay: StateFlow<Long?> = repository.getStepBackReturnEpochDay()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    // Bright Spot â€” one completed habit surfaced when week is rough (< 50% consistency)
    val brightSpotHabit: StateFlow<DailyHabitItem?> = combine(todayHabits, thisWeekConsistency) { habits, consistency ->
        if (consistency == 0 || consistency >= 50) null
        else habits.filter { it.isCompleted() }.firstOrNull()
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    // Season Awareness â€” prompt when user is struggling but not in step-back mode
    val showSeasonAwareness: StateFlow<Boolean> = combine(thisWeekConsistency, stepBackActive) { consistency, stepBack ->
        !stepBack && consistency in 1..39
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    // Milestone Wins â€” identity-level achievements from anchor, identity sentences, and graduated habits
    data class MilestoneWin(val emoji: String, val text: String)

    val milestoneWins: StateFlow<List<MilestoneWin>> = combine(
        identitySentences,
        anchorHabit,
        graduatedHabits
    ) { sentences, anchor, graduated ->
        buildList {
            anchor?.let {
                val multiplier = it.multiplier.toInt().coerceAtLeast(2)
                add(MilestoneWin("⚓", "${it.habitName} — your anchor habit. Doing it makes you ${multiplier}x more likely to complete the rest of your day."))
            }
            sentences.forEach { sentence ->
                add(MilestoneWin("✦", sentence.sentence))
            }
            graduated.forEach { habit ->
                add(MilestoneWin("🎓", "${habit.name} — internalized. You no longer need to track it — it's who you are."))
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    fun saveReflection(habitId: Long, date: LocalDate, quality: Int) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            repository.updateEntryQuality(user.id, habitId, date, quality)
        }
    }

    fun pauseHabit(habit: HabitDefinition) {
        viewModelScope.launch {
            repository.setHabitLifecycle(habit, HabitLifecycleStatus.PAUSED)
        }
    }

    // ── On Hold ───────────────────────────────────────────────────────────────

    // Habits currently on hold (time-bound pauses with a return date), sorted by return date.
    val onHoldHabits: StateFlow<List<HabitDefinition>> = activeUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getOnHoldHabitsForUser(user.id)
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    fun putOnHold(habitId: Long, untilDate: LocalDate?) {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId) ?: return@launch
            repository.putHabitOnHold(habit, untilDate)
        }
    }

    fun resumeFromHold(habit: HabitDefinition) {
        viewModelScope.launch { repository.resumeHabitFromHold(habit) }
    }

    fun logTwoMinutes(habit: DailyHabitItem, date: LocalDate = selectedDate.value) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            repository.saveDailyHabitEntry(
                userId = user.id,
                date = date,
                habitId = habit.habitId,
                type = habit.type,
                numericValue = 1.0,
                textValue = "Did 2 mins"
            )
        }
    }

    private fun isEntryCompleted(entry: DailyHabitEntry, type: HabitType): Boolean = when (type) {
        HabitType.BOOLEAN, HabitType.ROUTINE -> entry.booleanValue == true
        HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT,
        HabitType.POMODORO, HabitType.TIMER, HabitType.TIME -> entry.numericValue != null
        HabitType.TEXT -> !entry.textValue.isNullOrBlank()
    }

    private fun DailyHabitItem.isCompleted(): Boolean {
        return when (type) {
            HabitType.BOOLEAN -> entryBooleanValue == true
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> entryNumericValue != null
            HabitType.TIME -> entryNumericValue != null
            HabitType.TEXT -> !entryTextValue.isNullOrBlank()
            HabitType.ROUTINE -> entryBooleanValue == true
        }
    }

    private fun computeLifeAreaCompletion(
        areas: List<LifeArea>,
        habits: List<DailyHabitItem>
    ): List<LifeAreaCompletion> {
        val grouped = habits.filter { it.lifeAreaId != null }.groupBy { it.lifeAreaId!! }

        return areas.map { area ->
            val areaHabits = grouped[area.id].orEmpty()
            val total = areaHabits.size
            val completed = areaHabits.count { it.isCompleted() }
            val percent = if (total == 0) 0f else (completed * 100f / total)

            LifeAreaCompletion(
                lifeAreaId = area.id,
                lifeAreaName = area.name,
                emoji = area.emoji,
                completedCount = completed,
                totalCount = total,
                completionPercent = percent,
                habits = areaHabits
            )
        }
    }

    private fun filterHabitsByAssignedAreas(
        habits: List<HabitDefinition>,
        assignedAreaIds: Set<Long>
    ): List<HabitDefinition> {
        if (assignedAreaIds.isEmpty()) return habits
        return habits.filter { habit -> habit.lifeAreaId != null && assignedAreaIds.contains(habit.lifeAreaId) }
    }

    private fun filterDailyItemsByAssignedAreas(
        items: List<DailyHabitItem>,
        assignedAreaIds: Set<Long>
    ): List<DailyHabitItem> {
        if (assignedAreaIds.isEmpty()) return items
        return items.filter { item -> item.lifeAreaId != null && assignedAreaIds.contains(item.lifeAreaId) }
    }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
