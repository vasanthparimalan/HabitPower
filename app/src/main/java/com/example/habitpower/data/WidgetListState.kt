package com.example.habitpower.data

data class WidgetListItem(
    val id: Long,
    val name: String,
    val isDone: Boolean,
    val isTaskItem: Boolean  // true = Task row, false = ChecklistItem row
)

sealed class WidgetListEntry {
    abstract val id: Long
    abstract val name: String
    abstract val items: List<WidgetListItem>
    abstract val isChecklist: Boolean

    data class TaskListEntry(
        override val id: Long,
        override val name: String,
        override val items: List<WidgetListItem>
    ) : WidgetListEntry() {
        override val isChecklist = false
    }

    data class ChecklistEntry(
        override val id: Long,
        override val name: String,
        override val items: List<WidgetListItem>
    ) : WidgetListEntry() {
        override val isChecklist = true
    }
}
