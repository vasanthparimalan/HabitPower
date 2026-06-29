package com.example.habitpower.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitpower.data.model.Checklist
import com.example.habitpower.data.model.ChecklistItem
import com.example.habitpower.data.model.Task
import com.example.habitpower.data.model.TaskList
import com.example.habitpower.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val taskLists by viewModel.taskLists.collectAsState()
    val checklists by viewModel.checklists.collectAsState()
    val userId by viewModel.userId.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddNamedItemDialog(
            title = if (selectedTab == 0) "New Task List" else "New Checklist",
            label = "Name",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                if (selectedTab == 0) viewModel.addTaskList(name)
                else viewModel.addChecklist(name)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Tasks", style = MaterialTheme.typography.titleLarge)
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Add task list" else "Add checklist")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Task Lists") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Checklists") })
            }

            if (selectedTab == 0) {
                TaskListsContent(
                    taskLists = taskLists,
                    _userId = userId,
                    viewModel = viewModel
                )
            } else {
                ChecklistsContent(
                    checklists = checklists,
                    _userId = userId,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun TaskListsContent(
    taskLists: List<TaskList>,
    _userId: Long,
    viewModel: TasksViewModel
) {
    if (taskLists.isEmpty()) {
        EmptyState("No task lists yet.\nTap + to create one.")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(taskLists, key = { it.id }) { list ->
            TaskListCard(list = list, viewModel = viewModel)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TaskListCard(list: TaskList, viewModel: TasksViewModel) {
    val tasks by viewModel.getTasksForList(list.id).collectAsState(initial = emptyList())
    var expanded by rememberSaveable(list.id) { mutableStateOf(true) }
    var addTaskText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                val done = tasks.count { it.isDone }
                if (tasks.isNotEmpty()) {
                    Text(
                        text = "$done/${tasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { viewModel.deleteTaskList(list) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete list",
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            if (expanded) {
                if (tasks.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    tasks.forEach { task ->
                        TaskRow(task = task, viewModel = viewModel)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = addTaskText,
                        onValueChange = { addTaskText = it },
                        placeholder = { Text("Add task…") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.addTask(list.id, addTaskText)
                            addTaskText = ""
                        },
                        enabled = addTaskText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add task")
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, viewModel: TasksViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { viewModel.toggleTask(task) }
        )
        Text(
            text = task.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = { viewModel.deleteTask(task) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete task",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChecklistsContent(
    checklists: List<Checklist>,
    _userId: Long,
    viewModel: TasksViewModel
) {
    if (checklists.isEmpty()) {
        EmptyState("No checklists yet.\nTap + to create one.")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(checklists, key = { it.id }) { checklist ->
            ChecklistCard(checklist = checklist, viewModel = viewModel)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ChecklistCard(checklist: Checklist, viewModel: TasksViewModel) {
    val items by viewModel.getItemsForChecklist(checklist.id).collectAsState(initial = emptyList())
    var addItemText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(checklist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (checklist.resetsDaily) {
                        Text("Resets daily", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val checked = items.count { it.isChecked }
                if (items.isNotEmpty()) {
                    Text("$checked/${items.size}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.resetChecklist(checklist) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset checklist")
                }
                IconButton(onClick = { viewModel.deleteChecklist(checklist) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete checklist",
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            if (items.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                items.forEach { item ->
                    ChecklistItemRow(item = item, viewModel = viewModel)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = addItemText,
                    onValueChange = { addItemText = it },
                    placeholder = { Text("Add item…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.addChecklistItem(checklist.id, addItemText)
                        addItemText = ""
                    },
                    enabled = addItemText.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(item: ChecklistItem, viewModel: TasksViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { viewModel.toggleChecklistItem(item) }
        )
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = { viewModel.deleteChecklistItem(item) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AddNamedItemDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
