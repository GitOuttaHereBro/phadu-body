package com.example.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.ActiveProgramState
import com.example.model.PersonalRecord
import com.example.model.Program
import com.example.model.Template
import com.example.model.Workout
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.ui.theme.bounceClick

@Composable
fun HomeScreen(
    repository: IronLogRepository,
    onStartWorkout: (templateId: String?) -> Unit,
    onResumeWorkout: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToTab: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var templates by remember { mutableStateOf<List<Template>>(emptyList()) }
    var workoutsList by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var recentWorkout by remember { mutableStateOf<Workout?>(null) }
    var activeWorkout by remember { mutableStateOf<Workout?>(null) }
    var activeProgramState by remember { mutableStateOf<ActiveProgramState?>(null) }
    var rawPrs by remember { mutableStateOf<List<PersonalRecord>>(emptyList()) }

    var showWarmupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.seedInitialExercises()
        
        launch { repository.getTemplates().collect { templates = it } }
        launch {
            repository.getWorkouts().collect { workouts ->
                workoutsList = workouts
                recentWorkout = workouts.filter { it.status == "completed" }.maxByOrNull { it.date }
            }
        }
        launch { repository.getActiveWorkout().collect { activeWorkout = it } }
        launch { repository.getActiveProgramState().collect { activeProgramState = it } }
        launch { repository.getPersonalRecords().collect { rawPrs = it } }
    }

    // Dynamic extraction of muscle groups trained today from template
    val nextWorkoutIndex = remember(activeProgramState, templates) {
        activeProgramState?.let {
            it.workoutsCompletedThisWeek.coerceAtMost(templates.size - 1)
        } ?: 0
    }
    
    val todayTemplate = remember(templates, nextWorkoutIndex, activeProgramState) {
        if (activeProgramState != null && templates.isNotEmpty()) {
            templates.getOrNull(nextWorkoutIndex)
        } else null
    }

    val todayMuscleGroups = remember(todayTemplate) {
        todayTemplate?.let { template ->
            // extract keywords or resolve
            val nameClean = template.name.lowercase()
            when {
                nameClean.contains("upper") -> listOf("CHEST", "BACK", "SHOULDERS", "ARMS")
                nameClean.contains("lower") -> listOf("QUADS", "HAMSTRINGS", "CALVES", "ABS")
                nameClean.contains("push") -> listOf("CHEST", "SHOULDERS", "TRICEPS")
                nameClean.contains("pull") -> listOf("BACK", "REAR DELTS", "BICEPS")
                nameClean.contains("legs") -> listOf("QUADS", "HAMSTRINGS", "CALVES")
                else -> listOf("GENERAL HYPERTROPHY")
            }
        } ?: emptyList()
    }

    // Precise consecutive calendar day streak
    val streakCount = remember(workoutsList) {
        val completed = workoutsList.filter { it.status == "completed" }
        val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDays = completed.map { sdfDay.format(Date(it.date)) }.toSet()
        
        var tempCalendar = Calendar.getInstance()
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

    // This week's total volume (completed last 7 days)
    val thisWeekVolume = remember(workoutsList) {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        workoutsList.filter { it.status == "completed" && it.date >= sevenDaysAgo }
            .sumOf { it.totalVolume }
    }

    // PRs set during the most recently completed workout
    val recentPrs = remember(recentWorkout, rawPrs) {
        recentWorkout?.let { wk ->
            rawPrs.filter { pr ->
                pr.bestWeight?.workoutId == wk.id || pr.bestEstimated1RM?.workoutId == wk.id
            }
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF16161A), Color.Black),
                    center = Offset(500f, -200f),
                    radius = 2500f
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcoming header block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "IRON LOGG",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "PRECISION INDEPENDENT ATHLETE SYSTEM",
                    color = com.example.ui.theme.GrayMedium,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(com.example.ui.theme.GlassDark, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, com.example.ui.theme.GlassBorderDark, shape = RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = Color.White
                )
            }
        }

        // 1. Warm Up protocol button at the top
        Button(
            onClick = { showWarmupDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(bottom = 12.dp)
                .testTag("warmup_protocol_button")
        ) {
            Text(
                text = "🔥 OPEN WARM UP PROTOCOL", 
                fontWeight = FontWeight.Black, 
                fontSize = 14.sp, 
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Scheduled routine / Choose split hero card
        if (activeProgramState != null && todayTemplate != null) {
            Text(
                text = "TODAY'S SCHEDULED WORKOUT (WEEK ${activeProgramState!!.currentWeekIndex + 1})",
                color = com.example.ui.theme.GrayMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("today_scheduled_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = todayTemplate.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        lineHeight = 30.sp
                    )
                    
                    if (todayMuscleGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            todayMuscleGroups.forEach { grp ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = grp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${todayTemplate.exercises.size} EXERCISES",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            val estDuration = todayTemplate.exercises.sumOf { it.targetSets * 4 }
                            Text(
                                text = "~$estDuration MIN DURATION",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { if (activeWorkout != null) onResumeWorkout() else onStartWorkout(todayTemplate.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_workout_button")
                    ) {
                        Text(
                            text = if (activeWorkout != null) "RESUME ACTIVE WORKOUT" else "START WORKOUT", 
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else if (activeProgramState == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToTab("programs") }
                    .testTag("onboarding_empty_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏋️‍♂️ SET UP STRUCTURED SPLIT",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You do not have any active training program. Select one of Jeff Nippard's premium science-based splits in the PROGRAM tab to begin tracking.",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. Horizontal scroll of quick stats (Streak, Volume, Weeks Left) on SOLID backdrop
        Text(
            text = "DASHBOARD ATHLETE METRICS",
            color = com.example.ui.theme.GrayMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1: Workout Streak
            Card(
                modifier = Modifier
                    .width(130.dp)
                    .height(90.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), // Non-translucent solid high-contrast, edge thin margins
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$streakCount DAYS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "RUNNING STREAK",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Stat 2: Week volume
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .height(90.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${thisWeekVolume.toInt()} KG",
                        color = com.example.ui.theme.AccentGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "WEEK'S TOTAL VOLUME",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Stat 3: Program Weeks left
            Card(
                modifier = Modifier
                    .width(130.dp)
                    .height(90.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    val remainingWeeks = if (activeProgramState != null) 12 - activeProgramState!!.currentWeekIndex else 12
                    Text(
                        text = "$remainingWeeks WEEKS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "PROGRAM LEFT",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // 4. Recently completed workout summary card
        if (recentWorkout != null) {
            Text(
                text = "RECENT COMPLETED WORKOUT LOG",
                color = com.example.ui.theme.GrayMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("recent_workout_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), // Solid high-contrast
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dateFormatted = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(recentWorkout!!.date)).uppercase()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = (recentWorkout!!.templateName ?: "AD-HOC WORKOUT").uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        
                        if (recentPrs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔥 ${recentPrs.size} PR",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = dateFormatted, 
                        color = com.example.ui.theme.GrayMedium, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${recentWorkout!!.totalVolume.toInt()} KG", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                            Text("VOLUME PRESSED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${recentWorkout!!.durationMinutes} MINS", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                            Text("DURATION MINUTES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                        }
                    }

                    if (recentPrs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⭐ PERSONAL RECORDS SET:",
                            color = com.example.ui.theme.AccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        recentPrs.forEach { pr ->
                            Text(
                                text = "• " + pr.exerciseId.uppercase().replace("_", " "),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Two side-by-side shortcut navigation link cards (Progress & PRs tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clickable { onNavigateToTab("progress") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PROGRESS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("ANALYTIC CHARTS", color = com.example.ui.theme.GrayMedium, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clickable { onNavigateToTab("prs") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
                border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PR TROPHIES", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("HALL OF FAME", color = com.example.ui.theme.GrayMedium, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
            }
        }

        // 5. Secondary "Start ad-hoc custom workout" link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "START AD-HOC CUSTOM WORKOUT",
                color = com.example.ui.theme.GrayMedium,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .background(com.example.ui.theme.GlassDark, RoundedCornerShape(8.dp))
                    .border(1.dp, com.example.ui.theme.GlassBorderDark, RoundedCornerShape(8.dp))
                    .clickable { onStartWorkout(null) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("start_ad_hoc_workout")
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // --- POPUP WARM UP DIALOG ---
    if (showWarmupDialog) {
        AlertDialog(
            onDismissRequest = { showWarmupDialog = false },
            containerColor = Color.Black,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            title = {
                Text(
                    text = "🏆 GENERAL WARM UP PROTOCOL",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Step 1: Cardio
                    Text(
                        text = "STAGE 1: LIGHT CARDIO",
                        color = com.example.ui.theme.AccentGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Perform 5 to 10 minutes of low-intensity cardio (treadmill walk, exercise bike, or row machine) to raise core body temperature and increase blood flow to target muscles.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 2: Dynamic stretches
                    Text(
                        text = "STAGE 2: DYNAMIC STRETCHES (10-15 REPS EACH)",
                        color = com.example.ui.theme.AccentGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active stretching to lubricate joints and prepare dynamic movement patterns. Tap the demo link next to each sequence to watch form details.",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val sequences = listOf(
                        Triple("Arm Swings", "Controlled horizontal and vertical arm swings to release the chest and shoulders.", "https://www.youtube.com/watch?v=330c9462u1w"),
                        Triple("Arm Circles", "Slow circles starting small and increasing radius to warm up rotator cuffs.", "https://www.youtube.com/watch?v=1b-bIatLdZ4"),
                        Triple("Front-to-Back Leg Swings", "Swing each leg back and forth dynamically while maintaining posture.", "https://www.youtube.com/watch?v=yW6WstYc4gM"),
                        Triple("Side-to-Side Leg Swings", "Swing leg across body to release hips, abductors, and groin.", "https://www.youtube.com/watch?v=eB22-Xm8X3E"),
                        Triple("Cable External Rotation", "Very light rotational resistance to prepare delicate shoulder stability.", "https://www.youtube.com/watch?v=a8I1bUf9OaY")
                    )

                    sequences.forEach { (name, desc, url) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(Color(0xFF141414), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Button(
                                    onClick = { uriHandler.openUri(url) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Watch",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WATCH DEMO", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showWarmupDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("DONE", fontWeight = FontWeight.Black)
                }
            }
        )
    }
}
