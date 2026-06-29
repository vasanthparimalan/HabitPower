package com.example.habitpower.data

import android.content.Context
import com.example.habitpower.data.dao.DailyHealthStatDao
import com.example.habitpower.data.dao.ChantDao
import com.example.habitpower.data.dao.TaskDao
import com.example.habitpower.data.dao.ExerciseDao
import com.example.habitpower.data.dao.HabitTrackingDao
import com.example.habitpower.data.dao.LifeAreaDao
import com.example.habitpower.data.dao.PomodoroSessionDao
import com.example.habitpower.data.dao.RoutineDao
import com.example.habitpower.data.dao.UserDao
import com.example.habitpower.data.dao.WorkoutSessionDao
import com.example.habitpower.data.model.DailyHabitEntry
import com.example.habitpower.data.dao.RoutineNotificationSettingsDao
import com.example.habitpower.data.model.PomodoroSession
import com.example.habitpower.data.model.DailyHabitItem
import com.example.habitpower.data.model.DailyHealthStat
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.ExerciseCategory
import com.example.habitpower.data.model.HabitDefinition
import com.example.habitpower.data.model.HabitLifecycleStatus
import com.example.habitpower.data.model.HabitRecurrenceType
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.data.model.LifeArea
import com.example.habitpower.data.model.Routine
import com.example.habitpower.data.model.Checklist
import com.example.habitpower.data.model.ChecklistItem
import com.example.habitpower.data.model.ChantDefinition
import com.example.habitpower.data.model.ChantSession
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import com.example.habitpower.data.model.RoutineExerciseCrossRef
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.data.model.RoutineNotificationSettings
import com.example.habitpower.data.model.UserHabitAssignment
import com.example.habitpower.data.model.TargetOperator
import com.example.habitpower.data.model.UserLifeAreaAssignment
import com.example.habitpower.data.model.UserProfile
import com.example.habitpower.data.model.WorkoutSession
import com.example.habitpower.reminder.HabitReminderScheduler
import com.example.habitpower.util.ExerciseImageSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val routineNotificationSettingsDao: RoutineNotificationSettingsDao,
    private val pomodoroSessionDao: PomodoroSessionDao,
    private val chantDao: ChantDao,
    private val meditationDao: com.example.habitpower.data.dao.MeditationDao,
    private val taskDao: TaskDao,
    private val database: HabitPowerDatabase
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
        val dailyIntention = userPreferencesRepository.getDailyIntention(today.toString()).first()
        // getWidgetHabitItems applies showInWidget + isScheduledOn(today) — correct filtering
        val allTodayHabits = getWidgetHabitItems(user.id, today).first()

        fun isDailyHabitCompleted(h: com.example.habitpower.data.model.DailyHabitItem): Boolean =
            when (h.type) {
                HabitType.BOOLEAN, HabitType.ROUTINE -> h.entryBooleanValue == true
                HabitType.NUMBER, HabitType.DURATION, HabitType.COUNT,
                HabitType.POMODORO, HabitType.TIMER, HabitType.TIME -> h.entryNumericValue != null
                HabitType.TEXT -> !h.entryTextValue.isNullOrBlank()
            }

        val completedCount = allTodayHabits.count { isDailyHabitCompleted(it) }
        val totalCount = allTodayHabits.size

        // Include all habits so Widget 2 can show the full checklist; Widget 1 filters to pending
        val allWidgetHabits = allTodayHabits.map { habit ->
            val completed = isDailyHabitCompleted(habit)
            val navigateTo = when (habit.type) {
                HabitType.TIMER, HabitType.POMODORO -> "focus"
                HabitType.ROUTINE -> "routines"
                else -> "daily_check_in"
            }
            WidgetHabit(
                habitId = habit.habitId,
                name = habit.name,
                streak = getHabitStreak(user.id, habit.habitId, habit.type, habit.targetValue, habit.operator),
                navigateTo = navigateTo,
                isCompleted = completed,
                isBoolean = habit.type == HabitType.BOOLEAN
            )
        }

        val state = WidgetState(
            userName = user.name,
            habits = allWidgetHabits,
            completedCount = completedCount,
            totalCount = totalCount,
            dailyIntention = dailyIntention
        )

        context.saveWidgetState(state)
    }

    fun getWidgetState(): Flow<WidgetState> = context.getWidgetState()

    val allQuotes: Flow<List<com.example.habitpower.data.model.Quote>> = quoteDao.getAllQuotes()
    suspend fun insertQuote(quote: com.example.habitpower.data.model.Quote) = quoteDao.insertQuote(quote)
    suspend fun deleteQuote(quote: com.example.habitpower.data.model.Quote) = quoteDao.deleteQuote(quote)
    suspend fun seedQuotesIfNeeded() {
        data class SeedQuote(val text: String, val source: String, val sourceUrl: String)

        val atomicHabitsUrl = "https://www.audible.com/search?keywords=Atomic+Habits+James+Clear"
        val gritUrl = "https://www.audible.com/search?keywords=Grit+Angela+Duckworth"
        val powerOfHabitUrl = "https://www.audible.com/search?keywords=The+Power+of+Habit+Charles+Duhigg"
        val tinyHabitsUrl = "https://www.audible.com/search?keywords=Tiny+Habits+BJ+Fogg"

        val defaults = listOf(
            // ── Atomic Habits — James Clear ──────────────────────────────────
            SeedQuote("Make the cue obvious: redesign your environment so the right action is the easy action.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("Make it attractive: pair a hard habit with something you already enjoy — temptation bundling works.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("Make it easy: shrink the habit until starting takes less than two minutes. The act of starting is the habit.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("Make it satisfying: immediate reward after a habit trains your brain to want it again tomorrow.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("Every action is a vote for the person you are becoming. Enough votes change the identity.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("You do not rise to the level of your goals. You fall to the level of your systems.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("Never miss twice. One missed day is an accident. Two missed days is the start of a new habit.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("The plateau of latent potential: results feel slow — until the day they suddenly are not.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("Habit stacking: after I do X, I will do Y. Anchor new habits to existing ones.", "Atomic Habits — James Clear", atomicHabitsUrl),
            SeedQuote("The environment is the invisible hand that shapes behaviour. Design it deliberately.", "Atomic Habits — James Clear", atomicHabitsUrl),

            // ── Grit — Angela Duckworth ───────────────────────────────────────
            SeedQuote("Talent × effort = skill. Skill × effort = achievement. Effort counts twice.", "Grit — Angela Duckworth", gritUrl),
            SeedQuote("Passion and perseverance for long-term goals outweigh raw talent nearly every time.", "Grit — Angela Duckworth", gritUrl),
            SeedQuote("Grit grows when work is interesting, practice is deliberate, and effort connects to purpose.", "Grit — Angela Duckworth", gritUrl),
            SeedQuote("A growth mindset — believing abilities can be developed — is the foundation that grit is built on.", "Grit — Angela Duckworth", gritUrl),
            SeedQuote("Enthusiasm is common. Endurance is rare. The long game is where character is built.", "Grit — Angela Duckworth", gritUrl),
            SeedQuote("Deliberate practice is not fun. It is focused, purposeful repetition with immediate feedback.", "Grit — Angela Duckworth", gritUrl),

            // ── The Power of Habit — Charles Duhigg ──────────────────────────
            SeedQuote("Habits follow a loop: cue triggers routine, routine delivers reward. Change the routine; keep the loop.", "The Power of Habit — Charles Duhigg", powerOfHabitUrl),
            SeedQuote("Keystone habits — a few routines that trigger progress in many areas — are worth protecting first.", "The Power of Habit — Charles Duhigg", powerOfHabitUrl),
            SeedQuote("Believing change is possible is itself what makes change possible.", "The Power of Habit — Charles Duhigg", powerOfHabitUrl),
            SeedQuote("When a craving appears, decide your routine before the moment arrives. Plans survive pressure.", "The Power of Habit — Charles Duhigg", powerOfHabitUrl),
            SeedQuote("The brain cannot distinguish between a good habit and a bad one. Only you can make that call.", "The Power of Habit — Charles Duhigg", powerOfHabitUrl),

            // ── Tiny Habits — BJ Fogg ─────────────────────────────────────────
            SeedQuote("Anchor a tiny habit to something you already do, then celebrate immediately after. Emotion creates habit.", "Tiny Habits — BJ Fogg", tinyHabitsUrl),
            SeedQuote("Tiny actions done consistently beat intense actions done occasionally — every time.", "Tiny Habits — BJ Fogg", tinyHabitsUrl),
            SeedQuote("Motivation is unreliable. Design for the version of yourself who is tired and busy.", "Tiny Habits — BJ Fogg", tinyHabitsUrl),
            SeedQuote("Reduce friction for habits you want. Add friction for habits you don't. Environment is leverage.", "Tiny Habits — BJ Fogg", tinyHabitsUrl)
        )

        val existing = quoteDao.getAllQuotesSync().map { it.text.trim() }.toSet()
        defaults.filterNot { it.text in existing }.forEach { q ->
            quoteDao.insertQuote(
                com.example.habitpower.data.model.Quote(text = q.text, source = q.source, sourceUrl = q.sourceUrl)
            )
        }
    }
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)

    /**
     * Seeds the exercises table from the bundled library on first launch (and picks up
     * any new exercises added to the library in future app updates).
     * Idempotent — skips names already present, so safe to call on every launch.
     */
    suspend fun seedExercisesIfNeeded(libraryItems: List<com.example.habitpower.data.model.ExerciseLibraryItem>) {
        val existingByName = exerciseDao.getAllExercisesSync()
            .associateBy { it.name.trim().lowercase() }

        libraryItems.forEach { item ->
            val key = item.name.trim().lowercase()
            val existing = existingByName[key]
            if (existing == null) {
                exerciseDao.insertExercise(
                    Exercise(
                        name = item.name,
                        description = item.primaryMuscle ?: "",
                        imageUri = item.imageUri,
                        notes = null,
                        instructions = item.instructions,
                        tags = item.category.name.lowercase(),
                        category = item.category,
                        wgerExerciseId = item.wgerExerciseId
                    )
                )
            } else {
                val merged = existing.copy(
                    description = existing.description.ifBlank { item.primaryMuscle.orEmpty() },
                    imageUri = item.imageUri ?: existing.imageUri,
                    instructions = existing.instructions?.takeIf { it.isNotBlank() } ?: item.instructions,
                    tags = existing.tags.ifBlank { item.category.name.lowercase() },
                    wgerExerciseId = existing.wgerExerciseId ?: item.wgerExerciseId
                )
                if (merged != existing) {
                    exerciseDao.updateExercise(merged)
                }
            }
        }
    }
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)

    suspend fun seedRoutinesIfNeeded() {
        val existingNames = routineDao.getAllRoutinesSync().map { it.name.trim().lowercase() }.toSet()
        val exercisesByName = exerciseDao.getAllExercisesSync()
            .associateBy { it.name.trim().lowercase() }

        fun ex(name: String) = exercisesByName[name.trim().lowercase()]

        data class SeedExercise(
            val name: String,
            val sets: Int? = null,
            val reps: Int? = null,
            val durationSec: Int? = null
        )
        data class SeedRoutine(
            val name: String,
            val description: String,
            val type: RoutineType = RoutineType.TIMED,
            val restTimeSeconds: Int = 0,
            val exercises: List<SeedExercise>
        )

        val seeds = listOf(
            SeedRoutine(
                name = "Morning Warrior",
                description = "Full-body wake-up to prime your energy for the day.",
                type = RoutineType.TIMED,
                restTimeSeconds = 20,
                exercises = listOf(
                    SeedExercise("Jumping Jacks", durationSec = 60),
                    SeedExercise("Push-Ups", sets = 3, reps = 12),
                    SeedExercise("Squats", sets = 3, reps = 15),
                    SeedExercise("Mountain climbers", durationSec = 45),
                    SeedExercise("Plank", durationSec = 60)
                )
            ),
            SeedRoutine(
                name = "Surya Flow",
                description = "Sun salutation sequence to open body and mind.",
                type = RoutineType.NORMAL,
                restTimeSeconds = 0,
                exercises = listOf(
                    SeedExercise("Sun Salutation A", reps = 5),
                    SeedExercise("Warrior I", durationSec = 60),
                    SeedExercise("Warrior II", durationSec = 60),
                    SeedExercise("Downward-Facing Dog", durationSec = 60),
                    SeedExercise("Child's Pose Stretch", durationSec = 60)
                )
            ),
            SeedRoutine(
                name = "Desk Break",
                description = "5-minute office reset — no equipment, minimal space.",
                type = RoutineType.TIMED,
                restTimeSeconds = 0,
                exercises = listOf(
                    SeedExercise("Neck Rotation Stretch", durationSec = 30),
                    SeedExercise("Shoulder Cross-Body Stretch", durationSec = 30),
                    SeedExercise("Standing Side Stretch", durationSec = 30),
                    SeedExercise("Wrist Flexor Stretch", durationSec = 30),
                    SeedExercise("Squats", sets = 1, reps = 10)
                )
            ),
            SeedRoutine(
                name = "HIIT Express",
                description = "Maximum intensity in minimum time. Repeat 3× for a full burn.",
                type = RoutineType.TIMED,
                restTimeSeconds = 15,
                exercises = listOf(
                    SeedExercise("Burpee", durationSec = 20),
                    SeedExercise("High knees", durationSec = 30),
                    SeedExercise("Mountain climbers", durationSec = 30),
                    SeedExercise("Squat Jump", durationSec = 20),
                    SeedExercise("Jump Rope", durationSec = 30)
                )
            ),
            SeedRoutine(
                name = "Evening Wind-Down",
                description = "Quiet the nervous system and prepare the body for deep sleep.",
                type = RoutineType.NORMAL,
                restTimeSeconds = 0,
                exercises = listOf(
                    SeedExercise("Cat-Cow Stretch", reps = 10),
                    SeedExercise("Pigeon Pose", durationSec = 60),
                    SeedExercise("Seated Forward Fold", durationSec = 60),
                    SeedExercise("Child's Pose Stretch", durationSec = 90),
                    SeedExercise("Supine Hip Abduction", durationSec = 30)
                )
            )
        )

        for (seed in seeds) {
            if (seed.name.trim().lowercase() in existingNames) continue
            val routineId = routineDao.insertRoutine(
                Routine(
                    name = seed.name,
                    description = seed.description,
                    type = seed.type,
                    restTimeSeconds = seed.restTimeSeconds
                )
            )
            seed.exercises.forEachIndexed { index, entry ->
                val exercise = ex(entry.name) ?: return@forEachIndexed
                routineDao.insertRoutineExerciseCrossRef(
                    RoutineExerciseCrossRef(
                        routineId = routineId,
                        exerciseId = exercise.id,
                        order = index,
                        sets = entry.sets,
                        reps = entry.reps,
                        durationSeconds = entry.durationSec
                    )
                )
            }
        }
    }

    fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()
    suspend fun getRoutineById(id: Long): Routine? = routineDao.getRoutineById(id)
    suspend fun insertRoutine(routine: Routine): Long = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutine(routine: Routine) = routineDao.deleteRoutine(routine)

    fun getExercisesForRoutine(routineId: Long): Flow<List<Exercise>> = routineDao.getExercisesForRoutine(routineId)
    fun getRoutineExercisesWithDetails(routineId: Long): Flow<List<com.example.habitpower.data.model.RoutineExerciseWithDetails>> = routineDao.getRoutineExercisesWithDetails(routineId)
    fun getExerciseCountForRoutine(routineId: Long): Flow<Int> = routineDao.getExerciseCountForRoutine(routineId)
    suspend fun addExerciseToRoutine(
        routineId: Long, exerciseId: Long, order: Int,
        sets: Int? = null, reps: Int? = null, durationSeconds: Int? = null
    ) {
        routineDao.insertRoutineExerciseCrossRef(
            RoutineExerciseCrossRef(routineId = routineId, exerciseId = exerciseId, order = order, sets = sets, reps = reps, durationSeconds = durationSeconds)
        )
    }

    suspend fun clearRoutineExercises(routineId: Long) {
        routineDao.deleteRoutineExercises(routineId)
    }

    // Routine Notification Settings
    fun getRoutineNotificationSettings(): Flow<RoutineNotificationSettings?> =
        routineNotificationSettingsDao.getSettings()

    suspend fun updateRoutineNotificationSettings(settings: RoutineNotificationSettings) =
        routineNotificationSettingsDao.updateSettings(settings)

    fun getCompletionSoundEnabled(): Flow<Boolean> = userPreferencesRepository.soundEnabled
    fun getCompletionSoundId(): Flow<String> = userPreferencesRepository.notificationSoundId
    fun getCompletionVibrationEnabled(): Flow<Boolean> = userPreferencesRepository.completionVibrationEnabled

    fun getRoutineStartSoundEnabled(): Flow<Boolean> = userPreferencesRepository.routineStartSoundEnabled
    fun getRoutineStartSoundId(): Flow<String> = userPreferencesRepository.routineStartSoundId
    fun getRoutineEndSoundEnabled(): Flow<Boolean> = userPreferencesRepository.routineEndSoundEnabled
    fun getRoutineEndSoundId(): Flow<String> = userPreferencesRepository.routineEndSoundId
    fun getRoutineTtsEnabled(): Flow<Boolean> = userPreferencesRepository.routineTtsEnabled
    suspend fun saveRoutineTtsEnabled(enabled: Boolean) = userPreferencesRepository.saveRoutineTtsEnabled(enabled)

    fun getStepBackActive(): Flow<Boolean> = userPreferencesRepository.stepBackActive
    fun getStepBackReturnEpochDay(): Flow<Long?> = userPreferencesRepository.stepBackReturnEpochDay

    fun getSessionsForDate(date: LocalDate): Flow<List<WorkoutSession>> = workoutSessionDao.getSessionsForDate(date)
    fun getSessionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutSession>> =
        workoutSessionDao.getSessionsForDateRange(startDate, endDate)

    suspend fun insertSession(session: WorkoutSession) = workoutSessionDao.insertSession(session)
    suspend fun deleteWorkoutSession(sessionId: Long) = workoutSessionDao.deleteSession(sessionId)

    fun getStatForDate(date: LocalDate): Flow<DailyHealthStat?> = dailyHealthStatDao.getStatForDate(date)
    fun getStatsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthStat>> =
        dailyHealthStatDao.getStatsForDateRange(startDate, endDate)

    suspend fun saveDailyStat(stat: DailyHealthStat) = dailyHealthStatDao.insertOrUpdate(stat)

    suspend fun importBulkEntries(entries: List<com.example.habitpower.data.model.DailyHabitEntry>) {
        if (entries.isEmpty()) return
        withContext(Dispatchers.IO) {
            entries.chunked(500).forEach { chunk -> habitTrackingDao.insertAllEntries(chunk) }
        }
        updateWidgetState()
    }

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
    fun getGraduatedHabitsForUser(userId: Long): Flow<List<HabitDefinition>> = habitTrackingDao.getGraduatedHabitsForUser(userId)

    suspend fun updateHabit(habit: HabitDefinition) {
        habitTrackingDao.updateHabit(habit)
        syncHabitReminders()
        updateWidgetState()
    }

    /**
     * Changes a habit's lifecycle status. Also keeps [HabitDefinition.isActive] in sync
     * so all existing DAO queries that filter `isActive = 1` continue to work correctly.
     * PAUSED / RETIRED / GRADUATED habits are excluded from daily tracking, reminders,
     * and streak calculations — but their full history is preserved.
     */
    suspend fun setHabitLifecycle(habit: HabitDefinition, status: com.example.habitpower.data.model.HabitLifecycleStatus) {
        val updated = habit.copy(
            lifecycleStatus = status,
            isActive = status.isTracked
        )
        habitTrackingDao.updateHabit(updated)
        syncHabitReminders()
        updateWidgetState()
    }

    // ── On Hold ───────────────────────────────────────────────────────────────────

    /**
     * Puts a habit on hold from today until [untilDate] (inclusive).
     * If [untilDate] is null the hold has no scheduled end — the user must resume manually.
     *
     * Reuses the existing PAUSED lifecycle (which already sets isActive=false, silences
     * reminders, and hides the habit from every DAO query that filters isActive=1).
     * The [pausedUntil] field distinguishes a time-bound hold from an indefinite pause
     * so the dashboard can show a dedicated "On Hold" section with return dates.
     */
    suspend fun putHabitOnHold(habit: HabitDefinition, untilDate: LocalDate?) {
        val updated = habit.copy(
            lifecycleStatus = HabitLifecycleStatus.PAUSED,
            isActive = false,
            pausedUntil = untilDate
        )
        habitTrackingDao.updateHabit(updated)
        syncHabitReminders()
        updateWidgetState()
    }

    /**
     * Manually resumes a habit that is currently on hold before its scheduled end date.
     * Also clears [pausedUntil] so it no longer appears in the On Hold section.
     */
    suspend fun resumeHabitFromHold(habit: HabitDefinition) {
        val updated = habit.copy(
            lifecycleStatus = HabitLifecycleStatus.ACTIVE,
            isActive = true,
            pausedUntil = null
        )
        habitTrackingDao.updateHabit(updated)
        syncHabitReminders()
        updateWidgetState()
    }

    /**
     * Called once on app launch. Auto-resumes all time-bound holds whose end date
     * is strictly before today — i.e. the habit resumes the morning after the hold ends.
     * After the bulk UPDATE, reminders are re-synced so notifications fire again.
     */
    suspend fun autoResumeExpiredHolds() {
        habitTrackingDao.autoResumeExpiredHolds(LocalDate.now())
        syncHabitReminders()
        updateWidgetState()
    }

    fun getOnHoldHabitsForUser(userId: Long) = habitTrackingDao.getOnHoldHabitsForUser(userId)

    suspend fun getHabitById(habitId: Long): HabitDefinition? = habitTrackingDao.getHabitById(habitId)

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
            if (habit.id in assignedHabitIds && habit.lifecycleStatus.isTracked) {
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
                .sortedWith(compareBy(
                    { if (it.commitmentMinutes != null) 0 else 1 },
                    { it.commitmentMinutes ?: 0 },
                    { it.effectiveDisplayOrder }
                ))
        }

    fun getFocusHabitItems(userId: Long, date: LocalDate): Flow<List<DailyHabitItem>> =
        observeRefresh().flatMapLatest {
            habitTrackingDao.getDailyHabitItems(userId, date).map { items ->
                items
                    .filter { it.isScheduledOn(date) }
                    .sortedWith(compareBy(
                        { if (it.commitmentMinutes != null) 0 else 1 },
                        { it.commitmentMinutes ?: 0 },
                        { it.effectiveDisplayOrder }
                    ))
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
                    .sortedWith(compareBy(
                        { if (it.commitmentMinutes != null) 0 else 1 },
                        { it.commitmentMinutes ?: 0 },
                        { it.effectiveDisplayOrder }
                    ))
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

    suspend fun getAllEntriesForUser(userId: Long): List<DailyHabitEntry> =
        habitTrackingDao.getAllEntriesForUser(userId)

    fun getAllHealthStats(): Flow<List<DailyHealthStat>> = dailyHealthStatDao.getAllStats()

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

    suspend fun updateEntryQuality(userId: Long, habitId: Long, date: LocalDate, quality: Int) {
        habitTrackingDao.updateEntryQuality(userId, habitId, date, quality)
    }

    suspend fun toggleBooleanHabit(userId: Long, habitId: Long, date: LocalDate) {
        val habit = habitTrackingDao.getHabitById(habitId) ?: return
        if (habit.type != HabitType.BOOLEAN && habit.type != HabitType.ROUTINE) return

        val currentItem = habitTrackingDao.getDailyHabitItems(userId, date).first().firstOrNull { it.habitId == habitId }
        val nextValue = currentItem?.entryBooleanValue != true
        saveDailyHabitEntry(
            userId = userId,
            date = date,
            habitId = habitId,
            type = habit.type,
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

        // Returns the asset URI for a bundled exercise image if the file exists under
        // app/src/main/assets/exercises/, otherwise falls back to the vector placeholder.
        // Naming convention: lowercase, spaces → underscores, e.g. "Push-Up" → "push_up.webp"
        // Build a one-time lookup so we only store bundled image URIs that really exist.
        val exerciseAssetNames = runCatching {
            context.assets.list("exercises")
                ?.map { it.lowercase() }
                ?.toSet()
                ?: emptySet()
        }.getOrDefault(emptySet())

        fun exerciseAssetUri(exerciseName: String): String? {
            return ExerciseImageSupport.resolveBundledAssetUri(exerciseName, exerciseAssetNames)
        }

        suspend fun insertRoutineWithExercises(name: String, entries: List<ExerciseEntry>, description: String) {
            val routineId = routineDao.insertRoutine(Routine(name = name, description = description))
            entries.forEachIndexed { index, entry ->
                val durationSeconds = if (entry.repsOrTime.contains("sec")) {
                    entry.repsOrTime.replace(Regex("[^0-9]"), "").toIntOrNull()
                } else null
                val reps = if (!entry.repsOrTime.contains("sec")) {
                    entry.repsOrTime.replace(Regex("[^0-9].*"), "").filter { it.isDigit() }.toIntOrNull()
                } else null
                val exercise = Exercise(
                    name = entry.name,
                    description = entry.notes,
                    imageUri = exerciseAssetUri(entry.name),
                    notes = "Target: ${entry.repsOrTime}. ${entry.notes}"
                )
                val exerciseId = exerciseDao.insertExercise(exercise)
                routineDao.insertRoutineExerciseCrossRef(RoutineExerciseCrossRef(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    order = index,
                    sets = entry.sets,
                    reps = reps,
                    durationSeconds = durationSeconds
                ))
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

    suspend fun patchExerciseInstructionsAndSeedYoga() {
        val instructionsByName: Map<String, String> = mapOf(
            // ── Existing routine exercises ────────────────────────────────────────
            "Squats" to "Stand with feet shoulder-width apart and toes slightly out. Brace your core and keep your chest tall. Push your hips back and bend your knees, lowering until your thighs are at least parallel to the floor. Keep knees tracking over your toes throughout. Drive through your heels to stand back up. Squeeze your glutes at the top of each rep.",
            "Romanian Deadlift (bilateral)" to "Hold dumbbells or a barbell in front of your thighs with a hip-width stance. Maintain a slight bend in your knees throughout — this is not a stiff-leg deadlift. Push your hips back while keeping your back flat, lowering the weight along your legs until you feel a strong hamstring stretch. Drive your hips forward to return to standing. Keep the bar or dumbbells close to your body at all times.",
            "Single-leg RDL" to "Stand on one leg with a soft bend in the knee. Hold a dumbbell in the opposite hand. Hinge at the hip, extending the free leg straight behind you for balance, and lower the weight toward the floor. Keep your hips square — do not let the floating hip rise. Drive through the standing heel to return upright. Complete all reps on one side before switching.",
            "Glute Bridge" to "Lie on your back with knees bent and feet flat on the floor, hip-width apart. Arms rest at your sides, palms down. Press through your heels and squeeze your glutes to lift your hips until your body forms a straight line from shoulders to knees. Pause at the top for 2 seconds. Lower slowly and with control back to the starting position. Do not hyperextend your lower back.",
            "Heel Raises" to "Stand with feet hip-width apart, toes pointing forward. You may lightly hold a wall or pole for balance. Rise slowly onto the balls of your feet, lifting your heels as high as possible. Pause briefly at the top. Lower your heels slowly back to the floor. Keep the movement deliberate — 2 seconds up, 1-second pause, 2 seconds down. Perform on one leg to progress.",
            "Side-lying Leg Raise" to "Lie on your side with your body in a straight line from head to heels. Rest your lower arm under your head. Keep the top leg straight and foot flexed. Lift the top leg to about 45 degrees, pause briefly, then lower under control. Avoid rolling your hip backward — the movement comes only from hip abduction. Complete all reps on one side before switching.",
            "Tibialis Raises" to "Stand with your back against a wall and your heels 6–8 inches away from the baseboard. Keep legs straight. Lift the front of both feet as high as possible, pulling your toes toward your shins (dorsiflexion). Pause at the top, then lower slowly. Keep your heels in contact with the floor at all times. To progress, rest a weight plate on the top of your feet.",
            "Inverted Handstand" to "Place your hands shoulder-width apart on the floor, about 8–12 inches from a wall. Kick up one leg at a time until both feet rest against the wall. Lock your arms straight, squeeze your core, and align your body in a vertical line from hands to heels. Push actively through the floor with your palms. Gaze at the space between your hands. Come down carefully by bending one knee and stepping down one foot at a time.",
            "Headstand" to "Kneel and interlace your fingers, placing your forearms flat on the mat. Set the crown of your head on the mat, cradling the back of your head in your palms. Walk your feet in until your hips stack over your shoulders. Engage your core strongly. Lift one knee at a time until both legs rise overhead. Straighten your legs and hold. Distribute weight on your forearms — your head supports only lightly. Come down the same way you went up.",
            "Bhastrika Pranayama, Jalandhara Bandha" to "Sit in a comfortable cross-legged position with spine tall and eyes closed. Inhale and exhale forcefully through the nose at an equal pace — this is Bhastrika (bellows breath). Both the inhale and exhale are active. After completing the rounds, take a full inhale, hold the breath, and bring your chin firmly down to your chest (Jalandhara Bandha — throat lock). Hold the retention for your target count. Release the chin lock, then exhale slowly and completely.",
            "Three Stage Pranayama" to "Sit with spine tall and eyes closed. Take one continuous inhale in three stages: Stage 1 — breathe into the belly, feeling it expand outward. Stage 2 — continue the inhale into the ribcage, expanding it sideways. Stage 3 — fill the upper chest and collarbones. Exhale slowly in reverse order — chest drops, ribs draw in, belly pulls toward the spine. This is one complete cycle. Perform 6 slow, full cycles without pause between stages.",
            "Indian / Hindu Pushups" to "Start in a downward dog position — hips high, arms and legs straight, forming an inverted V. Lower your chest toward the floor in a forward sweeping arc, skimming close to the ground. Push your chest forward and up into a cobra position with arms straight. Reverse the arc back to downward dog to complete one rep. Keep the motion fluid and continuous, tracing a smooth figure-8 arc through space.",
            "Dips" to "Grip parallel bars with arms straight and shoulders above your hands. Lean slightly forward to emphasise the chest. Bend your elbows and lower your body in a controlled manner until your upper arms are roughly parallel to the floor. Press through your palms to push back up to full extension. Keep your core tight and shoulders depressed — do not shrug toward your ears throughout the movement.",
            "Pullups" to "Hang from a bar with an overhand (pronated) grip, hands slightly wider than shoulder width. Depress and retract your shoulder blades to initiate the pull. Pull your chest toward the bar by driving your elbows down and back. Pause briefly at the top. Lower yourself with full control to a complete dead hang before the next rep. Avoid swinging, kipping, or using momentum.",
            "Bent-over DB Row" to "Stand with feet hip-width apart and hinge at the hips to roughly 45 degrees, keeping your back flat and core braced. Hold a dumbbell in each hand with arms hanging straight down. Pull each weight toward your hip, leading with your elbow — not your hand. Squeeze your lat and rear delt at the top. Lower with control. Avoid rotating your torso or using momentum to lift.",
            "External Rotation (DB)" to "Lie on your side or sit with your elbow bent at 90 degrees and firmly tucked against your side. Hold a very light dumbbell. Keeping the elbow pinned and the forearm horizontal, rotate the forearm outward as far as your shoulder allows comfortably. Slowly return to the start position. This targets the infraspinatus and teres minor — use light weight and prioritise slow, precise control over load.",
            "Deadhang" to "Grip a pull-up bar with both hands, shoulder-width apart or slightly wider. Release your feet and hang with arms fully extended. Consciously let your shoulders rise toward your ears and then depress them — finding that active hang position that decompresses the spine. Breathe slowly. Hold for the target duration. To end safely, bend your knees and lower yourself to the floor in a controlled manner.",
            "Hand Grips (40 lb)" to "Hold a grip strengthener in one hand, spring facing away from you, thumb on top. Squeeze all four fingers fully, closing the gripper completely. Hold the closed position for a count, then open slowly under control — do not let it snap open. Perform all reps on one hand, then switch. Maintain a neutral wrist — do not flex the wrist to assist the grip. Progress by increasing resistance when you can complete all reps with control.",
            "Wrist Extension (DB)" to "Sit with your forearm resting along your thigh, wrist hanging off the edge of your knee, palm facing down. Hold a light dumbbell. Extend your wrist upward as far as possible. Hold briefly, then lower slowly. Keep the forearm completely still — only the wrist joint moves. This strengthens the wrist extensors to balance the wrist flexors engaged in most pulling and gripping work.",
            "Hollow Hold (Naukasana)" to "Lie on your back with arms extended overhead. Press your entire lower back flat into the floor — this is non-negotiable. Lift your shoulders, arms, and straight legs simultaneously off the floor, toes pointed. Your body forms a shallow curved 'boat' or 'banana' shape. Hold while breathing steadily — do not hold your breath. If your lower back lifts off the floor, bend your knees to reduce the lever arm until you build the core strength to extend.",
            "Superman" to "Lie face down with arms extended straight overhead. Simultaneously lift your arms, chest, and legs off the floor by squeezing your glutes and engaging your lower back and posterior chain. Hold the top position, then lower slowly and with control. Keep your neck neutral — look at the floor directly beneath you, not forward. Avoid jerking or using momentum; prioritise a slow, controlled extension.",
            "Indian Situps" to "Sit on the floor in a cross-legged position. Interlace your fingers and place them behind your head lightly — do not pull on your neck. Lie back until your shoulders touch the floor. Sit up while rotating one elbow to the opposite knee. Lower back down and repeat on the other side, alternating each rep. The cross-legged position engages more hip flexors and adductors compared to a standard crunch.",
            "Crawl" to "Begin on hands and knees with your knees hovering approximately one inch off the floor. Your back must remain flat like a table — no sagging hips or arching back. Move the right hand and left knee forward simultaneously a small step. Then move the left hand and right knee forward. Maintain a slow, deliberate pace — the challenge is keeping your hips level and your core completely braced throughout every step.",
            "Single-leg Balance" to "Stand on one foot with a very slight, natural bend in the standing knee. Fix your gaze on a stationary point at eye level. Let the other foot hover just off the floor. Hold without touching down for the target duration. To progress: close your eyes (removes visual stabilisation), or stand on a folded towel or balance pad. Switch sides after the hold. Build up duration gradually before adding instability.",
            "Butterfly Pose" to "Sit on the floor and bring the soles of your feet together in front of you, letting your knees fall outward toward the floor. Hold your feet or ankles with your hands. Sit tall with your spine upright — do not round your lower back. For a deeper inner-thigh stretch, gently press your knees down with your elbows. Hold and breathe slowly into your inner thighs and hips. Release tension with each exhale.",
            "Cat and Camel" to "Begin on hands and knees with a neutral spine — hips over knees, shoulders over wrists. On a slow exhale, round your entire spine toward the ceiling: tuck your tailbone, draw your navel inward, and release your head (Cat pose). On a slow inhale, let your belly drop toward the floor, lift your chest and tailbone toward the ceiling, and gaze slightly upward (Cow pose). Flow between the two positions in sync with your breath, moving slowly to mobilise each segment of the spine.",

            // ── Yoga Asanas ───────────────────────────────────────────────────────
            "Tadasana (Mountain Pose)" to "Stand with your feet together or hip-width apart. Spread your toes wide and press all four corners of each foot into the floor. Engage your thighs and lift your kneecaps slightly. Lengthen your spine, draw your shoulders down and back, and let your arms hang naturally at your sides with palms facing forward. Tuck your chin slightly and imagine a string pulling the crown of your head toward the ceiling. Breathe steadily and hold for 5–10 breaths. This is the foundation of all standing poses.",
            "Vrikshasana (Tree Pose)" to "Stand in Tadasana. Shift your weight onto your right foot. Bend your left knee and place the sole of your left foot on the inner right thigh (above or below the knee — never on the knee joint). Press your foot into your thigh and your thigh back into your foot equally. Bring your palms together at your chest, or raise your arms overhead. Fix your gaze on a still point. Hold for 5–8 breaths, then switch sides.",
            "Adho Mukha Svanasana (Downward Dog)" to "Begin on hands and knees. Tuck your toes and press through your palms to lift your hips up and back, forming an inverted V shape. Straighten your legs as much as possible — a bend in the knees is fine if your hamstrings are tight. Press your chest toward your thighs. Rotate your upper arms externally and spread your fingers wide, pressing through your knuckles. Hold for 5–10 breaths. Pedal your feet alternately to warm the calves and hamstrings.",
            "Bhujangasana (Cobra Pose)" to "Lie face down with your palms flat on the floor beneath your shoulders, elbows close to your body. Press the tops of your feet and your pubic bone into the floor. On an inhale, press your palms lightly into the floor and lift your chest, rolling your shoulders back and down. Keep a slight bend in your elbows — do not lock them straight. Gaze forward or slightly upward. Hold for 3–5 breaths, then lower on an exhale. Avoid squeezing your glutes; the lower back lift should come from your back muscles.",
            "Balasana (Child's Pose)" to "Kneel on the floor with your big toes touching and knees wide apart (or together for a more passive version). Sit back toward your heels as far as comfortable. Extend your arms forward on the mat with palms down, or rest them alongside your body. Let your forehead rest on the mat. Breathe into your lower back and hips, allowing them to soften with each exhale. This is a restorative pose — hold for 1–3 minutes to release tension.",
            "Trikonasana (Triangle Pose)" to "Stand with feet 3–4 feet apart. Turn your right foot 90 degrees out and your left foot slightly inward. Extend your arms out to the sides at shoulder height. On an exhale, reach your right hand toward your right shin, ankle, or the floor — whatever you can reach without rounding your spine. Extend your left arm straight up. Stack your shoulders and open your chest toward the ceiling. Gaze up at your left hand. Hold for 5 breaths, then switch sides.",
            "Virabhadrasana I (Warrior I)" to "Step your right foot forward into a lunge, bending the right knee to 90 degrees directly over the ankle. Your left leg is straight and the left heel is on the floor at a 45-degree angle. Square your hips toward the front as much as possible. Raise your arms overhead, palms facing each other or joined. Draw your tailbone down, lengthen your spine, and lift your chest. Hold for 5 breaths, then switch sides.",
            "Virabhadrasana II (Warrior II)" to "Stand with feet 3–4 feet apart, right foot turned 90 degrees out, left foot slightly in. Bend the right knee to 90 degrees over the right ankle without letting it collapse inward. Extend both arms out to the sides at shoulder height, parallel to the floor. Gaze over your right fingertips. Keep your torso directly over your pelvis — do not lean forward. Hold for 5 breaths, then switch sides.",
            "Paschimottanasana (Seated Forward Bend)" to "Sit on the floor with both legs extended straight ahead and feet flexed. Sit tall — if your lower back rounds immediately, sit on a folded blanket. On an inhale, lengthen your spine. On an exhale, hinge at your hips and fold forward, reaching for your shins, ankles, or feet. Lead with your chest rather than your forehead. Hold each exhale to deepen the stretch. Hold for 1–3 minutes. Never force the stretch by pulling hard on your feet.",
            "Ardha Matsyendrasana (Half Spinal Twist)" to "Sit with both legs extended. Bend your right knee and place your right foot flat on the floor outside your left knee. Keep your left leg extended or bend it so the left heel rests beside your right hip. On an inhale, lengthen your spine. On an exhale, twist your torso to the right, hooking your left elbow to the outside of your right knee. Place your right hand on the floor behind you. Gaze over your right shoulder. Hold for 5–8 breaths, then switch sides.",
            "Savasana (Corpse Pose)" to "Lie flat on your back with legs extended, feet falling naturally outward. Arms rest 6–8 inches from your body, palms facing up. Close your eyes. Consciously release tension from every body part, starting from your feet up to your face. Allow your body to become completely still and heavy. Focus on your natural breath without controlling it. Remain in this pose for 5–10 minutes. To exit, gently wiggle fingers and toes, roll to your right side, and slowly sit up.",
            "Padmasana (Lotus Pose)" to "Sit on the floor with legs extended. Bend your right knee and place your right foot on top of your left thigh, sole facing up, heel toward your navel. Bend your left knee and carefully place your left foot on top of your right thigh in the same manner. Rest your hands on your knees with palms up (chin mudra or simply open). Sit tall with your spine erect. If full Lotus is not accessible, practice Ardha Padmasana (Half Lotus) or Sukhasana (Easy Pose) and build hip mobility gradually.",
            "Ustrasana (Camel Pose)" to "Kneel with knees hip-width apart and thighs perpendicular to the floor. Place your hands on your hips. On an inhale, lengthen your spine and begin to arch backward, pressing your hips forward and lifting your chest. Reach your hands back to your heels one at a time, gripping firmly. Push your hips forward over your knees and let your head drop back if comfortable. Hold for 3–5 breaths. To exit, bring hands back to hips and slowly sit up. This is a strong backbend — warm up thoroughly beforehand.",
            "Dhanurasana (Bow Pose)" to "Lie face down with arms at your sides. Bend both knees and reach back to grasp your ankles on the outside. Flex your feet. On an inhale, kick your feet into your hands while simultaneously lifting your chest off the floor. Your body forms a bow shape — thighs, hips, and chest all lift. Breathe into your chest. Hold for 3–5 breaths, then release on an exhale. To deepen: try to bring feet and hands higher by kicking more actively.",
            "Halasana (Plow Pose)" to "Lie on your back. Swing your legs overhead until your toes touch the floor behind your head (or as far as they reach). Support your back with your hands or lay your arms flat on the floor. Keep your weight on your shoulders, not your neck. Lengthen the back of your neck — do not turn your head while in this pose. Breathe steadily. Hold for 5–10 breaths. To exit, slowly roll down vertebra by vertebra.",
            "Sarvangasana (Shoulder Stand)" to "Lie on your back and swing your legs overhead as in Plow Pose. Place your hands on your lower back for support and extend your legs straight up toward the ceiling. Your weight rests on the backs of your shoulders and upper arms — never on your neck. Keep your chin away from your chest. Engage your legs and press your heels toward the ceiling. Hold for 30 seconds to 2 minutes. Exit by bending your knees to your forehead and rolling down slowly.",
            "Surya Namaskar (Sun Salutation)" to "This is a flowing sequence of 12 linked poses. Begin in Tadasana (Mountain Pose). 1. Raise arms overhead (Urdhva Hastasana). 2. Forward Fold (Uttanasana). 3. Halfway lift (Ardha Uttanasana). 4. Step or jump back to Plank. 5. Lower to Chaturanga (low plank). 6. Upward Dog (Urdhva Mukha Svanasana). 7. Downward Dog (hold 3 breaths). 8–10. Step right foot forward, halfway lift, full forward fold. 11. Rise with arms overhead. 12. Return to Tadasana. Repeat on the opposite side for one complete round. Coordinate every movement with the breath.",
            "Viparita Karani (Legs Up the Wall)" to "Sit sideways close to a wall. Swing your legs up the wall as you lower your back to the floor. Scoot your hips as close to the wall as comfortable. Arms rest at your sides or on your belly. Close your eyes and breathe slowly. This gentle inversion reverses blood flow in the legs, calms the nervous system, and relieves tired or swollen legs. Remain for 5–15 minutes. To exit, bend your knees, roll to one side, and rest before sitting up.",
            "Anjaneyasana (Low Lunge)" to "From Downward Dog, step your right foot between your hands. Lower your left knee to the mat and untuck the left toes. Sink your hips forward and down toward the floor. On an inhale, raise your torso and extend both arms overhead, palms facing each other. Draw your tailbone down and lengthen your lower back. Press down through the top of your left foot and feel the stretch along the front of the left hip and thigh. Hold for 5 breaths, then switch sides.",
            "Utkata Konasana (Goddess Pose)" to "Stand with feet 3–4 feet apart, toes pointing outward at 45 degrees. Bend both knees deeply, tracking them directly over your toes. Bring your thighs as close to parallel with the floor as comfortable. Raise your arms to shoulder height and bend elbows to 90 degrees, palms forward (cactus arms), or bring hands to hips. Engage your core, keep your chest lifted and spine tall. Hold for 5–10 breaths, pulsing slightly to increase intensity.",

            // ── Stretches ─────────────────────────────────────────────────────────
            "Hip Flexor Stretch" to "Kneel on your left knee, right foot flat on the floor in front, right knee at 90 degrees. Place both hands on your right knee for balance. Shift your hips forward until you feel a stretch along the front of your left hip and thigh. Keep your torso upright — do not lean forward. For a deeper stretch, raise your left arm overhead and side-bend gently to the right. Hold for 30–45 seconds. Switch sides.",
            "Pigeon Pose" to "From Downward Dog, bring your right knee forward toward your right wrist and lower your right shin toward the floor at an angle (the right foot points toward the left). Extend your left leg straight behind you. Lower your hips toward the mat — use a folded blanket under your right hip if the floor is not accessible. Walk your hands forward and lower your chest over your right shin. Hold for 1–2 minutes, breathing into the outer right hip. Switch sides.",
            "Standing Hamstring Stretch" to "Stand with feet hip-width apart. Step your right foot forward about 12 inches. Flex your right foot (heel down, toes up). With a flat back, hinge at the hip and lean forward, resting both hands on your left thigh for support. Keep your right leg straight and avoid rounding your lower back. Hold for 30 seconds, feeling the stretch along the back of the right thigh. Repeat on the left side.",
            "Standing Quad Stretch" to "Stand tall and shift your weight onto your left leg, using a wall for balance if needed. Bend your right knee, bringing your right heel toward your glutes. Hold your right ankle with your right hand. Press your bent knee down toward the floor and stand tall — do not lean forward. Feel the stretch along the front of your right thigh. Hold for 30 seconds, then switch sides.",
            "Chest Opener Stretch" to "Stand or sit tall. Interlace your fingers behind your back. Squeeze your shoulder blades together, straighten your arms, and gently lift your hands away from your body. Open your chest and let your collarbones broaden. Tilt your chin slightly upward if comfortable. Hold for 20–30 seconds. Breathe into your chest. Release and repeat. This counteracts the hunched posture from sitting and screen use.",
            "Thoracic Spine Stretch" to "Sit in a chair or kneel. Place both hands behind your head and gently arch backward over the top of the chair back (or a foam roller placed under your mid-back on the floor). Hold the arched position for 3–5 seconds, then return to neutral. Move to the next thoracic segment (a few centimetres up or down) and repeat. This targets thoracic extension which is commonly restricted from prolonged sitting.",
            "Neck Side Stretch" to "Sit or stand tall with shoulders relaxed. Drop your right ear toward your right shoulder without raising your shoulder. Place your right hand gently on top of your head to add light overpressure — do not pull. Hold for 20–30 seconds, feeling the stretch along the left side of your neck. Release and repeat on the left side. Also perform a gentle chin-tuck (draw chin straight back) before the stretch to position the cervical spine optimally.",
            "Cross-body Shoulder Stretch" to "Stand or sit tall. Bring your right arm straight across your chest at shoulder height. Hook your left arm under or over the right arm and draw it closer to your chest. Hold for 20–30 seconds, feeling the stretch in the posterior shoulder (rear deltoid). Keep your right shoulder down — do not let it rise toward your ear. Repeat on the other side.",
            "IT Band Stretch" to "Stand near a wall for balance. Cross your right leg behind your left leg. Lean your torso to the left while pushing your right hip out to the right. You should feel a stretch along the outer right thigh and hip (the IT band and TFL). Hold for 30 seconds. Switch sides. Alternatively, lie on your back, cross your right knee over your left thigh, and let it drop toward the floor.",
            "Calf Stretch" to "Stand facing a wall with both hands resting on it. Step your right foot back 2–3 feet, keeping the right heel flat on the floor and the right leg straight. Bend your left knee slightly and lean forward — feel the stretch in your right calf (gastrocnemius). For the deeper calf muscle (soleus), slightly bend the back knee while keeping the heel down. Hold each position for 30 seconds. Switch sides.",
            "Figure-4 Stretch (Piriformis)" to "Lie on your back with both knees bent, feet flat on the floor. Cross your right ankle over your left knee, forming a figure-4 shape. Flex your right foot to protect the knee. Either stay here or clasp your hands behind your left thigh and draw both legs toward your chest. Hold for 30–60 seconds, breathing into the outer right hip and glute. Switch sides. This is one of the best stretches for the piriformis and hip rotators.",
            "Doorway Chest Stretch" to "Stand in a doorway and place both forearms on the door frame at 90-degree angles, elbows at shoulder height. Step forward with one foot until you feel a stretch across your chest and front shoulders. Do not lean your head forward — keep it neutral. Hold for 20–30 seconds. You can vary the stretch by raising your arms higher on the frame. This opens the pectoral muscles tightened by forward-slumped posture.",
            "Overhead Tricep Stretch" to "Stand or sit tall. Raise your right arm overhead, then bend your elbow, dropping your right hand toward the centre of your upper back. Use your left hand to gently press on the right elbow, pushing it further behind your head. Hold for 20–30 seconds, feeling the stretch in the back of the right arm (tricep and lat). Keep your spine tall — do not let your lower back arch. Switch sides.",
            "Seated Spinal Twist" to "Sit on the floor with both legs extended. Bend your right knee and place your right foot on the floor beside your left knee. On an inhale, lengthen your spine. On an exhale, rotate your torso to the right, placing your left hand on the right knee and your right hand on the floor behind you. Hold for 5–8 breaths, lengthening on each inhale and deepening the twist on each exhale. Switch sides.",

            // ── New exercises from Routine A & B ────────────────────────────────
            "Push-Up" to "Start in a high plank with hands slightly wider than shoulder-width and body in a straight line from head to heels. Lower your chest to just above the floor, bending your elbows at roughly 45 degrees to your body. Keep your core tight and hips level — do not sag or pike. Press through your palms to return to the starting position, fully extending your arms without locking the elbows. To scale down, perform from your knees.",
            "Pike Push-Up" to "Start in a downward dog position — hips high, body forming an inverted V with arms and legs straight. Shift your weight slightly forward over your hands. Bend your elbows and lower the crown of your head toward the floor between your hands. Press back up through your palms to the starting position. Keep your hips high throughout — do not let them drop as you lower. This targets the shoulders far more than a standard push-up.",
            "Diamond Push-Up" to "Start in a high plank and bring both hands together directly under your chest, forming a diamond (or triangle) shape with your thumbs and index fingers. Lower your chest toward your hands, keeping your elbows close to your sides. Press back to full extension. The narrow grip heavily targets the triceps and inner chest. If too difficult, perform from your knees.",
            "DB Overhead Press" to "Stand or sit with a dumbbell in each hand at shoulder height, palms facing forward. Brace your core and keep your lower back neutral — do not arch. Press both dumbbells straight overhead until your arms are fully extended. Lower slowly back to shoulder height under control. Avoid using your legs or leaning back to assist the press. Perform seated for stricter shoulder isolation.",
            "Indian Club Full Rotation" to "Hold an Indian club in one hand with a firm grip, arm hanging at your side. Initiate a smooth swing forward and upward, letting the club arc overhead and continue in a full circle back to the starting position. Keep the motion fluid — the wrist and forearm rotate naturally through the arc. Perform equal circles in the forward and reverse directions on both arms. Start slowly to learn the path before building speed.",
            "Wrist Curl (DB)" to "Sit with your forearm resting on your thigh, wrist and hand hanging off the edge of your knee, palm facing up. Hold a light dumbbell. Curl your wrist upward as far as possible, contracting the forearm flexors fully. Lower slowly back to the starting position. Keep the forearm completely still — only the wrist joint moves. Perform for both arms equally. This strengthens the wrist flexors and forearm musculature.",
            "Bulgarian Split Squat" to "Stand about 2 feet in front of a bench or elevated surface (knee height). Place one foot behind you on the bench, laces down. With your front foot flat on the floor and positioned so your shin is vertical at the bottom, lower your back knee toward the ground by bending the front knee to 90 degrees. Keep your torso upright and front knee tracking over your toes — do not let it cave inward. Drive through your front heel to return to standing. Complete all reps on one leg before switching.",
            "SL Glute Bridge" to "Lie on your back with both knees bent and feet flat on the floor, hip-width apart. Extend one leg straight, keeping both thighs parallel. Press through the heel of the bent leg and squeeze your glute to lift your hips until your body forms a straight line from shoulders to knee. Pause for 2 seconds at the top. Lower with control. Complete all reps on one side before switching. The single-leg version dramatically increases glute activation compared to the bilateral version.",
            "SL Calf Raise" to "Stand on one foot on the edge of a step (or flat floor), holding a wall lightly for balance. Keep the other foot raised. Rise onto the ball of your standing foot as high as possible. Pause briefly at the top. If on a step, lower your heel slowly below the step edge for a full range of motion stretch. Perform all reps on one leg before switching. This isolates the gastrocnemius and soleus more effectively than the bilateral version.",
            "Plank" to "Place your forearms flat on the floor with elbows directly below your shoulders and forearms parallel. Extend your legs behind you and balance on your toes. Your body must form a perfectly straight line from head to heels — no sagging hips, no raised hips. Brace your core as if absorbing a punch to the stomach. Breathe steadily. If your form breaks before the target time, rest and restart rather than holding with a compromised position.",
            "Side Plank" to "Lie on your side and prop yourself up on one forearm, elbow directly below your shoulder and forearm pointing forward. Stack your feet or stagger them (top foot in front) for stability. Lift your hips off the floor until your body forms a straight line from head to feet. Avoid letting your hips sag toward the floor. Place your top hand on your hip or extend it toward the ceiling. Hold for the target duration on one side, then switch.",
            "Dead Bug" to "Lie on your back with arms pointing straight up toward the ceiling and knees bent at 90 degrees in a tabletop position. Press your entire lower back firmly into the floor — this must be maintained throughout. Slowly lower your right arm overhead and your left leg toward the floor simultaneously, keeping both hovering just above the floor. Return to the starting position, then repeat on the other side (left arm, right leg). Do not let your lower back arch off the floor at any point.",
            "Leg Raise" to "Lie flat on your back with legs straight and arms at your sides (or gripping a surface behind your head for stability). Press your lower back into the floor. Keeping legs straight — or slightly bent if hamstrings are very tight — lift both legs up to 90 degrees. Lower them slowly, stopping just above the floor without touching. The lower back must stay pressed down throughout. If you feel your back lift off the floor, raise the stopping point or bend the knees slightly.",
            "Thread the Needle" to "Start on hands and knees (quadruped position), wrists under shoulders and knees under hips. Take your right arm and slide it along the floor, threading it under your left arm while rotating your torso to the left. Continue until your right shoulder and the right side of your head rest on the floor. Hold the thoracic rotation stretch for 3–5 seconds, breathing into the upper back. Return to the starting position and repeat on the other side. This opens the thoracic spine and rear shoulder.",
            "Deep Squat Hold" to "Stand with feet shoulder-width apart or slightly wider, toes turned out 30–45 degrees. Lower yourself into the deepest squat you can reach with heels flat on the floor. Hold onto a pole, TRX, or door frame for balance if needed. Let your elbows gently press your knees outward. Hold this position while breathing normally into your belly. This simultaneously improves hip flexion, ankle dorsiflexion, and thoracic mobility — three of the most commonly restricted movement patterns.",
            "Arm Circles + Shoulder Rolls" to "Stand tall with arms extended straight out to the sides at shoulder height. Make small forward circles, gradually increasing to large circles over 10 reps. Reverse direction for 10 more reps. Then let your arms hang and roll your shoulders: shrug them up toward your ears, roll them back, and press them down in a slow circle. Complete 10 forward rolls, then 10 backward rolls. This warms up the shoulder joint, rotator cuff, and upper trapezius before any pressing or pulling work.",
            "Jump Rope" to "Hold one handle in each hand with the rope hanging behind you. Swing the rope forward overhead and jump with both feet as it passes under, landing softly on the balls of your feet. Keep jumps small — just high enough to clear the rope. Maintain an upright posture, elbows close to your sides, and use wrist rotation rather than large arm circles to turn the rope. Vary intensity between moderate pace (~80 JPM) and fast pace (~130 JPM) within each set. If you miss, restart immediately.",
            "Jumping Jacks" to "Stand with feet together and arms at your sides. Jump your feet out to slightly wider than hip-width while simultaneously raising both arms overhead. Jump back to the starting position with feet together and arms down. Land softly with slightly bent knees to absorb impact. Maintain a consistent rhythm and keep your core lightly engaged. Speed up to increase cardiovascular demand or slow down for a warm-up pace.",
            "High Knees" to "Stand with feet hip-width apart and arms at your sides. Drive your right knee up toward your chest while simultaneously pumping your left arm forward. Quickly switch — extend the right leg back to the floor as you drive the left knee up and pump the right arm forward. Stay on the balls of your feet and maintain a fast, upright pace. Aim to bring each knee to at least hip height. This elevates heart rate and activates the hip flexors.",
            "Mountain Climbers" to "Start in a high plank with wrists directly under your shoulders and body in a straight line. Drive your right knee toward your chest. Quickly switch — extend the right leg back as you drive the left knee in. Alternate legs continuously at a controlled or fast pace. Keep your hips level throughout — avoid letting them rise into a pike or sag toward the floor. Breathe rhythmically. Slower tempo emphasises core strength; faster tempo raises cardiovascular demand.",
            "Butt Kicks" to "Stand with feet hip-width apart. Jog in place, alternately kicking your heels up toward your glutes as high as possible. Drive your arms in opposition (right arm forward when left heel kicks up). Stay on the balls of your feet and lean slightly forward. Focus on actively pulling the heel up with your hamstring rather than just letting it bounce passively. This dynamically warms up the hamstrings and mimics the recovery phase of running.",
            "Hammer Curl" to "Stand with feet hip-width apart holding a dumbbell in each hand, arms at your sides and palms facing each other (neutral grip). Keeping your elbows pinned firmly to your sides, curl both dumbbells simultaneously toward your shoulders without rotating your wrists — the neutral grip is maintained throughout. Lower slowly to full extension. The neutral grip targets the brachialis and brachioradialis in addition to the biceps, building overall arm and forearm thickness.",
            "Goblet Squat" to "Hold a single dumbbell vertically at your chest, cupping the top end with both hands as if holding a goblet. Stand with feet shoulder-width apart, toes turned out 20–30 degrees. Keeping the dumbbell close to your chest and elbows tucked in, sit back and down into a deep squat until thighs are parallel or below, letting your elbows brush the inside of your knees to push them out. Drive through your heels to return to standing. The front-loaded position counterbalances and naturally encourages an upright torso.",
            "Step Ups" to "Stand facing a sturdy step, box, or stair approximately knee height. Place your right foot fully flat on the step surface. Drive through your right heel to press your body upward onto the step, bringing your left foot up beside the right. Step back down with control — left foot first, then right. Complete all reps leading with the right leg, then switch to lead with the left, or alternate legs each rep. Avoid pushing off the trailing foot to cheat the movement. Keep your torso upright throughout.",
            "Side Lunges" to "Stand with feet hip-width apart. Take a large step to the right, landing with your right foot flat on the floor and toes pointing slightly outward. Bend your right knee and sit back into the right hip, lowering until your right thigh is roughly parallel to the floor while your left leg remains straight. Keep your chest tall and back flat. Push through your right heel to return to standing. Complete all reps on one side or alternate. This targets the inner thighs, glutes, and quads through lateral movement.",
            "Prone Y" to "Lie face down on the floor with arms extended straight overhead in a Y shape, thumbs pointing toward the ceiling. Keeping your arms straight, lift both arms off the floor simultaneously by engaging your lower trapezius and rear deltoids. Hold for 2 seconds at the top, then lower with control. Keep your neck neutral — gaze at the floor directly below you. Use bodyweight only or add very light dumbbells. This is critical for lower trapezius strength and shoulder blade control.",
            "Prone T" to "Lie face down on the floor with arms extended straight out to the sides in a T shape, thumbs pointing toward the ceiling. Keeping your arms straight, lift both arms off the floor by squeezing your mid-trapezius and pulling your shoulder blades together. Hold for 2 seconds at the top, then lower slowly. Keep your neck neutral. The T position targets the mid-trapezius more specifically than the Y and is essential for counteracting the forward-rounded posture from sitting.",
            "Wall Angels" to "Stand with your back flat against a wall and feet about 6 inches forward. Press your lower back, upper back, and head against the wall. Raise your arms and press the backs of your arms (elbows and wrists) against the wall at 90-degree angles (goalpost position). Slowly slide your arms upward along the wall until fully extended overhead, then slide back down — maintaining contact with the wall throughout. If your lower back arches off the wall, you've found a mobility restriction to work on. This trains thoracic extension and shoulder mobility simultaneously.",
            "Scapular Push-Up" to "Start in a high plank with arms locked completely straight — do not bend the elbows at any point during this exercise. Without bending your elbows, let your chest sink toward the floor by allowing your shoulder blades to pinch together (scapular retraction). Then push the floor away, spreading your shoulder blades wide (full scapular protraction). This is a small movement of only 2–3 cm. Focus entirely on the shoulder blades moving. This strengthens the serratus anterior, which is essential for shoulder stability and injury prevention.",
            "Hindu Squats (Baithak)" to "Stand with feet shoulder-width apart and arms at your sides. Begin to squat down while simultaneously rising onto the balls of your feet and swinging your arms forward to shoulder height — arms lead the downward movement. At the bottom of the squat, heels are off the floor and arms are extended forward. Rise back up, lowering your heels as your arms pull back. The movement is rhythmic and continuous — arms and heels move together in a coordinated pump. Start slowly to internalise the rhythm, then build speed for endurance work.",
            "Self-Resistance Ext Rotation" to "No equipment needed. Bend your right elbow to 90 degrees with your upper arm at your side. Place your left palm firmly against the back of your right hand. Attempt to externally rotate your right forearm outward while your left hand resists the movement — hold this isometric contraction for 5 seconds. Then switch: place your right palm against the front of your left hand and resist an internal rotation attempt for 5 seconds. Alternate for the target reps. This trains the rotator cuff at therapeutic intensity with zero equipment."
        )

        val yogaAndStretchesToSeed = listOf(
            Triple("Tadasana (Mountain Pose)", ExerciseCategory.YOGA, "foundation, standing, posture, balance"),
            Triple("Vrikshasana (Tree Pose)", ExerciseCategory.YOGA, "balance, standing, focus, hip opener"),
            Triple("Adho Mukha Svanasana (Downward Dog)", ExerciseCategory.YOGA, "full body, hamstrings, shoulders, spine"),
            Triple("Bhujangasana (Cobra Pose)", ExerciseCategory.YOGA, "backbend, spine, chest, core"),
            Triple("Balasana (Child's Pose)", ExerciseCategory.YOGA, "restorative, hips, lower back, rest"),
            Triple("Trikonasana (Triangle Pose)", ExerciseCategory.YOGA, "standing, hips, hamstrings, side body"),
            Triple("Virabhadrasana I (Warrior I)", ExerciseCategory.YOGA, "standing, strength, hip flexors, focus"),
            Triple("Virabhadrasana II (Warrior II)", ExerciseCategory.YOGA, "standing, strength, hips, endurance"),
            Triple("Paschimottanasana (Seated Forward Bend)", ExerciseCategory.YOGA, "hamstrings, lower back, calming"),
            Triple("Ardha Matsyendrasana (Half Spinal Twist)", ExerciseCategory.YOGA, "spine, detox, mobility, seated"),
            Triple("Savasana (Corpse Pose)", ExerciseCategory.YOGA, "restorative, relaxation, nervous system"),
            Triple("Padmasana (Lotus Pose)", ExerciseCategory.YOGA, "meditation, hips, flexibility, seated"),
            Triple("Ustrasana (Camel Pose)", ExerciseCategory.YOGA, "backbend, chest opener, hip flexors"),
            Triple("Dhanurasana (Bow Pose)", ExerciseCategory.YOGA, "backbend, core, chest, hip flexors"),
            Triple("Halasana (Plow Pose)", ExerciseCategory.YOGA, "inversion, spine, hamstrings, shoulders"),
            Triple("Sarvangasana (Shoulder Stand)", ExerciseCategory.YOGA, "inversion, thyroid, legs, core"),
            Triple("Surya Namaskar (Sun Salutation)", ExerciseCategory.YOGA, "flow, full body, warm up, breath"),
            Triple("Viparita Karani (Legs Up the Wall)", ExerciseCategory.YOGA, "restorative, legs, inversion, calming"),
            Triple("Anjaneyasana (Low Lunge)", ExerciseCategory.YOGA, "hip flexors, groin, standing, balance"),
            Triple("Utkata Konasana (Goddess Pose)", ExerciseCategory.YOGA, "strength, hips, groin, standing"),
            Triple("Hip Flexor Stretch", ExerciseCategory.STRETCHING, "hips, flexibility, cooldown, mobility"),
            Triple("Pigeon Pose", ExerciseCategory.STRETCHING, "hips, glutes, IT band, deep stretch"),
            Triple("Standing Hamstring Stretch", ExerciseCategory.STRETCHING, "hamstrings, flexibility, cooldown"),
            Triple("Standing Quad Stretch", ExerciseCategory.STRETCHING, "quads, hip flexors, balance, cooldown"),
            Triple("Chest Opener Stretch", ExerciseCategory.STRETCHING, "chest, shoulders, posture, cooldown"),
            Triple("Thoracic Spine Stretch", ExerciseCategory.STRETCHING, "spine, mobility, posture, upper back"),
            Triple("Neck Side Stretch", ExerciseCategory.STRETCHING, "neck, tension, posture, cooldown"),
            Triple("Cross-body Shoulder Stretch", ExerciseCategory.STRETCHING, "shoulders, rear delt, cooldown"),
            Triple("IT Band Stretch", ExerciseCategory.STRETCHING, "IT band, hips, lateral thigh, cooldown"),
            Triple("Calf Stretch", ExerciseCategory.STRETCHING, "calves, Achilles, ankles, cooldown"),
            Triple("Figure-4 Stretch (Piriformis)", ExerciseCategory.STRETCHING, "glutes, piriformis, hips, lower back"),
            Triple("Doorway Chest Stretch", ExerciseCategory.STRETCHING, "chest, shoulders, posture, pectorals"),
            Triple("Overhead Tricep Stretch", ExerciseCategory.STRETCHING, "triceps, lats, arms, cooldown"),
            Triple("Seated Spinal Twist", ExerciseCategory.STRETCHING, "spine, mobility, hips, detox"),

            // ── Routine A & B exercises ──────────────────────────────────────────
            Triple("Push-Up", ExerciseCategory.STRENGTH, "bodyweight, chest, triceps, core"),
            Triple("Pike Push-Up", ExerciseCategory.STRENGTH, "bodyweight, shoulders, triceps, push"),
            Triple("Diamond Push-Up", ExerciseCategory.STRENGTH, "bodyweight, triceps, chest, push"),
            Triple("DB Overhead Press", ExerciseCategory.STRENGTH, "shoulders, dumbbell, pressing, upper body"),
            Triple("Indian Club Full Rotation", ExerciseCategory.STRENGTH, "shoulder mobility, grip, Indian clubs, rotational"),
            Triple("Wrist Curl (DB)", ExerciseCategory.STRENGTH, "forearms, wrist flexors, grip, dumbbell"),
            Triple("Bulgarian Split Squat", ExerciseCategory.STRENGTH, "quads, glutes, single leg, lower body"),
            Triple("SL Glute Bridge", ExerciseCategory.STRENGTH, "glutes, single leg, hamstrings, core"),
            Triple("SL Calf Raise", ExerciseCategory.STRENGTH, "calves, single leg, ankles, lower leg"),
            Triple("Plank", ExerciseCategory.STRENGTH, "core, stability, isometric, full body"),
            Triple("Side Plank", ExerciseCategory.STRENGTH, "core, obliques, stability, isometric"),
            Triple("Dead Bug", ExerciseCategory.STRENGTH, "core, anti-extension, stability, coordination"),
            Triple("Leg Raise", ExerciseCategory.STRENGTH, "core, hip flexors, abs, lower body"),
            Triple("Thread the Needle", ExerciseCategory.STRETCHING, "thoracic mobility, shoulders, rotation, spine"),
            Triple("Deep Squat Hold", ExerciseCategory.STRETCHING, "hips, ankles, mobility, squat"),
            Triple("Arm Circles + Shoulder Rolls", ExerciseCategory.STRETCHING, "warm up, shoulders, mobility, rotator cuff"),
            Triple("Jump Rope", ExerciseCategory.CARDIO_OTHER, "cardio, coordination, calves, full body"),
            Triple("Jumping Jacks", ExerciseCategory.CARDIO_OTHER, "cardio, warm up, full body, coordination"),
            Triple("High Knees", ExerciseCategory.CARDIO_OTHER, "cardio, hip flexors, coordination, warm up"),
            Triple("Mountain Climbers", ExerciseCategory.CARDIO_OTHER, "cardio, core, full body, plank"),
            Triple("Butt Kicks", ExerciseCategory.CARDIO_OTHER, "cardio, hamstrings, warm up, running"),
            Triple("Hammer Curl", ExerciseCategory.STRENGTH, "biceps, brachialis, forearms, dumbbell"),
            Triple("Goblet Squat", ExerciseCategory.STRENGTH, "quads, glutes, dumbbell, squat"),
            Triple("Step Ups", ExerciseCategory.STRENGTH, "quads, glutes, single leg, lower body"),
            Triple("Side Lunges", ExerciseCategory.STRENGTH, "inner thighs, glutes, quads, lateral"),
            Triple("Prone Y", ExerciseCategory.STRENGTH, "lower trap, rear delt, shoulder health, posterior"),
            Triple("Prone T", ExerciseCategory.STRENGTH, "mid trap, rear delt, shoulder health, posterior"),
            Triple("Wall Angels", ExerciseCategory.STRETCHING, "thoracic, shoulder mobility, posture, serratus"),
            Triple("Scapular Push-Up", ExerciseCategory.STRENGTH, "serratus anterior, shoulder stability, scapula, push"),
            Triple("Hindu Squats (Baithak)", ExerciseCategory.STRENGTH, "legs, traditional, functional, endurance"),
            Triple("Self-Resistance Ext Rotation", ExerciseCategory.STRENGTH, "rotator cuff, shoulder health, bodyweight, no equipment")
        )

        val existingExercises = exerciseDao.getAllExercisesSync()
        val existingByName = existingExercises.associateBy { it.name.trim().lowercase() }

        // Patch missing instructions on existing exercises
        existingExercises.forEach { exercise ->
            val instr = instructionsByName[exercise.name.trim()]
            if (!instr.isNullOrBlank() && exercise.instructions.isNullOrBlank()) {
                exerciseDao.updateExercise(exercise.copy(instructions = instr))
            }
        }

        // Insert new yoga / stretching exercises that don't exist yet
        yogaAndStretchesToSeed.forEach { (name, category, tags) ->
            if (existingByName[name.trim().lowercase()] == null) {
                exerciseDao.insertExercise(
                    Exercise(
                        name = name,
                        description = "",
                        imageUri = null,
                        instructions = instructionsByName[name],
                        tags = tags,
                        category = category
                    )
                )
            }
        }
    }

    suspend fun addHabitFromTemplate(template: com.example.habitpower.data.model.HabitTemplate): Boolean {
        val user = getResolvedActiveUser().firstOrNull() ?: return false
        val allHabits = habitTrackingDao.getAllHabits().first()
        if (allHabits.any { it.name.equals(template.name, ignoreCase = true) }) return false

        val nextOrder = (allHabits.maxOfOrNull { it.displayOrder } ?: -1) + 1
        val habitId = habitTrackingDao.insertHabit(
            HabitDefinition(
                name = template.name,
                goalIdentityStatement = "I am someone who ${template.identityStatement}.",
                description = template.description,
                type = template.type,
                unit = template.unit,
                targetValue = template.targetValue,
                commitmentTime = String.format(
                    java.util.Locale.US, "%02d:%02d",
                    template.commitmentHour, template.commitmentMinute
                ),
                recurrenceType = HabitRecurrenceType.DAILY,
                recurrenceStartDate = LocalDate.now(),
                displayOrder = nextOrder,
                showInDailyCheckIn = true,
                showInWidget = true,
                operator = TargetOperator.GREATER_THAN_OR_EQUAL
            )
        )

        val assignedIds = habitTrackingDao.getAssignedHabitIdsForUser(user.id).first().toMutableSet()
        assignedIds.add(habitId)
        val orderedIds = habitTrackingDao.getAllHabits().first()
            .filter { it.id in assignedIds }
            .sortedBy { it.displayOrder }
            .map { it.id }
        replaceAssignmentsForUser(user.id, orderedIds)
        triggerRefresh()
        return true
    }

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

    // ── Pomodoro free sessions ─────────────────────────────────────────────────

    suspend fun saveFreeSession(userId: Long, durationMinutes: Int): PomodoroSession {
        val session = PomodoroSession(
            userId = userId,
            date = LocalDate.now(),
            durationMinutes = durationMinutes,
            completedAt = System.currentTimeMillis()
        )
        val id = pomodoroSessionDao.insert(session)
        return session.copy(id = id)
    }

    fun getUnlinkedSessionsForDate(userId: Long, date: LocalDate): Flow<List<PomodoroSession>> =
        pomodoroSessionDao.getUnlinkedSessionsForDate(userId, date)

    suspend fun linkSessionToHabit(
        sessionId: Long,
        habitId: Long,
        userId: Long,
        currentPomodoroCount: Double
    ) {
        pomodoroSessionDao.linkToHabit(sessionId, habitId)
        saveDailyHabitEntry(
            userId = userId,
            date = LocalDate.now(),
            habitId = habitId,
            type = HabitType.POMODORO,
            numericValue = currentPomodoroCount + 1.0
        )
    }

    suspend fun discardSession(session: PomodoroSession) {
        pomodoroSessionDao.delete(session)
    }

    suspend fun cleanupOldUnlinkedSessions(userId: Long) {
        pomodoroSessionDao.deleteOldUnlinked(userId, LocalDate.now())
    }

    // ── Chant ────────────────────────────────────────────────────────────────

    fun getAllChants(): Flow<List<ChantDefinition>> = chantDao.getAllChants()

    suspend fun insertChant(chant: ChantDefinition): Long = chantDao.insertChant(chant)

    suspend fun updateChant(chant: ChantDefinition) = chantDao.updateChant(chant)

    suspend fun deleteChant(chant: ChantDefinition) {
        if (chant.isBuiltIn) {
            val prefs = context.getSharedPreferences("chant_prefs", android.content.Context.MODE_PRIVATE)
            val dismissed = prefs.getStringSet("dismissed_builtin_chants", emptySet())?.toMutableSet() ?: mutableSetOf()
            dismissed.add(chant.name.trim().lowercase())
            prefs.edit().putStringSet("dismissed_builtin_chants", dismissed).apply()
        }
        chantDao.deleteChant(chant)
    }

    suspend fun insertChantSession(session: ChantSession): Long = chantDao.insertSession(session)

    fun getRecentChantSessions(userId: Long): Flow<List<ChantSession>> =
        chantDao.getRecentSessions(userId)

    suspend fun logMeditationSession(userId: Long, presetName: String, durationSeconds: Long) {
        meditationDao.insertSession(
            com.example.habitpower.data.model.MeditationSession(
                userId = userId,
                presetName = presetName,
                durationSeconds = durationSeconds
            )
        )
    }

    fun getMeditationSessionCountForDate(userId: Long, date: LocalDate): Flow<Int> {
        val fromMs = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMs = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return meditationDao.countSessionsInRange(userId, fromMs, toMs)
    }

    suspend fun seedChantsIfNeeded() {
        val prefs = context.getSharedPreferences("chant_prefs", android.content.Context.MODE_PRIVATE)
        val dismissed = prefs.getStringSet("dismissed_builtin_chants", emptySet())?.map { it.lowercase() }?.toSet() ?: emptySet()
        val existing = chantDao.getAllChantsSync().map { it.name.trim().lowercase() }.toSet()
        val builtIn = listOf(
            ChantDefinition(name = "Om", text = "ॐ", tradition = "Hindu / Universal", defaultCount = 108, isBuiltIn = true),
            ChantDefinition(name = "Om Namah Shivaya", text = "ॐ नमः शिवाय", tradition = "Hindu (Shaiva)", defaultCount = 108, isBuiltIn = true),
            ChantDefinition(name = "Gayatri Mantra", text = "ॐ भूर्भुवः स्वः\nतत्सवितुर्वरेण्यं\nभर्गो देवस्य धीमहि\nधियो यो नः प्रचोदयात्", tradition = "Hindu (Vedic)", defaultCount = 108, isBuiltIn = true),
            ChantDefinition(name = "So Hum", text = "So Hum", tradition = "Hindu / Yoga", defaultCount = 108, isBuiltIn = true),
            ChantDefinition(name = "Om Mani Padme Hum", text = "ॐ मणि पद्मे हूं", tradition = "Buddhist (Tibetan)", defaultCount = 108, isBuiltIn = true),
            ChantDefinition(name = "Hare Krishna", text = "Hare Krishna Hare Krishna\nKrishna Krishna Hare Hare\nHare Rama Hare Rama\nRama Rama Hare Hare", tradition = "Hindu (Vaishnava)", defaultCount = 108, isBuiltIn = true),
            ChantDefinition(name = "Gratitude Affirmation", text = "I am grateful for this moment.\nI have everything I need.\nI am enough.", tradition = null, defaultCount = 21, isBuiltIn = true)
        )
        builtIn.filter { it.name.trim().lowercase() !in existing && it.name.trim().lowercase() !in dismissed }
            .forEach { chantDao.insertChant(it) }
    }

    // ── Tasks ─────────────────────────────────────────────────────────────

    fun getTaskLists(userId: Long): Flow<List<TaskList>> = taskDao.getTaskLists(userId)

    suspend fun insertTaskList(list: TaskList): Long = taskDao.insertTaskList(list)

    suspend fun updateTaskList(list: TaskList) = taskDao.updateTaskList(list)

    suspend fun deleteTaskList(list: TaskList) {
        taskDao.deleteTasksForList(list.id)
        taskDao.deleteTaskList(list)
    }

    fun getTasksForList(listId: Long): Flow<List<Task>> = taskDao.getTasksForList(listId)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun toggleTask(task: Task) = taskDao.updateTask(
        task.copy(
            isDone = !task.isDone,
            completedAt = if (!task.isDone) System.currentTimeMillis() else null
        )
    )

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    // ── Checklists ────────────────────────────────────────────────────────

    fun getChecklists(userId: Long): Flow<List<Checklist>> = taskDao.getChecklists(userId)

    suspend fun insertChecklist(checklist: Checklist): Long = taskDao.insertChecklist(checklist)

    suspend fun updateChecklist(checklist: Checklist) = taskDao.updateChecklist(checklist)

    suspend fun deleteChecklist(checklist: Checklist) {
        taskDao.deleteItemsForChecklist(checklist.id)
        taskDao.deleteChecklist(checklist)
    }

    fun getItemsForChecklist(checklistId: Long): Flow<List<ChecklistItem>> =
        taskDao.getItemsForChecklist(checklistId)

    suspend fun insertChecklistItem(item: ChecklistItem): Long = taskDao.insertChecklistItem(item)

    suspend fun toggleChecklistItem(item: ChecklistItem) = taskDao.updateChecklistItem(
        item.copy(
            isChecked = !item.isChecked,
            lastCheckedAt = if (!item.isChecked) System.currentTimeMillis() else null
        )
    )

    suspend fun deleteChecklistItem(item: ChecklistItem) = taskDao.deleteChecklistItem(item)

    suspend fun resetChecklist(checklistId: Long) = taskDao.resetChecklist(checklistId)

    // ── Widget List Entries ───────────────────────────────────────────────

    suspend fun getWidgetListEntries(userId: Long): List<WidgetListEntry> {
        val result = mutableListOf<WidgetListEntry>()
        taskDao.getTaskLists(userId).first().forEach { list ->
            val items = taskDao.getTasksForList(list.id).first().map { task ->
                WidgetListItem(id = task.id, name = task.name, isDone = task.isDone, isTaskItem = true)
            }
            result.add(WidgetListEntry.TaskListEntry(id = list.id, name = list.name, items = items))
        }
        taskDao.getChecklists(userId).first().forEach { checklist ->
            val items = taskDao.getItemsForChecklist(checklist.id).first().map { item ->
                WidgetListItem(id = item.id, name = item.name, isDone = item.isChecked, isTaskItem = false)
            }
            result.add(WidgetListEntry.ChecklistEntry(id = checklist.id, name = checklist.name, items = items))
        }
        return result
    }

    suspend fun toggleTaskById(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTask(task.copy(
            isDone = !task.isDone,
            completedAt = if (!task.isDone) System.currentTimeMillis() else null
        ))
    }

    suspend fun toggleChecklistItemById(itemId: Long) {
        val item = taskDao.getChecklistItemById(itemId) ?: return
        taskDao.updateChecklistItem(item.copy(
            isChecked = !item.isChecked,
            lastCheckedAt = if (!item.isChecked) System.currentTimeMillis() else null
        ))
    }

    // ── .hpex Export helpers ──────────────────────────────────────────────────

    suspend fun getAllHabitAssignmentsForExport(): List<UserHabitAssignment> =
        habitTrackingDao.getAllAssignments()

    suspend fun getAllLifeAreaAssignmentsForExport(): List<UserLifeAreaAssignment> =
        lifeAreaDao.getAllLifeAreaAssignments()

    suspend fun getAllRoutineCrossRefsForExport(): List<RoutineExerciseCrossRef> =
        routineDao.getAllCrossRefsSync()

    suspend fun getAllExercisesForExport(): List<Exercise> =
        exerciseDao.getAllExercisesSync()

    suspend fun getAllTaskListsForExport(): List<TaskList> =
        taskDao.getAllTaskLists()

    suspend fun getAllTasksForExport(): List<Task> =
        taskDao.getAllTasks()

    suspend fun getAllChecklistsForExport(): List<Checklist> =
        taskDao.getAllChecklists()

    suspend fun getAllChecklistItemsForExport(): List<ChecklistItem> =
        taskDao.getAllChecklistItems()

    suspend fun getAllChantsForExport(): List<ChantDefinition> =
        chantDao.getAllChantsSync()

    suspend fun getAllQuotesForExport(): List<com.example.habitpower.data.model.Quote> =
        quoteDao.getAllQuotesSync()

    // ── .hpex Direct inserts (preserve original IDs for restore) ─────────────

    suspend fun insertUserDirect(user: UserProfile): Long = userDao.insertUser(user)

    suspend fun insertLifeAreaDirect(area: LifeArea): Long = lifeAreaDao.insertLifeArea(area)

    suspend fun insertUserLifeAreaAssignmentDirect(a: UserLifeAreaAssignment) =
        lifeAreaDao.upsertLifeAreaAssignment(a)

    suspend fun insertHabitDirect(habit: HabitDefinition): Long =
        habitTrackingDao.insertHabit(habit)

    suspend fun insertUserHabitAssignmentDirect(a: UserHabitAssignment) =
        habitTrackingDao.upsertAssignment(a)

    suspend fun insertExerciseDirect(exercise: Exercise): Long =
        exerciseDao.insertExercise(exercise)

    suspend fun insertRoutineDirect(routine: Routine): Long =
        routineDao.insertRoutine(routine)

    suspend fun insertRoutineCrossRefDirect(crossRef: RoutineExerciseCrossRef) =
        routineDao.insertRoutineExerciseCrossRef(crossRef)

    suspend fun insertChantDirect(chant: ChantDefinition): Long =
        chantDao.insertChant(chant)

    suspend fun insertQuoteDirect(quote: com.example.habitpower.data.model.Quote) =
        quoteDao.insertQuote(quote)

    // ── Factory reset ─────────────────────────────────────────────────────────

    suspend fun clearForRestore() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        userPreferencesRepository.clearActiveUserId()
    }

    suspend fun resetAllData() {
        com.example.habitpower.util.CrashLogger.log("factory reset initiated")
        withContext(Dispatchers.IO) { database.clearAllTables() }
        userPreferencesRepository.clearActiveUserId()
        seedChantsIfNeeded()
        syncHabitReminders()
        updateWidgetState()
        triggerRefresh()
        com.example.habitpower.util.CrashLogger.log("factory reset complete")
    }
}
