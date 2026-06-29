package com.example.habitpower.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.LocalDate
import com.example.habitpower.reminder.NotificationChannelType
import androidx.datastore.preferences.preferencesDataStore
import com.example.habitpower.util.NotificationSoundOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private val ACTIVE_USER_ID_KEY = intPreferencesKey("active_user_id")

    val activeUserId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_USER_ID_KEY]?.toLong()
    }

    suspend fun saveActiveUserId(userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_USER_ID_KEY] = userId.toInt()
        }
    }

    suspend fun clearActiveUserId() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACTIVE_USER_ID_KEY)
        }
    }

    private val STREAK_BASE_DAYS_KEY = intPreferencesKey("streak_base_days")

    val streakBaseDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[STREAK_BASE_DAYS_KEY] ?: 7
    }

    suspend fun saveStreakBaseDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[STREAK_BASE_DAYS_KEY] = days
        }
    }

    // ── Notification sound ──────────────────────────────────────────────────────

    private val SOUND_ENABLED_KEY = booleanPreferencesKey("notification_sound_enabled")
    private val SOUND_ID_KEY = stringPreferencesKey("notification_sound_id")
    private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("completion_vibration_enabled")
    private val ROUTINE_START_SOUND_ENABLED_KEY = booleanPreferencesKey("routine_start_sound_enabled")
    private val ROUTINE_START_SOUND_ID_KEY = stringPreferencesKey("routine_start_sound_id")
    private val ROUTINE_END_SOUND_ENABLED_KEY = booleanPreferencesKey("routine_end_sound_enabled")
    private val ROUTINE_END_SOUND_ID_KEY = stringPreferencesKey("routine_end_sound_id")

    /** Whether the completion notification sound is enabled (default: true). */
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED_KEY] ?: true
    }

    /** The ID of the selected notification tone (see [NotificationSoundOption]). */
    val notificationSoundId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ID_KEY] ?: NotificationSoundOption.entries.first().id
    }

    /** Whether the completion haptic vibration is enabled (default: true). */
    val completionVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED_KEY] ?: true
    }

    val routineStartSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ROUTINE_START_SOUND_ENABLED_KEY] ?: true
    }

    val routineStartSoundId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ROUTINE_START_SOUND_ID_KEY] ?: NotificationSoundOption.SHORT_BEEP.id
    }

    val routineEndSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ROUTINE_END_SOUND_ENABLED_KEY] ?: true
    }

    val routineEndSoundId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ROUTINE_END_SOUND_ID_KEY] ?: NotificationSoundOption.POSITIVE.id
    }

    suspend fun saveSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED_KEY] = enabled }
    }

    suspend fun saveNotificationSoundId(id: String) {
        context.dataStore.edit { it[SOUND_ID_KEY] = id }
    }

    suspend fun saveCompletionVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED_KEY] = enabled }
    }

    suspend fun saveRoutineStartSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ROUTINE_START_SOUND_ENABLED_KEY] = enabled }
    }

    suspend fun saveRoutineStartSoundId(id: String) {
        context.dataStore.edit { it[ROUTINE_START_SOUND_ID_KEY] = id }
    }

    suspend fun saveRoutineEndSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ROUTINE_END_SOUND_ENABLED_KEY] = enabled }
    }

    suspend fun saveRoutineEndSoundId(id: String) {
        context.dataStore.edit { it[ROUTINE_END_SOUND_ID_KEY] = id }
    }

    // ── TTS ──────────────────────────────────────────────────────────────────

    private val TTS_ENABLED_KEY = booleanPreferencesKey("routine_tts_enabled")

    val routineTtsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TTS_ENABLED_KEY] ?: false
    }

    suspend fun saveRoutineTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TTS_ENABLED_KEY] = enabled }
    }

    // ── Pomodoro settings ──────────────────────────────────────────────────────

    private val POMODORO_FOCUS_MINUTES_KEY = intPreferencesKey("pomodoro_focus_minutes")
    private val POMODORO_SHORT_BREAK_MINUTES_KEY = intPreferencesKey("pomodoro_short_break_minutes")
    private val POMODORO_LONG_BREAK_MINUTES_KEY = intPreferencesKey("pomodoro_long_break_minutes")
    private val POMODORO_CYCLES_BEFORE_LONG_BREAK_KEY = intPreferencesKey("pomodoro_cycles_before_long_break")

    val pomodoroSettings: Flow<PomodoroSettings> = combine(
        context.dataStore.data.map { it[POMODORO_FOCUS_MINUTES_KEY] ?: 25 },
        context.dataStore.data.map { it[POMODORO_SHORT_BREAK_MINUTES_KEY] ?: 5 },
        context.dataStore.data.map { it[POMODORO_LONG_BREAK_MINUTES_KEY] ?: 15 },
        context.dataStore.data.map { it[POMODORO_CYCLES_BEFORE_LONG_BREAK_KEY] ?: 4 }
    ) { focus, shortBreak, longBreak, cycles ->
        PomodoroSettings(focus, shortBreak, longBreak, cycles)
    }

    suspend fun savePomodoroSettings(settings: PomodoroSettings) {
        context.dataStore.edit {
            it[POMODORO_FOCUS_MINUTES_KEY] = settings.focusMinutes
            it[POMODORO_SHORT_BREAK_MINUTES_KEY] = settings.shortBreakMinutes
            it[POMODORO_LONG_BREAK_MINUTES_KEY] = settings.longBreakMinutes
            it[POMODORO_CYCLES_BEFORE_LONG_BREAK_KEY] = settings.cyclesBeforeLongBreak
        }
    }

    // ── App open date tracking (for Missed-Day Welcome) ────────────────────────

    private val LAST_OPENED_EPOCH_DAY_KEY = longPreferencesKey("last_opened_epoch_day")

    val lastOpenedEpochDay: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LAST_OPENED_EPOCH_DAY_KEY]
    }

    suspend fun saveLastOpenedEpochDay(epochDay: Long) {
        context.dataStore.edit { it[LAST_OPENED_EPOCH_DAY_KEY] = epochDay }
    }

    // ── Notification channel preferences ──────────────────────────────────────

    private val ENABLED_NOTIFICATION_CHANNELS_KEY =
        stringSetPreferencesKey("enabled_notification_channels")

    val enabledNotificationChannels: Flow<Set<NotificationChannelType>> =
        context.dataStore.data.map { prefs ->
            prefs[ENABLED_NOTIFICATION_CHANNELS_KEY]
                ?.mapNotNull { name -> runCatching { NotificationChannelType.valueOf(name) }.getOrNull() }
                ?.toSet()
                ?: setOf(NotificationChannelType.HABIT_REMINDERS, NotificationChannelType.PRACTICE_NUDGE)
        }

    suspend fun setEnabledNotificationChannels(channels: Set<NotificationChannelType>) {
        context.dataStore.edit { prefs ->
            prefs[ENABLED_NOTIFICATION_CHANNELS_KEY] = channels.map { it.name }.toSet()
        }
    }

    // ── Step-Back Mode ────────────────────────────────────────────────────────

    private val STEP_BACK_ACTIVE_KEY = booleanPreferencesKey("step_back_active")
    private val STEP_BACK_RETURN_EPOCH_DAY_KEY = longPreferencesKey("step_back_return_epoch_day")

    val stepBackActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[STEP_BACK_ACTIVE_KEY] ?: false
    }

    val stepBackReturnEpochDay: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[STEP_BACK_RETURN_EPOCH_DAY_KEY]
    }

    suspend fun setStepBack(active: Boolean, returnEpochDay: Long?) {
        context.dataStore.edit { prefs ->
            prefs[STEP_BACK_ACTIVE_KEY] = active
            if (returnEpochDay != null) {
                prefs[STEP_BACK_RETURN_EPOCH_DAY_KEY] = returnEpochDay
            } else {
                prefs.remove(STEP_BACK_RETURN_EPOCH_DAY_KEY)
            }
        }
    }

    // ── Habit completion sound ────────────────────────────────────────────────

    private val HABIT_COMPLETION_SOUND_ENABLED_KEY = booleanPreferencesKey("habit_completion_sound_enabled")
    private val HABIT_COMPLETION_SOUND_ID_KEY = stringPreferencesKey("habit_completion_sound_id")

    val habitCompletionSoundEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[HABIT_COMPLETION_SOUND_ENABLED_KEY] ?: true
    }

    val habitCompletionSoundId: Flow<String> = context.dataStore.data.map {
        it[HABIT_COMPLETION_SOUND_ID_KEY] ?: NotificationSoundOption.POSITIVE.id
    }

    suspend fun saveHabitCompletionSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HABIT_COMPLETION_SOUND_ENABLED_KEY] = enabled }
    }

    suspend fun saveHabitCompletionSoundId(id: String) {
        context.dataStore.edit { it[HABIT_COMPLETION_SOUND_ID_KEY] = id }
    }

    private val LAST_SEASON_REVIEW_EPOCH_DAY_KEY = longPreferencesKey("last_season_review_epoch_day")

    val lastSeasonReviewEpochDay: Flow<Long?> = context.dataStore.data.map { it[LAST_SEASON_REVIEW_EPOCH_DAY_KEY] }

    suspend fun saveLastSeasonReviewEpochDay(epochDay: Long) {
        context.dataStore.edit { it[LAST_SEASON_REVIEW_EPOCH_DAY_KEY] = epochDay }
    }

    private val DRIVE_ACCOUNT_NAME_KEY = stringPreferencesKey("drive_account_name")
    private val DRIVE_LAST_SYNC_AT_KEY = longPreferencesKey("drive_last_sync_at")

    val driveAccountName: Flow<String?> = context.dataStore.data.map { it[DRIVE_ACCOUNT_NAME_KEY] }
    val driveLastSyncAt: Flow<Long?> = context.dataStore.data.map { it[DRIVE_LAST_SYNC_AT_KEY] }

    suspend fun setDriveAccount(name: String?) {
        context.dataStore.edit { prefs ->
            if (name != null) prefs[DRIVE_ACCOUNT_NAME_KEY] = name
            else prefs.remove(DRIVE_ACCOUNT_NAME_KEY)
        }
    }

    suspend fun setDriveLastSyncAt(timeMs: Long) {
        context.dataStore.edit { it[DRIVE_LAST_SYNC_AT_KEY] = timeMs }
    }

    // ── Self Standup commitments ───────────────────────────────────────────────

    fun getStandupCommitment(cadence: String): Flow<String> = context.dataStore.data.map {
        it[stringPreferencesKey("standup_commitment_$cadence")] ?: ""
    }

    suspend fun saveStandupCommitment(cadence: String, text: String) {
        context.dataStore.edit {
            it[stringPreferencesKey("standup_commitment_$cadence")] = text
            it[longPreferencesKey("standup_done_ms_$cadence")] = System.currentTimeMillis()
        }
    }

    fun getStandupLastCompletedMs(cadence: String): Flow<Long?> = context.dataStore.data.map {
        it[longPreferencesKey("standup_done_ms_$cadence")]
    }

    // ── Daily intention ───────────────────────────────────────────────────────

    fun getDailyIntention(date: String): Flow<String> = context.dataStore.data.map {
        it[stringPreferencesKey("daily_intention_$date")] ?: ""
    }

    suspend fun saveDailyIntention(date: String, text: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("daily_intention_$date")] = text
            // Remove entries older than 30 days to prevent unbounded growth
            val cutoff = runCatching { LocalDate.parse(date).minusDays(30) }.getOrNull() ?: return@edit
            prefs.asMap().keys
                .filter { it.name.startsWith("daily_intention_") }
                .forEach { key ->
                    val keyDate = runCatching {
                        LocalDate.parse(key.name.removePrefix("daily_intention_"))
                    }.getOrNull()
                    if (keyDate != null && keyDate.isBefore(cutoff)) {
                        prefs.remove(stringPreferencesKey(key.name))
                    }
                }
        }
    }
}
