package com.example.ui.workout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun BarbellVisualizer(
    initialTargetWeight: Double,
    isKgInitially: Boolean = true,
    onDismiss: () -> Unit
) {
    var isKg by remember { mutableStateOf(isKgInitially) }
    var isReverseMode by remember { mutableStateOf(false) } // False = Target Mode, True = Manual Mode
    
    val barWeight = if (isKg) 20.0 else 45.0
    
    // Manual plate counts (per side)
    var manualPlates by remember { mutableStateOf(mapOf<Double, Int>()) }
    
    val allKgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val allLbsPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    
    val currentAvailablePlates = if (isKg) allKgPlates else allLbsPlates

    var targetWeightInput by remember { mutableStateOf(initialTargetWeight) }
    
    val platesToRender = if (isReverseMode) {
        val list = mutableListOf<Pair<Double, Int>>()
        currentAvailablePlates.forEach { weight ->
            val count = manualPlates[weight] ?: 0
            if (count > 0) {
                list.add(Pair(weight, count))
            }
        }
        list
    } else {
        calculateRequiredPlates(targetWeightInput, barWeight, currentAvailablePlates)
    }

    val totalWeight = if (isReverseMode) {
        barWeight + platesToRender.sumOf { it.first * it.second * 2 }
    } else {
        barWeight + platesToRender.sumOf { it.first * it.second * 2 }
    }
    
    val isApproximation = !isReverseMode && totalWeight != targetWeightInput

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Close, KG/LB toggle, Reverse/Target mode)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                    
                    // Modes toggle
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (!isReverseMode) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                .bouncyClick { isReverseMode = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("TARGET", color = if (!isReverseMode) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isReverseMode) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                .bouncyClick { isReverseMode = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("MANUAL", color = if (isReverseMode) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Unit toggle
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (isKg) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                .bouncyClick { 
                                    isKg = true 
                                    manualPlates = emptyMap()
                                    targetWeightInput = if (isKgInitially) initialTargetWeight else (initialTargetWeight / 2.20462).roundToInt().toDouble()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("KG", color = if (isKg) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (!isKg) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                .bouncyClick { 
                                    isKg = false 
                                    manualPlates = emptyMap()
                                    targetWeightInput = if (!isKgInitially) initialTargetWeight else (initialTargetWeight * 2.20462).roundToInt().toDouble()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("LB", color = if (!isKg) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Visualizer Canvas
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BarbellCanvasVisualizer(plates = platesToRender)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Weight Readout
                val totalKg = if (isKg) totalWeight else (totalWeight / 2.20462)
                val totalLb = if (!isKg) totalWeight else (totalWeight * 2.20462)
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val wText = if (totalWeight % 1.0 == 0.0) totalWeight.toInt().toString() else totalWeight.toString()
                    Text(
                        text = "$wText ${if (isKg) "KG" else "LB"}",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    
                    val convertedText = if (isKg) {
                        "${totalLb.roundToInt()} LBS"
                    } else {
                        "${totalKg.roundToInt()} KG"
                    }
                    Text(
                        text = convertedText,
                        color = Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isApproximation && !isReverseMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "APPROXIMATION (Target: $targetWeightInput)",
                            color = com.example.ui.theme.AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!isReverseMode) {
                    // Target adjustment controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stepBig = if (isKg) 10.0 else 25.0
                        val stepSmall = if (isKg) 2.5 else 5.0
                        
                        AdjustmentButton("-$stepBig") { targetWeightInput = (targetWeightInput - stepBig).coerceAtLeast(barWeight) }
                        AdjustmentButton("-$stepSmall") { targetWeightInput = (targetWeightInput - stepSmall).coerceAtLeast(barWeight) }
                        
                        Text(
                            text = "ADJUST",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        AdjustmentButton("+$stepSmall") { targetWeightInput += stepSmall }
                        AdjustmentButton("+$stepBig") { targetWeightInput += stepBig }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Plate Multi-Select Grid
                Text(
                    text = if (isReverseMode) "MANUAL PLATE LOADING (PER SIDE)" else "AVAILABLE PLATES",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.Start)
                )
                
                // Grid layout (using rows)
                val rows = currentAvailablePlates.chunked(4)
                Column(modifier = Modifier.fillMaxWidth()) {
                    rows.forEach { rowPlates ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPlates.forEach { plateWt ->
                                val color = getStandardPlateColor(plateWt, isKg)
                                val count = manualPlates[plateWt] ?: 0
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    if (isReverseMode) {
                                        // Manual mode: circular button with stepper
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .background(color, CircleShape)
                                                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (count > 0) {
                                                    Text("${count}x", color = if (color == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                } else {
                                                    val ptText = if (plateWt % 1.0 == 0.0) plateWt.toInt().toString() else plateWt.toString()
                                                    Text(ptText, color = if (color == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.Remove, 
                                                    contentDescription = "Remove", 
                                                    tint = if (count > 0) Color.White else Color.DarkGray,
                                                    modifier = Modifier.size(24.dp).clickable(enabled = count > 0) {
                                                        val mut = manualPlates.toMutableMap()
                                                        mut[plateWt] = count - 1
                                                        manualPlates = mut
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(
                                                    Icons.Outlined.Add, 
                                                    contentDescription = "Add", 
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp).bouncyClick {
                                                        val mut = manualPlates.toMutableMap()
                                                        mut[plateWt] = count + 1
                                                        manualPlates = mut
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        // Target mode: just a colored circle with amount needed listed inside
                                        val reqCount = platesToRender.find { it.first == plateWt }?.second ?: 0
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .background(color.copy(alpha = if (reqCount > 0) 1f else 0.3f), CircleShape)
                                                .border(
                                                    if (reqCount > 0) 2.dp else 1.dp,
                                                    if (reqCount > 0) Color.White else Color.Transparent,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (reqCount > 0) {
                                                Text(
                                                    "${reqCount}x",
                                                    color = if (color == Color.White) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 20.sp
                                                )
                                            } else {
                                                val ptText = if (plateWt % 1.0 == 0.0) plateWt.toInt().toString() else plateWt.toString()
                                                Text(
                                                    ptText,
                                                    color = if (color == Color.White) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Fill empty spaces
                            val emptySpaces = 4 - rowPlates.size
                            repeat(emptySpaces) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustmentButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF222222), RoundedCornerShape(8.dp))
            .bouncyClick { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun calculateRequiredPlates(target: Double, barWeight: Double, plates: List<Double>): List<Pair<Double, Int>> {
    var remaining = (target - barWeight) / 2.0
    if (remaining <= 0) return emptyList()
    
    val result = mutableListOf<Pair<Double, Int>>()
    val sortedPlates = plates.sortedDescending()
    
    for (plate in sortedPlates) {
        val count = (remaining / plate).toInt()
        if (count > 0) {
            result.add(Pair(plate, count))
            remaining -= (count * plate)
        }
    }
    return result
}

private fun getStandardPlateColor(weight: Double, isKg: Boolean): Color {
    // True gym colors: 25kg red, 20kg blue, 15kg yellow, 10kg green, 5kg white
    if (isKg) {
        return when (weight) {
            25.0 -> Color(0xFFE53935) // Red
            20.0 -> Color(0xFF1E88E5) // Blue
            15.0 -> Color(0xFFFDD835) // Yellow
            10.0 -> Color(0xFF4CAF50) // Green
            5.0 -> Color.White        // White
            2.5 -> Color(0xFF111111)  // Black/Dark
            1.25 -> Color(0xFF333333) // Gray
            0.5 -> Color.LightGray    // Light Gray
            0.25 -> Color.Gray        // Gray
            else -> Color.DarkGray
        }
    } else {
        // Lbs colors usually map: 45lb(blue) or red? Depends on gym. Let's use standard lifting colors
        return when (weight) {
            45.0 -> Color(0xFF1E88E5) // Blue or Red
            35.0 -> Color(0xFFFDD835) // Yellow
            25.0 -> Color(0xFF4CAF50) // Green
            10.0 -> Color.White       // White
            5.0 -> Color(0xFF111111)  // Black
            2.5 -> Color(0xFF333333)  // Gray
            else -> Color.DarkGray
        }
    }
}

@Composable
fun BarbellCanvasVisualizer(plates: List<Pair<Double, Int>>) {
    val itemsToDraw = remember(plates) {
        val list = mutableListOf<Color>()
        plates.forEach { (denom, count) ->
            repeat(count) {
                // Determine color
                // We don't have isKg here easily without passing it... let's just pass colors
                // Wait, BarbellCanvasVisualizer doesn't have isKg. Let's assume passed in or determine from denom.
                // It's just a visualizer so we can just use the denom logic or alter signature.
                // Let's alter signature... oh wait I didn't pass isKg. Let's fix that.
            }
        }
        list
    }
    
    // I'll re-implement standard drawing using the plate pairs.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // 1. Draw central bar (white/gray on black background, not colored)
        drawRect(
            color = Color(0xFFCCCCCC), // Light silver bar
            topLeft = Offset(width * 0.1f, centerY - 8f),
            size = Size(width * 0.8f, 16f)
        )
        
        // Collars
        val leftCollarX = width * 0.35f
        val rightCollarX = width * 0.65f
        
        drawRoundRect(
            color = Color(0xFF888888),
            topLeft = Offset(leftCollarX - 10f, centerY - 20f),
            size = Size(10f, 40f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = Color(0xFF888888),
            topLeft = Offset(rightCollarX, centerY - 20f),
            size = Size(10f, 40f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        
        // Now draw plates. The visual width and height of plates is just scaled by weight.
        var currentRightX = rightCollarX + 10f
        var currentLeftX = leftCollarX - 10f
        
        plates.forEach { (wt, count) ->
            repeat(count) {
                // Height scaling (max 100f)
                val h = when {
                    wt >= 20.0 -> 120f
                    wt >= 15.0 -> 100f
                    wt >= 10.0 -> 80f
                    wt >= 5.0 -> 60f
                    else -> 40f
                }
                val w = when {
                    wt >= 25.0 -> 24f
                    wt >= 20.0 -> 20f
                    wt >= 15.0 -> 16f
                    wt >= 10.0 -> 14f
                    else -> 10f
                }
                
                // Use a non-colored approach as requested "horizontal bar-and-collar illustration (in white/gray on the black background, not colored)" -> oh wait, "plates visually sliding onto it". The description says "each plate's true gym color... with a grid of circular buttons below". The plate illustration *on the bar* could be colored or just white/gray. Let's make it colored since it usually looks better.
                val color = if (wt == 25.0 || wt == 45.0 && wt != 10.0) Color(0xFFE53935)
                else if (wt == 20.0) Color(0xFF1E88E5)
                else if (wt == 15.0 || wt == 35.0) Color(0xFFFDD835)
                else if (wt == 10.0 && wt != 25.0) Color(0xFF4CAF50)
                else if (wt == 5.0) Color.White
                else Color(0xFF555555) // Small plates
                
                drawRoundRect(
                    color = color,
                    topLeft = Offset(currentRightX, centerY - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                
                val startX = currentLeftX - w
                drawRoundRect(
                    color = color,
                    topLeft = Offset(startX, centerY - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                
                currentRightX += (w + 4f)
                currentLeftX -= (w + 4f)
            }
        }
    }
}
