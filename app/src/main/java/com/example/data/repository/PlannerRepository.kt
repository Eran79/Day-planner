package com.example.data.repository

import android.content.Context
import com.example.calendar.CalendarManager
import com.example.calendar.DeviceCalendarInfo
import com.example.calendar.IcsCalendarHelper
import com.example.data.local.PlannerDao
import com.example.data.model.CategoryGoalProgress
import com.example.data.model.DailyStat
import com.example.data.model.DayProgressSummary
import com.example.data.model.PlannerTask
import com.example.data.model.Subtask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.WeeklyProgressSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlannerRepository(
    private val plannerDao: PlannerDao,
    private val context: Context
) {
    fun getTasksForDate(date: String): Flow<List<PlannerTask>> =
        plannerDao.getTasksForDate(date)

    fun getCompletedHistory(): Flow<List<PlannerTask>> =
        plannerDao.getCompletedTasksHistory()

    fun getAllTasks(): Flow<List<PlannerTask>> =
        plannerDao.getAllTasks()

    suspend fun insertTask(task: PlannerTask): Long {
        return plannerDao.insertTask(task)
    }

    suspend fun updateTask(task: PlannerTask) {
        plannerDao.updateTask(task)
    }

    suspend fun deleteTask(task: PlannerTask) {
        if (task.calendarEventId != null) {
            CalendarManager.deleteTaskFromCalendar(context, task.calendarEventId)
        }
        plannerDao.deleteTask(task)
    }

    suspend fun toggleTaskCompletion(task: PlannerTask) {
        val newCompleted = !task.isCompleted
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        plannerDao.updateCompletion(task.id, newCompleted, completedAt)

        // If synced with calendar, update event status
        if (task.calendarEventId != null) {
            val updated = task.copy(isCompleted = newCompleted, completedAt = completedAt)
            // find primary or first calendar
            val calendars = CalendarManager.getAvailableCalendars(context)
            val primary = calendars.firstOrNull { it.isPrimary } ?: calendars.firstOrNull()
            if (primary != null) {
                CalendarManager.syncTaskToCalendar(context, primary.id, updated)
            }
        }
    }

    suspend fun reorderTasks(tasks: List<PlannerTask>) {
        val updatedList = tasks.mapIndexed { index, task ->
            task.copy(orderIndex = index)
        }
        plannerDao.updateTasks(updatedList)
    }

    suspend fun clearCompletedHistory() {
        plannerDao.clearCompletedTasks()
    }

    /**
     * Calculates day progress summary
     */
    fun getDayProgress(date: String): Flow<DayProgressSummary> {
        return plannerDao.getTasksForDate(date).map { tasks ->
            if (tasks.isEmpty()) {
                DayProgressSummary()
            } else {
                val total = tasks.size
                val completed = tasks.count { it.isCompleted }
                val percent = (completed.toFloat() / total) * 100f
                val plannedMins = tasks.sumOf { it.durationMinutes }
                val completedMins = tasks.filter { it.isCompleted }.sumOf { it.durationMinutes }
                val p1 = tasks.count { it.priority == TaskPriority.P1_CRITICAL }
                val p1Done = tasks.count { it.priority == TaskPriority.P1_CRITICAL && it.isCompleted }

                DayProgressSummary(
                    totalTasks = total,
                    completedTasks = completed,
                    completionPercentage = percent,
                    totalPlannedMinutes = plannedMins,
                    completedMinutes = completedMins,
                    p1Count = p1,
                    p1CompletedCount = p1Done
                )
            }
        }
    }

    /**
     * Calculates weekly progress and daily bars for the 7 days centered on/leading to targetDate
     */
    fun getWeeklyProgress(anchorDate: String): Flow<WeeklyProgressSummary> {
        val (startDate, endDate, dayDates) = computeWeekRange(anchorDate)
        return plannerDao.getTasksForDateRange(startDate, endDate).map { tasks ->
            val total = tasks.size
            val completed = tasks.count { it.isCompleted }
            val percent = if (total > 0) (completed.toFloat() / total) * 100f else 0f

            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val dailyStats = dayDates.map { dateStr ->
                val dateTasks = tasks.filter { it.date == dateStr }
                val dDate = try { dateParser.parse(dateStr) } catch (_: Exception) { Date() }
                val label = dayFormat.format(dDate ?: Date())
                DailyStat(
                    date = dateStr,
                    dayLabel = label,
                    total = dateTasks.size,
                    completed = dateTasks.count { it.isCompleted },
                    isToday = dateStr == todayStr
                )
            }

            // Simple streak calculation: how many consecutive past days had at least 1 completed task
            var streak = 0
            for (stat in dailyStats.reversed()) {
                if (stat.completed > 0) {
                    streak++
                } else if (!stat.isToday) {
                    break
                }
            }

            WeeklyProgressSummary(
                startDate = startDate,
                endDate = endDate,
                totalTasks = total,
                completedTasks = completed,
                completionPercentage = percent,
                dayStats = dailyStats,
                currentStreakDays = streak
            )
        }
    }

    /**
     * Calculates category / project goals progress for selected date
     */
    fun getCategoryGoals(date: String): Flow<List<CategoryGoalProgress>> {
        return plannerDao.getTasksForDate(date).map { tasks ->
            TaskCategory.values().mapNotNull { category ->
                val catTasks = tasks.filter { it.category == category }
                if (catTasks.isNotEmpty()) {
                    val completed = catTasks.count { it.isCompleted }
                    CategoryGoalProgress(
                        category = category,
                        totalTasks = catTasks.size,
                        completedTasks = completed,
                        percentage = (completed.toFloat() / catTasks.size) * 100f
                    )
                } else null
            }
        }
    }

    suspend fun syncTasksToDeviceCalendar(calendarId: Long, date: String): Int {
        val tasks = plannerDao.getTasksForDate(date).first()
        var synced = 0
        for (task in tasks) {
            val eventId = CalendarManager.syncTaskToCalendar(context, calendarId, task)
            if (eventId != null) {
                if (task.calendarEventId != eventId) {
                    plannerDao.updateCalendarEventId(task.id, eventId)
                }
                synced++
            }
        }
        return synced
    }

    suspend fun importFromDeviceCalendar(calendarId: Long, date: String): Int {
        val imported = CalendarManager.importEventsForDate(context, calendarId, date)
        if (imported.isEmpty()) return 0

        val existing = plannerDao.getTasksForDate(date).first()
        val existingEventIds = existing.mapNotNull { it.calendarEventId }.toSet()
        val existingTitles = existing.map { it.title.trim().lowercase() }.toSet()

        val newTasks = imported.filter {
            (it.calendarEventId == null || !existingEventIds.contains(it.calendarEventId)) &&
                    !existingTitles.contains(it.title.trim().lowercase())
        }

        if (newTasks.isNotEmpty()) {
            val startOrder = existing.size
            val indexed = newTasks.mapIndexed { i, t -> t.copy(orderIndex = startOrder + i) }
            plannerDao.insertTasks(indexed)
        }
        return newTasks.size
    }

    suspend fun importIcsContent(icsContent: String, date: String): Int {
        val tasks = IcsCalendarHelper.parseIcsContent(icsContent, date)
        if (tasks.isEmpty()) return 0
        val existing = plannerDao.getTasksForDate(date).first()
        val startOrder = existing.size
        val indexed = tasks.mapIndexed { i, t -> t.copy(orderIndex = startOrder + i) }
        plannerDao.insertTasks(indexed)
        return indexed.size
    }

    suspend fun seedSampleDataIfEmpty(date: String) {
        val current = plannerDao.getTasksForDate(date).first()
        if (current.isNotEmpty()) return

        val sampleTasks = listOf(
            PlannerTask(
                title = "Review product roadmap & sprint goals",
                notes = "Align team deliverables with Q4 milestones and review user feedback tickets.",
                date = date,
                orderIndex = 0,
                priority = TaskPriority.P1_CRITICAL,
                category = TaskCategory.WORK,
                startTime = "09:00",
                durationMinutes = 60,
                isCompleted = true,
                completedAt = System.currentTimeMillis() - 7200000,
                subtasksRaw = PlannerTask.encodeSubtasks(
                    listOf(
                        Subtask("Audit backlog tickets", true),
                        Subtask("Verify latency benchmarks", true),
                        Subtask("Prepare team standup brief", true)
                    )
                )
            ),
            PlannerTask(
                title = "Design drag-and-drop interaction polish",
                notes = "Refine drag elevation, haptic response, and smooth swap transitions.",
                date = date,
                orderIndex = 1,
                priority = TaskPriority.P1_CRITICAL,
                category = TaskCategory.CREATIVE,
                startTime = "10:30",
                durationMinutes = 45,
                isCompleted = false,
                subtasksRaw = PlannerTask.encodeSubtasks(
                    listOf(
                        Subtask("Verify pointer gesture handling", true),
                        Subtask("Implement item offset springs", false),
                        Subtask("Add drop target indicator", false)
                    )
                )
            ),
            PlannerTask(
                title = "Sync calendar events with device schedule",
                notes = "Test CalendarContract two-way updates and universal .ics export.",
                date = date,
                orderIndex = 2,
                priority = TaskPriority.P2_HIGH,
                category = TaskCategory.WORK,
                startTime = "13:00",
                durationMinutes = 30,
                isCompleted = false,
                subtasksRaw = PlannerTask.encodeSubtasks(
                    listOf(
                        Subtask("Verify READ_CALENDAR permissions", true),
                        Subtask("Test cross-platform RFC 5545 export", false)
                    )
                )
            ),
            PlannerTask(
                title = "Afternoon cardio & hydration break",
                notes = "30-minute interval workout or outdoor brisk walk for energy reset.",
                date = date,
                orderIndex = 3,
                priority = TaskPriority.P3_MEDIUM,
                category = TaskCategory.HEALTH,
                startTime = "15:00",
                durationMinutes = 30,
                isCompleted = true,
                completedAt = System.currentTimeMillis() - 3600000
            ),
            PlannerTask(
                title = "Review Kotlin Compose performance guide",
                notes = "Chapter on stability, derivedStateOf, and memory allocation tips.",
                date = date,
                orderIndex = 4,
                priority = TaskPriority.P2_HIGH,
                category = TaskCategory.STUDY,
                startTime = "16:30",
                durationMinutes = 45,
                isCompleted = false
            ),
            PlannerTask(
                title = "Organize monthly subscription expenses",
                notes = "Cancel unused cloud trials and categorize invoice receipts.",
                date = date,
                orderIndex = 5,
                priority = TaskPriority.P4_LOW,
                category = TaskCategory.FINANCE,
                startTime = null,
                durationMinutes = 20,
                isCompleted = false
            )
        )

        plannerDao.insertTasks(sampleTasks)
    }

    private fun computeWeekRange(dateStr: String): Triple<String, String, List<String>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try { sdf.parse(dateStr) } catch (_: Exception) { Date() } ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val days = mutableListOf<String>()
        for (i in 0 until 7) {
            days.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return Triple(days.first(), days.last(), days)
    }
}
