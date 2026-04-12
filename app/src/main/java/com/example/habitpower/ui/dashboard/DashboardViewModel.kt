package com.example.habitpower.ui.dashboard

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LifeAreaCompletion(
    val lifeAreaId: Long,
    val lifeAreaName: String,
    val completedCount: Int,
    val totalCount: Int,
    val completionPercent: Float
)

class DashboardViewModel(
    private val repository: HabitPowerRepository,
    private val application: android.app.Application
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    init {
        viewModelScope.launch {
            repository.seedQuotesIfNeeded()
        }
    }

    val atomicQuotes: StateFlow<List<com.example.habitpower.data.model.Quote>> = repository.allQuotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val dailyQuote: StateFlow<String> = atomicQuotes.map { list ->
        if (list.isEmpty()) "" else list[LocalDate.now().dayOfYear % list.size].text
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = "")

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
            repository.saveActiveUserId(userId)
            try {
                com.example.habitpower.ui.widget.HabitPowerWidget().updateAll(application)
            } catch (e: Exception) { // Ignore if update fails
            }
        }
    }

    // Heatmap Calculation
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedDateHabits: StateFlow<List<DailyHabitItem>> =
        combine(activeUser, selectedDate) { user, date -> user to date }
            .flatMapLatest { (user, date) ->
                if (user == null) flowOf(emptyList()) else repository.getFocusHabitItems(user.id, date)
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayHabits: StateFlow<List<DailyHabitItem>> = activeUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.getFocusHabitItems(user.id, LocalDate.now())
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val lifeAreaCompletionForSelectedDate: StateFlow<List<LifeAreaCompletion>> =
        combine(activeUser, selectedDate, assignedLifeAreas, assignedLifeAreaIds) { user, date, areas, assignedAreaIds ->
            Quad(user, date, areas, assignedAreaIds)
        }.flatMapLatest { (user, date, areas, assignedAreaIds) ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                repository.getDailyHabitItems(user.id, date).map { habits ->
                    val scopedHabits = filterDailyItemsByAssignedAreas(habits, assignedAreaIds)
                    computeLifeAreaCompletion(areas = areas, habits = scopedHabits)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Missed Yesterday
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val missedYesterdayHabitIds: StateFlow<Set<Long>> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptySet())
        val yesterday = LocalDate.now().minusDays(1)
        repository.getDailyHabitItems(user.id, yesterday).map { items ->
            items.filter { !it.isCompleted() }.map { it.habitId }.toSet()
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptySet())

    // KPI: Current Streak
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    // Selected Date Routines
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionsForSelectedDate: StateFlow<List<com.example.habitpower.data.model.WorkoutSession>> = _selectedDate
        .flatMapLatest { date -> repository.getSessionsForDate(date) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { repository.deleteWorkoutSession(sessionId) }
    }

    fun toggleHabit(habitId: Long, date: LocalDate = selectedDate.value) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            repository.toggleBooleanHabit(user.id, habitId, date)
        }
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

    private fun DailyHabitItem.isCompleted(): Boolean {
        return when (type) {
            HabitType.BOOLEAN -> entryBooleanValue == true
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> entryNumericValue != null
            HabitType.TIME -> entryNumericValue != null
            HabitType.TEXT -> !entryTextValue.isNullOrBlank()
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
                completedCount = completed,
                totalCount = total,
                completionPercent = percent
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
