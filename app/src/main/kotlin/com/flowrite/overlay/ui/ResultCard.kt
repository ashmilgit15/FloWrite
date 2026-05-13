package com.flowrite.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Input
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flowrite.ui.theme.FloWriteBorder
import com.flowrite.ui.theme.FloWriteError
import com.flowrite.ui.theme.FloWriteOnSurface
import com.flowrite.ui.theme.FloWriteOnSurfaceVariant
import com.flowrite.ui.theme.FloWritePrimary
import com.flowrite.ui.theme.FloWriteSecondary
import com.flowrite.ui.theme.FloWriteSurface

/**
 * Ultra premium dark result card composable shown after transcription.
 * Displays transcribed text with sleek cyber icon triggers.
 */
@Composable
fun ResultCard(
    text: String,
    isError: Boolean = false,
    visible: Boolean = true,
    onInject: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 290.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = FloWritePrimary.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(18.dp))
                .background(FloWriteSurface)
                .border(1.dp, FloWriteBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            // Transcribed text
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) FloWriteError else FloWriteOnSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isError) {
                    // Inject button
                    IconButton(
                        onClick = onInject,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Input,
                            contentDescription = "Inject text",
                            tint = FloWriteSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Copy button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            tint = FloWritePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = FloWriteOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
