package com.flowrite.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowrite.ui.theme.FloWriteAccent
import com.flowrite.ui.theme.FloWriteBorder
import com.flowrite.ui.theme.FloWriteOnSurface
import com.flowrite.ui.theme.FloWritePrimary
import com.flowrite.ui.theme.FloWritePrimaryDark
import com.flowrite.ui.theme.FloWriteSecondary

/**
 * Premium ultra dark Home screen dashboard.
 * Features glowing cyber controls, sleek obsidian cards, and top-tier layout depth.
 */
@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    hasOverlayPermission: Boolean
) {
    var overlayRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure AMOLED pitch black background
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Premium Header Badge
        Box(
            modifier = Modifier
                .border(1.dp, FloWriteBorder, RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0A0A), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (overlayRunning) FloWriteAccent else FloWriteSecondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (overlayRunning) "SERVICE ACTIVE" else "SYSTEM READY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App title
        Text(
            text = "FloWrite",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = FloWriteOnSurface
        )

        Text(
            text = "Voice to text, seamlessly anywhere",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Futuristic Large Mic Button
        Box(
            modifier = Modifier
                .size(130.dp)
                .shadow(24.dp, CircleShape, ambientColor = FloWritePrimary.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0F0F0F), Color(0xFF020202))
                    )
                )
                .border(2.dp, Brush.linearGradient(listOf(FloWritePrimary, FloWritePrimaryDark)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = FloWritePrimary,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Overlay control button
        Button(
            onClick = {
                if (overlayRunning) {
                    onStopOverlay()
                    overlayRunning = false
                } else {
                    onStartOverlay()
                    overlayRunning = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (overlayRunning) FloWriteAccent else FloWriteSecondary
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = if (overlayRunning) FloWriteAccent else FloWriteSecondary)
        ) {
            Icon(
                if (overlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (overlayRunning) "Stop Overlay Service" else "Start Overlay Service",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }



        Spacer(modifier = Modifier.height(28.dp))

        // Quick action cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuickActionCard(
                title = "History",
                icon = Icons.Default.History,
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer
        Text(
            text = "POWERED BY GROQ WHISPER",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(105.dp)
            .border(1.dp, FloWriteBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = FloWritePrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = FloWriteOnSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
