package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calendar.IcsCalendarHelper
import com.example.data.model.PlannerTask
import com.example.data.model.Subtask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DayWise Planner", appName)
    }

    @Test
    fun `subtasks encoding and decoding works correctly`() {
        val originalSubtasks = listOf(
            Subtask("Step 1", false),
            Subtask("Step 2", true)
        )
        val encoded = PlannerTask.encodeSubtasks(originalSubtasks)
        val task = PlannerTask(
            title = "Test Task",
            date = "2026-10-03",
            subtasksRaw = encoded
        )
        val decoded = task.getSubtasks()
        assertEquals(2, decoded.size)
        assertEquals("Step 1", decoded[0].title)
        assertFalse(decoded[0].isDone)
        assertEquals("Step 2", decoded[1].title)
        assertTrue(decoded[1].isDone)
    }

    @Test
    fun `ics export and parse roundtrip preserves tasks`() {
        val tasks = listOf(
            PlannerTask(
                id = 1,
                title = "Team Standup",
                notes = "Daily sync",
                date = "2026-10-03",
                priority = TaskPriority.P1_CRITICAL,
                category = TaskCategory.WORK,
                startTime = "10:00",
                durationMinutes = 30
            )
        )
        val ics = IcsCalendarHelper.generateIcs(tasks)
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("Team Standup"))

        val parsed = IcsCalendarHelper.parseIcsContent(ics, "2026-10-03")
        assertEquals(1, parsed.size)
        assertEquals("Team Standup", parsed[0].title)
        assertEquals(TaskPriority.P1_CRITICAL, parsed[0].priority)
    }
}
