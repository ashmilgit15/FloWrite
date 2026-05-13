package com.flowrite.overlay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flowrite.overlay.OverlayUiState
import com.flowrite.ui.theme.FloWriteAccent
import com.flowrite.ui.theme.FloWritePrimary
import com.flowrite.ui.theme.FloWritePrimaryDark
import com.flowrite.ui.theme.FloWriteRecording
import com.flowrite.ui.theme.FloWriteSecondary

/**
 * Premium sleek Floating microphone button.
 * - Idle: Electric cyan/emerald ambient gradient with an ultra-deep charcoal core
 * - Recording: Dynamic glowing pulse in deep cyber coral
 * - Processing: High-tech infinite spinning ring
 */
@Composable
fun FloatingButton(
    state: OverlayUiState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = state is OverlayUiState.Recording
    val isProcessing = state is OverlayUiState.Processing

    // Gentle floating pulse animation for recording state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val scale by animateFloatAsState(
        targetValue = if (isRecording) pulseScale else 1f,
        animationSpec = tween(250),
        label = "scale"
    )

    // Sleek ambient ambient color for shadow
    val shadowColor by animateColorAsState(
        targetValue = when (state) {
            is OverlayUiState.Recording -> FloWriteRecording
            is OverlayUiState.Processing -> FloWritePrimary
            else -> FloWritePrimary.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "shadowColor"
    )

    val gradientColors = if (isRecording) {
        listOf(FloWriteRecording, Color(0xFFCC0044))
    } else {
        listOf(Color(0xFF0F0F0F), Color(0xFF030303))
    }

    val borderColor = if (isRecording) {
        FloWriteAccent
    } else if (isProcessing) {
        FloWritePrimary
    } else {
        FloWritePrimary.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .size(58.dp)
            .scale(scale)
            .shadow(
                elevation = if (isRecording || isProcessing) 16.dp else 10.dp,
                shape = CircleShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(CircleShape)
            .background(Brush.radialGradient(gradientColors))
            .border(width = 1.5.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isProcessing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = FloWritePrimary,
                    trackColor = FloWritePrimary.copy(alpha = 0.1f),
                    strokeWidth = 2.5.dp
                )
            }
            isRecording -> {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop recording",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Start recording",
                    tint = FloWritePrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
