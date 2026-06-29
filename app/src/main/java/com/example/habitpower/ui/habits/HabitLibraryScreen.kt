package com.example.habitpower.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.HabitTemplate
import com.example.habitpower.data.model.HabitType
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitLibraryScreen(
    navigateBack: () -> Unit,
    viewModel: HabitLibraryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    val selectedArchetype = uiState.selectedArchetype
    val visible = viewModel.templates.filter { template ->
        selectedArchetype == null || template.archetype == selectedArchetype
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Habit Library") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Start with 2–3 habits, not all of them.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "New to habits? Start with Human — the biological foundation every person needs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    // Archetype filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedArchetype == null,
                                onClick = { viewModel.setArchetype(null) },
                                label = { Text("All  ${viewModel.templates.size}") }
                            )
                        }
                        items(HabitTemplate.Archetype.entries) { archetype ->
                            val count = viewModel.templates.count { it.archetype == archetype }
                            FilterChip(
                                selected = selectedArchetype == archetype,
                                onClick = {
                                    viewModel.setArchetype(
                                        if (selectedArchetype == archetype) null else archetype
                                    )
                                },
                                label = {
                                    Text("${archetypeEmoji(archetype)} ${archetype.label}  $count")
                                }
                            )
                        }
                    }
                }
            }

            // ── Archetype context banner ────────────────────────────────────────
            if (selectedArchetype != null) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${archetypeEmoji(selectedArchetype)} ${selectedArchetype.label}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = selectedArchetype.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // ── Habit cards — grouped by archetype when showing All ─────────────
            if (selectedArchetype == null) {
                HabitTemplate.Archetype.entries.forEach { archetype ->
                    val group = visible.filter { it.archetype == archetype }
                    if (group.isEmpty()) return@forEach
                    item(key = "header_${archetype.name}") {
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = 16.dp, bottom = 4.dp
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = archetypeEmoji(archetype),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = archetype.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = archetype.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(group, key = { it.name }) { template ->
                        HabitLibraryCard(
                            template = template,
                            isAdded = template.name.lowercase() in uiState.addedNames,
                            isAdding = uiState.isAdding,
                            onAdd = { viewModel.addTemplate(template) }
                        )
                    }
                }
            } else {
                items(visible, key = { it.name }) { template ->
                    HabitLibraryCard(
                        template = template,
                        isAdded = template.name.lowercase() in uiState.addedNames,
                        isAdding = uiState.isAdding,
                        onAdd = { viewModel.addTemplate(template) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitLibraryCard(
    template: HabitTemplate,
    isAdded: Boolean,
    isAdding: Boolean,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "I am someone who ${template.identityStatement}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (isAdded) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Added", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                } else {
                    Button(
                        onClick = onAdd,
                        enabled = !isAdding,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TypeChip(template.type)
                val target = templateTargetLabel(template)
                if (target != null) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(target, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            commitmentLabel(template.commitmentHour, template.commitmentMinute),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TypeChip(type: HabitType) {
    val label = when (type) {
        HabitType.BOOLEAN  -> "Yes / No"
        HabitType.DURATION -> "Duration"
        HabitType.COUNT    -> "Count"
        HabitType.POMODORO -> "Pomodoro"
        HabitType.TEXT     -> "Journal"
        HabitType.TIMER    -> "Timer"
        HabitType.TIME     -> "Time"
        HabitType.ROUTINE  -> "Routine"
        HabitType.NUMBER   -> "Number"
    }
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

private fun templateTargetLabel(template: HabitTemplate): String? {
    val v = template.targetValue ?: return null
    return when (template.type) {
        HabitType.DURATION -> {
            val mins = v.toInt()
            if (mins >= 60) {
                val h = mins / 60
                val m = mins % 60
                if (m == 0) "${h}h" else "${h}h ${m}m"
            } else "${mins}m"
        }
        HabitType.COUNT    -> "${v.toInt()} ${template.unit ?: ""}".trim()
        HabitType.POMODORO -> "${v.toInt()} sessions"
        else               -> null
    }
}

private fun commitmentLabel(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "am" else "pm"
    return "$h:${minute.toString().padStart(2, '0')} $amPm"
}

private fun archetypeEmoji(archetype: HabitTemplate.Archetype) = when (archetype) {
    HabitTemplate.Archetype.HUMAN   -> "🌿" // 🌿
    HabitTemplate.Archetype.BUILDER -> "🏗" // 🏗
    HabitTemplate.Archetype.ATHLETE -> "⚡"        // ⚡
    HabitTemplate.Archetype.STUDENT -> "📚" // 📚
    HabitTemplate.Archetype.MINDFUL -> "🧘" // 🧘
}
