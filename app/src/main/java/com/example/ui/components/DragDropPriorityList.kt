package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.PlannerTask
import com.example.data.model.TaskPriority
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DragDropPriorityList(
    tasks: List<PlannerTask>,
    onToggleCompleted: (PlannerTask) -> Unit,
    onTaskClick: (PlannerTask) -> Unit,
    onDeleteTask: (PlannerTask) -> Unit,
    onToggleSubtask: (PlannerTask, Int) -> Unit,
    onPriorityChange: (PlannerTask, TaskPriority) -> Unit,
    onReorderCommitted: (List<PlannerTask>) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Local mutable list for drag operations
    var currentList by remember(tasks) { mutableStateOf(tasks) }

    // Index of the item currently being dragged
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Approximate threshold height for swapping items
    val itemThresholdPx = remember(density) { with(density) { 100.dp.toPx() } }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (header != null) {
            item(key = "header") {
                header()
            }
        }

        itemsIndexed(
            items = currentList,
            key = { _, item -> item.id }
        ) { index, task ->
            val isCurrentDragging = draggingIndex == index

            val animatedY = remember { Animatable(0f) }
            LaunchedEffect(dragOffsetY, isCurrentDragging) {
                if (isCurrentDragging) {
                    animatedY.snapTo(dragOffsetY)
                } else {
                    animatedY.animateTo(
                        0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isCurrentDragging) 10f else 1f)
                    .offset { IntOffset(0, animatedY.value.roundToInt()) }
            ) {
                TaskCard(
                    task = task,
                    onToggleCompleted = { onToggleCompleted(task) },
                    onClick = { onTaskClick(task) },
                    onDelete = { onDeleteTask(task) },
                    onToggleSubtask = { subIndex -> onToggleSubtask(task, subIndex) },
                    onPriorityChange = { newPri -> onPriorityChange(task, newPri) },
                    onMoveUp = if (index > 0) {
                        {
                            val mutable = currentList.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            currentList = mutable
                            onReorderCommitted(mutable)
                        }
                    } else null,
                    onMoveDown = if (index < currentList.lastIndex) {
                        {
                            val mutable = currentList.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            currentList = mutable
                            onReorderCommitted(mutable)
                        }
                    } else null,
                    isDragging = isCurrentDragging,
                    dragHandleModifier = Modifier.pointerInput(task.id) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetY = 0f
                                triggerHapticTick(context)
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffsetY = 0f
                                onReorderCommitted(currentList)
                                triggerHapticTick(context)
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffsetY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount

                                val activeIndex = draggingIndex ?: return@detectVerticalDragGestures

                                // Dragging down and exceeded threshold
                                if (dragOffsetY > itemThresholdPx && activeIndex < currentList.lastIndex) {
                                    val targetIndex = activeIndex + 1
                                    val mutable = currentList.toMutableList()
                                    val item = mutable.removeAt(activeIndex)
                                    mutable.add(targetIndex, item)
                                    currentList = mutable
                                    draggingIndex = targetIndex
                                    dragOffsetY -= itemThresholdPx
                                    triggerHapticTick(context)
                                }
                                // Dragging up and exceeded negative threshold
                                else if (dragOffsetY < -itemThresholdPx && activeIndex > 0) {
                                    val targetIndex = activeIndex - 1
                                    val mutable = currentList.toMutableList()
                                    val item = mutable.removeAt(activeIndex)
                                    mutable.add(targetIndex, item)
                                    currentList = mutable
                                    draggingIndex = targetIndex
                                    dragOffsetY += itemThresholdPx
                                    triggerHapticTick(context)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

private fun triggerHapticTick(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    } catch (_: Exception) {}
}
