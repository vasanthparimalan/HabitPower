package com.example.habitpower.ui.dashboard

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitpower.data.HabitPowerRepository
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

    val activeLifeAreas: StateFlow<List<LifeArea>> = repository.getActiveLifeAreas().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeUser: StateFlow<UserProfile?> = repository.getResolvedActiveUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
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
            repository.getEntriesForUserInRange(user.id, start, end)
        ) { assignedHabits, entries ->
            val entriesByDate = entries.groupBy { it.date }
            val maxPoints = assignedHabits.size
            val hasHabits = assignedHabits.isNotEmpty()

            val heatmap = mutableMapOf<LocalDate, Pair<Float, Boolean>>()
            var date = start
            while (!date.isAfter(end)) {
                val dayEntries = entriesByDate[date] ?: emptyList()

                val points = dayEntries.size.toFloat()
                val ratio = if (maxPoints > 0) (points / maxPoints).coerceIn(0f, 1f) else 0f

                heatmap[date] = Pair(ratio, hasHabits)
                date = date.plusDays(1)
            }
            heatmap
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyMap())

    // Today's Habits
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayHabits: StateFlow<List<DailyHabitItem>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getFocusHabitItems(user.id, LocalDate.now())
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val lifeAreaCompletionForSelectedDate: StateFlow<List<LifeAreaCompletion>> =
        combine(activeUser, selectedDate, activeLifeAreas) { user, date, areas ->
            Triple(user, date, areas)
        }.flatMapLatest { (user, date, areas) ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                repository.getDailyHabitItems(user.id, date).map { habits ->
                    computeLifeAreaCompletion(areas = areas, habits = habits)
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
    val currentStreak: StateFlow<Int> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(0)

        repository.getAssignedHabitsForUser(user.id).flatMapLatest { habits ->
            val habitIds = habits.map { it.id }
            if (habitIds.isEmpty()) return@flatMapLatest flowOf(0)

            val start = LocalDate.now().minusDays(89)
            val end = LocalDate.now()

            repository.getEntriesForUserInRange(user.id, start, end).map { entries ->
                val entriesByDate = entries.groupBy { it.date }
                var streak = 0
                var date = LocalDate.now()

                while (true) {
                    val dayEntries = entriesByDate[date] ?: emptyList()
                    val completedHabits = dayEntries.map { it.habitId }.distinct()
                    val allCompleted = habitIds.all { it in completedHabits }

                    if (allCompleted) {
                        streak++
                        date = date.minusDays(1)
                    } else {
                        break
                    }
                }
                streak
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    // KPI: This Week Consistency %
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val thisWeekConsistency: StateFlow<Int> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(0)

        repository.getAssignedHabitsForUser(user.id).flatMapLatest { habits ->
            val habitIds = habits.map { it.id }
            if (habitIds.isEmpty()) return@flatMapLatest flowOf(0)

            val today = LocalDate.now()
            val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())

            repository.getEntriesForUserInRange(user.id, startOfWeek, today).map { entries ->
                val entriesByDate = entries.groupBy { it.date }
                var completedCount = 0
                var totalCount = 0

                var date = startOfWeek
                while (!date.isAfter(today)) {
                    val dayEntries = entriesByDate[date] ?: emptyList()
                    val completedHabits = dayEntries.map { it.habitId }.distinct()
                    completedCount += completedHabits.count { it in habitIds }
                    totalCount += habitIds.size
                    date = date.plusDays(1)
                }

                if (totalCount == 0) 0 else ((completedCount * 100) / totalCount)
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    // KPI: Best Personal Record %
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bestPersonalRecord: StateFlow<Int> = activeUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(0)

        repository.getAssignedHabitsForUser(user.id).flatMapLatest { habits ->
            val habitIds = habits.map { it.id }
            if (habitIds.isEmpty()) return@flatMapLatest flowOf(0)

            val start = LocalDate.now().minusDays(89)
            val end = LocalDate.now()

            repository.getEntriesForUserInRange(user.id, start, end).map { entries ->
                val entriesByDate = entries.groupBy { it.date }
                var bestWeekPercentage = 0

                var weekStart = start
                while (!weekStart.isAfter(end)) {
                    val weekEnd = weekStart.plusDays(6).let { if (it.isAfter(end)) end else it }
                    var weekCompleted = 0
                    var weekTotal = 0

                    var date = weekStart
                    while (!date.isAfter(weekEnd)) {
                        val dayEntries = entriesByDate[date] ?: emptyList()
                        val completedHabits = dayEntries.map { it.habitId }.distinct()
                        weekCompleted += completedHabits.count { it in habitIds }
                        weekTotal += habitIds.size
                        date = date.plusDays(1)
                    }

                    if (weekTotal > 0) {
                        val weekPercentage = (weekCompleted * 100) / weekTotal
                        if (weekPercentage > bestWeekPercentage) {
                            bestWeekPercentage = weekPercentage
                        }
                    }
                    weekStart = weekStart.plusDays(7)
                }
                bestWeekPercentage
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

    fun toggleHabit(habitId: Long) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            repository.toggleBooleanHabit(user.id, habitId, LocalDate.now())
        }
    }

    fun logTwoMinutes(habit: DailyHabitItem) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            repository.saveDailyHabitEntry(
                userId = user.id,
                date = LocalDate.now(),
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
}
