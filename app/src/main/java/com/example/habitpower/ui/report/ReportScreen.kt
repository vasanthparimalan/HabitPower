package com.example.habitpower.ui.report

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.ui.AppViewModelProvider
import com.example.habitpower.ui.theme.SectionHeader
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navigateBack: (() -> Unit)? = null,
    onNavigateToYearInReview: (() -> Unit)? = null,
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val state = viewModel.uiState

    fun showDatePicker(initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    if (navigateBack != null) {
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Headline card ──────────────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.headline,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = state.subheadline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        Text(
                            text = "User: ${state.activeUser?.name ?: "No active user"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Period selector (pinned near top — controls everything below) ──
            item {
                PeriodSelector(
                    selectedPeriod = state.selectedPeriod,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    onSelectPeriod = viewModel::selectPeriod,
                    onUpdateStartDate = viewModel::updateStartDate,
                    onUpdateEndDate = viewModel::updateEndDate,
                    showDatePicker = ::showDatePicker
                )
            }

            // ── Year in Review entry point ─────────────────────────────────────
            if (onNavigateToYearInReview != null) {
                item {
                    YearInReviewBanner(
                        year = java.time.LocalDate.now().year,
                        onClick = onNavigateToYearInReview
                    )
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.emptyMessage != null) {
                item {
                    Card {
                        Text(
                            text = state.emptyMessage,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // ══ SNAPSHOT ══════════════════════════════════════════════════
                item {
                    SectionHeader(
                        title = "Snapshot",
                        subtitle = "Where you stand in the selected period"
                    )
                }

                // ── KPIs ───────────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.kpis.forEach { kpi ->
                            AnalyticsKpiCard(
                                label = kpi.label,
                                value = kpi.value,
                                subtitle = kpi.supportingText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── Life area gauges ───────────────────────────────────────────
                if (state.lifeAreaGauges.isNotEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Life Areas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                state.lifeAreaGauges.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { gauge ->
                                            LifeAreaGaugeCard(gauge = gauge, modifier = Modifier.weight(1f))
                                        }
                                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Encouragement cards (strongest + weakest area) ─────────────
                if (state.strongestAreaMessage != null || state.weakestAreaMessage != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.strongestAreaMessage?.let { message ->
                                EncouragementCard(
                                    title = "What is working",
                                    message = message,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                            state.weakestAreaMessage?.let { message ->
                                EncouragementCard(
                                    title = "Next breakthrough",
                                    message = message,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }
                        }
                    }
                }

                // ── Divider between Snapshot and Trends ────────────────────────
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                // ══ TRENDS ════════════════════════════════════════════════════
                item {
                    SectionHeader(
                        title = "Trends",
                        subtitle = "How your habits are moving over time"
                    )
                }

                // ── Trend chart ────────────────────────────────────────────────
                if (state.weeklyTrend.isNotEmpty()) {
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val isDaily = state.weeklyTrend.size <= 14
                                Text(
                                    text = if (isDaily) "Daily Completion" else "Week-by-Week Completion",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TrendBarChart(
                                    trends = state.weeklyTrend,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── Habit breakdown ────────────────────────────────────────────
                if (state.habitConsistency.isNotEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Habit Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lowest consistency first — your next focus is at the top",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                state.habitConsistency.forEach { habit ->
                                    HabitConsistencyRow(habit = habit)
                                }
                            }
                        }
                    }
                }

                // ── Footer ─────────────────────────────────────────────────────
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            text = "Your report is a mirror, not a verdict. Build one better week, then another, then another. That is how multi-year transformation quietly happens.",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    startDate: LocalDate,
    endDate: LocalDate,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onUpdateStartDate: (LocalDate) -> Unit,
    onUpdateEndDate: (LocalDate) -> Unit,
    showDatePicker: (LocalDate, (LocalDate) -> Unit) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Period", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(ReportPeriod.DAYS_7, ReportPeriod.DAYS_30, ReportPeriod.DAYS_90, ReportPeriod.CUSTOM)
                    .forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            label = { Text(period.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
            }
            if (selectedPeriod == ReportPeriod.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker(startDate, onUpdateStartDate) }
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(startDate.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker(endDate, onUpdateEndDate) }
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(endDate.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendBarChart(
    trends: List<WeekTrend>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (trends.isEmpty()) return@Canvas
            val count = trends.size
            val gap = 4.dp.toPx()
            val totalGaps = gap * (count - 1)
            val barWidth = (size.width - totalGaps) / count
            val maxBarHeight = size.height - 2.dp.toPx()

            trends.forEachIndexed { index, trend ->
                val ratio = trend.completionRatio.coerceIn(0f, 1f)
                val barHeight = (ratio * maxBarHeight).coerceAtLeast(4.dp.toPx())
                val left = index * (barWidth + gap)
                val barColor = when {
                    ratio >= 0.8f -> primaryColor
                    ratio >= 0.5f -> tertiaryColor
                    else -> errorColor
                }
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // Filled bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-axis labels — thin out if too many bars
        val labelIndices: Set<Int> = when {
            trends.size <= 7 -> trends.indices.toSet()
            trends.size <= 12 -> trends.indices.filter { it % 2 == 0 }.toSet()
            else -> setOf(0, trends.size / 2, trends.size - 1)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            trends.forEachIndexed { index, trend ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (index in labelIndices) {
                        Text(
                            text = trend.weekLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitConsistencyRow(habit: HabitConsistency) {
    val barColor = when {
        habit.consistencyRatio >= 0.8f -> MaterialTheme.colorScheme.primary
        habit.consistencyRatio >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val pct = "${(habit.consistencyRatio * 100).toInt()}%"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.habitName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = pct,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(trackColor, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(habit.consistencyRatio.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(3.dp))
            )
        }
        Text(
            text = "${habit.completedCount} of ${habit.totalDays} days",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnalyticsKpiCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LifeAreaGaugeCard(
    gauge: LifeAreaGauge,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (gauge.emoji != null) {
                Text(
                    text = gauge.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Text(
                text = gauge.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            GaugeChart(
                progress = gauge.completionRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Text(
                text = "${(gauge.completionRatio * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${gauge.completedCount} / ${gauge.totalCount} completed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = gauge.encouragement,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GaugeChart(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val baseArcColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val progressArcColor = when {
        progress >= 0.8f -> MaterialTheme.colorScheme.primary
        progress >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            drawArc(
                color = baseArcColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressArcColor,
                startAngle = 180f,
                sweepAngle = 180f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun EncouragementCard(
    title: String,
    message: String,
    containerColor: Color
) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun YearInReviewBanner(year: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$year — Year in Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "See your full-year stats, best streak, and personal headline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
