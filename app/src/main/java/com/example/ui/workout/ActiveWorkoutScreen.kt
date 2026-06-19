package com.example.ui.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import com.example.data.IronLogRepository
import com.example.model.Exercise
import com.example.model.LoggedExercise
import com.example.model.Workout
import com.example.model.WorkoutSet
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    repository: IronLogRepository,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var activeWorkout by remember { mutableStateOf<Workout?>(null) }
    var availableExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToSwapIndex by remember { mutableStateOf<Int?>(null) }
    var prs by remember { mutableStateOf<Map<String, com.example.model.PersonalRecord>>(emptyMap()) }
    var showPlateCalcWeight by remember { mutableStateOf<Double?>(null) }
    
    val listState = rememberLazyListState()
    var expandedExerciseIndex by remember { mutableStateOf(0) }
    var activeRestTimerEnd by remember { mutableStateOf<Long?>(null) }
    var activeRestTimerDuration by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        launch { repository.getActiveWorkout().collect { activeWorkout = it } }
        launch { repository.getExercises().collect { availableExercises = it } }
        launch { repository.getPersonalRecords().collect { recs -> prs = recs.associateBy { it.exerciseId } } }
    }

    if (activeWorkout == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(IronSpacing.Large), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).skeleton().clip(RoundedCornerShape(IronSpacing.CardCornerRadius)))
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF1C1C22), Color.Black),
                center = Offset(500f, -200f),
                radius = 2500f
            )
        ),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text((activeWorkout!!.templateName ?: "FREE WORKOUT").uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.finishWorkout(activeWorkout!!)
                                    onFinish()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("FINISH", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                )
                val completedExCount = activeWorkout!!.loggedExercises.count { ex -> ex.sets.isNotEmpty() && ex.sets.all { it.completedAt != null } }
                val totalExCount = activeWorkout!!.loggedExercises.size
                Text(
                    text = "EXERCISE ${completedExCount.takeIf { it < totalExCount }?.plus(1) ?: totalExCount} OF $totalExCount",
                    color = com.example.ui.theme.GrayMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Exercise")
            }
        },
        bottomBar = {
            if (activeRestTimerEnd != null) {
                RestTimerBar(
                    endTimeMillis = activeRestTimerEnd!!,
                    totalDurationSeconds = activeRestTimerDuration,
                    onDismiss = { activeRestTimerEnd = null }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(activeWorkout!!.loggedExercises, key = { it.exerciseId }) { exercise ->
                val index = activeWorkout!!.loggedExercises.indexOf(exercise)
                val pr = prs[exercise.exerciseId]
                LoggedExerciseCard(
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(300)),
                    loggedExercise = exercise,
                    isExpanded = index == expandedExerciseIndex,
                    pr = pr,
                    onToggleExpand = {
                        expandedExerciseIndex = if (expandedExerciseIndex == index) -1 else index
                    },
                    onUpdate = { updatedExercise ->
                        val updatedList = activeWorkout!!.loggedExercises.toMutableList()
                        updatedList[index] = updatedExercise
                        coroutineScope.launch {
                            repository.saveWorkout(activeWorkout!!.copy(loggedExercises = updatedList))
                        }
                    },
                    onSwap = { exerciseToSwapIndex = index },
                    onCalculatePlates = { weight -> showPlateCalcWeight = weight },
                    onSetCompleted = { restDurationSeconds, isLastSet ->
                        if (restDurationSeconds > 0) {
                            activeRestTimerDuration = restDurationSeconds
                            activeRestTimerEnd = System.currentTimeMillis() + (restDurationSeconds * 1000L)
                        }
                        if (isLastSet && index < activeWorkout!!.loggedExercises.size - 1) {
                            expandedExerciseIndex = index + 1
                            coroutineScope.launch {
                                listState.animateScrollToItem(index + 1)
                            }
                        }
                    },
                    onNextExercise = {
                        if (index < activeWorkout!!.loggedExercises.size - 1) {
                            expandedExerciseIndex = index + 1
                            coroutineScope.launch {
                                listState.animateScrollToItem(index + 1)
                            }
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showAddExerciseDialog || exerciseToSwapIndex != null) {
        val isSwapping = exerciseToSwapIndex != null
        AlertDialog(
            onDismissRequest = { 
                showAddExerciseDialog = false
                exerciseToSwapIndex = null
            },
            title = { Text(if (isSwapping) "SWAP EXERCISE" else "ADD EXERCISE", fontWeight = FontWeight.Black) },
            containerColor = Color.Black,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFA0A0A0),
            shape = RoundedCornerShape(0.dp),
            text = {
                LazyColumn {
                    if (isSwapping) {
                        val indexStr = exerciseToSwapIndex!!
                        val currentEx = activeWorkout!!.loggedExercises.getOrNull(indexStr)
                        if (currentEx != null && currentEx.substitutionOpts.isNotEmpty()) {
                            item {
                                Text(
                                    "RECOMMENDED SUBSTITUTIONS",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(currentEx.substitutionOpts) { subName ->
                                Text(
                                    text = subName.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bouncyClick {
                                            val newList = activeWorkout!!.loggedExercises.toMutableList()
                                            newList[indexStr] = currentEx.copy(
                                                exerciseId = subName,
                                                exerciseName = subName,
                                                isSubstitution = true
                                            )
                                            coroutineScope.launch {
                                                repository.saveWorkout(activeWorkout!!.copy(loggedExercises = newList))
                                                exerciseToSwapIndex = null
                                            }
                                        }
                                        .padding(vertical = 14.dp)
                                )
                                HorizontalDivider(color = Color(0xFF222222))
                            }
                            item {
                                Text(
                                    "ALL EXERCISES",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(availableExercises) { _, ex ->
                        Text(
                            text = ex.name.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClick {
                                    if (isSwapping) {
                                        val indexStr = exerciseToSwapIndex!!
                                        val currentEx = activeWorkout!!.loggedExercises.getOrNull(indexStr)
                                        if (currentEx != null) {
                                            val newList = activeWorkout!!.loggedExercises.toMutableList()
                                            newList[indexStr] = currentEx.copy(
                                                exerciseId = ex.id,
                                                exerciseName = ex.name,
                                                isSubstitution = true
                                            )
                                            coroutineScope.launch {
                                                repository.saveWorkout(activeWorkout!!.copy(loggedExercises = newList))
                                                exerciseToSwapIndex = null
                                            }
                                        }
                                    } else {
                                        val newList = activeWorkout!!.loggedExercises + LoggedExercise(
                                            exerciseId = ex.id,
                                            exerciseName = ex.name,
                                            sets = listOf(WorkoutSet())
                                        )
                                        coroutineScope.launch {
                                            repository.saveWorkout(activeWorkout!!.copy(loggedExercises = newList))
                                            showAddExerciseDialog = false
                                        }
                                    }
                                }
                                .padding(vertical = 16.dp)
                        )
                        HorizontalDivider(color = Color(0xFF333333))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {  
                    showAddExerciseDialog = false
                    exerciseToSwapIndex = null 
                 }, modifier = Modifier.bouncy()) { Text("CANCEL", color = Color.White) }
            }
        )
    }

    if (showPlateCalcWeight != null) {
        BarbellVisualizer(
            initialTargetWeight = showPlateCalcWeight!!,
            isKgInitially = true,
            onDismiss = { showPlateCalcWeight = null }
        )
    }
}

@Composable
fun LoggedExerciseCard(
    modifier: Modifier = Modifier,
    loggedExercise: LoggedExercise,
    isExpanded: Boolean = true,
    pr: com.example.model.PersonalRecord? = null,
    onToggleExpand: () -> Unit = {},
    onUpdate: (LoggedExercise) -> Unit,
    onSwap: () -> Unit,
    onCalculatePlates: (Double) -> Unit,
    onSetCompleted: (Int, Boolean) -> Unit = { _, _ -> },
    onNextExercise: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .bouncyClick { onToggleExpand() }
            .glassCard(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = loggedExercise.exerciseName, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loggedExercise.videoUrl != null) {
                        IconButton(onClick = {  uriHandler.openUri(loggedExercise.videoUrl!!)  }, modifier = Modifier.bouncy()) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = "Watch Video", tint = Color.White)
                        }
                    }
                    TextButton(onClick = onSwap) {
                        Text("SWAP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            val totalSets = loggedExercise.sets.size
            val completedSets = loggedExercise.sets.count { it.completedAt != null }
            
            if (!isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$completedSets / $totalSets Sets Completed", color = com.example.ui.theme.GrayMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (completedSets == totalSets) {
                        Icon(Icons.Outlined.Check, contentDescription = "Done", tint = Color.White)
                    }
                }
            } else {
            
                if (loggedExercise.note != null || loggedExercise.targetRestStr != null || loggedExercise.techniqueRequirements != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    loggedExercise.targetRestStr?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("Rest: $it", color = Color.White, fontSize = 13.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0x1Fffffff))
                        )
                    }
                    loggedExercise.techniqueRequirements?.let {
                        if (it != "N/A" && it.isNotEmpty()) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Technique: $it", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0x3300FF66))
                            )
                        }
                    }
                }
                loggedExercise.note?.let {
                    Text(
                        text = "Cue: $it",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            val isBarbell = remember(loggedExercise.exerciseName) {
                val lower = loggedExercise.exerciseName.lowercase()
                lower.contains("barbell") || lower.contains("squat") || lower.contains("press") || lower.contains("deadlift") || lower.contains("row") || lower.contains("bench") || lower.contains("rdl") || lower.contains("clean") || lower.contains("snatch") || lower.contains("smith") || lower.contains("thruster") || lower.contains("curl")
            }

            if (isBarbell) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val firstSetWeight = loggedExercise.sets.firstOrNull()?.weight ?: 100.0
                            val targetWeight = if (firstSetWeight > 0) firstSetWeight else 100.0
                            onCalculatePlates(targetWeight)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CALCULATE PLATES \uD83C\uDFCB\uFE0F",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SET", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = com.example.ui.theme.GrayMedium)
                    Text("KG", modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = com.example.ui.theme.GrayMedium)
                    Text("REPS", modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = com.example.ui.theme.GrayMedium)
                    Text("DONE", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = com.example.ui.theme.GrayMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))

                loggedExercise.sets.forEachIndexed { setIndex, set ->
                    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                        
                        val isDone = set.completedAt != null
                        val isPrTriggered = isDone && pr != null && pr.bestWeight != null && set.weight > pr.bestWeight.value
                        val setScale by animateFloatAsState(
                            targetValue = if (isPrTriggered) 1.03f else if (isDone) 1.02f else 1f, 
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
                        )
                        
                        val glowColor = if (isPrTriggered) com.example.ui.theme.ErrorColor else Color.White
                        val baseModifier = Modifier
                            .fillMaxWidth()
                            .scale(setScale)
                        val glowModifier = if (isDone) {
                            baseModifier.shadow(if (isPrTriggered) 12.dp else 4.dp, RoundedCornerShape(12.dp), spotColor = glowColor)
                        } else {
                            baseModifier
                        }

                        Row(
                            modifier = glowModifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(0.5f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${setIndex + 1}",
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                if (pr != null && pr.bestWeight != null && set.weight > pr.bestWeight.value) {
                                    Box(
                                        modifier = Modifier
                                            .background(com.example.ui.theme.ErrorColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("PR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            
                            if (editingSetIndex == setIndex) {
                                Column(modifier = Modifier.weight(3f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        StepperControl(
                                            value = set.weight,
                                            step = 2.5,
                                            onValueChange = { newVal ->
                                                val newSets = loggedExercise.sets.toMutableList()
                                                newSets[setIndex] = set.copy(weight = newVal)
                                                onUpdate(loggedExercise.copy(sets = newSets))
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StepperControl(
                                            value = set.reps.toDouble(),
                                            step = 1.0,
                                            onValueChange = { newVal ->
                                                val newSets = loggedExercise.sets.toMutableList()
                                                newSets[setIndex] = set.copy(reps = newVal.toInt())
                                                onUpdate(loggedExercise.copy(sets = newSets))
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Button(
                                        onClick = { 
                                            editingSetIndex = null 
                                            if (set.completedAt == null) {
                                                val newSets = loggedExercise.sets.toMutableList()
                                                newSets[setIndex] = set.copy(completedAt = System.currentTimeMillis())
                                                onUpdate(loggedExercise.copy(sets = newSets))
                                                val isLastSetInExercise = setIndex == totalSets - 1
                                                val rest = if (!set.isWarmup) (set.restTimeSeconds ?: 120) else 0
                                                onSetCompleted(rest, isLastSetInExercise)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.GlassLight),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("CONFIRM", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // View Mode
                                val wText = if (set.weight == 0.0) "-" else if (set.weight % 1.0 == 0.0) set.weight.toInt().toString() else set.weight.toString()
                                val rText = if (set.reps == 0) "-" else set.reps.toString()
                                Box(
                                    modifier = Modifier.weight(1.5f).height(44.dp).padding(horizontal = 4.dp).background(com.example.ui.theme.GlassDark, RoundedCornerShape(12.dp)).bouncyClick { editingSetIndex = setIndex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(wText, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                }
                                Box(
                                    modifier = Modifier.weight(1.5f).height(44.dp).padding(horizontal = 4.dp).background(com.example.ui.theme.GlassDark, RoundedCornerShape(12.dp)).bouncyClick { editingSetIndex = setIndex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(rText, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                }
                            }
                            
                            
                            val boxColor by animateColorAsState(if (isDone) Color.White else Color.Transparent, animationSpec = tween(300))
                            val borderColor by animateColorAsState(if (isDone) Color.Transparent else com.example.ui.theme.GlassBorderLight, animationSpec = tween(300))
                            
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .height(44.dp)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        color = boxColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        borderColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .bouncyClick {
                                        val newSets = loggedExercise.sets.toMutableList()
                                        if (set.completedAt == null) {
                                            newSets[setIndex] = set.copy(completedAt = System.currentTimeMillis())
                                            onUpdate(loggedExercise.copy(sets = newSets))
                                            // Trigger rest timer
                                            val isLastSetInExercise = setIndex == totalSets - 1
                                            val rest = if (!set.isWarmup) (set.restTimeSeconds ?: 120) else 0
                                            onSetCompleted(rest, isLastSetInExercise)
                                        } else {
                                            newSets[setIndex] = set.copy(completedAt = null)
                                            onUpdate(loggedExercise.copy(sets = newSets))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Done", tint = Color.White)
                                }
                            }
                        }
                        
                        // Target goals
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (set.isWarmup) {
                                "${set.notes?.replace("target load", "working weight") ?: "Warm-up set"} — ${set.targetReps ?: "-"} reps"
                            } else {
                                "Target: ${set.targetReps ?: "-"} reps @ RPE ${set.targetRpe ?: "-"}"
                            },
                            color = if (set.isWarmup) Color(0xFFFFD700) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (!set.isWarmup && set.notes != null) {
                            Text(
                                text = set.notes,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 8.dp).weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // RPE Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RPE: ${set.rpe ?: "-"}", 
                            color = com.example.ui.theme.GrayMedium, 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Slider(
                            value = set.rpe ?: 8f,
                            onValueChange = { newVal ->
                                val rounded = (Math.round(newVal * 2) / 2.0).toFloat()
                                val newSets = loggedExercise.sets.toMutableList()
                                newSets[setIndex] = set.copy(rpe = rounded)
                                onUpdate(loggedExercise.copy(sets = newSets))
                            },
                            valueRange = 1f..10f,
                            steps = 17,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = com.example.ui.theme.GlassBorderLight
                            )
                        )
                    }
                }
            } // Close forEachIndexed
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val lastSet = loggedExercise.sets.lastOrNull() ?: WorkoutSet()
                            val newSets = loggedExercise.sets + lastSet.copy(completedAt = null, isWarmup = false, setNumber = lastSet.setNumber + 1)
                            onUpdate(loggedExercise.copy(sets = newSets))
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.GlassLight, contentColor = Color.White),
                        border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ ADD SET", fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                    
                    Button(
                        onClick = onNextExercise,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("NEXT \u2192", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
}

@Composable
fun StepperControl(
    value: Double,
    step: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(36.dp)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(com.example.ui.theme.GlassLight, RoundedCornerShape(12.dp))
                .bouncyClick { onValueChange((value - step).coerceAtLeast(0.0)) },
            contentAlignment = Alignment.Center
        ) {
            Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        
        Text(
            text = if (value == 0.0) "-" else if (value % 1.0 == 0.0) value.toInt().toString() else value.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(com.example.ui.theme.GlassLight, RoundedCornerShape(12.dp))
                .bouncyClick { onValueChange(value + step) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}
