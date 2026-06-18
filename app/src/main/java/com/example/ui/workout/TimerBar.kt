package com.example.ui.workout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay

@Composable
fun RestTimerBar(
    endTimeMillis: Long,
    totalDurationSeconds: Int,
    onDismiss: () -> Unit
) {
    var remainingSeconds by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(endTimeMillis, totalDurationSeconds) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = endTimeMillis - now
            if (diff <= 0) {
                remainingSeconds = 0
                progress = 0f
                onDismiss()
                break
            } else {
                remainingSeconds = (diff / 1000).toInt()
                progress = diff.toFloat() / (totalDurationSeconds * 1000f)
            }
            delay(100) // Update smoothly
        }
    }

    AnimatedVisibility(
        visible = remainingSeconds > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            color = com.example.ui.theme.GlassDark,
            border = BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background progress bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(com.example.ui.theme.AccentGreen.copy(alpha = 0.2f))
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "REST TIMER",
                        color = com.example.ui.theme.AccentGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val mins = remainingSeconds / 60
                        val secs = remainingSeconds % 60
                        Text(
                            String.format("%d:%02d", mins, secs),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Dismiss Timer",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(28.dp)
                                .bouncyClick { onDismiss() }
                        )
                    }
                }
            }
        }
    }
}
