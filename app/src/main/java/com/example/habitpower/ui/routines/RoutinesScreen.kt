package com.example.habitpower.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.Exercise
import com.example.habitpower.data.model.Routine
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.exercises.ExerciseItem
import com.example.habitpower.ui.exercises.ExercisesViewModel
import com.example.habitpower.ui.navigation.Screen

enum class RoutinesSection {
    ROUTINES,
    EXERCISES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onNavigate: (String) -> Unit,
    initialSection: RoutinesSection = RoutinesSection.ROUTINES,
    viewModel: RoutinesViewModel = viewModel(factory = AppViewModelProvider.Factory),
    exercisesViewModel: ExercisesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val routines by viewModel.routines.collectAsState()
    val exercises by exercisesViewModel.exercises.collectAsState()
    var selectedTabIndex by rememberSaveable(initialSection) {
        mutableIntStateOf(if (initialSection == RoutinesSection.ROUTINES) 0 else 1)
    }
    val selectedSection = remember(selectedTabIndex) {
        if (selectedTabIndex == 0) RoutinesSection.ROUTINES else RoutinesSection.EXERCISES
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Routines") })
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Routines") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Exercises") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedSection) {
                        RoutinesSection.ROUTINES -> onNavigate(Screen.AddEditRoutine.createRoute())
                        RoutinesSection.EXERCISES -> onNavigate(Screen.AddEditExercise.createRoute())
                    }
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (selectedSection == RoutinesSection.ROUTINES) {
                        "Add Routine"
                    } else {
                        "Add Exercise"
                    }
                )
            }
        }
    ) { innerPadding ->
        when (selectedSection) {
            RoutinesSection.ROUTINES -> RoutinesList(
                modifier = Modifier
                    .padding(innerPadding),
                routines = routines,
                getExerciseCount = viewModel::getExerciseCount,
                onNavigate = onNavigate,
                onDeleteRoutine = viewModel::deleteRoutine
            )

            RoutinesSection.EXERCISES -> ExercisesList(
                modifier = Modifier.padding(innerPadding),
                exercises = exercises,
                onNavigate = onNavigate,
                onDeleteExercise = exercisesViewModel::deleteExercise
            )
        }
    }
}

@Composable
private fun RoutinesList(
    modifier: Modifier = Modifier,
    routines: List<Routine>,
    getExerciseCount: (Long) -> kotlinx.coroutines.flow.Flow<Int>,
    onNavigate: (String) -> Unit,
    onDeleteRoutine: (Routine) -> Unit
) {
    if (routines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No routines yet.\nTap + to create your first routine.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(routines, key = { it.id }) { routine ->
                val exerciseCount by getExerciseCount(routine.id).collectAsState(initial = 0)
                RoutineItem(
                    routine = routine,
                    exerciseCount = exerciseCount,
                    onItemClick = { onNavigate(Screen.AddEditRoutine.createRoute(routine.id)) },
                    onDeleteClick = { onDeleteRoutine(routine) },
                    onPlayClick = { onNavigate(Screen.ExecuteRoutine.createRoute(routine.id)) }
                )
            }
        }
    }
}

@Composable
private fun ExercisesList(
    modifier: Modifier = Modifier,
    exercises: List<Exercise>,
    onNavigate: (String) -> Unit,
    onDeleteExercise: (Exercise) -> Unit
) {
    if (exercises.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No exercises yet.\nTap + to create your first exercise.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(exercises, key = { it.id }) { exercise ->
                ExerciseItem(
                    exercise = exercise,
                    onItemClick = { onNavigate(Screen.AddEditExercise.createRoute(exercise.id)) },
                    onDeleteClick = { onDeleteExercise(exercise) }
                )
            }
        }
    }
}

@Composable
fun RoutineItem(
    routine: Routine,
    exerciseCount: Int,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = routine.name, style = MaterialTheme.typography.titleMedium)
                if (routine.description.isNotBlank()) {
                    Text(text = routine.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(
                    text = "$exerciseCount exercise${if (exerciseCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (exerciseCount == 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            IconButton(onClick = onPlayClick, enabled = exerciseCount > 0) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Execute Routine",
                    tint = if (exerciseCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
