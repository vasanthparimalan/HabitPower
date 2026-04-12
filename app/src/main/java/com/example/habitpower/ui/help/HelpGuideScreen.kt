package com.example.habitpower.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.habitpower.ui.theme.SectionHeader

private data class GuideSection(
    val title: String,
    val points: List<String>
)

private data class GuideFlowChart(
    val title: String,
    val description: String,
    val lines: List<String>
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HelpGuideScreen(
    navigateBack: () -> Unit
) {
    val sections = listOf(
        GuideSection(
            title = "Quick Start",
            points = listOf(
                "Open Admin > Users to add people using this device.",
                "Open Admin > Habits to create habit definitions once, then reuse for all users.",
                "Open Admin > Assignments to pick habits and life areas for each user.",
                "Use Dashboard to switch user, review KPIs, and open Daily Check-In.",
                "Use Focus for timer/pomodoro habits that need deep-work sessions."
            )
        ),
        GuideSection(
            title = "Life Areas",
            points = listOf(
                "Life areas are pre-seeded so setup is faster: Health, Learning, Mindset, Work, Family.",
                "Each user can have different assigned life areas.",
                "Charts and KPI calculations are filtered by the user's assigned life areas.",
                "If a life area is not assigned for a user, it is excluded from that user's analytics scope."
            )
        ),
        GuideSection(
            title = "Gamification & Achievements",
            points = listOf(
                "Gain XP for every completed habit and extra XP for perfect days.",
                "Build streaks by completing all scheduled habits for consecutive days.",
                "Level up through tiers from Seeker to Enlightened.",
                "Unlock badges for milestones such as 7-day streaks, level milestones, and total completions.",
                "Use the Dashboard gamification card to track streak, level progress, and XP."
            )
        ),
        GuideSection(
            title = "Learning Path (First 30 Days)",
            points = listOf(
                "Week 1: Focus only on showing up daily. Keep goals small and easy.",
                "Week 2: Protect streak consistency and avoid missing two days in a row.",
                "Week 3: Improve quality by tuning reminders and life-area assignments.",
                "Week 4: Review weekly trends and remove any habit that feels noisy or vague.",
                "Use this app like a long-term friend: adjust slowly, stay honest, and keep momentum."
            )
        ),
        GuideSection(
            title = "Multiuser Workflow",
            points = listOf(
                "One shared device can support multiple people, including members without personal devices.",
                "Configure habits once, then assign tailored subsets per person.",
                "Switch active user from Dashboard before each check-in so entries stay separate.",
                "Use guided or assisted check-ins where needed (self, family, or coach-led).",
                "Use life-area assignments to keep each person's analytics focused and fair.",
                "Use quotes, streaks, badges, and celebration overlays for positive reinforcement."
            )
        ),
        GuideSection(
            title = "What You Can Achieve",
            points = listOf(
                "Create consistent morning and evening routines for any user profile.",
                "Track behavior trends over weeks using heatmaps and reports.",
                "Coach weaker life areas with focused assignments and review loops.",
                "Build accountability without nagging by combining reminders and game progress.",
                "Turn daily check-ins into a motivating, repeatable ritual."
            )
        )
    )

    val flowCharts = listOf(
        GuideFlowChart(
            title = "Core Relationship Flow",
            description = "How setup pieces connect so progress tracking stays accurate.",
            lines = listOf(
                "Create Person/User",
                "   -> assign Life Areas (Health, Learning, Family, etc.)",
                "      -> assign Habits under those areas",
                "         -> complete Daily Check-In entries",
                "            -> Dashboard + Report calculate streaks and consistency"
            )
        ),
        GuideFlowChart(
            title = "Daily Usage Flow",
            description = "A simple daily loop designed to keep momentum without overwhelm.",
            lines = listOf(
                "Open Dashboard",
                "   -> confirm active User",
                "      -> finish Today Tasks (or open Daily Check-In)",
                "         -> get XP, streak, and completion feedback",
                "            -> review weekly and life-area trends",
                "               -> adjust assignments in Admin when needed"
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guide & Help") },
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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "How To Use HabitPower",
                    subtitle = "Practical guidance for personal, family, and coaching use." 
                )
            }

            items(sections) { section ->
                GuideSectionCard(section = section)
            }

            item {
                SectionHeader(
                    title = "Flowcharts",
                    subtitle = "Quick visual map of setup and daily usage in HabitPower."
                )
            }

            items(flowCharts) { chart ->
                FlowChartCard(chart = chart)
            }
        }
    }
}

@Composable
private fun GuideSectionCard(section: GuideSection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium
            )
            section.points.forEach { point ->
                Text(
                    text = "• $point",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FlowChartCard(chart: GuideFlowChart) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = chart.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = chart.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            chart.lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
