package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DateItem(
    val dateString: String, // YYYY-MM-DD
    val dayOfWeek: String, // Mon, Tue...
    val dayOfMonth: String, // 03, 04...
    val isToday: Boolean
)

@Composable
fun DateStrip(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Generate date items for -14 days to +28 days around today
    val dateItems = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())
        val todayStr = sdf.format(Date())

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -10)
        }

        val items = mutableListOf<DateItem>()
        for (i in 0..40) {
            val dStr = sdf.format(cal.time)
            items.add(
                DateItem(
                    dateString = dStr,
                    dayOfWeek = dayOfWeekFormat.format(cal.time),
                    dayOfMonth = dayOfMonthFormat.format(cal.time),
                    isToday = dStr == todayStr
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        items
    }

    // Scroll to selected date on change
    LaunchedEffect(selectedDate) {
        val index = dateItems.indexOfFirst { it.dateString == selectedDate }
        if (index >= 0) {
            listState.animateScrollToItem((index - 2).coerceAtLeast(0))
        }
    }

    // Display formatted header, e.g. "Saturday, Oct 3, 2026"
    val headerFormatted = remember(selectedDate) {
        try {
            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = inFormat.parse(selectedDate) ?: Date()
            val outFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            outFormat.format(d)
        } catch (_: Exception) {
            selectedDate
        }
    }

    val todayString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        // Top row with Day Title, Today button, and Calendar Picker button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = headerFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedDate == todayString) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedDate != todayString) {
                    AssistChip(
                        onClick = { onDateSelected(todayString) },
                        label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("jump_today_chip")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        val parts = selectedDate.split("-")
                        if (parts.size == 3) {
                            cal.set(
                                parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR),
                                (parts[1].toIntOrNull() ?: 1) - 1,
                                parts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                            )
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val picked = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                onDateSelected(picked)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.testTag("open_date_picker_button")
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Pick Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Days Strip
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dateItems, key = { it.dateString }) { item ->
                val isSelected = item.dateString == selectedDate
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    label = "dateBg"
                )
                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDateSelected(item.dateString) }
                        .testTag("date_chip_${item.dateString}"),
                    color = bgColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.dayOfWeek.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = textColor.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.dayOfMonth,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = textColor
                        )

                        if (item.isToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(9.dp))
                        }
                    }
                }
            }
        }
    }
}
