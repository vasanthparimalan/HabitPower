package com.example.habitpower.ui.routines

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.HabitPowerRepository
import com.example.habitpower.data.model.RoutineType
import com.example.habitpower.ui.AppViewModelProvider
import kotlinx.coroutines.launch

class ExecuteRoutineRouterViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitPowerRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: -1L
    var routineType by mutableStateOf<RoutineType?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId)
            routineType = routine?.type
            isLoading = false
        }
    }
}

@Composable
fun ExecuteRoutineScreen(
    navigateBack: () -> Unit,
    onRoutineComplete: () -> Unit,
    viewModel: ExecuteRoutineRouterViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    when {
        viewModel.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        viewModel.routineType == RoutineType.TIMED -> {
            ExecuteTimedRoutineScreen(
                navigateBack = navigateBack,
                onRoutineComplete = onRoutineComplete
            )
        }
        viewModel.routineType == RoutineType.NORMAL -> {
            // For now, navigate back or show a placeholder
            // In the future, this could be replaced with a NormalRoutineExecutionScreen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Normal routine execution coming soon")
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Routine not found")
            }
        }
    }
}
