package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== SPACING SYSTEM ====================
object IronSpacing {
    val None = 0.dp
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
    val XXXLarge = 48.dp
    val Huge = 64.dp
    
    // Semantic
    val CardPadding = Large // 16.dp
    val CardPaddingLarge = XLarge // 24.dp
    val SectionGap = XLarge // 24.dp
    val SectionGapLarge = XXLarge // 32.dp
    val CardCornerRadius = 16.dp
    val ElementCornerRadius = 12.dp
}

// ==================== TYPE SYSTEM ====================
val TypeCaption = androidx.compose.ui.text.TextStyle(
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    color = GrayMedium
)

val TypeBody = androidx.compose.ui.text.TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
    color = Color.White
)

val TypeButton = androidx.compose.ui.text.TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
    color = Color.White
)

val TypeHeader = androidx.compose.ui.text.TextStyle(
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
    color = Color.White
)

val TypeHero = androidx.compose.ui.text.TextStyle(
    fontSize = 34.sp,
    fontWeight = FontWeight.Bold,
    color = Color.White,
    letterSpacing = (-0.5).sp
)

val TypeGiant = androidx.compose.ui.text.TextStyle(
    fontSize = 48.sp,
    fontWeight = FontWeight.Black,
    color = Color.White,
    letterSpacing = (-1).sp
)

// ==================== INTERACTION: PRESS FEEDBACK ====================
fun Modifier.bouncy(enabled: Boolean = true): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bounce"
    )

    this
        .scale(scale)
        .pointerInput(enabled) {
            if (enabled) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
        }
}

fun Modifier.bouncyClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    this
        .bouncy(enabled)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// ==================== ELEVATION / GLASS SYSTEM ====================
fun Modifier.glassCard(): Modifier = this
    .background(GlassDark, RoundedCornerShape(IronSpacing.CardCornerRadius))
    .border(1.dp, GlassBorderDark, RoundedCornerShape(IronSpacing.CardCornerRadius))

fun Modifier.glassFloating(): Modifier = this
    .shadow(8.dp, RoundedCornerShape(IronSpacing.CardCornerRadius), spotColor = Color.Black.copy(alpha = 0.5f))
    .background(GlassDark.copy(alpha = 0.15f), RoundedCornerShape(IronSpacing.CardCornerRadius))
    .border(1.dp, GlassBorderDark.copy(alpha = 0.2f), RoundedCornerShape(IronSpacing.CardCornerRadius))

fun Modifier.glassModal(): Modifier = this
    .shadow(24.dp, RoundedCornerShape(IronSpacing.XXLarge), spotColor = Color.Black)
    .background(Color(0xFF1E1E1E).copy(alpha = 0.95f), RoundedCornerShape(IronSpacing.XXLarge))
    .border(1.dp, GlassBorderDark.copy(alpha = 0.3f), RoundedCornerShape(IronSpacing.XXLarge))

// ==================== LOADING (SKELETONS) ====================
fun Modifier.skeleton(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2C2C2E),
            Color(0xFF3A3A3C),
            Color(0xFF2C2C2E)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}

// ==================== EMPTY STATE ====================
@Composable
fun EmptyState(
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(IronSpacing.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simple subtle icon placeholder
        Box(
            modifier = Modifier
                .size(IronSpacing.Huge)
                .background(GlassDark, RoundedCornerShape(IronSpacing.ElementCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Text("📭", fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.height(IronSpacing.Large))
        
        Text(
            text = message,
            style = TypeBody.copy(color = GrayMedium, textAlign = TextAlign.Center),
            modifier = Modifier.padding(horizontal = IronSpacing.XLarge)
        )
        
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(IronSpacing.XLarge))
            
            Box(
                modifier = Modifier
                    .bouncyClick(onClick = onActionClick)
                    .background(Color.White, RoundedCornerShape(IronSpacing.ElementCornerRadius))
                    .padding(horizontal = IronSpacing.XLarge, vertical = IronSpacing.Medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    style = TypeButton.copy(color = Color.Black)
                )
            }
        }
    }
}
