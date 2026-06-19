package com.example.ui.home

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    repository: IronLogRepository,
    onStartWorkout: (Workout) -> Unit,
    onResumeWorkout: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeWorkout by remember { mutableStateOf<Workout?>(null) }
    var activeProgramState by remember { mutableStateOf<ActiveProgramState?>(null) }
    var workoutsList by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var program by remember { mutableStateOf<Program?>(null) }
    var isLoadingProgram by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.seedInitialExercises()
        launch { repository.getActiveWorkout().collect { activeWorkout = it } }
        launch { repository.getActiveProgramState().collect { activeProgramState = it } }
        launch { repository.getWorkouts().collect { workoutsList = it } }
    }

    LaunchedEffect(Unit) {
        isLoadingProgram = true
        try {
            val json = context.assets.open("jeff_nippard.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(Program::class.java)
            val rawProgram = adapter.fromJson(json)
            program = ProgramValidator.validateAndSanitize(rawProgram)
        } catch (e: Exception) {
        }
        isLoadingProgram = false
    }

    if (activeProgramState == null) {
        // Detailed program landing page if no active program
        Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
            val p = program
            if (p != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val brush = Brush.verticalGradient(
                                    colors = listOf(
                                        TextPrimaryColor.copy(alpha = 0.15f),
                                        BgColor
                                    )
                                )
                                drawRect(brush)
                                
                                // Decorative light streaks
                                drawLine(
                                    color = TextPrimaryColor.copy(alpha = 0.05f),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 2f
                                )
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, 
                            modifier = Modifier
                                .padding(IronSpacing.x24)
                                .padding(top = IronSpacing.x48, bottom = 60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(TextPrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(IronCorner.RadiusLg))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = TextPrimaryColor, modifier = Modifier.size(40.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(IronSpacing.x24))
                            
                            Text(
                                "SYSTEM PROTOCOL",
                                style = IronTypography.Caption.copy(
                                    color = TextTertiaryColor, 
                                    letterSpacing = 3.sp, 
                                    fontWeight = FontWeight.Black
                                )
                            )
                            Spacer(modifier = Modifier.height(IronSpacing.x12))
                            Text(
                                p.programName.uppercase(),
                                style = IronTypography.Title1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = IronSpacing.x16)
                            )
                            Spacer(modifier = Modifier.height(IronSpacing.x12))
                            Text(
                                "ENGINEERED BY ${p.author.uppercase()}",
                                style = IronTypography.Footnote.copy(
                                    color = TextSecondaryColor.copy(alpha = 0.6f), 
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = IronSpacing.x20)
                            .offset(y = (-24).dp),
                        verticalArrangement = Arrangement.spacedBy(IronSpacing.x20)
                    ) {
                        // Section: Program Overview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                .padding(IronSpacing.x20)
                        ) {
                            Column {
                                Text(
                                    "PROGRAM OVERVIEW",
                                    style = IronTypography.Caption.copy(
                                        color = TextTertiaryColor, 
                                        letterSpacing = 1.5.sp, 
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = IronSpacing.x8)
                                )
                                Text(
                                    "This protocol utilizes daily undulating periodization (DUP) paired with progressive overload. You will cycle through intensity blocks focusing on foundational strength (1-5 reps) and hypertrophy optimization (8-15 reps). Designed for maximum muscle fiber recruitment and recovery efficiency.",
                                    style = IronTypography.Body
                                )
                            }
                        }

                        // Section: Functional Specs (Duration, Effort)
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.spacedBy(IronSpacing.x16)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                    .padding(IronSpacing.x16)
                            ) {
                                Column {
                                    Text(
                                        "DURATION", 
                                        style = IronTypography.Caption.copy(
                                            color = TextTertiaryColor, 
                                            fontWeight = FontWeight.Bold, 
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(IronSpacing.x4))
                                    Text("${p.weeks.size} WEEKS", style = IronTypography.Headline)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                    .padding(IronSpacing.x16)
                            ) {
                                Column {
                                    Text(
                                        "EFFORT", 
                                        style = IronTypography.Caption.copy(
                                            color = TextTertiaryColor, 
                                            fontWeight = FontWeight.Bold, 
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(IronSpacing.x4))
                                    Text("ELITE", style = IronTypography.Headline)
                                }
                            }
                        }

                        // Section: Detailed Specs
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                .padding(IronSpacing.x20)
                        ) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "MUSCLE GROUPS",
                                            style = IronTypography.Caption.copy(
                                                color = TextTertiaryColor, 
                                                letterSpacing = 1.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(bottom = IronSpacing.x12)
                                        )
                                        listOf("Full Body", "Upper/Lower Split", "Push/Pull/Legs").forEach {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically, 
                                                modifier = Modifier.padding(vertical = IronSpacing.x4)
                                            ) {
                                                Box(modifier = Modifier.size(4.dp).background(TextPrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                                Spacer(modifier = Modifier.width(IronSpacing.x8))
                                                Text(it, style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "EQUIPMENT",
                                            style = IronTypography.Caption.copy(
                                                color = TextTertiaryColor, 
                                                letterSpacing = 1.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(bottom = IronSpacing.x12)
                                        )
                                        listOf("Barbell", "Dumbbells", "Cables", "Squat Rack").forEach {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically, 
                                                modifier = Modifier.padding(vertical = IronSpacing.x4)
                                            ) {
                                                Box(modifier = Modifier.size(4.dp).background(TextPrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                                Spacer(modifier = Modifier.width(IronSpacing.x8))
                                                Text(it, style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Section: Performance Goals
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                .padding(IronSpacing.x20)
                        ) {
                            Column {
                                Text(
                                    "PERFORMANCE GOALS",
                                    style = IronTypography.Caption.copy(
                                        color = TextTertiaryColor, 
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = IronSpacing.x12)
                                )
                                listOf(
                                    "Increase 1RM in major compounds",
                                    "Optimized sarcoplasmic hypertrophy",
                                    "Enhanced work capacity and recovery"
                                ).forEach {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically, 
                                        modifier = Modifier.padding(vertical = IronSpacing.x4)
                                    ) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(IronSpacing.x8))
                                        Text(it, style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(IronSpacing.x16))

                        Button(
                            onClick = {
                                val state = ActiveProgramState(
                                    programKey = "jeff_nippard.json",
                                    programName = p.programName,
                                    currentWeekIndex = 0,
                                    currentDayIndex = 0,
                                    completedWorkoutsMap = emptyMap(),
                                    freeNavigationEnabled = true,
                                    workoutsCompletedThisWeek = 0,
                                    totalWorkoutsThisWeek = p.weeks["week1"]?.days?.count { !it.isRestDay } ?: 0
                                )
                                coroutineScope.launch { repository.saveActiveProgramState(state) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .bouncy(),
                            colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = BgColor),
                            shape = RoundedCornerShape(IronCorner.RadiusMd)
                        ) {
                            Text(
                                "INITIALIZE PROTOCOL", 
                                style = IronTypography.Headline.copy(
                                    color = BgColor,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(IronSpacing.x48))
                    }

                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimaryColor)
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = BgColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val p = program
            if (p != null) {
                DashboardClean(
                    program = p,
                    activeProgramState = activeProgramState!!,
                    activeWorkout = activeWorkout,
                    workoutsList = workoutsList,
                    onResumeWorkout = onResumeWorkout,
                    onStartWorkout = onStartWorkout,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = IronTypography.Caption.copy(color = TextTertiaryColor, fontSize = 10.sp, letterSpacing = 1.sp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = IronTypography.Title2)
    }
}
@Composable
fun DashboardClean(
    program: Program,
    activeProgramState: ActiveProgramState,
    activeWorkout: Workout?,
    workoutsList: List<Workout>,
    onResumeWorkout: () -> Unit,
    onStartWorkout: (Workout) -> Unit,
    onProfileClick: () -> Unit
) {
    val completedMap = activeProgramState.completedWorkoutsMap

    val weekKey = "week${activeProgramState.currentWeekIndex + 1}"
    val daysList = program.weeks[weekKey]?.days ?: emptyList()
    
    // Auto find the correct day to show
    var targetDayIndex = activeProgramState.currentDayIndex
    if (activeProgramState.freeNavigationEnabled && daysList.isNotEmpty()) {
        val firstIncomplete = daysList.indices.firstOrNull { idx ->
            !daysList[idx].isRestDay && completedMap["${weekKey}_$idx"] != true
        }
        if (firstIncomplete != null) {
            targetDayIndex = firstIncomplete
        }
    }
    val selectedDay = daysList.getOrNull(targetDayIndex)

    val currentStreak = remember(workoutsList) {
        val completed = workoutsList.filter { it.status == "completed" }
        val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDays = completed.map { sdfDay.format(Date(it.date)) }.toSet()
        val tempCalendar = Calendar.getInstance()
        var streak = 0
        val todayStr = sdfDay.format(tempCalendar.time)
        tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdfDay.format(tempCalendar.time)
        if (uniqueDays.contains(todayStr)) {
            streak = 1
            tempCalendar.time = Date()
            tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
            while (uniqueDays.contains(sdfDay.format(tempCalendar.time))) {
                streak++
                tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (uniqueDays.contains(yesterdayStr)) {
            streak = 1
            tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
            while (uniqueDays.contains(sdfDay.format(tempCalendar.time))) {
                streak++
                tempCalendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else {
            streak = if (uniqueDays.isNotEmpty()) 1 else 0
        }
        streak
    }

    val weeklyVolume = remember(workoutsList) {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        workoutsList.filter { it.status == "completed" && it.date >= sevenDaysAgo }.sumOf { it.totalVolume }
    }
    
    val totalWorkouts = workoutsList.count { it.status == "completed" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(IronSpacing.x16)
    ) {
        // Top row: App wordmark and Profile avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = IronSpacing.x32)
                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                .padding(horizontal = IronSpacing.x16, vertical = IronSpacing.x12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).background(TextPrimaryColor, RoundedCornerShape(4.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = BgColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "GYM KRTA H JI",
                    style = IronTypography.Title3.copy(letterSpacing = 1.5.sp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(IronCorner.RadiusFull))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(IronCorner.RadiusFull))
                    .bouncyClick { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // Quick Actions
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = IronSpacing.x32), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .glassRecipe(RoundedCornerShape(IronCorner.RadiusMd))
                    .bouncyClick { /* Warm Up action */ },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Whatshot, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WARM UP", style = IronTypography.Headline, fontSize = 14.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(56.dp)
                    .background(TextPrimaryColor, RoundedCornerShape(IronCorner.RadiusMd))
                    .bouncyClick { 
                        // Start empty workout
                        val emptyWorkout = Workout(templateName = "New Workout", date = System.currentTimeMillis())
                        onStartWorkout(emptyWorkout)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = BgColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUICK LOG", style = IronTypography.Headline, fontSize = 14.sp, color = BgColor)
                }
            }
        }

        // Hero card (Current Day Info)
        if (selectedDay != null) {
            Text(
                "TODAY'S MISSION",
                style = IronTypography.Caption.copy(color = TextSecondaryColor, letterSpacing = 2.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                    .padding(IronSpacing.x24)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        text = selectedDay.dayName,
                        style = IronTypography.Title1
                    )
                    Box(
                        modifier = Modifier
                            .background(SuccessColor.copy(alpha = 0.1f), RoundedCornerShape(IronCorner.RadiusFull))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Day ${targetDayIndex + 1}", style = IronTypography.Caption.copy(color = SuccessColor))
                    }
                }
                
                Spacer(modifier = Modifier.height(IronSpacing.x12))
                
                // Muscle tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(IronSpacing.x8)
                ) {
                    val muscleSet = selectedDay.exercises.mapNotNull { it.muscleGroup }.distinct()
                    val musclesToShow = if (muscleSet.isNotEmpty()) muscleSet else listOf("Full Body")
                    
                    musclesToShow.take(3).forEach { m ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFFFFFFF).copy(alpha=0.15f), RoundedCornerShape(IronCorner.RadiusFull))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(m, style = IronTypography.Caption)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(IronSpacing.x24))
                
                val duration = if (selectedDay.isRestDay) 0 else 60
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("${selectedDay.exercises.size}", style = IronTypography.Title2)
                        Text("EXERCISES", style = IronTypography.Caption.copy(color = TextSecondaryColor, fontSize = 9.sp))
                    }
                    Column {
                        Text("$duration", style = IronTypography.Title2)
                        Text("MINUTES", style = IronTypography.Caption.copy(color = TextSecondaryColor, fontSize = 9.sp))
                    }
                }

                Spacer(modifier = Modifier.height(IronSpacing.x32))

                if (activeWorkout != null) {
                    Button(
                        onClick = onResumeWorkout,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = BgColor),
                        shape = RoundedCornerShape(IronCorner.RadiusMd)
                    ) {
                        Text("RESUME ACTIVE LOGGING", style = IronTypography.Headline, color = BgColor)
                    }
                } else if (selectedDay.isRestDay) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp).glassRecipe(RoundedCornerShape(IronCorner.RadiusMd)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("RECOVER & REST", style = IronTypography.Headline, color = TextPrimaryColor)
                    }
                } else {
                    Button(
                        onClick = {
                            var newW = selectedDay.toWorkout(weekKey, targetDayIndex)
                            // Prefill sets logic kept minimal
                            val completedWorkouts = workoutsList.filter { it.status == "completed" }.sortedByDescending { it.date }
                            val newExs = newW.loggedExercises.map { ex ->
                                val lastEx = completedWorkouts.mapNotNull { w: Workout -> w.loggedExercises.find { it.exerciseId == ex.exerciseId } }.firstOrNull()
                                if (lastEx != null) {
                                    val newSets = ex.sets.map { set: WorkoutSet ->
                                        val pastSet = lastEx.sets.find { it.isWarmup == set.isWarmup && it.setNumber == set.setNumber }
                                            ?: lastEx.sets.lastOrNull { it.isWarmup == set.isWarmup }
                                        if (pastSet != null) {
                                            set.copy(weight = pastSet.weight, reps = pastSet.reps)
                                        } else {
                                            set.copy(reps = set.targetReps ?: 0)
                                        }
                                    }
                                    ex.copy(sets = newSets)
                                } else {
                                    ex.copy(sets = ex.sets.map { it.copy(reps = it.targetReps ?: 0) })
                                }
                            }
                            newW = newW.copy(loggedExercises = newExs)
                            onStartWorkout(newW)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = BgColor),
                        shape = RoundedCornerShape(IronCorner.RadiusMd)
                    ) {
                        Text("BEGIN WORKOUT", style = IronTypography.Headline, color = BgColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(IronSpacing.x32))

        // Stats row (horizontal scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(IronSpacing.x12)
        ) {
            listOf(
                Pair("$currentStreak", "STREAK"),
                Pair("${weeklyVolume.toInt()}kg", "WEEK VOL"),
                Pair("$totalWorkouts", "WORKOUTS")
            ).forEach { (value, label) ->
                Column(
                    modifier = Modifier
                        .widthIn(min = 100.dp)
                        .glassRecipe(RoundedCornerShape(IronCorner.RadiusMd))
                        .padding(IronSpacing.x16),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(value, style = IronTypography.Title1)
                    Text(label, style = IronTypography.Caption.copy(color = TextSecondaryColor))
                }
            }
        }

        Spacer(modifier = Modifier.height(IronSpacing.x32))

        // Recent workout card
        val lastWorkout = workoutsList.filter { it.status == "completed" }.maxByOrNull { it.date }
        if (lastWorkout != null) {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassRecipe(RoundedCornerShape(IronCorner.RadiusMd))
                    .padding(IronSpacing.x20),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recent Workout", style = IronTypography.Headline)
                    Spacer(modifier = Modifier.height(IronSpacing.x4))
                    Text(sdf.format(Date(lastWorkout.date)), style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                }
                Text("${lastWorkout.totalVolume.toInt()} kg", style = IronTypography.Title2)
            }
        }

        Spacer(modifier = Modifier.height(IronSpacing.x32))

        // Program Overview section
        Text(
            "PROGRAM OVERVIEW",
            style = IronTypography.Caption.copy(color = TextSecondaryColor, letterSpacing = 2.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                .padding(IronSpacing.x20)
        ) {
            Column {
                Text(program.programName, style = IronTypography.Headline)
                Spacer(modifier = Modifier.height(4.dp))
                Text("by ${program.author}", style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This system is designed for maximum hypertrophy through scientific exercise selection, focusing on both strength foundations and high-volume metabolic stress. Progress through 8 weeks of targeted periodization.",
                    style = IronTypography.Body.copy(color = TextSecondaryColor, fontSize = 13.sp, lineHeight = 18.sp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Progress bar
                val totalDays = program.weeks.values.sumOf { it.days.count { d -> !d.isRestDay } }
                val completedDaysCount = completedMap.size
                val progress = if (totalDays > 0) completedDaysCount.toFloat() / totalDays else 0f
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(TextPrimaryColor, RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("${(progress * 100).toInt()}%", style = IronTypography.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(IronSpacing.x48))
    }
}
