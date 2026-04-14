package com.example.habitpower.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.habitpower.util.NotificationSoundOption
import kotlinx.coroutines.flow.Flow
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
}
