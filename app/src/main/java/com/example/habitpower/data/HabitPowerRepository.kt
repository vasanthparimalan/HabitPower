package com.example.habitpower.data

import android.content.Context
import com.example.habitpower.data.dao.DailyHealthStatDao
import com.example.habitpower.data.dao.ExerciseDao
import com.example.habitpower.data.dao.HabitTrackingDao
import com.example.habitpower.data.dao.LifeAreaDao
import com.example.habitpower.data.dao.RoutineDao
import com.example.habitpower.data.dao.UserDao
import com.example.habitpower.data.dao.WorkoutSessionDao
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.dao.RoutineNotificationSettingsDao
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import com.example.habitpower.data.model.RoutineNotificationSettings
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.data.model.UserLifeAreaAssignment
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.data.model.WorkoutSession
import com.example.habitpower.reminder.HabitReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HabitPowerRepository(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val dailyHealthStatDao: DailyHealthStatDao,
    private val userDao: UserDao,
    private val habitTrackingDao: HabitTrackingDao,
    private val lifeAreaDao: LifeAreaDao,
    private val quoteDao: com.example.habitpower.data.dao.QuoteDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val routineNotificationSettingsDao: RoutineNotificationSettingsDao
) {
    private val refreshTrigger = MutableStateFlow(0)

    fun triggerRefresh() {
        refreshTrigger.value++
    }

    fun observeRefresh(): StateFlow<Int> = refreshTrigger.asStateFlow()

    suspend fun forceRefresh() {
        triggerRefresh()
    }

    suspend fun updateWidgetState() {
        val user = getResolvedActiveUser().firstOrNull() ?: return
        val today = LocalDate.now()
        val habits = habitTrackingDao.getDailyHabitItems(user.id, today).firstOrNull() ?: emptyList()

        val widgetHabits = habits.map { habit ->
            val isCompleted = when (habit.type) {
                HabitType.BOOLEAN -> habit.entryBooleanValue == true
                HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> habit.entryNumericValue != null
                HabitType.TIME -> habit.entryNumericValue != null
                HabitType.ROUTINE -> habit.entryBooleanValue == true
                HabitType.TEXT -> !habit.entryTextValue.isNullOrBlank()
            }
            WidgetHabit(
                name = habit.name,
                isCompleted = isCompleted,
                streak = getHabitStreak(user.id, habit.habitId, habit.type, habit.targetValue, habit.operator)
            )
        }

        val state = WidgetState(
            userName = user.name,
            habits = widgetHabits
        )

        context.saveWidgetState(state)
    }

    fun getWidgetState(): Flow<WidgetState> = context.getWidgetState()

    val allQuotes: Flow<List<com.example.habitpower.data.model.Quote>> = quoteDao.getAllQuotes()
    suspend fun insertQuote(quote: com.example.habitpower.data.model.Quote) = quoteDao.insertQuote(quote)
    suspend fun deleteQuote(quote: com.example.habitpower.data.model.Quote) = quoteDao.deleteQuote(quote)
    suspend fun seedQuotesIfNeeded() {
        val defaults = listOf(
            "Make the cue obvious: redesign your environment so the next right action is easy to see. (Atomic Habits)",
            "Make it attractive: pair a difficult habit with something you already enjoy. (Atomic Habits)",
            "Make it easy: shrink today's version until starting feels effortless. (Atomic Habits)",
            "Make it satisfying: close your day with a visible win, however small. (Atomic Habits)",
            "Small daily gains compound into major change over time. (Atomic Habits)",
            "Focus on systems and processes, not only goals and outcomes. (Atomic Habits)",
            "A habit loop has cue, routine, and reward - improve one loop at a time. (The Power of Habit)",
            "When a cue appears, decide your routine before the moment arrives. (The Power of Habit)",
            "Cravings drive habits; attach your routine to a meaningful identity. (The Power of Habit)",
            "Keystone habits trigger progress in other areas - protect your keystone first. (The Power of Habit)",
            "Tiny actions done consistently beat intense actions done occasionally. (Tiny Habits)",
            "Reduce friction for good habits and add friction for distracting ones. (Behavior Design)"
        )

        val existing = quoteDao.getAllQuotesSync().map { it.text.trim() }.toSet()
        defaults.filterNot { it in existing }
            .forEach { text -> quoteDao.insertQuote(com.example.habitpower.data.model.Quote(text = text)) }
    }
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)

    fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()
    suspend fun getRoutineById(id: Long): Routine? = routineDao.getRoutineById(id)
    suspend fun insertRoutine(routine: Routine): Long = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutine(routine: Routine) = routineDao.deleteRoutine(routine)

    fun getExercisesForRoutine(routineId: Long): Flow<List<Exercise>> = routineDao.getExercisesForRoutine(routineId)
    fun getExerciseCountForRoutine(routineId: Long): Flow<Int> = routineDao.getExerciseCountForRoutine(routineId)
    suspend fun addExerciseToRoutine(routineId: Long, exerciseId: Long, order: Int) {
        routineDao.insertRoutineExerciseCrossRef(RoutineExerciseCrossRef(routineId, exerciseId, order))
    }

    suspend fun clearRoutineExercises(routineId: Long) {
        routineDao.deleteRoutineExercises(routineId)
    }

    // Routine Notification Settings
    fun getRoutineNotificationSettings(): Flow<RoutineNotificationSettings?> =
        routineNotificationSettingsDao.getSettings()

    suspend fun updateRoutineNotificationSettings(settings: RoutineNotificationSettings) =
        routineNotificationSettingsDao.updateSettings(settings)

    fun getRoutineStartSoundEnabled(): Flow<Boolean> = userPreferencesRepository.routineStartSoundEnabled
    fun getRoutineStartSoundId(): Flow<String> = userPreferencesRepository.routineStartSoundId
    fun getRoutineEndSoundEnabled(): Flow<Boolean> = userPreferencesRepository.routineEndSoundEnabled
    fun getRoutineEndSoundId(): Flow<String> = userPreferencesRepository.routineEndSoundId

    fun getSessionsForDate(date: LocalDate): Flow<List<WorkoutSession>> = workoutSessionDao.getSessionsForDate(date)
    fun getSessionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>> =
        workoutSessionDao.getSessionsForDateRange(startDate, endDate)

    suspend fun insertSession(session: WorkoutSession) = workoutSessionDao.insertSession(session)
    suspend fun deleteWorkoutSession(sessionId: Long) = workoutSessionDao.deleteSession(sessionId)

    fun getStatForDate(date: LocalDate): Flow<DailyHealthStat?> = dailyHealthStatDao.getStatForDate(date)
    fun getStatsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthStat>> =
        dailyHealthStatDao.getStatsForDateRange(startDate, endDate)

    suspend fun saveDailyStat(stat: DailyHealthStat) = dailyHealthStatDao.insertOrUpdate(stat)

    val activeUserId: Flow<Long?> = userPreferencesRepository.activeUserId
    val streakBaseDays: Flow<Int> =
        observeRefresh().flatMapLatest {
            userPreferencesRepository.streakBaseDays
        }

    suspend fun saveActiveUserId(userId: Long) {
        userPreferencesRepository.saveActiveUserId(userId)
        syncHabitReminders()
        updateWidgetState()
    }

    suspend fun saveStreakBaseDays(days: Int) = userPreferencesRepository.saveStreakBaseDays(days)

    fun getAllUsers(): Flow<List<UserProfile>> = userDao.getAllUsers()
    fun getActiveUsers(): Flow<List<UserProfile>> = userDao.getActiveUsers()
    suspend fun getUserById(userId: Long): UserProfile? = userDao.getUserById(userId)

    fun getResolvedActiveUser(): Flow<UserProfile?> =
        observeRefresh().flatMapLatest {
            combine(getActiveUsers(), activeUserId) { users, storedUserId ->
                when {
                    users.isEmpty() -> null
                    storedUserId == null -> users.first()
                    else -> users.firstOrNull { it.id == storedUserId } ?: users.first()
                }
            }
        }

    suspend fun createUser(name: String): Long {
        val safeName = InputSanitizer.sanitize(name, 100) ?: "User"
        return userDao.insertUser(UserProfile(name = safeName))
    }
    suspend fun updateUser(user: UserProfile) = userDao.updateUser(user)
    suspend fun deleteUser(user: UserProfile) = userDao.deleteUser(user)

    fun getAllHabits(): Flow<List<HabitDefinition>> = habitTrackingDao.getAllHabits()
    fun getAssignedHabitsForUser(userId: Long): Flow<List<HabitDefinition>> = habitTrackingDao.getAssignedHabitsForUser(userId)
    fun getAssignedHabitIdsForUser(userId: Long): Flow<List<Long>> = habitTrackingDao.getAssignedHabitIdsForUser(userId)

    suspend fun updateHabit(habit: HabitDefinition) {
        habitTrackingDao.updateHabit(habit)
        syncHabitReminders()
        updateWidgetState()
    }

    suspend fun updateHabitCommitmentTime(habitId: Long, time: String?) {
        val habit = habitTrackingDao.getHabitById(habitId) ?: return
        habitTrackingDao.updateHabit(habit.copy(commitmentTime = time))
        syncHabitReminders()
    }

    suspend fun deleteHabit(habit: HabitDefinition) {
        habitTrackingDao.deleteHabit(habit)
        HabitReminderScheduler.cancelForHabit(context, habit.id)
        updateWidgetState()
    }

    suspend fun createHabit(
        name: String,
        goalIdentityStatement: String,
        description: String,
        type: HabitType,
        routineId: Long? = null,
        unit: String?,
        targetValue: Double?,
        operator: TargetOperator = TargetOperator.GREATER_THAN_OR_EQUAL,
        lifeAreaId: Long? = null,
        showInDailyCheckIn: Boolean = true,
        commitmentTime: String? = null,
        commitmentLocation: String = "",
        preReminderMinutes: Int? = null,
        recurrenceType: HabitRecurrenceType = HabitRecurrenceType.DAILY,
        recurrenceInterval: Int = 1,
        recurrenceDaysOfWeekMask: Int = 0,
        recurrenceDayOfMonth: Int? = null,
        recurrenceWeekOfMonth: Int? = null,
        recurrenceWeekday: Int? = null,
        recurrenceYearlyDates: String = "",
        recurrenceAnchorDate: LocalDate? = null,
        recurrenceStartDate: LocalDate? = null,
        recurrenceEndDate: LocalDate? = null
    ): Long {
        val nextOrder = (habitTrackingDao.getAllHabits().first().maxOfOrNull { it.displayOrder } ?: -1) + 1
        val safeName = InputSanitizer.sanitize(name, 120) ?: throw IllegalArgumentException("Habit name required")
        val safeGoalIdentity = InputSanitizer.sanitize(goalIdentityStatement, 180)
            ?: throw IllegalArgumentException("Goal identity statement required")
        val safeDescription = InputSanitizer.sanitize(description, 1000) ?: ""
        val habitId = habitTrackingDao.insertHabit(
            HabitDefinition(
                name = safeName,
                goalIdentityStatement = safeGoalIdentity,
                description = safeDescription,
                commitmentTime = commitmentTime,
                commitmentLocation = InputSanitizer.sanitize(commitmentLocation, 180) ?: "",
                preReminderMinutes = preReminderMinutes,
                recurrenceType = recurrenceType,
                recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
                recurrenceDaysOfWeekMask = recurrenceDaysOfWeekMask,
                recurrenceDayOfMonth = recurrenceDayOfMonth,
                recurrenceWeekOfMonth = recurrenceWeekOfMonth,
                recurrenceWeekday = recurrenceWeekday,
                recurrenceYearlyDates = recurrenceYearlyDates,
                recurrenceAnchorDate = recurrenceAnchorDate,
                recurrenceStartDate = recurrenceStartDate,
                recurrenceEndDate = recurrenceEndDate,
                type = type,
                unit = unit?.trim()?.ifBlank { null },
                targetValue = targetValue,
                displayOrder = nextOrder,
                operator = operator,
                lifeAreaId = lifeAreaId,
                routineId = routineId,
                showInDailyCheckIn = showInDailyCheckIn
            )
        )
        // Reminders must only be active for habits assigned to the active user.
        // A newly created habit is not necessarily assigned yet.
        syncHabitReminders()
        return habitId
    }

    suspend fun syncHabitReminders() {
        val activeUser = getResolvedActiveUser().first()
        val allHabits = habitTrackingDao.getAllHabits().first()
        if (activeUser == null) {
            allHabits.forEach { HabitReminderScheduler.cancelForHabit(context, it.id) }
            return
        }

        val assignedHabitIds = habitTrackingDao.getAssignedHabitIdsForUser(activeUser.id).first().toSet()
        allHabits.forEach { habit ->
            if (habit.id in assignedHabitIds) {
                HabitReminderScheduler.scheduleForHabit(context, habit)
            } else {
                HabitReminderScheduler.cancelForHabit(context, habit.id)
            }
        }
    }

    // LifeArea helpers
    fun getAllLifeAreas(): Flow<List<com.example.habitpower.data.model.LifeArea>> = lifeAreaDao.getAllLifeAreas()
    fun getActiveLifeAreas(): Flow<List<com.example.habitpower.data.model.LifeArea>> = lifeAreaDao.getActiveLifeAreas()
    fun getAssignedLifeAreasForUser(userId: Long): Flow<List<com.example.habitpower.data.model.LifeArea>> =
        lifeAreaDao.getAssignedLifeAreasForUser(userId)

    fun getAssignedLifeAreaIdsForUser(userId: Long): Flow<List<Long>> = lifeAreaDao.getAssignedLifeAreaIdsForUser(userId)

    suspend fun createLifeArea(l: com.example.habitpower.data.model.LifeArea): Long {
        val safeName = InputSanitizer.sanitize(l.name, 120) ?: throw IllegalArgumentException("LifeArea name required")
        val safeDesc = InputSanitizer.sanitize(l.description, 1000)
        return lifeAreaDao.insertLifeArea(l.copy(name = safeName, description = safeDesc))
    }

    suspend fun updateLifeArea(l: com.example.habitpower.data.model.LifeArea) {
        val safeName = InputSanitizer.sanitize(l.name, 120) ?: throw IllegalArgumentException("LifeArea name required")
        val safeDesc = InputSanitizer.sanitize(l.description, 1000)
        lifeAreaDao.updateLifeArea(l.copy(name = safeName, description = safeDesc))
    }
    suspend fun deleteLifeArea(l: com.example.habitpower.data.model.LifeArea) = lifeAreaDao.deleteLifeArea(l)

    suspend fun replaceAssignmentsForUser(userId: Long, habitIds: List<Long>) {
        habitTrackingDao.clearAssignmentsForUser(userId)
        habitIds.forEachIndexed { index, habitId ->
            habitTrackingDao.upsertAssignment(
                UserHabitAssignment(
                    userId = userId,
                    habitId = habitId,
                    displayOrder = index
                )
            )
        }
        syncHabitReminders()
        updateWidgetState()
    }

    suspend fun replaceLifeAreaAssignmentsForUser(userId: Long, lifeAreaIds: List<Long>) {
        lifeAreaDao.clearLifeAreaAssignmentsForUser(userId)
        lifeAreaIds.forEachIndexed { index, lifeAreaId ->
            lifeAreaDao.upsertLifeAreaAssignment(
                UserLifeAreaAssignment(
                    userId = userId,
                    lifeAreaId = lifeAreaId,
                    displayOrder = index
                )
            )
        }
        triggerRefresh()
    }

    fun getDailyHabitItems(userId: Long, date: LocalDate): Flow<List<DailyHabitItem>> =
        habitTrackingDao.getDailyHabitItems(userId, date).map { items ->
            items
                .filter { it.showInDailyCheckIn && it.isScheduledOn(date) }
                .sortedBy { it.effectiveDisplayOrder }
        }

    fun getFocusHabitItems(userId: Long, date: LocalDate): Flow<List<DailyHabitItem>> =
        observeRefresh().flatMapLatest {
            habitTrackingDao.getDailyHabitItems(userId, date).map { items ->
                items
                    .filter { it.isScheduledOn(date) }
                    .sortedBy { it.effectiveDisplayOrder }
            }
        }

    /**
     * Focus needs assigned habits (e.g., TIMER/POMODORO) even when they are hidden
     * from Daily Check-In. So we intentionally do not filter by `showInDailyCheckIn`.
     */

    fun getWidgetHabitItems(userId: Long, date: LocalDate): Flow<List<DailyHabitItem>> =
        observeRefresh().flatMapLatest {
            habitTrackingDao.getDailyHabitItems(userId, date).map { items ->
                items
                    .filter { it.showInWidget && it.isScheduledOn(date) }
                    .sortedBy { it.effectiveDisplayOrder }
            }
        }

    /** Returns a list of [days] entries (today first), null where no entry exists. */
    suspend fun getHabitHistoryDays(userId: Long, habitId: Long, days: Int): List<Pair<LocalDate, DailyHabitEntry?>> {
        val today = LocalDate.now()
        val from = today.minusDays((days - 1).toLong())
        val entries = habitTrackingDao.getEntriesForHabitInRange(userId, habitId, from, today)
        val entryByDate = entries.associateBy { it.date }
        return (0 until days).map { offset ->
            val date = today.minusDays(offset.toLong())
            date to entryByDate[date]
        }
    }

    fun getEntriesForUserInRange(userId: Long, from: LocalDate, to: LocalDate): Flow<List<DailyHabitEntry>> {
        return habitTrackingDao.getEntriesForUserInRange(userId, from, to)
    }

    suspend fun saveDailyHabitEntry(
        userId: Long,
        date: LocalDate,
        habitId: Long,
        type: HabitType,
        booleanValue: Boolean? = null,
        numericValue: Double? = null,
        textValue: String? = null
    ) {
        val hasValue = when (type) {
            HabitType.BOOLEAN -> booleanValue == true
            HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> numericValue != null
            HabitType.TIME -> numericValue != null
            HabitType.TEXT -> !textValue.isNullOrBlank()
            HabitType.ROUTINE -> booleanValue == true
        }
        if (!hasValue) {
            habitTrackingDao.deleteDailyEntry(userId, habitId, date)
            updateWidgetState()
            return
        }
        habitTrackingDao.upsertDailyEntry(
            DailyHabitEntry(
                userId = userId,
                habitId = habitId,
                date = date,
                booleanValue = if (type == HabitType.BOOLEAN || type == HabitType.ROUTINE) booleanValue else null,
                numericValue = if (
                    type == HabitType.BOOLEAN || type == HabitType.TEXT || type == HabitType.ROUTINE
                ) null else numericValue,
                textValue = if (type == HabitType.TEXT) textValue?.trim() else null
            )
        )
        updateWidgetState()
    }

    suspend fun completeRoutineLinkedHabits(routineId: Long, date: LocalDate = LocalDate.now()) {
        val activeUser = getResolvedActiveUser().firstOrNull() ?: return
        val linkedHabits = habitTrackingDao.getDailyHabitItems(activeUser.id, date).first()
            .filter { it.type == HabitType.ROUTINE && it.routineId == routineId }

        linkedHabits.forEach { habit ->
            saveDailyHabitEntry(
                userId = activeUser.id,
                date = date,
                habitId = habit.habitId,
                type = HabitType.ROUTINE,
                booleanValue = true
            )
        }
    }

    /** Snap current wall-clock time and log it against a TIME habit immediately (used by widget). */
    suspend fun logTimeHabitNow(userId: Long, habitId: Long) {
        val habit = habitTrackingDao.getHabitById(habitId) ?: return
        if (habit.type != HabitType.TIME) return
        saveDailyHabitEntry(
            userId = userId,
            date = LocalDate.now(),
            habitId = habitId,
            type = HabitType.TIME,
            numericValue = LocalTime.now().minutesFromNoon()
        )
    }

    /** Convert a LocalTime to a monotonically increasing "minutes from noon" offset.
     *  Noon = 0, 10 PM = 600, midnight = 720, 6 AM = 1080.
     *  This ensures early-morning times (past midnight) are always > evening times. */
    fun LocalTime.minutesFromNoon(): Double {
        val minutesFromMidnight = hour * 60 + minute
        return ((minutesFromMidnight - 12 * 60 + 24 * 60) % (24 * 60)).toDouble()
    }

    suspend fun toggleBooleanHabit(userId: Long, habitId: Long, date: LocalDate) {
        val habit = habitTrackingDao.getHabitById(habitId) ?: return
        if (habit.type != HabitType.BOOLEAN) return

        val currentItem = habitTrackingDao.getDailyHabitItems(userId, date).first().firstOrNull { it.habitId == habitId }
        val nextValue = currentItem?.entryBooleanValue != true
        saveDailyHabitEntry(
            userId = userId,
            date = date,
            habitId = habitId,
            type = HabitType.BOOLEAN,
            booleanValue = nextValue
        )
    }

    suspend fun getHabitStreak(userId: Long, habitId: Long, habitType: HabitType, targetValue: Double?, operator: TargetOperator = TargetOperator.GREATER_THAN_OR_EQUAL): Int {
        val entries = habitTrackingDao.getEntriesForHabitDesc(userId, habitId)
        if (entries.isEmpty()) return 0

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val completedDates = entries.filter { entry ->
            when (habitType) {
                HabitType.BOOLEAN -> entry.booleanValue == true
                HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT, HabitType.POMODORO, HabitType.TIMER -> {
                    if (targetValue != null) {
                        (entry.numericValue ?: 0.0) >= targetValue
                    } else {
                        entry.numericValue != null
                    }
                }
                HabitType.TIME -> {
                    val logged = entry.numericValue ?: return@filter false
                    if (targetValue != null) {
                        when (operator) {
                            TargetOperator.LESS_THAN_OR_EQUAL -> logged <= targetValue
                            TargetOperator.GREATER_THAN_OR_EQUAL -> logged >= targetValue
                            TargetOperator.EQUAL -> logged == targetValue
                        }
                    } else {
                        true
                    }
                }
                HabitType.ROUTINE -> entry.booleanValue == true
                HabitType.TEXT -> !entry.textValue.isNullOrBlank()
            }
        }.map { it.date }.distinct()

        if (completedDates.isEmpty()) return 0
        if (!completedDates.first().isEqual(today) && !completedDates.first().isEqual(yesterday)) return 0

        var currentDate = if (completedDates.first().isEqual(today)) today else yesterday
        var streak = 0
        for (date in completedDates) {
            if (date.isEqual(currentDate)) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    suspend fun getPastDaysStats(days: Int): List<DailyHealthSummary> {
        val today = LocalDate.now()
        val startDate = today.minusDays((days - 1).toLong())
        val statsList = dailyHealthStatDao.getStatsForDateRange(startDate, today).first()
        val sessionsList = workoutSessionDao.getSessionsForDateRange(startDate, today).first()
        val summaryList = mutableListOf<DailyHealthSummary>()

        var date = startDate
        while (!date.isAfter(today)) {
            val stat = statsList.find { it.date == date }
            val completedSession = sessionsList.any { it.date == date && it.isCompleted }
            summaryList.add(
                DailyHealthSummary(
                    date = date,
                    sleep = stat?.sleepHours ?: 0f,
                    steps = stat?.stepsCount ?: 0,
                    routineCompleted = completedSession,
                    meditationCompleted = stat?.meditationCompleted ?: false
                )
            )
            date = date.plusDays(1)
        }
        return summaryList
    }

    data class DailyHealthSummary(
        val date: LocalDate,
        val sleep: Float,
        val steps: Int,
        val routineCompleted: Boolean,
        val meditationCompleted: Boolean
    )

    suspend fun seedHabitTrackingIfNeeded() {
        val existingUsers = userDao.getAllUsers().first()
        val defaultUserId = if (existingUsers.isEmpty()) {
            userDao.insertUser(UserProfile(name = "Primary User"))
        } else {
            existingUsers.first().id
        }

        if (activeUserId.first() == null) {
            userPreferencesRepository.saveActiveUserId(defaultUserId)
        }

        // Always ensure starter life areas exist so new users can assign immediately.
        val lifeAreaIdsByName = ensureStarterLifeAreas()
        val orderedLifeAreaIds = lifeAreaIdsByName.values.toList()
        ensureLifeAreaAssignmentsForUser(defaultUserId, orderedLifeAreaIds)

        val existingHabits = habitTrackingDao.getAllHabits().first()
        if (existingHabits.isNotEmpty()) return

        val createdHabitIds = listOf(
            HabitDefinition(
                name = "Sleep",
                goalIdentityStatement = "I protect my energy with quality sleep",
                description = "Hours slept last night",
                type = HabitType.DURATION,
                unit = "hrs",
                targetValue = 8.0,
                lifeAreaId = lifeAreaIdsByName["Health"],
                displayOrder = 0
            ),
            HabitDefinition(
                name = "Steps",
                goalIdentityStatement = "I stay active through the day",
                description = "Total steps walked today",
                type = HabitType.NUMBER,
                unit = "steps",
                targetValue = 6000.0,
                lifeAreaId = lifeAreaIdsByName["Health"],
                displayOrder = 1
            ),
            HabitDefinition(
                name = "Meditation",
                goalIdentityStatement = "I train calm focus daily",
                description = "Completed meditation practice",
                lifeAreaId = lifeAreaIdsByName["Mindset"],
                type = HabitType.BOOLEAN,
                displayOrder = 2
            )
        ).mapIndexed { index, habit ->
            val habitId = habitTrackingDao.insertHabit(habit)
            habitTrackingDao.upsertAssignment(UserHabitAssignment(defaultUserId, habitId, index))
            habitId
        }

        val existingStats = dailyHealthStatDao.getAllStats().first()
        existingStats.forEach { stat ->
            saveDailyHabitEntry(defaultUserId, stat.date, createdHabitIds[0], HabitType.DURATION, numericValue = stat.sleepHours.toDouble())
            saveDailyHabitEntry(defaultUserId, stat.date, createdHabitIds[1], HabitType.NUMBER, numericValue = stat.stepsCount.toDouble())
            saveDailyHabitEntry(defaultUserId, stat.date, createdHabitIds[2], HabitType.BOOLEAN, booleanValue = stat.meditationCompleted)
        }
    }

    private suspend fun ensureStarterLifeAreas(): LinkedHashMap<String, Long> {
        val starterAreas = listOf(
            LifeArea(name = "Health", description = "Sleep, movement, nutrition, and recovery", displayOrder = 0),
            LifeArea(name = "Learning", description = "Reading, study, and skill growth", displayOrder = 1),
            LifeArea(name = "Mindset", description = "Meditation, journaling, gratitude, and reflection", displayOrder = 2),
            LifeArea(name = "Work", description = "Deep work, focus blocks, and output", displayOrder = 3),
            LifeArea(name = "Family", description = "Parenting, partner time, and shared routines", displayOrder = 4)
        )

        val currentAreas = lifeAreaDao.getAllLifeAreas().first()
        val byName = LinkedHashMap<String, Long>()

        starterAreas.forEach { starter ->
            val existing = currentAreas.firstOrNull { it.name.equals(starter.name, ignoreCase = true) }
            val id = existing?.id ?: lifeAreaDao.insertLifeArea(starter)
            byName[starter.name] = id
        }

        return byName
    }

    private suspend fun ensureLifeAreaAssignmentsForUser(userId: Long, orderedLifeAreaIds: List<Long>) {
        if (orderedLifeAreaIds.isEmpty()) return
        val currentAssignments = lifeAreaDao.getAssignedLifeAreaIdsForUser(userId).first()
        if (currentAssignments.isNotEmpty()) return

        orderedLifeAreaIds.forEachIndexed { index, lifeAreaId ->
            lifeAreaDao.upsertLifeAreaAssignment(
                UserLifeAreaAssignment(
                    userId = userId,
                    lifeAreaId = lifeAreaId,
                    displayOrder = index
                )
            )
        }
    }

    suspend fun prepopulateRoutinesIfNeeded() {
        val currentRoutines = routineDao.getAllRoutines().first()
        if (currentRoutines.isNotEmpty()) return

        val routine1 = listOf(
            ExerciseEntry("Squats", 3, "12", "Main quad driver"),
            ExerciseEntry("Romanian Deadlift (bilateral)", 3, "8-10", "Hamstrings and hinge"),
            ExerciseEntry("Single-leg RDL", 2, "6 / leg", "Glute balance"),
            ExerciseEntry("Glute Bridge", 3, "12-15", "Pause at top"),
            ExerciseEntry("Heel Raises", 3, "15", "Slow"),
            ExerciseEntry("Side-lying Leg Raise", 2, "12 / side", "Hip stability"),
            ExerciseEntry("Tibialis Raises", 2, "15", "Ankle balance"),
            ExerciseEntry("Inverted Handstand", 1, "60 sec", ""),
            ExerciseEntry("Headstand", 1, "120 sec", ""),
            ExerciseEntry("Bhastrika Pranayama, Jalandhara Bandha", 3, "15 Count", ""),
            ExerciseEntry("Three Stage Pranayama", 1, "6 Count", "")
        )

        val routine2 = listOf(
            ExerciseEntry("Indian / Hindu Pushups", 3, "10-12", "Shoulder flow"),
            ExerciseEntry("Dips", 3, "6-8", "Triceps and chest"),
            ExerciseEntry("Pullups", 3, "6-8", "Vertical pull"),
            ExerciseEntry("Bent-over DB Row", 3, "8 / arm", "Horizontal pull"),
            ExerciseEntry("External Rotation (DB)", 2, "12", "Shoulder insurance"),
            ExerciseEntry("Deadhang", 2, "30 sec", "Decompression"),
            ExerciseEntry("Hand Grips (40 lb)", 2, "30 sec", "Grip"),
            ExerciseEntry("Wrist Extension (DB)", 2, "12-15", "Elbow balance"),
            ExerciseEntry("Inverted Handstand", 1, "60 sec", ""),
            ExerciseEntry("Headstand", 1, "120 sec", ""),
            ExerciseEntry("Bhastrika Pranayama, Jalandhara Bandha", 3, "15 Count", ""),
            ExerciseEntry("Three Stage Pranayama", 1, "6 Count", "")
        )

        val routine3 = listOf(
            ExerciseEntry("Hollow Hold (Naukasana)", 3, "30 sec", "Anterior core"),
            ExerciseEntry("Superman", 3, "20 sec", "Posterior chain"),
            ExerciseEntry("Indian Situps", 2, "12", "Controlled"),
            ExerciseEntry("Crawl", 3, "40-60 sec", "Integrated core"),
            ExerciseEntry("Single-leg Balance", 3, "30 sec / leg", "Proprioception"),
            ExerciseEntry("Butterfly Pose", 2, "60 sec", "Hip adductors"),
            ExerciseEntry("Cat and Camel", 2, "8-10 rounds", "Spine health"),
            ExerciseEntry("Inverted Handstand", 1, "60 sec", ""),
            ExerciseEntry("Headstand", 1, "120 sec", ""),
            ExerciseEntry("Bhastrika Pranayama, Jalandhara Bandha", 3, "15 Count", ""),
            ExerciseEntry("Three Stage Pranayama", 1, "6 Count", "")
        )

        val imageUri = "android.resource://com.example.habitpower/drawable/exercise_placeholder"

        suspend fun insertRoutineWithExercises(name: String, entries: List<ExerciseEntry>, description: String) {
            val routineId = routineDao.insertRoutine(Routine(name = name, description = description))
            entries.forEachIndexed { index, entry ->
                val exercise = Exercise(
                    name = entry.name,
                    description = entry.notes,
                    imageUri = imageUri,
                    targetDurationSeconds = if (entry.repsOrTime.contains("sec")) {
                        entry.repsOrTime.replace(Regex("[^0-9]"), "").toIntOrNull()
                    } else {
                        null
                    },
                    targetReps = if (!entry.repsOrTime.contains("sec")) {
                        entry.repsOrTime.replace(Regex("[^0-9].*"), "").filter { it.isDigit() }.toIntOrNull()
                    } else {
                        null
                    },
                    targetSets = entry.sets,
                    notes = "Target: ${entry.repsOrTime}. ${entry.notes}"
                )
                val exerciseId = exerciseDao.insertExercise(exercise)
                routineDao.insertRoutineExerciseCrossRef(RoutineExerciseCrossRef(routineId, exerciseId, index))
            }
        }

        insertRoutineWithExercises("Routine 1: Legs and Inversions", routine1, "Focus on lower body strength and breathing techniques.")
        insertRoutineWithExercises("Routine 2: Upper Body and Grip", routine2, "Strength training for chest, back, and shoulders with grip work.")
        insertRoutineWithExercises("Routine 3: Core and Mobility", routine3, "Core stability, mobility, and pranayama practice.")
    }

    private data class ExerciseEntry(
        val name: String,
        val sets: Int?,
        val repsOrTime: String,
        val notes: String
    )

    suspend fun applyStarterHabitStackForUser(userId: Long? = null): Int {
        val targetUserId = userId ?: getResolvedActiveUser().firstOrNull()?.id ?: return 0
        val lifeAreaIds = ensureStarterLifeAreas()

        val starterStack = listOf(
            StarterHabitSpec(
                name = "Sleep On Time",
                goalIdentityStatement = "I protect my recovery by respecting sleep timing",
                description = "Go to bed at or before your planned time",
                type = HabitType.BOOLEAN,
                lifeAreaName = "Health"
            ),
            StarterHabitSpec(
                name = "Move 20 Minutes",
                goalIdentityStatement = "I keep my body active every day",
                description = "Walk, mobility, or workout for at least 20 minutes",
                type = HabitType.DURATION,
                unit = "mins",
                targetValue = 20.0,
                lifeAreaName = "Health"
            ),
            StarterHabitSpec(
                name = "Deep Work Block",
                goalIdentityStatement = "I make focused progress on meaningful work",
                description = "One distraction-free work or study block",
                type = HabitType.POMODORO,
                unit = "sessions",
                targetValue = 1.0,
                lifeAreaName = "Work"
            ),
            StarterHabitSpec(
                name = "Read 10 Pages",
                goalIdentityStatement = "I learn a little every day",
                description = "Read at least 10 pages",
                type = HabitType.COUNT,
                unit = "pages",
                targetValue = 10.0,
                lifeAreaName = "Learning"
            ),
            StarterHabitSpec(
                name = "2-Min Reflection",
                goalIdentityStatement = "I reflect and reset daily",
                description = "Write one short reflection or gratitude note",
                type = HabitType.TEXT,
                lifeAreaName = "Mindset"
            ),
            StarterHabitSpec(
                name = "Family Check-In",
                goalIdentityStatement = "I stay present for my people",
                description = "Meaningful 10-minute family conversation",
                type = HabitType.BOOLEAN,
                lifeAreaName = "Family"
            )
        )

        val existingHabits = habitTrackingDao.getAllHabits().first()
        val assignedIds = habitTrackingDao.getAssignedHabitIdsForUser(targetUserId).first().toMutableSet()
        val currentMaxOrder = existingHabits.maxOfOrNull { it.displayOrder } ?: -1
        var nextOrder = currentMaxOrder + 1
        var createdCount = 0

        starterStack.forEach { spec ->
            val existing = existingHabits.firstOrNull { it.name.equals(spec.name, ignoreCase = true) }
            val habitId = if (existing != null) {
                existing.id
            } else {
                createdCount += 1
                habitTrackingDao.insertHabit(
                    HabitDefinition(
                        name = spec.name,
                        goalIdentityStatement = spec.goalIdentityStatement,
                        description = spec.description,
                        type = spec.type,
                        unit = spec.unit,
                        targetValue = spec.targetValue,
                        lifeAreaId = lifeAreaIds[spec.lifeAreaName],
                        displayOrder = nextOrder++,
                        showInDailyCheckIn = true,
                        operator = TargetOperator.GREATER_THAN_OR_EQUAL
                    )
                )
            }

            if (!assignedIds.contains(habitId)) {
                assignedIds.add(habitId)
            }
        }

        val orderedAssignedIds = habitTrackingDao
            .getAllHabits()
            .first()
            .filter { assignedIds.contains(it.id) }
            .sortedBy { it.displayOrder }
            .map { it.id }

        replaceAssignmentsForUser(targetUserId, orderedAssignedIds)
        ensureLifeAreaAssignmentsForUser(targetUserId, lifeAreaIds.values.toList())
        triggerRefresh()
        return createdCount
    }

    private data class StarterHabitSpec(
        val name: String,
        val goalIdentityStatement: String,
        val description: String,
        val type: HabitType,
        val unit: String? = null,
        val targetValue: Double? = null,
        val lifeAreaName: String
    )
}
