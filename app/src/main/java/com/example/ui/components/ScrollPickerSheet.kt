package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.List

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StepperChip(
    value: Double,
    unit: String,
    onValueChange: (Double) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    step: Double = 2.5,
    displayValueOverride: String? = null
) {
    var showQuickAdjust by remember { mutableStateOf(false) }
    var localValue by remember(value) { mutableStateOf(value) }
    var pendingChange by remember { mutableStateOf(false) }

    LaunchedEffect(showQuickAdjust) {
        if (showQuickAdjust) {
            delay(3000)
            showQuickAdjust = false
        }
    }

    LaunchedEffect(localValue, pendingChange) {
        if (pendingChange) {
            delay(400) // Debounce time
            onValueChange(localValue)
            pendingChange = false
        }
    }
    
    Row(
        modifier = modifier
            .height(44.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showQuickAdjust = true }
            )
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showQuickAdjust) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable { 
                        localValue = maxOf(0.0, localValue - step)
                        pendingChange = true
                        showQuickAdjust = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("-", style = IronTypography.Subheading, color = TextPrimaryColor)
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(2f)
        ) {
            val displayVal = if (pendingChange) {
                if (localValue % 1.0 == 0.0) "${localValue.toInt()}" else String.format("%.1f", localValue)
            } else {
                displayValueOverride ?: (if (value % 1.0 == 0.0) "${value.toInt()}" else String.format("%.1f", value))
            }
            Text(
                text = displayVal,
                style = IronTypography.Headline,
                color = TextPrimaryColor
            )
        }
        
        if (showQuickAdjust) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable { 
                        localValue += step
                        pendingChange = true
                        showQuickAdjust = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = IronTypography.Subheading, color = TextPrimaryColor)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollPickerSheet(
    initialValue: Double,
    type: String, // "WEIGHT" or "REPS"
    onDismiss: () -> Unit,
    onDone: (Double) -> Unit
) {
    var textValue by remember { 
        val initialText = if (type == "WEIGHT") {
            if (initialValue % 1.0 == 0.0) initialValue.toInt().toString() else initialValue.toString()
        } else {
            initialValue.toInt().toString()
        }
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = initialText,
                selection = androidx.compose.ui.text.TextRange(0, initialText.length)
            )
        ) 
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = {
            Text(if (type == "WEIGHT") "Enter Weight (kg)" else "Enter Reps", style = IronTypography.Title, color = TextPrimaryColor)
        },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { 
                    // Allow only digits and a single decimal point
                    if (it.text.isEmpty() || it.text.matches(Regex("^\\d*\\.?\\d*$"))) {
                        textValue = it
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = IronTypography.Display.copy(fontSize = 32.sp, textAlign = TextAlign.Center),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimaryColor,
                    unfocusedBorderColor = TextSecondaryColor,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor
                )
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.text.toDoubleOrNull() ?: initialValue
                    onDone(parsed)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimaryColor, contentColor = Color.Black)
            ) {
                Text("SAVE", style = IronTypography.Headline, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondaryColor)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalWheelPicker(
    state: PagerState,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    VerticalPager(
        state = state,
        modifier = modifier.height(192.dp), // 64 * 3
        contentPadding = PaddingValues(vertical = 64.dp),
        pageSize = PageSize.Fixed(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { page ->
        val pageOffset = (state.currentPage - page) + state.currentPageOffsetFraction
        val absoluteOffset = abs(pageOffset)
        
        val alphaVal = 1f - minOf(1f, absoluteOffset * 0.6f)
        val scaleVal = 1f - minOf(0.4f, absoluteOffset * 0.2f)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .alpha(alphaVal)
                .graphicsLayer {
                    scaleX = scaleVal
                    scaleY = scaleVal
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = items[page],
                style = IronTypography.Display.copy(fontSize = 32.sp), 
                color = Color.White
            )
        }
    }
}
