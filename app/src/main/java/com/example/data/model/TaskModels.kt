package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.CategoryCreative
import com.example.ui.theme.CategoryFinance
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryStudy
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.PriorityP1
import com.example.ui.theme.PriorityP2
import com.example.ui.theme.PriorityP3
import com.example.ui.theme.PriorityP4

enum class TaskPriority(val code: String, val displayName: String, val matrixQuadrant: String) {
    P1_CRITICAL("P1", "Critical", "Do First (Urgent & Important)"),
    P2_HIGH("P2", "High", "Schedule (Important, Not Urgent)"),
    P3_MEDIUM("P3", "Medium", "Delegate (Urgent, Not Important)"),
    P4_LOW("P4", "Low", "Eliminate (Neither)");

    val color: Color
        get() = when (this) {
            P1_CRITICAL -> PriorityP1
            P2_HIGH -> PriorityP2
            P3_MEDIUM -> PriorityP3
            P4_LOW -> PriorityP4
        }
}

enum class TaskCategory(val displayName: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    HEALTH("Health"),
    STUDY("Study"),
    FINANCE("Finance"),
    CREATIVE("Creative");

    val color: Color
        get() = when (this) {
            WORK -> CategoryWork
            PERSONAL -> CategoryPersonal
            HEALTH -> CategoryHealth
            STUDY -> CategoryStudy
            FINANCE -> CategoryFinance
            CREATIVE -> CategoryCreative
        }
}

data class Subtask(
    val title: String,
    val isDone: Boolean = false
)

@Entity(tableName = "planner_tasks")
data class PlannerTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val date: String = "", // Format: YYYY-MM-DD
    val orderIndex: Int = 0,
    val priority: TaskPriority = TaskPriority.P2_HIGH,
    val category: TaskCategory = TaskCategory.WORK,
    val startTime: String? = null, // e.g. "09:30"
    val durationMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val calendarEventId: Long? = null,
    val subtasksRaw: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getSubtasks(): List<Subtask> {
        if (subtasksRaw.isBlank()) return emptyList()
        return subtasksRaw.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split("|||")
                if (parts.size >= 2) {
                    Subtask(parts[0], parts[1].toBoolean())
                } else {
                    Subtask(parts[0], false)
                }
            }
    }

    companion object {
        fun encodeSubtasks(subtasks: List<Subtask>): String {
            return subtasks.joinToString("\n") { "${it.title}|||${it.isDone}" }
        }
    }
}

data class DayProgressSummary(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionPercentage: Float = 0f,
    val totalPlannedMinutes: Int = 0,
    val completedMinutes: Int = 0,
    val p1Count: Int = 0,
    val p1CompletedCount: Int = 0
)

data class DailyStat(
    val date: String,
    val dayLabel: String,
    val total: Int,
    val completed: Int,
    val isToday: Boolean
)

data class WeeklyProgressSummary(
    val startDate: String = "",
    val endDate: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionPercentage: Float = 0f,
    val dayStats: List<DailyStat> = emptyList(),
    val currentStreakDays: Int = 0
)

data class CategoryGoalProgress(
    val category: TaskCategory,
    val totalTasks: Int,
    val completedTasks: Int,
    val percentage: Float
)
