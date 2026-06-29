package com.example.habitpower.ui.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.habitpower.HabitPowerApp
import com.example.habitpower.MainActivity
import com.example.habitpower.R
import com.example.habitpower.data.WidgetListEntry
import com.example.habitpower.data.WidgetListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull

private const val LIST_PREFS = "widget_list_prefs"
private const val KEY_INDEX = "list_index"

data class ListWidgetData(
    val entries: List<WidgetListEntry> = emptyList(),
    val index: Int = 0
) {
    val current: WidgetListEntry? get() = entries.getOrNull(index)
    val total: Int get() = entries.size
}

private val LW = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color.White,
    night = androidx.compose.ui.graphics.Color.White
)
private val LG = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF9E9E9E),
    night = androidx.compose.ui.graphics.Color(0xFF9E9E9E)
)
private val LGreen = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF4CAF50),
    night = androidx.compose.ui.graphics.Color(0xFF4CAF50)
)
private val LDarkBg = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF141414),
    night = androidx.compose.ui.graphics.Color(0xFF141414)
)
private val LRowBg = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    night = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
)
private val LDoneBg = androidx.glance.color.ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF1A2B1A),
    night = androidx.compose.ui.graphics.Color(0xFF1A2B1A)
)

val widgetListEntryIdKey = ActionParameters.Key<Long>("widgetListEntryId")
val widgetItemIdKey = ActionParameters.Key<Long>("widgetItemId")

class HabitChecklistWidget : GlanceAppWidget() {

    companion object {
        private val _data = MutableStateFlow(ListWidgetData())
        val data: StateFlow<ListWidgetData> = _data.asStateFlow()

        /** Loads all entries from DB and updates the StateFlow. */
        suspend fun refreshData(context: Context) {
            val repo = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
            val user = try { repo.getResolvedActiveUser().firstOrNull() } catch (_: Exception) { null }
            val entries: List<WidgetListEntry> = if (user != null) {
                try { repo.getWidgetListEntries(user.id) } catch (_: Exception) { emptyList() }
            } else emptyList()
            val prefs = context.getSharedPreferences(LIST_PREFS, Context.MODE_PRIVATE)
            val saved = prefs.getInt(KEY_INDEX, 0)
            val index = if (entries.isEmpty()) 0 else saved.coerceIn(0, entries.size - 1)
            _data.value = ListWidgetData(entries = entries, index = index)
        }

        /** Advances or retreats the current list index and persists it. */
        fun navigate(context: Context, direction: Int) {
            val current = _data.value
            if (current.total == 0) return
            val newIndex = when {
                direction > 0 -> if (current.index >= current.total - 1) 0 else current.index + 1
                else -> if (current.index <= 0) current.total - 1 else current.index - 1
            }
            context.getSharedPreferences(LIST_PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_INDEX, newIndex).apply()
            _data.value = current.copy(index = newIndex)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        refreshData(context)
        provideContent {
            val state by data.collectAsState()
            GlanceTheme {
                ListWidgetLayout(state = state)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ListWidgetLayout(state: ListWidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(LDarkBg)
                .padding(12.dp)
        ) {
            if (state.total == 0) {
                EmptyState()
                return@Column
            }

            val entry = state.current

            // ── Header ─────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_prev),
                    contentDescription = "Previous",
                    modifier = GlanceModifier.size(28.dp).padding(2.dp).clickable(
                        actionRunCallback<WidgetListPrevCallback>()
                    )
                )
                Text(
                    text = entry?.name ?: "",
                    style = TextStyle(color = LW, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_next),
                    contentDescription = "Next",
                    modifier = GlanceModifier.size(28.dp).padding(2.dp).clickable(
                        actionRunCallback<WidgetListNextCallback>()
                    )
                )
                // Reset button — checklists only
                if (entry?.isChecklist == true) {
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_refresh),
                        contentDescription = "Reset checklist",
                        modifier = GlanceModifier.size(28.dp).padding(4.dp).clickable(
                            actionRunCallback<ResetWidgetChecklistCallback>(
                                actionParametersOf(widgetListEntryIdKey to entry.id)
                            )
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(2.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_checkin),
                    contentDescription = "Open app",
                    modifier = GlanceModifier.size(28.dp).padding(4.dp).clickable(
                        actionStartActivity<MainActivity>()
                    )
                )
            }

            // ── Status line ─────────────────────────────────────────────
            val items = entry?.items ?: emptyList()
            val doneCount = items.count { it.isDone }
            Spacer(modifier = GlanceModifier.height(3.dp))
            val allDone = items.isNotEmpty() && doneCount == items.size
            val statusText = when {
                items.isEmpty() -> "${state.index + 1} of ${state.total}"
                allDone -> "All done ✓  ·  ${state.index + 1} of ${state.total}"
                else -> "$doneCount / ${items.size} done  ·  ${state.index + 1} of ${state.total}"
            }
            Text(
                text = statusText,
                style = TextStyle(color = if (allDone) LGreen else LG, fontSize = 10.sp)
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // ── Items ───────────────────────────────────────────────────
            if (items.isEmpty()) {
                Text(text = "No items", style = TextStyle(color = LG, fontSize = 12.sp))
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    itemsIndexed(items) { _, item ->
                        ListItemRow(item = item)
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ListItemRow(item: WidgetListItem) {
        val isDone = item.isDone
        val toggleAction = if (item.isTaskItem) {
            actionRunCallback<ToggleWidgetTaskCallback>(
                actionParametersOf(widgetItemIdKey to item.id)
            )
        } else {
            actionRunCallback<ToggleWidgetChecklistItemCallback>(
                actionParametersOf(widgetItemIdKey to item.id)
            )
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .background(if (isDone) LDoneBg else LRowBg)
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .clickable(toggleAction),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isDone) "✓" else "○",
                style = TextStyle(
                    color = if (isDone) LGreen else LW,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = item.name,
                style = TextStyle(color = if (isDone) LG else LW, fontSize = 12.sp),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun EmptyState() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "No lists yet", style = TextStyle(color = LG, fontSize = 13.sp))
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Add a list or checklist in the app",
                style = TextStyle(color = LG, fontSize = 11.sp)
            )
        }
    }
}

// ── ActionCallbacks ──────────────────────────────────────────────────────────

class WidgetListPrevCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // refreshData first: _data is empty after process death, navigate() would no-op otherwise
        HabitChecklistWidget.refreshData(context)
        HabitChecklistWidget.navigate(context, -1)
        HabitChecklistWidget().update(context, glanceId)
    }
}

class WidgetListNextCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HabitChecklistWidget.refreshData(context)
        HabitChecklistWidget.navigate(context, +1)
        HabitChecklistWidget().update(context, glanceId)
    }
}

class ToggleWidgetTaskCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[widgetItemIdKey] ?: return
        try {
            val repo = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
            repo.toggleTaskById(itemId)
        } catch (_: Exception) {}
        HabitChecklistWidget().update(context, glanceId)
    }
}

class ToggleWidgetChecklistItemCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[widgetItemIdKey] ?: return
        try {
            val repo = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
            repo.toggleChecklistItemById(itemId)
        } catch (_: Exception) {}
        HabitChecklistWidget().update(context, glanceId)
    }
}

class ResetWidgetChecklistCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val checklistId = parameters[widgetListEntryIdKey] ?: return
        try {
            val repo = (context.applicationContext as HabitPowerApp).container.habitPowerRepository
            repo.resetChecklist(checklistId)
        } catch (_: Exception) {}
        HabitChecklistWidget().update(context, glanceId)
    }
}

class RefreshListWidgetCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        HabitChecklistWidget().update(context, glanceId)
    }
}
