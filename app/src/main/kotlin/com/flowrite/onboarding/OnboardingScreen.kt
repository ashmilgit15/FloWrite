package com.flowrite.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowrite.ui.theme.FloWriteAccent
import com.flowrite.ui.theme.FloWriteBorder
import com.flowrite.ui.theme.FloWriteOnSurface
import com.flowrite.ui.theme.FloWritePrimary
import com.flowrite.ui.theme.FloWriteSecondary

/**
 * Premium AMOLED dark 3-step onboarding wizard.
 */
@Composable
fun OnboardingScreen(
    onApiKeySaved: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    hasMicPermission: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityEnabled: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure AMOLED pitch black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Step indicator
            StepIndicator(currentStep = currentStep, totalSteps = 3)

            Spacer(modifier = Modifier.height(40.dp))

            // Step content with smooth premium transitions
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                label = "step",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> StepApiKey(onApiKeySaved = { apiKey ->
                        onApiKeySaved(apiKey)
                        currentStep = 1
                    })
                    1 -> StepPermissions(
                        hasMicPermission = hasMicPermission,
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestMicPermission = onRequestMicPermission,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onNext = { currentStep = 2 }
                    )
                    2 -> StepAccessibility(
                        hasAccessibilityEnabled = hasAccessibilityEnabled,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onComplete = onComplete
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, FloWriteBorder, RoundedCornerShape(20.dp))
            .background(Color(0xFF0A0A0A), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentStep) FloWritePrimary
                        else if (index < currentStep) FloWriteSecondary
                        else Color(0xFF333333)
                    )
            )
            if (index < totalSteps - 1) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun StepApiKey(onApiKeySaved: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    StepLayout(
        icon = Icons.Default.Key,
        title = "Welcome to FloWrite",
        description = "Enter your Groq API key to power instant transcription. Stored securely inside device encrypted memory."
    ) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Groq API Key", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            placeholder = { Text("gsk_...", color = Color(0xFF444444)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = FloWritePrimary
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FloWritePrimary,
                unfocusedBorderColor = FloWriteBorder,
                focusedContainerColor = Color(0xFF0A0A0A),
                unfocusedContainerColor = Color(0xFF0A0A0A),
                cursorColor = FloWritePrimary,
                focusedTextColor = FloWriteOnSurface,
                unfocusedTextColor = FloWriteOnSurface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { if (apiKey.isNotBlank()) onApiKeySaved(apiKey.trim()) },
            enabled = apiKey.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = FloWritePrimary,
                disabledContainerColor = FloWriteBorder
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = if (apiKey.isNotBlank()) FloWritePrimary else Color.Transparent)
        ) {
            Text("Authorize & Continue", fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = { onApiKeySaved("") },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, FloWriteBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Skip setup for now", color = FloWriteOnSurface)
        }
    }
}

@Composable
private fun StepPermissions(
    hasMicPermission: Boolean,
    hasOverlayPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onNext: () -> Unit
) {
    StepLayout(
        icon = Icons.Default.Mic,
        title = "Core System Access",
        description = "Grant high-speed capture channels to sample your voice, plus persistent overlay layers to show the dictation trigger anywhere."
    ) {
        PermissionButton(
            label = "Microphone Access",
            isGranted = hasMicPermission,
            onClick = onRequestMicPermission
        )

        Spacer(modifier = Modifier.height(14.dp))

        PermissionButton(
            label = "Draw Over Other Apps",
            isGranted = hasOverlayPermission,
            onClick = onRequestOverlayPermission
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = FloWritePrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = FloWritePrimary)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
private fun StepAccessibility(
    hasAccessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onComplete: () -> Unit
) {
    StepLayout(
        icon = Icons.Default.AccessibilityNew,
        title = "Seamless Auto-Fill",
        description = "Enable FloWrite's custom Injection Service to pipe transcribed clean text straight into your focused target fields automatically."
    ) {
        PermissionButton(
            label = "Enable Injection Service",
            isGranted = hasAccessibilityEnabled,
            onClick = onOpenAccessibilitySettings
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = FloWriteSecondary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = FloWriteSecondary)
        ) {
            Text("Launch Voice OS!", fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

@Composable
private fun StepLayout(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .border(1.dp, FloWriteBorder, CircleShape)
                .background(Color(0xFF0A0A0A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FloWritePrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = FloWriteOnSurface,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        content()
    }
}

@Composable
private fun PermissionButton(
    label: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isGranted,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isGranted) Color(0xFF021A0A) else Color(0xFF0A0A0A)
        ),
        border = BorderStroke(1.dp, if (isGranted) FloWriteSecondary.copy(alpha = 0.4f) else FloWriteBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) FloWriteSecondary else FloWriteAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isGranted) "$label Granted" else label,
            color = if (isGranted) FloWriteSecondary else FloWriteOnSurface,
            fontWeight = if (isGranted) FontWeight.Bold else FontWeight.Medium
        )
    }
}
