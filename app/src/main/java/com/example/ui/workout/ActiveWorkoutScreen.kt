package com.example.ui.workout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.Exercise
import com.example.model.LoggedExercise
import com.example.model.Workout
import com.example.model.WorkoutSet
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    repository: IronLogRepository,
    onNavigateToPlateCalc: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var activeWorkout by remember { mutableStateOf<Workout?>(null) }
    var availableExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var expandedExerciseIndex by remember { mutableStateOf(0) }
    
    var activeRestTimerEnd by remember { mutableStateOf<Long?>(null) }
    var activeRestTimerDuration by remember { mutableStateOf(0) }

    var showPlateCalcWeight by remember { mutableStateOf<Double?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { repository.getActiveWorkout().collect { activeWorkout = it } }
        launch { repository.getExercises().collect { availableExercises = it } }
    }

    if (activeWorkout == null) {
        Box(modifier = Modifier.fillMaxSize().background(BgColor), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TextPrimaryColor)
        }
        return
    }

    val workout = activeWorkout!!
    val totalExCount = workout.loggedExercises.size
    val completedExCount = workout.loggedExercises.count { ex -> ex.sets.isNotEmpty() && ex.sets.all { it.completedAt != null } }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = IronSpacing.x16, vertical = IronSpacing.x12)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimaryColor)
                    }
                    Spacer(modifier = Modifier.width(IronSpacing.x8))
                    Text(
                        text = workout.templateName ?: "Workout",
                        style = IronTypography.Title3,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateToPlateCalc) {
                            Icon(Icons.Outlined.Calculate, contentDescription = "Plate Calculator", tint = TextPrimaryColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.finishWorkout(workout)
                                    onFinish()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = BgColor),
                            shape = RoundedCornerShape(IronCorner.RadiusSm),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("FINISH", style = IronTypography.Headline, fontSize = 12.sp, color = BgColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(IronSpacing.x8))
                Text(
                    text = "Exercise ${minOf(completedExCount + 1, totalExCount)} of $totalExCount",
                    style = IronTypography.Caption.copy(color = TextSecondaryColor)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = TextPrimaryColor,
                contentColor = BgColor
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            itemsIndexed(workout.loggedExercises, key = { index, ex -> "${ex.exerciseId}_$index" }) { index, ex ->
                    val isActive = index == expandedExerciseIndex
                    val isCompleted = ex.sets.isNotEmpty() && ex.sets.all { it.completedAt != null }
                    
                    if (isCompleted && !isActive) {
                        // Collapsed row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = IronSpacing.x16, vertical = IronSpacing.x8)
                                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                .bouncyClick { expandedExerciseIndex = index }
                                .padding(IronSpacing.x16),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ex.exerciseName, style = IronTypography.Body)
                            Text("✓ ${ex.sets.size} sets", style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                        }
                    } else {
                        // Full card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = IronSpacing.x16, vertical = IronSpacing.x8)
                                .alpha(if (isActive) 1f else 0.5f)
                                .glassRecipe(RoundedCornerShape(IronCorner.RadiusLg))
                                .bouncyClick { expandedExerciseIndex = index }
                                .padding(IronSpacing.x20)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ex.exerciseName, style = IronTypography.Title3, modifier = Modifier.weight(1f))
                                    if (ex.videoUrl != null) {
                                        val uriHandler = LocalUriHandler.current
                                        Icon(
                                            imageVector = Icons.Outlined.PlayArrow,
                                            contentDescription = "Watch Video",
                                            tint = TextPrimaryColor,
                                            modifier = Modifier.bouncyClick { uriHandler.openUri(ex.videoUrl!!) }.padding(start = IronSpacing.x12)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(IronSpacing.x4))
                                
                                val targetGroup = ex.muscleGroup ?: "General"
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.15f), RoundedCornerShape(IronCorner.RadiusFull))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(targetGroup, style = IronTypography.Caption)
                                }
                                
                                Spacer(modifier = Modifier.height(IronSpacing.x16))
                                
                                // Stats grid: "target sets: 3 • reps: 10"
                                val totalWorkingSets = ex.sets.count { !it.isWarmup }
                                val firstTargetReps = ex.sets.firstOrNull { !it.isWarmup }?.targetReps ?: "N/A"
                                Text("Target sets: $totalWorkingSets • reps: $firstTargetReps", style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                                
                                ex.note?.let {
                                    Spacer(modifier = Modifier.height(IronSpacing.x8))
                                    Text("Note: $it", style = IronTypography.Footnote.copy(color = TextSecondaryColor))
                                }

                                Spacer(modifier = Modifier.height(IronSpacing.x24))
                                Text("LOG WORKING SETS", style = IronTypography.Caption.copy(color = TextTertiaryColor))
                                Spacer(modifier = Modifier.height(IronSpacing.x16))
                                
                                ex.sets.forEachIndexed { setIdx, set ->
                                    val isSetCompleted = set.completedAt != null
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = IronSpacing.x16),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("${setIdx + 1}", style = IronTypography.Headline, modifier = Modifier.width(24.dp))
                                        
                                        StepperChip(
                                            value = set.weight,
                                            unit = "KG",
                                            onValueChange = { newVal ->
                                                val updatedSets = ex.sets.toMutableList()
                                                updatedSets[setIdx] = set.copy(weight = newVal)
                                                val updatedEx = ex.copy(sets = updatedSets)
                                                val updatedList = workout.loggedExercises.toMutableList()
                                                updatedList[index] = updatedEx
                                                coroutineScope.launch { repository.saveWorkout(workout.copy(loggedExercises = updatedList)) }
                                            },
                                            modifier = Modifier.weight(1.2f)
                                        )

                                        StepperChip(
                                            value = set.reps.toDouble(),
                                            unit = "REPS",
                                            onValueChange = { newVal ->
                                                val updatedSets = ex.sets.toMutableList()
                                                updatedSets[setIdx] = set.copy(reps = newVal.toInt())
                                                val updatedEx = ex.copy(sets = updatedSets)
                                                val updatedList = workout.loggedExercises.toMutableList()
                                                updatedList[index] = updatedEx
                                                coroutineScope.launch { repository.saveWorkout(workout.copy(loggedExercises = updatedList)) }
                                            },
                                            modifier = Modifier.weight(1.1f),
                                            step = 1.0
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(if (isSetCompleted) TextPrimaryColor else Color.White.copy(alpha = 0.05f), RoundedCornerShape(IronCorner.RadiusMd))
                                                .border(
                                                    1.dp,
                                                    if (isSetCompleted) Color.Transparent else Color.White.copy(alpha = 0.25f),
                                                    RoundedCornerShape(IronCorner.RadiusMd)
                                                )
                                                .bouncyClick {
                                                    val nowDone = !isSetCompleted
                                                    val updatedSets = ex.sets.toMutableList()
                                                    updatedSets[setIdx] = set.copy(completedAt = if (nowDone) System.currentTimeMillis() else null)
                                                    
                                                    if (nowDone && set.rpe == null) {
                                                        updatedSets[setIdx] = updatedSets[setIdx].copy(rpe = 8.0f)
                                                    }
                                                    
                                                    val updatedEx = ex.copy(sets = updatedSets)
                                                    val updatedList = workout.loggedExercises.toMutableList()
                                                    updatedList[index] = updatedEx
                                                    coroutineScope.launch { repository.saveWorkout(workout.copy(loggedExercises = updatedList)) }
                                                    
                                                    if (nowDone) {
                                                        val restSecs = set.restTimeSeconds ?: 90
                                                        activeRestTimerDuration = restSecs
                                                        activeRestTimerEnd = System.currentTimeMillis() + (restSecs * 1000L)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Check, contentDescription = "Done", tint = if (isSetCompleted) BgColor else TextPrimaryColor, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                
                                val allSetsCompletedHere = ex.sets.isNotEmpty() && ex.sets.all { it.completedAt != null }
                                if (isActive && allSetsCompletedHere) {
                                    Spacer(modifier = Modifier.height(IronSpacing.x24))
                                    Button(
                                        onClick = {
                                            if (index < workout.loggedExercises.size - 1) expandedExerciseIndex = index + 1
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = BgColor),
                                        shape = RoundedCornerShape(IronCorner.RadiusSm)
                                    ) {
                                        Text("Next →", style = IronTypography.Headline, color = BgColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Rest Timer
            if (activeRestTimerEnd != null) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(IronSpacing.x16)
                ) {
                    com.example.ui.workout.RestTimerBar(
                        endTimeMillis = activeRestTimerEnd!!,
                        totalDurationSeconds = activeRestTimerDuration,
                        onDismiss = { activeRestTimerEnd = null }
                    )
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            containerColor = Color(0xFF1C1C1E),
            confirmButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancel", color = TextPrimaryColor) }
            },
            title = { Text("Add Exercise", style = IronTypography.Title3, color = TextPrimaryColor) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(availableExercises) { ex ->
                        Text(
                            text = ex.name,
                            style = IronTypography.Body,
                            color = TextPrimaryColor,
                            modifier = Modifier.fillMaxWidth().bouncyClick {
                                val newList = workout.loggedExercises + LoggedExercise(exerciseId = ex.id, exerciseName = ex.name, sets = listOf(WorkoutSet(reps=10)))
                                coroutineScope.launch {
                                    repository.saveWorkout(workout.copy(loggedExercises = newList))
                                    showAddExerciseDialog = false
                                }
                            }.padding(vertical = 14.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun StepperChip(
    value: Double,
    unit: String,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    step: Double = 2.5
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(IronCorner.RadiusMd))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(IronCorner.RadiusMd)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = IronCorner.RadiusMd, bottomStart = IronCorner.RadiusMd))
                .clickable { onValueChange(maxOf(0.0, value - step)) },
            contentAlignment = Alignment.Center
        ) {
            Text("-", style = IronTypography.Headline, color = TextPrimaryColor)
        }
        
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val displayVal = if (value % 1.0 == 0.0) "${value.toInt()}" else String.format("%.1f", value)
                Text(
                    text = displayVal,
                    style = IronTypography.Headline,
                    fontSize = 13.sp
                )
                Text(
                    text = unit,
                    style = IronTypography.Caption.copy(fontSize = 8.sp, color = TextSecondaryColor, fontWeight = FontWeight.Bold)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = IronCorner.RadiusMd, bottomEnd = IronCorner.RadiusMd))
                .clickable { onValueChange(value + step) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", style = IronTypography.Headline, color = TextPrimaryColor)
        }
    }
}
