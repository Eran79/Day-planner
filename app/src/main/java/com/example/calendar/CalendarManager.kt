package com.example.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.PlannerTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object CalendarManager {
    private const val TAG = "CalendarManager"

    fun hasPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun getAvailableCalendars(context: Context): List<DeviceCalendarInfo> {
        if (!hasPermissions(context)) return emptyList()

        val calendars = mutableListOf<DeviceCalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val primaryIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val name = it.getString(nameIdx) ?: "Default Calendar"
                    val accName = it.getString(accNameIdx) ?: "Local"
                    val accType = it.getString(accTypeIdx) ?: "device"
                    val color = it.getInt(colorIdx)
                    val isPrimary = it.getInt(primaryIdx) == 1

                    calendars.add(
                        DeviceCalendarInfo(
                            id = id,
                            displayName = name,
                            accountName = accName,
                            accountType = accType,
                            color = color,
                            isPrimary = isPrimary
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendars", e)
        }

        return calendars
    }

    fun syncTaskToCalendar(context: Context, calendarId: Long, task: PlannerTask): Long? {
        if (!hasPermissions(context)) return null

        try {
            val (startMillis, endMillis) = calculateEventTime(task.date, task.startTime, task.durationMinutes)
            val timeZone = TimeZone.getDefault().id

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "[DayWise] ${task.title}")
                put(
                    CalendarContract.Events.DESCRIPTION,
                    "${task.notes}\n\nCategory: ${task.category.displayName}\nPriority: ${task.priority.code}"
                )
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
                put(
                    CalendarContract.Events.STATUS,
                    if (task.isCompleted) CalendarContract.Events.STATUS_CONFIRMED else CalendarContract.Events.STATUS_TENTATIVE
                )
            }

            // If task already has a calendarEventId, try updating it first
            if (task.calendarEventId != null) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, task.calendarEventId)
                val rows = context.contentResolver.update(updateUri, values, null, null)
                if (rows > 0) {
                    return task.calendarEventId
                }
            }

            // Otherwise, insert new event
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val newId = uri?.lastPathSegment?.toLongOrNull()
            return newId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync task to calendar: ${task.title}", e)
            return null
        }
    }

    fun deleteTaskFromCalendar(context: Context, eventId: Long): Boolean {
        if (!hasPermissions(context)) return false
        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete calendar event $eventId", e)
            false
        }
    }

    fun importEventsForDate(context: Context, calendarId: Long, dateString: String): List<PlannerTask> {
        if (!hasPermissions(context)) return emptyList()

        val importedTasks = mutableListOf<PlannerTask>()
        val (startOfDay, endOfDay) = getDayStartAndEndMillis(dateString)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )

        val selection = "(${CalendarContract.Events.CALENDAR_ID} = ?) AND " +
                "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(calendarId.toString(), startOfDay.toString(), endOfDay.toString())

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val startIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                var index = 0
                while (it.moveToNext()) {
                    val eventId = it.getLong(idIdx)
                    val rawTitle = it.getString(titleIdx) ?: "Untitled Event"
                    // If event was created by DayWise, strip prefix
                    val title = rawTitle.replace("[DayWise] ", "")
                    val description = it.getString(descIdx) ?: ""
                    val startMillis = it.getLong(startIdx)
                    val endMillis = it.getLong(endIdx)
                    val isAllDay = it.getInt(allDayIdx) == 1

                    val startTimeStr = if (isAllDay) null else timeFormat.format(java.util.Date(startMillis))
                    val durationMin = if (endMillis > startMillis && !isAllDay) {
                        ((endMillis - startMillis) / (60 * 1000)).toInt().coerceIn(15, 480)
                    } else {
                        45
                    }

                    importedTasks.add(
                        PlannerTask(
                            id = 0,
                            title = title,
                            notes = description,
                            date = dateString,
                            orderIndex = index++,
                            priority = TaskPriority.P2_HIGH,
                            category = if (title.contains("meeting", ignoreCase = true) || title.contains("review", ignoreCase = true)) TaskCategory.WORK else TaskCategory.PERSONAL,
                            startTime = startTimeStr,
                            durationMinutes = durationMin,
                            isCompleted = false,
                            calendarEventId = eventId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing events from device calendar", e)
        }

        return importedTasks
    }

    private fun calculateEventTime(dateStr: String, startTimeStr: String?, durationMinutes: Int): Pair<Long, Long> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try {
            sdf.parse(dateStr)
        } catch (_: Exception) {
            null
        } ?: java.util.Date()

        val cal = Calendar.getInstance().apply {
            time = date
            if (startTimeStr != null && startTimeStr.contains(":")) {
                val parts = startTimeStr.split(":")
                set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 9)
                set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            } else {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val startMillis = cal.timeInMillis
        cal.add(Calendar.MINUTE, durationMinutes.coerceAtLeast(15))
        val endMillis = cal.timeInMillis

        return Pair(startMillis, endMillis)
    }

    private fun getDayStartAndEndMillis(dateStr: String): Pair<Long, Long> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try {
            sdf.parse(dateStr)
        } catch (_: Exception) {
            null
        } ?: java.util.Date()

        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
