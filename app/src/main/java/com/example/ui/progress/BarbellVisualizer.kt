package com.example.ui.progress

import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.GlassDark
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GrayMedium

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BarbellVisualizer(
    weight: Double,
    onWeightChange: (Double) -> Unit,
    availablePlates: List<Double>,
    isKg: Boolean,
    barbellWeight: Double,
    modifier: Modifier = Modifier
) {
    // Determine which plates are currently loaded using the standard greedy algorithm
    val loadedPlatesBreakdown: List<Pair<Double, Int>> = remember(weight, barbellWeight, availablePlates) {
        calculatePlatesVisualizer(weight, barbellWeight, availablePlates.sortedDescending())
    }

    // Flatten representation for simple rendering & indexing
    val itemsToDraw: List<Double> = remember(loadedPlatesBreakdown) {
        val list = mutableListOf<Double>()
        loadedPlatesBreakdown.forEach { (denom, count) ->
            repeat(count) {
                list.add(denom)
            }
        }
        list
    }

    // Coordinates mapping to handle interactive taps on the canvas
    var rightSpans by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var leftSpans by remember { mutableStateOf<List<Rect>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Interactive Barbell Sleeve Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(GlassDark, RoundedCornerShape(24.dp))
                .border(2.dp, GlassBorderDark, RoundedCornerShape(24.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Hints
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TACTILE BARBELL BAR",
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    
                    Text(
                        text = "👉 TAP A LOADED PLATE TO UNLOAD IT",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Interactive Barbell Draw Area
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .padding(horizontal = 4.dp)
                        .pointerInput(itemsToDraw, leftSpans, rightSpans) {
                            detectTapGestures { tapOffset ->
                                // Check right sleeve clicks
                                val clickedRightIndex = rightSpans.indexOfFirst { it.contains(tapOffset) }
                                if (clickedRightIndex != -1) {
                                    val plateToUnload = itemsToDraw[clickedRightIndex]
                                    val nextWeight = (weight - plateToUnload * 2.0).coerceAtLeast(barbellWeight)
                                    onWeightChange(nextWeight)
                                    return@detectTapGestures
                                }

                                // Check left sleeve clicks (symmetrical)
                                val clickedLeftIndex = leftSpans.indexOfFirst { it.contains(tapOffset) }
                                if (clickedLeftIndex != -1) {
                                    val plateToUnload = itemsToDraw[clickedLeftIndex]
                                    val nextWeight = (weight - plateToUnload * 2.0).coerceAtLeast(barbellWeight)
                                    onWeightChange(nextWeight)
                                    return@detectTapGestures
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f

                    // 1. Main Steel Barbell Core Shaft
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF78909C), Color(0xFF37474F))
                        ),
                        topLeft = Offset(width * 0.04f, centerY - 5f),
                        size = Size(width * 0.92f, 10f)
                    )

                    // 2. Inner collars/stops limits (Left at 34%, Right at 66%)
                    val leftCollarX = width * 0.33f
                    val rightCollarX = width * 0.67f

                    // Draw inner brass stops
                    drawRoundRect(
                        color = Color(0xFF151515),
                        topLeft = Offset(leftCollarX - 12f, centerY - 32f),
                        size = Size(12f, 64f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    drawRoundRect(
                        color = Color(0xFF151515),
                        topLeft = Offset(rightCollarX, centerY - 32f),
                        size = Size(12f, 64f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )

                    // Metal chrome sleeves extending outwards from the collars
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFECEFF1), Color(0xFF90A4AE), Color(0xFF37474F))
                        ),
                        topLeft = Offset(width * 0.04f, centerY - 13f),
                        size = Size(leftCollarX - width * 0.04f - 12f, 26f)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFECEFF1), Color(0xFF90A4AE), Color(0xFF37474F))
                        ),
                        topLeft = Offset(rightCollarX + 12f, centerY - 13f),
                        size = Size(width * 0.96f - rightCollarX - 12f, 26f)
                    )

                    // Calculate local rect boundary bounds for direct click events
                    val tempRightSpans = mutableListOf<Rect>()
                    val tempLeftSpans = mutableListOf<Rect>()

                    // Symmetrical Draws going outwards
                    // Right direction: start at RightCollar + padding
                    var currentRightX = rightCollarX + 16f
                    itemsToDraw.forEach { plate ->
                        val h = getPlateHeightVisualizer(plate, isKg) * 0.72f
                        val w = getPlateWidthVisualizer(plate, isKg) * 0.65f
                        val color = getPlateColorVisualizer(plate, isKg)

                        // Save bounds
                        tempRightSpans.add(
                            Rect(currentRightX, centerY - h / 2f, currentRightX + w, centerY + h / 2f)
                        )

                        // Draw bumper plate outline and fill
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(currentRightX, centerY - h / 2f),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Steel center hub insert ring
                        drawCircle(
                            color = Color(0xFFB0BEC5),
                            radius = 4f,
                            center = Offset(currentRightX + w / 2f, centerY)
                        )

                        // Inner circular visual ridge
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.2f),
                            radius = h / 3f,
                            center = Offset(currentRightX + w / 2f, centerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )

                        // Draw weight markings
                        val labelText = if (plate % 1.0 == 0.0) "${plate.toInt()}" else "$plate"
                        val textPaint = Paint().apply {
                            this.color = if (color == Color.White) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            textSize = 14f
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            currentRightX + w / 2f,
                            centerY + 5f,
                            textPaint
                        )

                        currentRightX += w + 2f
                    }

                    // Left direction (Symmetrical reverse): from LeftCollar going leftwards
                    var currentLeftX = leftCollarX - 16f
                    itemsToDraw.forEach { plate ->
                        val h = getPlateHeightVisualizer(plate, isKg) * 0.72f
                        val w = getPlateWidthVisualizer(plate, isKg) * 0.65f
                        val color = getPlateColorVisualizer(plate, isKg)
                        val startX = currentLeftX - w

                        // Save bounds
                        tempLeftSpans.add(
                            Rect(startX, centerY - h / 2f, startX + w, centerY + h / 2f)
                        )

                        // Draw bumper plate
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(startX, centerY - h / 2f),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Center hub
                        drawCircle(
                            color = Color(0xFFB0BEC5),
                            radius = 4f,
                            center = Offset(startX + w / 2f, centerY)
                        )

                        // Inner ridge
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.2f),
                            radius = h / 3f,
                            center = Offset(startX + w / 2f, centerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )

                        // Markings
                        val labelText = if (plate % 1.0 == 0.0) "${plate.toInt()}" else "$plate"
                        val textPaint = Paint().apply {
                            this.color = if (color == Color.White) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                            textSize = 14f
                            isFakeBoldText = true
                            textAlign = Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            startX + w / 2f,
                            centerY + 5f,
                            textPaint
                        )

                        currentLeftX -= (w + 2f)
                    }

                    // Store calculated spans for interaction handlers
                    rightSpans = tempRightSpans
                    leftSpans = tempLeftSpans

                    // Clamps / Collars outer locking limits
                    if (itemsToDraw.isNotEmpty()) {
                        drawRoundRect(
                            color = Color(0xFFCFD8DC),
                            topLeft = Offset(currentRightX + 2f, centerY - 20f),
                            size = Size(8f, 40f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = Color(0xFFCFD8DC),
                            topLeft = Offset(currentLeftX - 10f, centerY - 20f),
                            size = Size(8f, 40f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Load Metric Dashboard display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TOTAL COMPLETED LOAD",
                    color = GrayMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${if (weight % 1.0 == 0.0) weight.toInt() else weight}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isKg) "KG" else "LBS",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            // Quick Operations Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clear Barbell (Reset to bar weight only)
                IconButton(
                    onClick = { onWeightChange(barbellWeight) },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Barbell",
                        tint = Color.LightGray
                    )
                }

                // Add 10% / Subtract 10% micro adjustments in a capsule
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    val unitStep = if (isKg) 2.5 else 5.0
                    TextButton(
                        onClick = {
                            val next = (weight - unitStep * 2).coerceAtLeast(barbellWeight)
                            onWeightChange(next)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("-$unitStep", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    TextButton(
                        onClick = {
                            val next = weight + unitStep * 2
                            onWeightChange(next)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("+$unitStep", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. TACTILE PLATE RACK GRID
        Text(
            text = "SELECT PATTERNS FROM THE PLATE RACK",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Tap any plate below to load one symmetrical pair (+2 plates) onto the barbell.",
            color = GrayMedium,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        // Plate selector array flow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            availablePlates.forEach { plateDenom ->
                val color = getPlateColorVisualizer(plateDenom, isKg)
                
                Surface(
                    onClick = {
                        val nextWeight = weight + (plateDenom * 2.0)
                        onWeightChange(nextWeight)
                    },
                    modifier = Modifier
                        .shadow(4.dp, CircleShape)
                        .size(80.dp),
                    shape = CircleShape,
                    color = color,
                    border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.3f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Gloss/Reflective ring for realistic bumper plate depth
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.85f)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        )
                        // Metal central ring
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color.White, Color(0xFF90A4AE), Color(0xFF37474F))
                                    ),
                                    CircleShape
                                )
                                .border(2.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                        )

                        // Weight indicator text label
                        Text(
                            text = "${if (plateDenom % 1.0 == 0.0) plateDenom.toInt() else plateDenom}",
                            color = if (color == Color.White) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.align(Alignment.Center)
                                .offset(y = (-14).dp)
                        )
                        
                        Text(
                            text = if (isKg) "KG" else "LBS",
                            color = if (color == Color.White) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            modifier = Modifier.align(Alignment.Center)
                                .offset(y = 16.dp)
                        )
                    }
                }
            }
        }

        // Details breakdown
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassDark),
            border = BorderStroke(1.dp, GlassBorderDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PLATE CONFIGURATION DETAILED LIST",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (loadedPlatesBreakdown.isEmpty()) {
                    Text(
                        text = "EMPTY BARBELL (${if (barbellWeight % 1.0 == 0.0) barbellWeight.toInt() else barbellWeight} ${if (isKg) "kg" else "lbs"} only)",
                        color = AccentGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    loadedPlatesBreakdown.forEach { (denom, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(getPlateColorVisualizer(denom, isKg), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${if (denom % 1.0 == 0.0) denom.toInt() else denom} ${if (isKg) "kg" else "lbs"}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Qty: $count Pair (${count * 2} Total Plates)",
                                color = AccentGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculatePlatesVisualizer(
    targetWeight: Double,
    barbellWeight: Double,
    availablePlates: List<Double>
): List<Pair<Double, Int>> {
    val remaining = targetWeight - barbellWeight
    if (remaining <= 0) return emptyList()
    
    val singleSide = remaining / 2.0
    var currentRemainder = singleSide
    val result = mutableListOf<Pair<Double, Int>>()
    
    for (plate in availablePlates) {
        if (plate <= 0) continue
        val countVal = (currentRemainder / plate).toInt()
        if (countVal > 0) {
            result.add(plate to countVal)
            currentRemainder -= countVal * plate
            currentRemainder = Math.round(currentRemainder * 100.0) / 100.0
        }
    }
    return result
}

private fun getPlateColorVisualizer(weight: Double, isKg: Boolean): Color {
    return if (isKg) {
        when {
            weight >= 25.0 -> Color(0xFFD32F2F) // Red
            weight >= 20.0 -> Color(0xFF1976D2) // Blue
            weight >= 15.0 -> Color(0xFFFBC02D) // Yellow
            weight >= 10.0 -> Color(0xFF388E3C) // Green
            weight >= 5.0 -> Color(0xFFEEEEEE)  // White
            weight >= 2.5 -> Color(0xFF37474F)  // Dark/Charcoal
            weight >= 1.25 -> Color(0xFF9E9E9E) // Medium Grey
            else -> Color(0xFFCFD8DC)           // Light Grey
        }
    } else {
        when {
            weight >= 45.0 -> Color(0xFFD32F2F) // Red
            weight >= 35.0 -> Color(0xFF1976D2) // Blue
            weight >= 25.0 -> Color(0xFFFBC02D) // Yellow
            weight >= 10.0 -> Color(0xFF388E3C) // Green
            weight >= 5.0 -> Color(0xFFEEEEEE)  // White
            else -> Color(0xFF37474F)           // Dark/Charcoal
        }
    }
}

private fun getPlateHeightVisualizer(weight: Double, isKg: Boolean): Float {
    return if (isKg) {
        when {
            weight >= 25.0 -> 120f
            weight >= 20.0 -> 110f
            weight >= 15.0 -> 100f
            weight >= 10.0 -> 90f
            weight >= 5.0 -> 75f
            weight >= 2.5 -> 62f
            weight >= 1.25 -> 52f
            else -> 42f
        }
    } else {
        when {
            weight >= 45.0 -> 120f
            weight >= 35.0 -> 110f
            weight >= 25.0 -> 98f
            weight >= 10.0 -> 85f
            weight >= 5.0 -> 72f
            else -> 58f
        }
    }
}

private fun getPlateWidthVisualizer(weight: Double, isKg: Boolean): Float {
    return if (isKg) {
        when {
            weight >= 25.0 -> 24f
            weight >= 20.0 -> 22f
            weight >= 15.0 -> 20f
            weight >= 10.0 -> 18f
            weight >= 5.0 -> 16f
            weight >= 2.5 -> 14f
            else -> 12f
        }
    } else {
        when {
            weight >= 45.0 -> 24f
            weight >= 35.0 -> 22f
            weight >= 25.0 -> 19f
            weight >= 10.0 -> 17f
            weight >= 5.0 -> 15f
            else -> 13f
        }
    }
}

