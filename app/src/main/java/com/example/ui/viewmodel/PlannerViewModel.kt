package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.CalendarManager
import com.example.calendar.DeviceCalendarInfo
import com.example.calendar.IcsCalendarHelper
import com.example.data.local.PlannerDatabase
import com.example.data.model.CategoryGoalProgress
import com.example.data.model.DayProgressSummary
import com.example.data.model.PlannerTask
import com.example.data.model.Subtask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.WeeklyProgressSummary
import com.example.data.repository.PlannerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PlannerTab(val title: String) {
    TODAY_PRIORITY("Priority"),
    TIMELINE("Timeline"),
    MATRIX("Matrix"),
    PROGRESS("Progress"),
    CALENDAR_SYNC("Sync")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlannerRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _currentTab = MutableStateFlow(PlannerTab.TODAY_PRIORITY)
    val currentTab: StateFlow<PlannerTab> = _currentTab.asStateFlow()

    // Task edit dialog state
    private val _editingTask = MutableStateFlow<PlannerTask?>(null)
    val editingTask: StateFlow<PlannerTask?> = _editingTask.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    // Calendar sync states
    private val _availableCalendars = MutableStateFlow<List<DeviceCalendarInfo>>(emptyList())
    val availableCalendars: StateFlow<List<DeviceCalendarInfo>> = _availableCalendars.asStateFlow()

    private val _selectedCalendarId = MutableStateFlow<Long?>(null)
    val selectedCalendarId: StateFlow<Long?> = _selectedCalendarId.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Drag-and-drop local preview state
    private val _reorderedListOverride = MutableStateFlow<List<PlannerTask>?>(null)
    val reorderedListOverride: StateFlow<List<PlannerTask>?> = _reorderedListOverride.asStateFlow()

    init {
        val database = PlannerDatabase.getDatabase(application)
        repository = PlannerRepository(database.plannerDao(), application)

        // Seed initial sample data for today if empty
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty(_selectedDate.value)
            refreshAvailableCalendars()
        }
    }

    val tasksForSelectedDate: StateFlow<List<PlannerTask>> = _selectedDate
        .flatMapLatest { date -> repository.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayProgress: StateFlow<DayProgressSummary> = _selectedDate
        .flatMapLatest { date -> repository.getDayProgress(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayProgressSummary())

    val weeklyProgress: StateFlow<WeeklyProgressSummary> = _selectedDate
        .flatMapLatest { date -> repository.getWeeklyProgress(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyProgressSummary())

    val categoryGoals: StateFlow<List<CategoryGoalProgress>> = _selectedDate
        .flatMapLatest { date -> repository.getCategoryGoals(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedHistory: StateFlow<List<PlannerTask>> = repository.getCompletedHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        _reorderedListOverride.value = null
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty(date)
        }
    }

    fun setTab(tab: PlannerTab) {
        _currentTab.value = tab
    }

    fun openAddTask(presetTime: String? = null, presetPriority: TaskPriority? = null) {
        _editingTask.value = PlannerTask(
            date = _selectedDate.value,
            startTime = presetTime,
            priority = presetPriority ?: TaskPriority.P2_HIGH
        )
        _isAddEditOpen.value = true
    }

    fun openEditTask(task: PlannerTask) {
        _editingTask.value = task
        _isAddEditOpen.value = true
    }

    fun dismissAddEdit() {
        _isAddEditOpen.value = false
        _editingTask.value = null
    }

    fun saveTask(task: PlannerTask, syncToCalendar: Boolean = false) {
        viewModelScope.launch {
            val taskId = if (task.id == 0L) {
                // Determine orderIndex as end of current tasks
                val currentTasks = tasksForSelectedDate.value
                val nextOrder = if (currentTasks.isNotEmpty()) currentTasks.maxOf { it.orderIndex } + 1 else 0
                repository.insertTask(task.copy(orderIndex = nextOrder))
            } else {
                repository.updateTask(task)
                task.id
            }

            if (syncToCalendar) {
                val calId = _selectedCalendarId.value ?: _availableCalendars.value.firstOrNull()?.id
                if (calId != null) {
                    val saved = task.copy(id = taskId)
                    val eventId = CalendarManager.syncTaskToCalendar(getApplication(), calId, saved)
                    if (eventId != null) {
                        repository.updateTask(saved.copy(calendarEventId = eventId))
                    }
                }
            }

            _isAddEditOpen.value = false
            _editingTask.value = null
        }
    }

    fun deleteTask(task: PlannerTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: PlannerTask) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    fun toggleSubtask(task: PlannerTask, subtaskIndex: Int) {
        viewModelScope.launch {
            val subtasks = task.getSubtasks().toMutableList()
            if (subtaskIndex in subtasks.indices) {
                val current = subtasks[subtaskIndex]
                subtasks[subtaskIndex] = current.copy(isDone = !current.isDone)
                val allDone = subtasks.all { it.isDone }
                val updatedTask = task.copy(
                    subtasksRaw = PlannerTask.encodeSubtasks(subtasks),
                    isCompleted = if (allDone) true else task.isCompleted,
                    completedAt = if (allDone && !task.isCompleted) System.currentTimeMillis() else task.completedAt
                )
                repository.updateTask(updatedTask)
            }
        }
    }

    fun updateTaskPriority(task: PlannerTask, newPriority: TaskPriority) {
        viewModelScope.launch {
            repository.updateTask(task.copy(priority = newPriority))
        }
    }

    /**
     * Called during drag-and-drop to update immediate local UI
     */
    fun updateLocalReorder(tasks: List<PlannerTask>) {
        _reorderedListOverride.value = tasks
    }

    /**
     * Called on drop release to persist the new order to Room DB
     */
    fun commitReorder(tasks: List<PlannerTask>) {
        viewModelScope.launch {
            repository.reorderTasks(tasks)
            _reorderedListOverride.value = null
        }
    }

    fun clearCompletedHistory() {
        viewModelScope.launch {
            repository.clearCompletedHistory()
            _syncMessage.value = "Completed history cleared"
        }
    }

    // Calendar sync methods
    fun refreshAvailableCalendars() {
        val list = CalendarManager.getAvailableCalendars(getApplication())
        _availableCalendars.value = list
        if (_selectedCalendarId.value == null && list.isNotEmpty()) {
            _selectedCalendarId.value = list.firstOrNull { it.isPrimary }?.id ?: list.first().id
        }
    }

    fun selectCalendar(calendarId: Long) {
        _selectedCalendarId.value = calendarId
    }

    fun syncDayToCalendar() {
        val calId = _selectedCalendarId.value ?: _availableCalendars.value.firstOrNull()?.id
        if (calId == null) {
            _syncMessage.value = "No calendar available or permission missing."
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val count = repository.syncTasksToDeviceCalendar(calId, _selectedDate.value)
            _isSyncing.value = false
            _syncMessage.value = "Successfully synced $count tasks to device calendar!"
        }
    }

    fun importFromDeviceCalendar() {
        val calId = _selectedCalendarId.value ?: _availableCalendars.value.firstOrNull()?.id
        if (calId == null) {
            _syncMessage.value = "No calendar selected."
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            val count = repository.importFromDeviceCalendar(calId, _selectedDate.value)
            _isSyncing.value = false
            _syncMessage.value = if (count > 0) "Imported $count events from device calendar!" else "No new events found for this day."
        }
    }

    fun importIcsFileContent(icsText: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val count = repository.importIcsContent(icsText, _selectedDate.value)
            _isSyncing.value = false
            _syncMessage.value = if (count > 0) "Imported $count tasks from .ics calendar!" else "Could not find valid events in .ics file."
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
