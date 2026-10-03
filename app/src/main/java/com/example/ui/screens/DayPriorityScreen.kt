package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayProgressSummary
import com.example.data.model.PlannerTask
import com.example.data.model.TaskPriority
import com.example.ui.components.DragDropPriorityList
import kotlin.math.roundToInt

@Composable
fun DayPriorityScreen(
    tasks: List<PlannerTask>,
    progress: DayProgressSummary,
    onToggleCompleted: (PlannerTask) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onDeleteTask: (PlannerTask) -> Unit,
    onToggleSubtask: (PlannerTask, Int) -> Unit,
    onPriorityChange: (PlannerTask, TaskPriority) -> Unit,
    onReorderCommitted: (List<PlannerTask>) -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "P1" -> tasks.filter { it.priority == TaskPriority.P1_CRITICAL }
            "PENDING" -> tasks.filter { !it.isCompleted }
            "DONE" -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    if (tasks.isEmpty()) {
        EmptyDayView(onAddTask = onAddTask, modifier = modifier)
    } else {
        DragDropPriorityList(
            tasks = filteredTasks,
            onToggleCompleted = onToggleCompleted,
            onTaskClick = onTaskClick,
            onDeleteTask = onDeleteTask,
            onToggleSubtask = onToggleSubtask,
            onPriorityChange = onPriorityChange,
            onReorderCommitted = { reorderedFiltered ->
                // If filtering is active, update original tasks positions
                if (selectedFilter == "ALL") {
                    onReorderCommitted(reorderedFiltered)
                } else {
                    onReorderCommitted(reorderedFiltered)
                }
            },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            modifier = modifier,
            header = {
                Column {
                    // Day Progress Hero Card
                    DayProgressCard(progress = progress)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Drag & Drop reorder tip
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DragIndicator,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hold & drag handle ⠿ or use arrows to prioritize tasks",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${tasks.size})") },
                            modifier = Modifier.testTag("filter_all")
                        )
                        FilterChip(
                            selected = selectedFilter == "P1",
                            onClick = { selectedFilter = "P1" },
                            label = { Text("P1 Critical") },
                            modifier = Modifier.testTag("filter_p1")
                        )
                        FilterChip(
                            selected = selectedFilter == "PENDING",
                            onClick = { selectedFilter = "PENDING" },
                            label = { Text("Pending (${tasks.count { !it.isCompleted }})") },
                            modifier = Modifier.testTag("filter_pending")
                        )
                        FilterChip(
                            selected = selectedFilter == "DONE",
                            onClick = { selectedFilter = "DONE" },
                            label = { Text("Done (${tasks.count { it.isCompleted }})") },
                            modifier = Modifier.testTag("filter_done")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        )
    }
}

@Composable
fun DayProgressCard(
    progress: DayProgressSummary,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = progress.completionPercentage / 100f,
        label = "progressAnim"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${progress.completedTasks} of ${progress.totalTasks} completed",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${progress.completedMinutes}m / ${progress.totalPlannedMinutes}m focus logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (progress.p1Count > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "P1 Critical: ${progress.p1CompletedCount}/${progress.p1Count} done",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (progress.p1CompletedCount == progress.p1Count) MaterialTheme.colorScheme.primary else TaskPriority.P1_CRITICAL.color
                    )
                }
            }

            // Circular progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { animatedPercent },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${progress.completionPercentage.roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EmptyDayView(
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Day is Clear!",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "No tasks planned for this day yet. Add tasks or import events from your calendar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddTask,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("empty_add_task_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add First Task")
        }
    }
}
