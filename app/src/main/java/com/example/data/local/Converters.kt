package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority

class Converters {
    @TypeConverter
    fun fromPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.P2_HIGH.name
    }

    @TypeConverter
    fun toPriority(value: String?): TaskPriority {
        return try {
            if (value != null) TaskPriority.valueOf(value) else TaskPriority.P2_HIGH
        } catch (_: Exception) {
            TaskPriority.P2_HIGH
        }
    }

    @TypeConverter
    fun fromCategory(category: TaskCategory?): String {
        return category?.name ?: TaskCategory.WORK.name
    }

    @TypeConverter
    fun toCategory(value: String?): TaskCategory {
        return try {
            if (value != null) TaskCategory.valueOf(value) else TaskCategory.WORK
        } catch (_: Exception) {
            TaskCategory.WORK
        }
    }
}
