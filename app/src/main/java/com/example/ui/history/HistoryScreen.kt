package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.Exercise
import com.example.model.PersonalRecord
import com.example.model.Workout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(repository: IronLogRepository) {
    var history by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var exercisesMap by remember { mutableStateOf<Map<String, Exercise>>(emptyMap()) }
    var prsList by remember { mutableStateOf<List<PersonalRecord>>(emptyList()) }
    var isCalendarView by remember { mutableStateOf(true) }
    var selectedDateStr by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        selectedDateStr = sdf.format(Date())
        repository.getWorkouts().combine(repository.getExercises()) { workouts, exercises ->
            history = workouts.filter { it.status == "completed" }
            exercisesMap = exercises.associateBy { it.id }
        }.combine(repository.getPersonalRecords()) { _, prs ->
            prsList = prs
        }.collect {}
    }

    // Helper to calculate primary muscle group of a workout
    val workoutMuscleGroups = remember(history, exercisesMap) {
        history.associate { workout ->
            val groups = workout.loggedExercises.mapNotNull {
                exercisesMap[it.exerciseId]?.muscleGroup?.lowercase()
            }
            val primaryGroup = groups.groupBy { it }
                .maxByOrNull { it.value.size }?.key ?: "general"
            workout.id to primaryGroup
        }
    }

    // Helper to determine if a specific date Str has a PR
    val prDatesSet = remember(prsList) {
        val dates = mutableSetOf<String>()
        prsList.forEach { pr ->
            pr.bestWeight?.let { dates.add(sdf.format(Date(it.date))) }
            pr.bestEstimated1RM?.let { dates.add(sdf.format(Date(it.date))) }
        }
        dates
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF16161A), Color.Black),
                center = Offset(500f, -200f),
                radius = 2500f
            )
        ),
        topBar = {
            TopAppBar(
                title = { Text("TRAINING LOGS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    // Modern high-contrast toggle with active borders
                    Row(
                        modifier = Modifier
                            .background(com.example.ui.theme.GlassDark, RoundedCornerShape(8.dp))
                            .border(1.dp, com.example.ui.theme.GlassBorderDark, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { isCalendarView = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isCalendarView) Color.White else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .testTag("toggle_calendar_view")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar View",
                                tint = if (isCalendarView) Color.Black else Color.White
                            )
                        }
                        IconButton(
                            onClick = { isCalendarView = false },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (!isCalendarView) Color.White else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .testTag("toggle_list_view")
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "List View",
                                tint = if (!isCalendarView) Color.Black else Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isCalendarView) {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TrainingCalendar(
                        workouts = history,
                        workoutMuscleGroups = workoutMuscleGroups,
                        prDates = prDatesSet,
                        selectedDate = selectedDateStr,
                        onDaySelected = { selectedDateStr = it }
                    )
                }

                // Show workouts logged on the selected day
                val matchedWorkouts = history.filter { sdf.format(Date(it.date)) == selectedDateStr }
                
                item {
                    Text(
                        text = "WORKOUTS ON " + if (selectedDateStr == sdf.format(Date())) "TODAY" else selectedDateStr,
                        color = com.example.ui.theme.GrayMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (matchedWorkouts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
                            border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🧘 RECOVERY & OFF DAY\n(No logs completed on this date)",
                                    color = com.example.ui.theme.GrayMedium,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    items(matchedWorkouts) { wk ->
                        WorkoutDetailCard(workout = wk, exercisesMap = exercisesMap, prDates = prDatesSet)
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        } else {
            // General plain scrollable list
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                if (history.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No workouts found in your history.",
                                color = com.example.ui.theme.GrayMedium,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(history.sortedByDescending { it.date }) { workout ->
                        WorkoutDetailCard(workout = workout, exercisesMap = exercisesMap, prDates = prDatesSet)
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun WorkoutDetailCard(
    workout: Workout,
    exercisesMap: Map<String, Exercise>,
    prDates: Set<String>
) {
    val dateStr = remember(workout.date) {
        SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault()).format(Date(workout.date)).uppercase()
    }
    
    // Check if PR was hit on this workout date
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isPrDay = prDates.contains(sdf.format(Date(workout.date)))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("workout_history_card_${workout.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
        border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (workout.templateName ?: "AD-HOC WORKOUT").uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    if (isPrDay) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔥", fontSize = 16.sp) // PR Flame Indicator!
                    }
                }
                
                if (isPrDay) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PR SET",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            
            Text(
                text = dateStr,
                color = com.example.ui.theme.GrayMedium,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            // Sub-list of executed exercises inside workout (Read-only view)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131313), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                workout.loggedExercises.forEach { loggedEx ->
                    val mGroup = exercisesMap[loggedEx.exerciseId]?.muscleGroup?.uppercase() ?: "GENERAL"
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = loggedEx.exerciseName.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = mGroup,
                                color = com.example.ui.theme.AccentGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        // Compact sets lists representation line
                        val setsRepLine = loggedEx.sets.filter { it.completedAt != null }.joinToString(", ") { set ->
                            "${if (set.weight % 1.0 == 0.0) set.weight.toInt() else set.weight}kg x ${set.reps}" + if (set.rpe != null) " (RPE ${set.rpe})" else ""
                        }
                        Text(
                            text = if (setsRepLine.isNotEmpty()) setsRepLine else "No completed sets",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("${workout.totalVolume.toInt()}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                    Text("VOLUME (KG)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${workout.durationMinutes}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                    Text("DURATION MINUTES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun TrainingCalendar(
    workouts: List<Workout>,
    workoutMuscleGroups: Map<String, String>,
    prDates: Set<String>,
    selectedDate: String,
    onDaySelected: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val workoutDates = remember(workouts) {
        workouts.associateBy { sdf.format(Date(it.date)) }
    }

    var currentMonthOffset by remember { mutableIntStateOf(0) }
    
    val calendar = remember(currentMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, currentMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
    
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
    
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
        border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonthOffset-- }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = Color.White)
                }
                
                Text(
                    text = monthName,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.5.sp
                )
                
                IconButton(onClick = { currentMonthOffset++ }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Days of week
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        color = com.example.ui.theme.GrayMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar Grid
            val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
            
            Column {
                for (row in 0 until (totalCells / 7)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayOfMonth = cellIndex - firstDayOfWeek + 1
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayOfMonth in 1..daysInMonth) {
                                    val cellCalendar = calendar.clone() as Calendar
                                    cellCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    val dateStr = sdf.format(cellCalendar.time)
                                    val workout = workoutDates[dateStr]
                                    val hasWorkout = workout != null
                                    val isToday = sdf.format(Date()) == dateStr
                                    val isSelected = selectedDate == dateStr
                                    val containsPr = prDates.contains(dateStr)

                                    // Primary muscle group dot coloring
                                    val primaryGroup = workout?.let { workoutMuscleGroups[it.id] } ?: ""
                                    val dotColor = remember(primaryGroup) {
                                        when (primaryGroup.lowercase()) {
                                            "chest" -> Color(0xFFE57373) // Red
                                            "back" -> Color(0xFF64B5F6) // Blue
                                            "legs", "quads", "hamstrings", "calves" -> Color(0xFF81C784) // Green
                                            "shoulders", "delts" -> Color(0xFFFFB74D) // Orange
                                            "arms", "biceps", "triceps" -> Color(0xFFBA68C8) // Purple
                                            "core", "abs" -> Color(0xFF4DB6AC) // Teal
                                            else -> Color(0xFFB0BEC5) // Blue Grey
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color.White.copy(alpha = 0.15f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> com.example.ui.theme.AccentGreen.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable { onDaySelected(dateStr) }
                                            .testTag("calendar_day_$dayOfMonth")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayOfMonth.toString(),
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> com.example.ui.theme.AccentGreen
                                                    else -> Color.White
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = if (hasWorkout || isToday) FontWeight.Black else FontWeight.Normal
                                            )
                                            
                                            // Star indicator if date has a PR
                                            if (containsPr) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 10.dp, y = (-8).dp)
                                                ) {
                                                    Text("⭐", fontSize = 8.sp)
                                                }
                                            }
                                        }
                                        
                                        // Colored muscle group dot
                                        if (hasWorkout) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(dotColor, CircleShape)
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
