package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DateStrip
import com.example.ui.components.TaskEditDialog
import com.example.ui.screens.CalendarSyncScreen
import com.example.ui.screens.DayPriorityScreen
import com.example.ui.screens.MatrixScreen
import com.example.ui.screens.ProgressTrackingScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.DayWiseTheme
import com.example.ui.viewmodel.PlannerTab
import com.example.ui.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayWiseTheme {
                DayWiseApp()
            }
        }
    }
}

@Composable
fun DayWiseApp(
    viewModel: PlannerViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val reorderedOverride by viewModel.reorderedListOverride.collectAsStateWithLifecycle()
    val dayProgress by viewModel.dayProgress.collectAsStateWithLifecycle()
    val weeklyProgress by viewModel.weeklyProgress.collectAsStateWithLifecycle()
    val categoryGoals by viewModel.categoryGoals.collectAsStateWithLifecycle()
    val completedHistory by viewModel.completedHistory.collectAsStateWithLifecycle()
    val allTasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()

    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()

    val availableCalendars by viewModel.availableCalendars.collectAsStateWithLifecycle()
    val selectedCalendarId by viewModel.selectedCalendarId.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            snackbarHostState.showSnackbar(syncMessage ?: "")
            viewModel.clearSyncMessage()
        }
    }

    val displayTasks = reorderedOverride ?: tasks

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DateStrip(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == PlannerTab.TODAY_PRIORITY,
                    onClick = { viewModel.setTab(PlannerTab.TODAY_PRIORITY) },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Priority Tasks") },
                    label = { Text("Priority") },
                    modifier = Modifier.testTag("nav_tab_priority"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == PlannerTab.TIMELINE,
                    onClick = { viewModel.setTab(PlannerTab.TIMELINE) },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Timeline") },
                    label = { Text("Timeline") },
                    modifier = Modifier.testTag("nav_tab_timeline"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == PlannerTab.MATRIX,
                    onClick = { viewModel.setTab(PlannerTab.MATRIX) },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Matrix") },
                    label = { Text("Matrix") },
                    modifier = Modifier.testTag("nav_tab_matrix"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == PlannerTab.PROGRESS,
                    onClick = { viewModel.setTab(PlannerTab.PROGRESS) },
                    icon = { Icon(Icons.Default.DonutLarge, contentDescription = "Progress") },
                    label = { Text("Progress") },
                    modifier = Modifier.testTag("nav_tab_progress"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == PlannerTab.CALENDAR_SYNC,
                    onClick = { viewModel.setTab(PlannerTab.CALENDAR_SYNC) },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
                    label = { Text("Sync") },
                    modifier = Modifier.testTag("nav_tab_sync"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentTab != PlannerTab.CALENDAR_SYNC,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { viewModel.openAddTask() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_task")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                PlannerTab.TODAY_PRIORITY -> {
                    DayPriorityScreen(
                        tasks = displayTasks,
                        progress = dayProgress,
                        onToggleCompleted = { viewModel.toggleTaskCompletion(it) },
                        onTaskClick = { viewModel.openEditTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onToggleSubtask = { task, index -> viewModel.toggleSubtask(task, index) },
                        onPriorityChange = { task, prio -> viewModel.updateTaskPriority(task, prio) },
                        onReorderCommitted = { reordered -> viewModel.commitReorder(reordered) },
                        onAddTask = { viewModel.openAddTask() }
                    )
                }

                PlannerTab.TIMELINE -> {
                    TimelineScreen(
                        tasks = displayTasks,
                        selectedDate = selectedDate,
                        onToggleCompleted = { viewModel.toggleTaskCompletion(it) },
                        onTaskClick = { viewModel.openEditTask(it) },
                        onSlotClick = { hourSlot -> viewModel.openAddTask(presetTime = hourSlot) }
                    )
                }

                PlannerTab.MATRIX -> {
                    MatrixScreen(
                        tasks = displayTasks,
                        onToggleCompleted = { viewModel.toggleTaskCompletion(it) },
                        onTaskClick = { viewModel.openEditTask(it) },
                        onAddWithPriority = { prio -> viewModel.openAddTask(presetPriority = prio) }
                    )
                }

                PlannerTab.PROGRESS -> {
                    ProgressTrackingScreen(
                        dayProgress = dayProgress,
                        weeklyProgress = weeklyProgress,
                        categoryGoals = categoryGoals,
                        completedHistory = completedHistory,
                        onRestoreTask = { viewModel.toggleTaskCompletion(it) },
                        onClearHistory = { viewModel.clearCompletedHistory() }
                    )
                }

                PlannerTab.CALENDAR_SYNC -> {
                    CalendarSyncScreen(
                        selectedDate = selectedDate,
                        availableCalendars = availableCalendars,
                        selectedCalendarId = selectedCalendarId,
                        isSyncing = isSyncing,
                        syncMessage = syncMessage,
                        dayTasks = tasks,
                        allTasks = allTasks,
                        onSelectCalendar = { viewModel.selectCalendar(it) },
                        onSyncDayToCalendar = { viewModel.syncDayToCalendar() },
                        onImportFromCalendar = { viewModel.importFromDeviceCalendar() },
                        onImportIcsText = { viewModel.importIcsFileContent(it) },
                        onRefreshCalendars = { viewModel.refreshAvailableCalendars() },
                        onDismissMessage = { viewModel.clearSyncMessage() }
                    )
                }
            }
        }

        // Add / Edit Task Dialog Sheet
        if (isAddEditOpen && editingTask != null) {
            TaskEditDialog(
                initialTask = editingTask!!,
                onSave = { task, syncToCal -> viewModel.saveTask(task, syncToCal) },
                onDismiss = { viewModel.dismissAddEdit() }
            )
        }
    }
}
