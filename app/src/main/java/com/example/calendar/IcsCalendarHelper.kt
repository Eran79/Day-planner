package com.example.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.PlannerTask
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object IcsCalendarHelper {
    private const val TAG = "IcsCalendarHelper"

    /**
     * Generates a standard RFC 5545 iCalendar string for the given list of tasks.
     */
    fun generateIcs(tasks: List<PlannerTask>, calendarTitle: String = "DayWise Schedule"): String {
        val sb = StringBuilder()
        val dtStampFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowStamp = dtStampFormat.format(Date())

        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//DayWise Planner//DayWise Android//EN\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:").append(sanitizeIcsText(calendarTitle)).append("\r\n")
        sb.append("X-WR-TIMEZONE:").append(TimeZone.getDefault().id).append("\r\n")

        for (task in tasks) {
            val (dtStart, dtEnd) = formatIcsDateTime(task.date, task.startTime, task.durationMinutes)
            val uid = "daywise-${task.id}-${task.date}-${UUID.randomUUID().toString().take(8)}@daywise.app"
            val priorityNum = when (task.priority) {
                TaskPriority.P1_CRITICAL -> 1
                TaskPriority.P2_HIGH -> 3
                TaskPriority.P3_MEDIUM -> 5
                TaskPriority.P4_LOW -> 9
            }

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:").append(uid).append("\r\n")
            sb.append("DTSTAMP:").append(nowStamp).append("\r\n")
            sb.append("DTSTART:").append(dtStart).append("\r\n")
            sb.append("DTEND:").append(dtEnd).append("\r\n")
            sb.append("SUMMARY:").append(sanitizeIcsText(task.title)).append("\r\n")

            val description = buildString {
                if (task.notes.isNotBlank()) append(task.notes).append("\n\n")
                append("Category: ").append(task.category.displayName).append("\n")
                append("Priority: ").append(task.priority.displayName)
                val subtasks = task.getSubtasks()
                if (subtasks.isNotEmpty()) {
                    append("\n\nChecklist:\n")
                    subtasks.forEach { sub ->
                        append(if (sub.isDone) " [x] " else " [ ] ").append(sub.title).append("\n")
                    }
                }
            }
            sb.append("DESCRIPTION:").append(sanitizeIcsText(description)).append("\r\n")
            sb.append("STATUS:").append(if (task.isCompleted) "COMPLETED" else "CONFIRMED").append("\r\n")
            sb.append("PRIORITY:").append(priorityNum).append("\r\n")
            sb.append("CATEGORIES:").append(task.category.displayName).append("\r\n")
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /**
     * Exports tasks to a temporary .ics file and launches the system share chooser.
     */
    fun shareIcsCalendar(context: Context, tasks: List<PlannerTask>, fileName: String = "daywise_schedule"): Intent? {
        try {
            val icsContent = generateIcs(tasks, "DayWise Planner - $fileName")
            val cacheDir = File(context.cacheDir, "calendar").apply { mkdirs() }
            val file = File(cacheDir, "${fileName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}.ics")

            FileOutputStream(file).use { fos ->
                fos.write(icsContent.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "DayWise Planner Schedule ($fileName)")
                putExtra(Intent.EXTRA_TEXT, "Here is your synchronized day schedule from DayWise Planner.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            return Intent.createChooser(shareIntent, "Export Calendar (.ics)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share .ics calendar", e)
            return null
        }
    }

    /**
     * Parses .ics content and returns a list of PlannerTask items.
     */
    fun parseIcsContent(icsText: String, defaultDate: String): List<PlannerTask> {
        val tasks = mutableListOf<PlannerTask>()
        try {
            val lines = icsText.lines()
            var inEvent = false
            var summary = ""
            var description = ""
            var dtStart = ""
            var dtEnd = ""
            var priorityStr = ""
            var categories = ""

            for (rawLine in lines) {
                val line = rawLine.trim()
                when {
                    line.startsWith("BEGIN:VEVENT", ignoreCase = true) -> {
                        inEvent = true
                        summary = ""
                        description = ""
                        dtStart = ""
                        dtEnd = ""
                        priorityStr = ""
                        categories = ""
                    }
                    line.startsWith("END:VEVENT", ignoreCase = true) -> {
                        if (inEvent && summary.isNotBlank()) {
                            val (parsedDate, startTime, duration) = parseIcsDates(dtStart, dtEnd, defaultDate)
                            val priority = when (priorityStr) {
                                "1", "2" -> TaskPriority.P1_CRITICAL
                                "3", "4" -> TaskPriority.P2_HIGH
                                "5", "6" -> TaskPriority.P3_MEDIUM
                                else -> TaskPriority.P4_LOW
                            }
                            val category = when {
                                categories.contains("Work", ignoreCase = true) -> TaskCategory.WORK
                                categories.contains("Study", ignoreCase = true) -> TaskCategory.STUDY
                                categories.contains("Health", ignoreCase = true) -> TaskCategory.HEALTH
                                categories.contains("Finance", ignoreCase = true) -> TaskCategory.FINANCE
                                categories.contains("Creative", ignoreCase = true) -> TaskCategory.CREATIVE
                                else -> TaskCategory.PERSONAL
                            }

                            tasks.add(
                                PlannerTask(
                                    title = summary,
                                    notes = description,
                                    date = parsedDate,
                                    orderIndex = tasks.size,
                                    priority = priority,
                                    category = category,
                                    startTime = startTime,
                                    durationMinutes = duration,
                                    isCompleted = false
                                )
                            )
                        }
                        inEvent = false
                    }
                    inEvent -> {
                        when {
                            line.startsWith("SUMMARY:", ignoreCase = true) -> {
                                summary = unescapeIcsText(line.substringAfter(":"))
                            }
                            line.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                                description = unescapeIcsText(line.substringAfter(":"))
                            }
                            line.startsWith("DTSTART", ignoreCase = true) -> {
                                dtStart = line.substringAfter(":")
                            }
                            line.startsWith("DTEND", ignoreCase = true) -> {
                                dtEnd = line.substringAfter(":")
                            }
                            line.startsWith("PRIORITY:", ignoreCase = true) -> {
                                priorityStr = line.substringAfter(":")
                            }
                            line.startsWith("CATEGORIES:", ignoreCase = true) -> {
                                categories = line.substringAfter(":")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing .ics content", e)
        }
        return tasks
    }

    private fun formatIcsDateTime(dateStr: String, startTimeStr: String?, durationMin: Int): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = try { sdf.parse(dateStr) } catch (_: Exception) { null } ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            if (startTimeStr != null && startTimeStr.contains(":")) {
                val parts = startTimeStr.split(":")
                set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 9)
                set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            } else {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0)
        }

        val outFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
        val startStr = outFormat.format(cal.time)
        cal.add(Calendar.MINUTE, durationMin.coerceAtLeast(15))
        val endStr = outFormat.format(cal.time)

        return Pair(startStr, endStr)
    }

    private fun parseIcsDates(dtStart: String, dtEnd: String, defaultDate: String): Triple<String, String?, Int> {
        val cleanStart = dtStart.replace("Z", "")
        if (cleanStart.length >= 8) {
            val y = cleanStart.substring(0, 4)
            val m = cleanStart.substring(4, 6)
            val d = cleanStart.substring(6, 8)
            val date = "$y-$m-$d"

            var startTime: String? = null
            var duration = 45
            if (cleanStart.length >= 15 && cleanStart.contains("T")) {
                val timePart = cleanStart.substringAfter("T")
                if (timePart.length >= 4) {
                    val hh = timePart.substring(0, 2)
                    val mm = timePart.substring(2, 4)
                    startTime = "$hh:$mm"
                }
            }

            val cleanEnd = dtEnd.replace("Z", "")
            if (cleanEnd.length >= 15 && cleanEnd.contains("T") && startTime != null) {
                val endH = cleanEnd.substringAfter("T").take(2).toIntOrNull() ?: 0
                val endM = cleanEnd.substringAfter("T").drop(2).take(2).toIntOrNull() ?: 0
                val startH = startTime.take(2).toIntOrNull() ?: 0
                val startM = startTime.takeLast(2).toIntOrNull() ?: 0
                val dur = (endH * 60 + endM) - (startH * 60 + startM)
                if (dur in 15..480) duration = dur
            }

            return Triple(date, startTime, duration)
        }
        return Triple(defaultDate, null, 30)
    }

    private fun sanitizeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun unescapeIcsText(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
