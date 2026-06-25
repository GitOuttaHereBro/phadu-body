package com.example.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.IronLogRepository
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DiagnosticsScreen(
    repository: IronLogRepository,
    onClose: () -> Unit
) {
    val activeProgramState by repository.getActiveProgramState().collectAsState(initial = null)
    val activeWorkout by repository.getActiveWorkout().collectAsState(initial = null)
    val auth = FirebaseAuth.getInstance()
    
    Column(modifier = Modifier.fillMaxSize().background(BgColor).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("DEVELOPER AUDIT MODE", style = IronTypography.Title, color = DestructiveColor)
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, null, tint = TextPrimaryColor)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                DiagRow("Current Week", activeProgramState?.currentWeek?.toString() ?: "None")
                DiagRow("Current Day Slot", activeProgramState?.currentDaySlot?.toString() ?: "None")
                DiagRow("Current Workout ID", activeWorkout?.id ?: "None")
                DiagRow("Exercise Count", activeWorkout?.loggedExercises?.size?.toString() ?: "0")
                DiagRow("Set Count", activeWorkout?.loggedExercises?.sumOf { it.sets.size }?.toString() ?: "0")
                DiagRow("Current User UID", auth.currentUser?.uid ?: "Not Authenticated")
                DiagRow("Firestore Sync", if (auth.currentUser != null) "Active" else "Local Only")
                Spacer(modifier = Modifier.height(16.dp))
                Text("WORKOUT STATE", style = IronTypography.Caption, color = TextTertiaryColor)
                activeWorkout?.loggedExercises?.forEach { ex ->
                    Text("- ${ex.exerciseName} (${ex.sets.size} sets)", color = TextPrimaryColor, style = IronTypography.Body)
                }
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondaryColor, style = IronTypography.Body)
        Text(value, color = SuccessColor, style = IronTypography.Body)
    }
}
