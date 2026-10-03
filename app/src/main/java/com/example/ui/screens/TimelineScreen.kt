package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    tasks: List<PlannerTask>,
    selectedDate: String,
    onToggleCompleted: (PlannerTask) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onSlotClick: (timeStr: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheduledTasks = remember(tasks) {
        tasks.filter { it.startTime != null }
    }
    val unscheduledTasks = remember(tasks) {
        tasks.filter { it.startTime == null }
    }

    val hours = remember {
        (6..22).map { h -> String.format(Locale.US, "%02d:00", h) }
    }

    val isSelectedDateToday = remember(selectedDate) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        selectedDate == today
    }

    val currentHourInt = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Unscheduled / Flexible Tasks Banner
        if (unscheduledTasks.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Flexible Anytime Tasks (${unscheduledTasks.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        unscheduledTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTaskClick(task) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(task.priority.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${task.durationMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Timeline Hours
        items(hours) { hourStr ->
            val hourInt = hourStr.take(2).toIntOrNull() ?: 0
            val matchingTasks = scheduledTasks.filter { task ->
                val taskHour = task.startTime?.take(2)?.toIntOrNull()
                taskHour == hourInt
            }

            val isCurrentHour = isSelectedDateToday && hourInt == currentHourInt

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (matchingTasks.isEmpty()) 72.dp else (matchingTasks.size * 76 + 16).dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time label column
                Column(
                    modifier = Modifier.width(56.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = hourStr,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isCurrentHour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (isCurrentHour) {
                        Text(
                            text = "NOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Timeline Line & Slot Content
                Column(modifier = Modifier.weight(1f)) {
                    HorizontalDivider(
                        color = if (isCurrentHour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = if (isCurrentHour) 2.dp else 1.dp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (matchingTasks.isEmpty()) {
                        // Empty slot - tap to add
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSlotClick(hourStr) }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add at $hourStr",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Available time slot",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        // Tasks scheduled in this hour block
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            matchingTasks.forEach { task ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onTaskClick(task) }
                                        .border(
                                            1.dp,
                                            task.priority.color.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Completion checkbox
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .border(2.dp, if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                                .clickable { onToggleCompleted(task) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (task.isCompleted) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${task.startTime} • ${task.durationMinutes}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${task.category.displayName}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = task.category.color)
                                                )
                                                if (task.calendarEventId != null) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        Icons.Default.CalendarToday,
                                                        contentDescription = "Synced",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
