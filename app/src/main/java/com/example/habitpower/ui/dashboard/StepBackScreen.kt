package com.example.habitpower.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.reminder.StepBackReturnReceiver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class StepBackDuration(val label: String, val weeks: Long?) {
    ONE_WEEK("1 Week", 1),
    ONE_MONTH("1 Month", 4),
    THREE_MONTHS("3 Months", 13),
    OPEN_ENDED("Open-ended", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepBackScreen(
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as HabitPowerApp).container.userPreferencesRepository
    val scope = rememberCoroutineScope()

    val isActive by prefs.stepBackActive.collectAsState(initial = false)
    val returnEpochDay by prefs.stepBackReturnEpochDay.collectAsState(initial = null)

    var selectedDuration by remember { mutableStateOf(StepBackDuration.ONE_MONTH) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Step Back") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isActive) {
                ActiveStepBackContent(
                    returnEpochDay = returnEpochDay,
                    onReturn = {
                        scope.launch {
                            StepBackReturnReceiver.cancel(context)
                            prefs.setStepBack(false, null)
                            navigateBack()
                        }
                    }
                )
            } else {
                ActivationContent(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { selectedDuration = it },
                    onConfirm = {
                        scope.launch {
                            val returnDate = selectedDuration.weeks?.let { LocalDate.now().plusWeeks(it) }
                            val epochDay = returnDate?.toEpochDay()
                            prefs.setStepBack(true, epochDay)
                            if (epochDay != null) {
                                StepBackReturnReceiver.schedule(context, epochDay)
                            }
                            navigateBack()
                        }
                    },
                    onCancel = navigateBack
                )
            }
        }
    }
}

@Composable
private fun ActiveStepBackContent(
    returnEpochDay: Long?,
    onReturn: () -> Unit
) {
    val returnDate = returnEpochDay?.let { LocalDate.ofEpochDay(it) }
    val formatter = DateTimeFormatter.ofPattern("MMMM d")

    Icon(
        Icons.Default.Pause,
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Text(
        "Your practice is resting.",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Text(
        if (returnDate != null)
            "You'll be welcomed back on ${returnDate.format(formatter)}."
        else
            "You'll be welcomed back whenever you're ready.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(8.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            "All reminders are silenced. Your data is fully preserved — nothing will be lost or reset while you're away.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = onReturn,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Return to my practice")
    }
}

@Composable
private fun ActivationContent(
    selectedDuration: StepBackDuration,
    onDurationSelected: (StepBackDuration) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Icon(
        Icons.Default.Pause,
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Text(
        "Step Back for a While",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Text(
        "Grief, illness, travel, life — sometimes other things need your full attention. " +
            "Your practice will be here when you return.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Return in:", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepBackDuration.entries.take(2).forEach { duration ->
                FilterChip(
                    selected = selectedDuration == duration,
                    onClick = { onDurationSelected(duration) },
                    label = { Text(duration.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepBackDuration.entries.drop(2).forEach { duration ->
                FilterChip(
                    selected = selectedDuration == duration,
                    onClick = { onDurationSelected(duration) },
                    label = { Text(duration.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val returnDate = selectedDuration.weeks?.let {
        LocalDate.now().plusWeeks(it).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }
    if (returnDate != null) {
        Text(
            "You'll receive a gentle notification on $returnDate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    } else {
        Text(
            "No return date set. Open the app whenever you're ready.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Begin Step-Back")
    }

    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Cancel")
    }
}
