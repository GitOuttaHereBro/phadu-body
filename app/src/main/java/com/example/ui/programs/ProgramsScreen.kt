package com.example.ui.programs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.ActiveProgramState
import com.example.model.Program
import com.example.model.ProgramDay
import com.example.model.Workout
import com.example.model.toWorkout
import com.example.model.LoggedExercise
import com.example.model.WorkoutSet
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProgramsScreen(repository: IronLogRepository, onProgramStarted: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var program by remember { mutableStateOf<Program?>(null) }
    var activeProgramState by remember { mutableStateOf<ActiveProgramState?>(null) }
    var workoutsList by remember { mutableStateOf<List<Workout>>(emptyList()) }

    LaunchedEffect(Unit) {
        launch { repository.getActiveProgramState().collect { activeProgramState = it } }
        launch { repository.getWorkouts().collect { workoutsList = it } }
        
        // Ensure static program is loaded
        try {
            val json = context.assets.open("jeff_nippard.json").bufferedReader().use { it.readText() }
            val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(Program::class.java)
            val rawProgram = adapter.fromJson(json)
            program = com.example.model.ProgramValidator.validateAndSanitize(rawProgram)
        } catch (e: Exception) {}
    }

    if (program == null || activeProgramState == null) {
        Box(modifier = Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TextPrimaryColor)
        }
        return
    }

    val state = activeProgramState!!
    var selectedWeekIndex by remember(state.currentWeekIndex) { mutableIntStateOf(state.currentWeekIndex) }
    val weekKeys = program!!.weeks.keys.sortedBy { it.replace("week", "").toIntOrNull() ?: 0 }
    val currentWeekKey = weekKeys.getOrNull(selectedWeekIndex) ?: "week1"
    val daysList = program!!.weeks[currentWeekKey]?.days ?: emptyList()
    var selectedDayIndex by remember(selectedWeekIndex) { 
        mutableIntStateOf(if (selectedWeekIndex == state.currentWeekIndex) state.currentDayIndex else 0) 
    }

    Scaffold(containerColor = BgColor) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = IronSpacing.x16, vertical = IronSpacing.x12)
        ) {
            // Program Header
            Text(
                text = program!!.programName.uppercase(),
                style = IronTypography.Caption.copy(color = TextSecondaryColor, letterSpacing = 2.sp, fontSize = 10.sp),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Protocol Directory",
                style = IronTypography.Title1,
                modifier = Modifier.padding(bottom = IronSpacing.x24)
            )

            // Week Selector
            Text(
                text = "WEEK PHASE",
                style = IronTypography.Caption.copy(color = TextTertiaryColor, letterSpacing = 2.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(IronSpacing.x12)
            ) {
                weekKeys.forEachIndexed { index, _ ->
                    val isSelected = selectedWeekIndex == index
                    val isCurrent = index == state.currentWeekIndex
                    
                    Box(
                        modifier = Modifier
                            .bouncyClick { selectedWeekIndex = index }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) TextPrimaryColor else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(IronCorner.RadiusFull)
                            )
                            .background(
                                if (isSelected) TextPrimaryColor.copy(alpha = 0.1f) else Color.Transparent,
                                RoundedCornerShape(IronCorner.RadiusFull)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "WEEK ${index + 1}",
                                style = IronTypography.Headline.copy(
                                    color = if (isSelected) TextPrimaryColor else TextSecondaryColor,
                                    fontSize = 13.sp
                                )
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.size(6.dp).background(SuccessColor, RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(IronSpacing.x32))

            // Day Selection
            Text(
                text = "SESSION SELECTOR",
                style = IronTypography.Caption.copy(color = TextTertiaryColor, letterSpacing = 2.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val dayAbbrs = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(IronSpacing.x12)
            ) {
                daysList.forEachIndexed { i, day ->
                    val isSelected = selectedDayIndex == i
                    val weekKey = weekKeys[selectedWeekIndex]
                    val isCompleted = workoutsList.any { it.status == "completed" && it.templateId == "${weekKey}_$i" }
                    val abbr = dayAbbrs.getOrNull(i) ?: "D${i+1}"
                    
                    Column(
                        modifier = Modifier
                            .width(54.dp)
                            .bouncyClick { selectedDayIndex = i }
                            .glassRecipe(RoundedCornerShape(IronCorner.RadiusMd))
                            .then(
                                if (isSelected) Modifier.background(TextPrimaryColor, RoundedCornerShape(IronCorner.RadiusMd))
                                else Modifier
                            )
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            abbr, 
                            style = IronTypography.Caption.copy(
                                color = if (isSelected) BgColor else TextSecondaryColor,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        )
                        if (day.isRestDay) {
                            Icon(
                                Icons.Outlined.Hotel, 
                                contentDescription = null, 
                                tint = if (isSelected) BgColor else TextTertiaryColor,
                                modifier = Modifier.size(12.dp).padding(top = 2.dp)
                            )
                        } else if (isCompleted) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.size(6.dp).background(if (isSelected) BgColor else SuccessColor, RoundedCornerShape(3.dp)))
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.size(2.dp).background(if (isSelected) BgColor else TextTertiaryColor, RoundedCornerShape(1.dp)))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(IronSpacing.x32))

            val day = daysList.getOrNull(selectedDayIndex)
            if (day != null) {
                // Selected Day Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                        .padding(IronSpacing.x24)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(4.dp).height(24.dp).background(TextPrimaryColor, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(day.dayName.uppercase(), style = IronTypography.Title2)
                            }
                            
                            if (day.isRestDay) {
                                Icon(Icons.Outlined.Hotel, contentDescription = null, tint = TextSecondaryColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(IronSpacing.x20))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DayStat(label = "LOAD", value = if (day.isRestDay) "REST" else if (selectedDayIndex == 2) "MAX" else "HIGH", modifier = Modifier.weight(1f))
                            DayStat(label = "EXERCISES", value = "${day.exercises.size}", modifier = Modifier.weight(1f))
                            DayStat(label = "MODULES", value = "CORE", modifier = Modifier.weight(1f))
                        }
                        
                        if (!day.isRestDay) {
                            Spacer(modifier = Modifier.height(IronSpacing.x24))
                            Button(
                                onClick = {
                                    val weekKey = weekKeys[selectedWeekIndex]
                                    var newW = day.toWorkout(weekKey, selectedDayIndex)
                                    val completedWorkouts = workoutsList.filter { it.status == "completed" }.sortedByDescending { it.date }
                                    val newExs = newW.loggedExercises.map { ex ->
                                        val lastEx = completedWorkouts.mapNotNull { w -> w.loggedExercises.find { it.exerciseId == ex.exerciseId } }.firstOrNull()
                                        if (lastEx != null) {
                                            val newSets = ex.sets.map { set ->
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
                                            ex.copy(sets = ex.sets.map { s -> s.copy(reps = s.targetReps ?: 0) })
                                        }
                                    }
                                    newW = newW.copy(loggedExercises = newExs)
                                    coroutineScope.launch {
                                        repository.saveWorkout(newW)
                                        onProgramStarted()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = BgColor),
                                shape = RoundedCornerShape(IronCorner.RadiusMd)
                            ) {
                                Text("INITIALIZE SESSION", style = IronTypography.Headline, color = BgColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(IronSpacing.x32))

                // Exercises
                if (!day.isRestDay) {
                    Text(
                        text = "SESSION PROTOCOL",
                        style = IronTypography.Caption.copy(color = TextTertiaryColor, letterSpacing = 2.sp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    day.exercises.forEach { ex ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = IronSpacing.x16)
                                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                .padding(IronSpacing.x20)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.name, style = IronTypography.Headline)
                                        Text(
                                            "${ex.muscleGroup?.uppercase() ?: "GENERAL"} • ${ex.workingSets ?: "3"} SETS",
                                            style = IronTypography.Caption.copy(color = TextSecondaryColor)
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${ex.repRange ?: ex.reps ?: "10"}",
                                            style = IronTypography.Title3,
                                            color = TextPrimaryColor
                                        )
                                        Text(
                                            text = "REPS",
                                            style = IronTypography.Caption.copy(fontSize = 8.sp, color = TextTertiaryColor, letterSpacing = 1.sp)
                                        )
                                    }
                                }
                                
                                ex.notes?.let {
                                    Spacer(modifier = Modifier.height(IronSpacing.x12))
                                    Text(
                                        it, 
                                        style = IronTypography.Footnote.copy(color = TextTertiaryColor, lineHeight = 16.sp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Rest day illustration/message
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = IronSpacing.x48),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Hotel, contentDescription = null, tint = TextPrimaryColor.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(IronSpacing.x16))
                        Text("RECOVERY PROTOCOL ACTIVE", style = IronTypography.Headline, color = TextSecondaryColor)
                        Text("Focus on hydration, mobility, and sleep.", style = IronTypography.Body.copy(color = TextTertiaryColor, textAlign = TextAlign.Center))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun DayStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = IronTypography.Caption.copy(color = TextTertiaryColor, fontSize = 9.sp, letterSpacing = 1.sp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = IronTypography.Headline, fontSize = 16.sp)
    }
}
