package com.example.ui.prs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.Exercise
import com.example.model.PersonalRecord
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRsScreen(repository: IronLogRepository) {
    var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var rawPrs by remember { mutableStateOf<List<PersonalRecord>>(emptyList()) }
    var selectedMuscleGroup by remember { mutableStateOf("ALL") }
    var sortByDate by remember { mutableStateOf(true) } // true: Date (Most Recent), false: Alphabetical (A-Z)

    LaunchedEffect(Unit) {
        repository.getExercises().combine(repository.getPersonalRecords()) { ex, recs ->
            exercises = ex
            rawPrs = recs
        }.collect {}
    }

    // Active muscle groups dynamically extracted from exercises that have records
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }
    val muscleGroups = remember(rawPrs, exerciseMap) {
        val groups = rawPrs.mapNotNull { pr ->
            exerciseMap[pr.exerciseId]?.muscleGroup?.uppercase()
        }.toSet()
        listOf("ALL") + groups.sorted()
    }

    // Filtered and sorted record items
    val recordItems = remember(rawPrs, exerciseMap, selectedMuscleGroup, sortByDate) {
        rawPrs.mapNotNull { pr ->
            val ex = exerciseMap[pr.exerciseId]
            if (ex != null) {
                PRItem(
                    pr = pr,
                    exercise = ex,
                    name = ex.name,
                    muscleGroup = ex.muscleGroup.uppercase()
                )
            } else null
        }.filter {
            selectedMuscleGroup == "ALL" || it.muscleGroup == selectedMuscleGroup
        }.sortedWith { a, b ->
            if (sortByDate) {
                // sort by date descending
                val dateA = maxOf(a.pr.bestWeight?.date ?: 0L, a.pr.bestEstimated1RM?.date ?: 0L)
                val dateB = maxOf(b.pr.bestWeight?.date ?: 0L, b.pr.bestEstimated1RM?.date ?: 0L)
                dateB.compareTo(dateA)
            } else {
                a.name.compareTo(b.name, ignoreCase = true)
            }
        }
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
                title = {
                    Text(
                        text = "TROPHY CASE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Muscle Group Horizontal Selector (Filter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                muscleGroups.forEach { group ->
                    val isSelected = selectedMuscleGroup == group
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) Color.White else com.example.ui.theme.GlassDark,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else com.example.ui.theme.GlassBorderDark,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .bouncyClick { selectedMuscleGroup = group }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("muscle_filter_$group")
                    ) {
                        Text(
                            text = group,
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Sorting bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recordItems.size} PERSONAL RECORDS",
                    color = com.example.ui.theme.GrayMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )

                Row(
                    modifier = Modifier
                        .glassCard()
                        .bouncyClick { sortByDate = !sortByDate }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("sort_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sortByDate) Icons.Outlined.ArrowDownward else Icons.Outlined.FilterList,
                        contentDescription = "Sort Icon",
                        tint = com.example.ui.theme.AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sortByDate) "MOST RECENT" else "ALPHABETICAL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recordItems.isEmpty()) {
                val message = if (selectedMuscleGroup != "ALL") 
                                "No records found for $selectedMuscleGroup muscle group." 
                                else "No Personal Records logged yet. Finish a program workout to establish your targets!"
                EmptyState(message = message, modifier = Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recordItems) { item ->
                        PRTrophyCard(item)
                    }
                }
            }
        }
    }
}

data class PRItem(
    val pr: PersonalRecord,
    val exercise: Exercise,
    val name: String,
    val muscleGroup: String
)

@Composable
fun PRTrophyCard(item: PRItem) {
    val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()
    
    // Determine if PR was earned within last 7 days
    val isNewWeight = (now - (item.pr.bestWeight?.date ?: 0L)) < sevenDaysMs
    val isNewE1rm = (now - (item.pr.bestEstimated1RM?.date ?: 0L)) < sevenDaysMs
    val isNew = isNewWeight || isNewE1rm

    val formattedDate = remember(item.pr) {
        val maxTime = maxOf(item.pr.bestWeight?.date ?: 0L, item.pr.bestEstimated1RM?.date ?: 0L)
        if (maxTime > 0L) {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(maxTime)).uppercase()
        } else "N/A"
    }

    // Glass enhancement if NEW
    val cardBackground = if (isNew) Color(0x3DFFFFFF) else com.example.ui.theme.GlassDark
    val borderHighlight = if (isNew) Color.White.copy(alpha = 0.5f) else com.example.ui.theme.GlassBorderDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pr_card_${item.exercise.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, borderHighlight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row (Muscle group tag + Optional NEW badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.muscleGroup,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (isNew) {
                    // Elevated brighter glowing glass treatment tag with full white text
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .border(1.dp, com.example.ui.theme.AccentGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Exercise Title
            Text(
                text = item.name,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.heightIn(min = 36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Best Weight details block (opaque background for heavy numbers readability)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131313), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "${item.pr.bestWeight?.value ?: 0.0} ${item.exercise.unit}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "BEST WEIGHT (${item.pr.bestWeight?.reps ?: 0} REPS)",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Best Estimated 1RM blocks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131313), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    val est1RM = item.pr.bestEstimated1RM?.value?.toInt() ?: 0
                    Text(
                        text = "$est1RM ${item.exercise.unit}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "ESTIMATED 1RM",
                        color = com.example.ui.theme.GrayMedium,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date set reference at bottom
            Text(
                text = "SET ON $formattedDate",
                color = com.example.ui.theme.GrayMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
