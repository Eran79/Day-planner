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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.TaskPriority

@Composable
fun MatrixScreen(
    tasks: List<PlannerTask>,
    onToggleCompleted: (PlannerTask) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onAddWithPriority: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    val q1Tasks = remember(tasks) { tasks.filter { it.priority == TaskPriority.P1_CRITICAL } }
    val q2Tasks = remember(tasks) { tasks.filter { it.priority == TaskPriority.P2_HIGH } }
    val q3Tasks = remember(tasks) { tasks.filter { it.priority == TaskPriority.P3_MEDIUM } }
    val q4Tasks = remember(tasks) { tasks.filter { it.priority == TaskPriority.P4_LOW } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Eisenhower Priority Matrix",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Categorize and tackle tasks by urgency and importance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            QuadrantCard(
                priority = TaskPriority.P1_CRITICAL,
                title = "DO FIRST",
                subtitle = "Urgent & Important",
                tasks = q1Tasks,
                onToggleCompleted = onToggleCompleted,
                onTaskClick = onTaskClick,
                onAdd = { onAddWithPriority(TaskPriority.P1_CRITICAL) }
            )
        }

        item {
            QuadrantCard(
                priority = TaskPriority.P2_HIGH,
                title = "SCHEDULE",
                subtitle = "Important, Not Urgent",
                tasks = q2Tasks,
                onToggleCompleted = onToggleCompleted,
                onTaskClick = onTaskClick,
                onAdd = { onAddWithPriority(TaskPriority.P2_HIGH) }
            )
        }

        item {
            QuadrantCard(
                priority = TaskPriority.P3_MEDIUM,
                title = "DELEGATE",
                subtitle = "Urgent, Not Important",
                tasks = q3Tasks,
                onToggleCompleted = onToggleCompleted,
                onTaskClick = onTaskClick,
                onAdd = { onAddWithPriority(TaskPriority.P3_MEDIUM) }
            )
        }

        item {
            QuadrantCard(
                priority = TaskPriority.P4_LOW,
                title = "ELIMINATE",
                subtitle = "Neither Urgent nor Important",
                tasks = q4Tasks,
                onToggleCompleted = onToggleCompleted,
                onTaskClick = onTaskClick,
                onAdd = { onAddWithPriority(TaskPriority.P4_LOW) }
            )
        }
    }
}

@Composable
fun QuadrantCard(
    priority: TaskPriority,
    title: String,
    subtitle: String,
    tasks: List<PlannerTask>,
    onToggleCompleted: (PlannerTask) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, priority.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(priority.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${priority.code}: $title",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = priority.color
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = priority.color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${tasks.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = priority.color
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(32.dp)
                        .background(priority.color.copy(alpha = 0.15f), CircleShape)
                        .testTag("add_matrix_${priority.code}")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add $title Task",
                        tint = priority.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks in this quadrant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTaskClick(task) },
                            color = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = task.category.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(color = task.category.color)
                                        )
                                        if (task.startTime != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${task.startTime}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
