package com.example.habitpower.ui.execution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habitpower.data.model.Exercise

@Composable
internal fun ExerciseSpecsRow(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    extraSpecs: List<Pair<String, String>> = emptyList()
) {
    val specs = buildList {
        exercise.targetSets?.let { add("Sets" to it.toString()) }
        exercise.targetReps?.let { add("Reps" to it.toString()) }
        exercise.targetDurationSeconds?.let { add("Target" to formatExerciseTime(it)) }
        addAll(extraSpecs)
    }

    if (specs.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        specs.forEach { (label, value) ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "$label: $value",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun ExerciseInstructionBlock(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    val lines = exercise.instructions
        ?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.toList()
        .orEmpty()
        .ifEmpty {
            exercise.notes
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toList()
                .orEmpty()
        }

    if (lines.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "How to do it",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun NextUpCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

internal fun formatExerciseTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

internal fun estimateTimedRoutineSeconds(
    exercises: List<Exercise>,
    totalRounds: Int,
    restTimeSeconds: Int
): Int {
    if (exercises.isEmpty()) return 0
    val totalExerciseTime = exercises.sumOf { it.targetDurationSeconds ?: 60 } * totalRounds.coerceAtLeast(1)
    val restTransitionsPerRound = (exercises.size - 1).coerceAtLeast(0)
    val totalRestTransitions = restTransitionsPerRound * totalRounds.coerceAtLeast(1) +
        (totalRounds.coerceAtLeast(1) - 1)
    return totalExerciseTime + totalRestTransitions * restTimeSeconds.coerceAtLeast(0)
}

internal fun describeNextTimedStep(
    exercises: List<Exercise>,
    currentExerciseIndex: Int,
    currentRound: Int,
    totalRounds: Int
): String? {
    val nextExercise = exercises.getOrNull(currentExerciseIndex + 1)
    if (nextExercise != null) {
        return nextExercise.name
    }
    if (currentRound < totalRounds && exercises.isNotEmpty()) {
        return "Round ${currentRound + 1}: ${exercises.first().name}"
    }
    return null
}
