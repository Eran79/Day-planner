package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlannerTask
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    @Query("SELECT * FROM planner_tasks WHERE date = :date ORDER BY orderIndex ASC, id ASC")
    fun getTasksForDate(date: String): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, orderIndex ASC")
    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks WHERE isCompleted = 1 ORDER BY coalesce(completedAt, createdAt) DESC")
    fun getCompletedTasksHistory(): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks ORDER BY date DESC, orderIndex ASC")
    fun getAllTasks(): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): PlannerTask?

    @Query("SELECT * FROM planner_tasks WHERE calendarEventId = :eventId LIMIT 1")
    suspend fun getTaskByCalendarEventId(eventId: Long): PlannerTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannerTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<PlannerTask>): List<Long>

    @Update
    suspend fun updateTask(task: PlannerTask)

    @Update
    suspend fun updateTasks(tasks: List<PlannerTask>)

    @Delete
    suspend fun deleteTask(task: PlannerTask)

    @Query("DELETE FROM planner_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE planner_tasks SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompletion(id: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE planner_tasks SET orderIndex = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: Long, newOrder: Int)

    @Query("UPDATE planner_tasks SET calendarEventId = :eventId WHERE id = :id")
    suspend fun updateCalendarEventId(id: Long, eventId: Long?)

    @Query("DELETE FROM planner_tasks WHERE isCompleted = 1")
    suspend fun clearCompletedTasks(): Int
}
