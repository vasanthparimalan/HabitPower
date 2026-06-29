package com.example.habitpower.ui.standup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.gamification.GrowthProjection
import com.example.habitpower.ui.AppViewModelProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfStandupScreen(
    navigateBack: () -> Unit,
    onNavigateToAdminHabits: () -> Unit,
    viewModel: SelfStandupViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    fun toggle(key: String) { expanded[key] = !(expanded.getOrElse(key) { key == state.smartExpandedKey }) }
    fun isExpanded(key: String) = expanded.getOrElse(key) { key == state.smartExpandedKey }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Self Standup", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Your personal review meeting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Header ────────────────────────────────────────────────────────
            item { HeaderCard(userName = state.userName) }

            // ── Daily intention ───────────────────────────────────────────────
            item {
                DailyIntentionCard(
                    intention = state.dailyIntention,
                    onSave = { viewModel.saveDailyIntention(it) }
                )
            }

            // ── Identity mirror ───────────────────────────────────────────────
            if (state.identityStatements.isNotEmpty()) {
                item {
                    SectionCard(
                        icon = "✦",
                        title = "Identity Mirror",
                        subtitle = "Who you are becoming based on the last 30 days",
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        expanded = isExpanded("identity"),
                        onToggle = { toggle("identity") }
                    ) {
                        state.identityStatements.forEach { sentence ->
                            InsightRow(text = sentence, isIdentity = true)
                        }
                    }
                }
            }

            // ── Weekly ────────────────────────────────────────────────────────
            state.weekly?.let { tf ->
                item {
                    TimeframeCard(
                        timeframe = tf,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        savedCommitment = state.commitments["weekly"] ?: "",
                        onSaveCommitment = { viewModel.saveCommitment("weekly", it) },
                        expanded = isExpanded("weekly"),
                        onToggle = { toggle("weekly") }
                    )
                }
            }

            // ── Monthly ───────────────────────────────────────────────────────
            state.monthly?.let { tf ->
                item {
                    TimeframeCard(
                        timeframe = tf,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        savedCommitment = state.commitments["monthly"] ?: "",
                        onSaveCommitment = { viewModel.saveCommitment("monthly", it) },
                        expanded = isExpanded("monthly"),
                        onToggle = { toggle("monthly") }
                    )
                }
            }

            // ── Quarterly ─────────────────────────────────────────────────────
            state.quarterly?.let { tf ->
                item {
                    TimeframeCard(
                        timeframe = tf,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        onColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        savedCommitment = state.commitments["quarterly"] ?: "",
                        onSaveCommitment = { viewModel.saveCommitment("quarterly", it) },
                        expanded = isExpanded("quarterly"),
                        onToggle = { toggle("quarterly") }
                    )
                }
            }

            // ── Yearly ────────────────────────────────────────────────────────
            state.yearly?.let { tf ->
                item {
                    TimeframeCard(
                        timeframe = tf,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        onColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        savedCommitment = state.commitments["yearly"] ?: "",
                        onSaveCommitment = { viewModel.saveCommitment("yearly", it) },
                        expanded = isExpanded("yearly"),
                        onToggle = { toggle("yearly") }
                    )
                }
            }

            // ── Empty state when user has no habits ───────────────────────────
            if (!state.hasHabits) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("No habits to review yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Add habits and start tracking to unlock your Weekly, Monthly, and Growth reviews.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── 5-Year growth path ────────────────────────────────────────────
            if (state.hasHabits) item {
                SectionCard(
                    icon = "🚀",
                    title = "5-Year Growth Path",
                    subtitle = "Where your current habits take you by ${LocalDate.now().year + 5}",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    expanded = isExpanded("5year"),
                    onToggle = { toggle("5year") }
                ) {
                    val onColor = MaterialTheme.colorScheme.onSecondaryContainer
                    MeetingMetaRow(FIVEYEAR_MEETING, onColor)
                    ObjectiveRow(FIVEYEAR_MEETING.objective, onColor)
                    Spacer(Modifier.height(4.dp))
                    state.fiveYearInsights.forEach { InsightRow(text = it) }
                    if (state.growthProjections.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "At your current pace:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onColor
                        )
                        Spacer(Modifier.height(4.dp))
                        state.growthProjections.forEach { proj ->
                            GrowthProjectionRow(proj = proj)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    ReflectionPrompt(prompt = state.fiveYearReflection)
                    Spacer(Modifier.height(4.dp))
                    CommitmentCard(
                        commitmentPrompt = FIVEYEAR_MEETING.commitmentPrompt,
                        savedCommitment = state.commitments["fiveyear"] ?: "",
                        onSave = { viewModel.saveCommitment("fiveyear", it) },
                        onColor = onColor
                    )
                }
            }

            // ── 10-Year vision ────────────────────────────────────────────────
            if (state.hasHabits) item {
                SectionCard(
                    icon = "🌌",
                    title = "10-Year Vision",
                    subtitle = "The person you're becoming by ${LocalDate.now().year + 10}",
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    expanded = isExpanded("10year"),
                    onToggle = { toggle("10year") }
                ) {
                    val onColor = MaterialTheme.colorScheme.onSurfaceVariant
                    MeetingMetaRow(TENYEAR_MEETING, onColor)
                    ObjectiveRow(TENYEAR_MEETING.objective, onColor)
                    Spacer(Modifier.height(4.dp))
                    if (state.growthProjections.isNotEmpty()) {
                        val top = state.growthProjections.first()
                        InsightRow(
                            text = "10 years of \"${top.habitName}\" at ${top.completionPercent}% = ~${top.tenYearSessions} sessions. " +
                                   "That is mastery, not a habit."
                        )
                    }
                    InsightRow(text = "In 10 years you won't remember why you hesitated. You'll only know who you became.")
                    Spacer(Modifier.height(4.dp))
                    ReflectionPrompt(prompt = state.tenYearReflection)
                    Spacer(Modifier.height(4.dp))
                    CommitmentCard(
                        commitmentPrompt = TENYEAR_MEETING.commitmentPrompt,
                        savedCommitment = state.commitments["tenyear"] ?: "",
                        onSave = { viewModel.saveCommitment("tenyear", it) },
                        onColor = onColor
                    )
                }
            }

            // ── Decisions ─────────────────────────────────────────────────────
            if (state.hasHabits) item {
                SectionCard(
                    icon = "✅",
                    title = "Decisions",
                    subtitle = "What your data is asking you to act on",
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    onColor = MaterialTheme.colorScheme.onErrorContainer,
                    expanded = isExpanded("decisions"),
                    onToggle = { toggle("decisions") }
                ) {
                    val onColor = MaterialTheme.colorScheme.onErrorContainer
                    MeetingMetaRow(DECISIONS_MEETING, onColor)
                    ObjectiveRow(DECISIONS_MEETING.objective, onColor)
                    Spacer(Modifier.height(4.dp))
                    state.decisionsInsights.forEach { InsightRow(text = it) }
                    if (state.graduateCandidates.isNotEmpty() || state.retireCandidates.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onNavigateToAdminHabits) {
                            Text("Open Habit Manager →", color = onColor)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    CommitmentCard(
                        commitmentPrompt = DECISIONS_MEETING.commitmentPrompt,
                        savedCommitment = state.commitments["decisions"] ?: "",
                        onSave = { viewModel.saveCommitment("decisions", it) },
                        onColor = onColor
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (userName.isNotBlank()) "Good to see you, $userName." else "Personal Review",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = LocalDate.now().format(DATE_FMT),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "Each section below is a meeting with yourself. Read the objective, sit with the reflection question, then write your commitment before moving on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

// ── Daily intention card ──────────────────────────────────────────────────────

@Composable
private fun DailyIntentionCard(
    intention: String,
    onSave: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf(intention) }
    LaunchedEffect(intention) { dialogText = intention }

    val onColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☀️", style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Today's Intention",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = onColor.copy(alpha = 0.6f)
                )
                if (intention.isNotBlank()) {
                    Text(
                        text = intention,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = onColor
                    )
                } else {
                    Text(
                        text = "What will you bring your full attention to today?",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = onColor.copy(alpha = 0.5f)
                    )
                }
            }
            Icon(
                imageVector = if (intention.isNotBlank()) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint = onColor.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Today's Intention") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What will you bring your full attention to today?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = dialogText,
                        onValueChange = { dialogText = it },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write your intention here…") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(dialogText.trim())
                    showDialog = false
                }) { Text("Set Intention") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Timeframe card ────────────────────────────────────────────────────────────

@Composable
private fun TimeframeCard(
    timeframe: StandupTimeframe,
    color: Color,
    onColor: Color,
    savedCommitment: String,
    onSaveCommitment: (String) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    SectionCard(
        icon = timeframe.icon,
        title = timeframe.label,
        subtitle = when {
            timeframe.hasData -> "${timeframe.consistencyPercent}% consistency · ${timeframe.meeting.duration}"
            timeframe.daysUntilUnlock != null -> "${timeframe.daysUntilUnlock}d more to unlock"
            else -> "Not enough data yet"
        },
        color = color,
        onColor = onColor,
        expanded = expanded,
        onToggle = onToggle,
        trailingBadge = when {
            timeframe.hasData -> "${timeframe.consistencyPercent}%"
            timeframe.daysUntilUnlock != null -> "${timeframe.daysUntilUnlock}d"
            else -> null
        },
        enabled = timeframe.hasData
    ) {
        MeetingMetaRow(timeframe.meeting, onColor)
        ObjectiveRow(timeframe.meeting.objective, onColor)
        Spacer(Modifier.height(6.dp))

        if (!timeframe.hasData) {
            timeframe.insights.forEach { InsightRow(text = it, onColor = onColor) }
            return@SectionCard
        }

        LinearProgressIndicator(
            progress = { (timeframe.consistencyPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = onColor.copy(alpha = 0.8f),
            trackColor = onColor.copy(alpha = 0.15f)
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            timeframe.topHabit?.let { MiniChip(label = "Best habit", value = it, onColor = onColor) }
            timeframe.topLifeArea?.let { MiniChip(label = "Best area", value = it, onColor = onColor) }
        }

        Spacer(Modifier.height(8.dp))
        timeframe.insights.forEach { InsightRow(text = it, onColor = onColor) }
        Spacer(Modifier.height(6.dp))
        ReflectionPrompt(prompt = timeframe.reflectionPrompt, onColor = onColor)
        Spacer(Modifier.height(6.dp))
        CommitmentCard(
            commitmentPrompt = timeframe.meeting.commitmentPrompt,
            savedCommitment = savedCommitment,
            onSave = onSaveCommitment,
            onColor = onColor
        )
    }
}

// ── Section card (collapsible) ────────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: String,
    title: String,
    subtitle: String,
    color: Color,
    onColor: Color = MaterialTheme.colorScheme.onSurface,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailingBadge: String? = null,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val cardColor = if (enabled) color else color.copy(alpha = 0.35f)
    val textColor = if (enabled) onColor else onColor.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (enabled) Modifier.clickable { onToggle() } else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, style = MaterialTheme.typography.titleMedium)
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.75f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingBadge?.let {
                        Box(
                            modifier = Modifier
                                .background(textColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                    Icon(
                        imageVector = when {
                            !enabled -> Icons.Default.Lock
                            expanded -> Icons.Default.ExpandLess
                            else -> Icons.Default.ExpandMore
                        },
                        contentDescription = when {
                            !enabled -> "Locked"
                            expanded -> "Collapse"
                            else -> "Expand"
                        },
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = enabled && expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = content
                )
            }
        }
    }
}

// ── Meeting metadata ──────────────────────────────────────────────────────────

@Composable
private fun MeetingMetaRow(meeting: StandupMeeting, onColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(onColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = meeting.type,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = onColor
            )
        }
        Text(
            text = "⏱ ${meeting.duration}",
            style = MaterialTheme.typography.labelSmall,
            color = onColor.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ObjectiveRow(objective: String, onColor: Color) {
    Text(
        text = objective,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = onColor.copy(alpha = 0.75f)
    )
}

// ── Commitment card ───────────────────────────────────────────────────────────

@Composable
private fun CommitmentCard(
    commitmentPrompt: String,
    savedCommitment: String,
    onSave: (String) -> Unit,
    onColor: Color
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf(savedCommitment) }

    LaunchedEffect(savedCommitment) { dialogText = savedCommitment }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(onColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Commitment",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = onColor.copy(alpha = 0.6f)
        )
        Text(
            text = commitmentPrompt,
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.8f)
        )
        if (savedCommitment.isNotBlank()) {
            Text(
                text = "→ $savedCommitment",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = onColor
            )
        }
        TextButton(
            onClick = { showDialog = true },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (savedCommitment.isBlank()) "Add commitment" else "Edit commitment",
                style = MaterialTheme.typography.labelMedium,
                color = onColor
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Your commitment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = commitmentPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = dialogText,
                        onValueChange = { dialogText = it },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write your commitment here…") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(dialogText.trim())
                    showDialog = false
                }) { Text("Commit") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun InsightRow(
    text: String,
    isIdentity: Boolean = false,
    onColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (isIdentity) "✦" else "•",
            style = MaterialTheme.typography.bodySmall,
            color = onColor.copy(alpha = 0.6f)
        )
        Text(
            text = text,
            style = if (isIdentity) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontStyle = if (isIdentity) FontStyle.Italic else FontStyle.Normal,
            color = onColor,
            fontWeight = if (isIdentity) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun ReflectionPrompt(
    prompt: String,
    onColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(onColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Reflect",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = onColor.copy(alpha = 0.6f)
        )
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = onColor
        )
    }
}

@Composable
private fun GrowthProjectionRow(proj: GrowthProjection) {
    val onColor = MaterialTheme.colorScheme.onSecondaryContainer
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = proj.habitName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = onColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${proj.completionPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = onColor.copy(alpha = 0.7f)
            )
        }
        LinearProgressIndicator(
            progress = { (proj.completionPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = onColor.copy(alpha = 0.7f),
            trackColor = onColor.copy(alpha = 0.15f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "5 yr: ~${proj.fiveYearSessions} sessions",
                style = MaterialTheme.typography.labelSmall,
                color = onColor.copy(alpha = 0.75f)
            )
            Text(
                text = "10 yr: ~${proj.tenYearSessions} sessions",
                style = MaterialTheme.typography.labelSmall,
                color = onColor.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun MiniChip(label: String, value: String, onColor: Color) {
    Box(
        modifier = Modifier
            .background(onColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = onColor.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = onColor,
                maxLines = 1
            )
        }
    }
}
