package com.example.habitpower.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habitpower.HabitPowerApp
import kotlinx.coroutines.flow.first

class DriveSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HabitPowerApp
        val prefs = app.container.userPreferencesRepository
        val accountName = prefs.driveAccountName.first() ?: return Result.success()

        val repository = app.container.habitPowerRepository

        val habitsCsv = buildHabitCsv(repository)
        val routinesCsv = buildRoutinesCsv(repository)
        val healthCsv = buildHealthCsv(repository)

        val files = mapOf(
            "habits.csv" to habitsCsv,
            "routines.csv" to routinesCsv,
            "health.csv" to healthCsv
        )

        return when (DriveSyncManager.syncToCloud(applicationContext, accountName, files)) {
            is SyncResult.Success -> {
                prefs.setDriveLastSyncAt(System.currentTimeMillis())
                Result.success()
            }
            is SyncResult.AuthRequired -> Result.retry()
            is SyncResult.Error -> Result.failure()
        }
    }

    private suspend fun buildHabitCsv(
        repository: com.example.habitpower.data.HabitPowerRepository
    ): String {
        val users = repository.getAllUsers().first()
        val habits = repository.getAllHabits().first()
        val lifeAreas = repository.getAllLifeAreas().first()
        val habitById = habits.associateBy { it.id }
        val userById = users.associateBy { it.id }
        val lifeAreaById = lifeAreas.associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("date,user,habit_name,type,life_area,value,target,completed")
        data class Row(val date: java.time.LocalDate, val line: String)
        val rows = mutableListOf<Row>()
        users.forEach { user ->
            val userName = (userById[user.id]?.name ?: user.id.toString()).csvEscape()
            repository.getAllEntriesForUser(user.id).forEach { entry ->
                val habit = habitById[entry.habitId]
                val lifeArea = habit?.lifeAreaId?.let { lifeAreaById[it]?.name } ?: ""
                val value = when {
                    entry.booleanValue != null -> entry.booleanValue.toString()
                    entry.numericValue != null -> entry.numericValue.toString()
                    entry.textValue != null -> entry.textValue.csvEscape()
                    else -> ""
                }
                val target = habit?.targetValue?.toString() ?: ""
                val completed = when {
                    entry.booleanValue == true -> "true"
                    entry.numericValue != null && habit?.targetValue != null ->
                        (entry.numericValue >= habit.targetValue).toString()
                    else -> ""
                }
                rows += Row(entry.date, "${entry.date},$userName,${(habit?.name ?: entry.habitId.toString()).csvEscape()},${habit?.type?.name ?: "UNKNOWN"},${lifeArea.csvEscape()},$value,$target,$completed")
            }
        }
        rows.sortedByDescending { it.date }.forEach { sb.appendLine(it.line) }
        return sb.toString()
    }

    private suspend fun buildRoutinesCsv(repository: com.example.habitpower.data.HabitPowerRepository): String {
        val routines = repository.getAllRoutines().first()
        val sb = StringBuilder()
        sb.appendLine("routine_name,exercise,sets,reps,duration_s,order")
        routines.forEach { routine ->
            repository.getRoutineExercisesWithDetails(routine.id).first().forEach { item ->
                sb.appendLine("${routine.name.csvEscape()},${item.exercise.name.csvEscape()},${item.crossRef.sets ?: ""},${item.crossRef.reps ?: ""},${item.crossRef.durationSeconds ?: ""},${item.crossRef.order}")
            }
        }
        return sb.toString()
    }

    private suspend fun buildHealthCsv(repository: com.example.habitpower.data.HabitPowerRepository): String {
        val stats = repository.getAllHealthStats().first()
        val sb = StringBuilder()
        sb.appendLine("date,sleep_hours,steps")
        stats.sortedByDescending { it.date }.forEach { sb.appendLine("${it.date},${it.sleepHours},${it.stepsCount}") }
        return sb.toString()
    }
}

private fun Any.csvEscape(): String {
    val s = toString()
    return if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"${s.replace("\"", "\"\"")}\"" else s
}
