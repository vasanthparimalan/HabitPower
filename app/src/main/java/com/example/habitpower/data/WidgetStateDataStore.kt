package com.example.habitpower.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.widgetStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_state")
private val WIDGET_STATE_KEY = stringPreferencesKey("widget_state")

suspend fun Context.saveWidgetState(state: WidgetState) {
    widgetStateDataStore.edit { preferences ->
        // sanitize user-visible strings before storing
        val safe = state.copy(
            userName = InputSanitizer.sanitize(state.userName, 100) ?: "",
            habits = state.habits.map { h ->
                WidgetHabit(
                    name = InputSanitizer.sanitize(h.name, 120) ?: "",
                    isCompleted = h.isCompleted,
                    streak = h.streak
                )
            }
        )
        preferences[WIDGET_STATE_KEY] = safe.toJson()
    }
}

fun Context.getWidgetState(): Flow<WidgetState> = widgetStateDataStore.data.map { preferences ->
    val json = preferences[WIDGET_STATE_KEY]
    if (json.isNullOrBlank()) WidgetState() else json.toWidgetState()
}

private fun WidgetState.toJson(): String {
    val json = JSONObject().apply {
        put("userName", userName)
        val habitsArray = JSONArray()
        habits.forEach { habit ->
            habitsArray.put(
                JSONObject().apply {
                    put("name", habit.name)
                    put("isCompleted", habit.isCompleted)
                    put("streak", habit.streak)
                }
            )
        }
        put("habits", habitsArray)
    }
    return json.toString()
}

private fun String.toWidgetState(): WidgetState {
    return try {
        val json = JSONObject(this)
        val userName = InputSanitizer.sanitize(json.optString("userName", ""), 100) ?: ""
        val habitsArray = json.optJSONArray("habits") ?: JSONArray()
        val habits = mutableListOf<WidgetHabit>()
        for (i in 0 until habitsArray.length()) {
            val habitObject = habitsArray.optJSONObject(i) ?: continue
            val rawName = habitObject.optString("name", "")
            val safeName = InputSanitizer.sanitize(rawName, 120) ?: ""
            habits.add(WidgetHabit(name = safeName, isCompleted = habitObject.optBoolean("isCompleted", false), streak = habitObject.optInt("streak", 0)))
        }
        WidgetState(userName = userName, habits = habits)
    } catch (e: Exception) {
        WidgetState()
    }
}
