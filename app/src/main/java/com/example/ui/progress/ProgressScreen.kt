package com.example.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IronLogRepository
import com.example.model.Exercise
import com.example.model.PersonalRecord
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(repository: IronLogRepository) {
    var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var prs by remember { mutableStateOf<Map<String, PersonalRecord>>(emptyMap()) }

    LaunchedEffect(Unit) {
        launch {
            repository.getExercises().combine(repository.getPersonalRecords()) { ex, recs ->
                exercises = ex
                prs = recs.associateBy { it.exerciseId }
            }.collect {}
        }
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
            TopAppBar(
                title = { Text("PROGRESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            val list = exercises.filter { prs.containsKey(it.id) }
            items(list) { ex ->
                val pr = prs[ex.id]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark),
                    border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(ex.name.uppercase(), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${pr?.bestWeight?.value ?: 0.0} ${ex.unit} x ${pr?.bestWeight?.reps ?: 0}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                Text("BEST WEIGHT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                            }
                            Column {
                                Text("${pr?.bestVolume?.value ?: 0.0} ${ex.unit}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                Text("BEST VOLUME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                            }
                            Column {
                                Text("${pr?.bestEstimated1RM?.value?.toInt() ?: 0} ${ex.unit}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                Text("EST 1RM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.GrayMedium, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
