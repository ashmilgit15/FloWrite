package com.flowrite.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowrite.ui.theme.FloWriteBorder
import com.flowrite.ui.theme.FloWriteError
import com.flowrite.ui.theme.FloWriteOnSurface
import com.flowrite.ui.theme.FloWritePrimary
import com.flowrite.ui.theme.FloWriteSecondary

/**
 * Premium AMOLED dark Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val apiKeyInput by viewModel.apiKeyInput.collectAsState()
    val language by viewModel.language.collectAsState()
    val vadSilenceMs by viewModel.vadSilenceMs.collectAsState()
    val vadThreshold by viewModel.vadThreshold.collectAsState()
    val autoPunctuate by viewModel.autoPunctuate.collectAsState()
    val theme by viewModel.theme.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences", fontWeight = FontWeight.Bold, color = FloWriteOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FloWriteOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF000000)
                )
            )
        },
        containerColor = Color(0xFF000000)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // API Key Section
            SettingsSection(title = "AUTHENTICATION") {
                ApiKeySection(
                    hasApiKey = hasApiKey,
                    apiKeyInput = apiKeyInput,
                    onApiKeyInputChange = { viewModel.updateApiKeyInput(it) },
                    onSaveApiKey = { viewModel.saveApiKey(apiKeyInput) },
                    onClearApiKey = { viewModel.clearApiKey() }
                )
            }

            // Language Section
            SettingsSection(title = "DICTATION LANGUAGE") {
                LanguageSelector(
                    selectedLanguage = language,
                    onLanguageSelected = { viewModel.setLanguage(it) }
                )
            }

            // Recording Section
            SettingsSection(title = "VOICE ACTIVITY DETECTION") {
                Text(
                    "Silence cutoff delay: ${vadSilenceMs}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FloWriteOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = vadSilenceMs.toFloat(),
                    onValueChange = { viewModel.setVadSilenceMs(it.toInt()) },
                    valueRange = 500f..5000f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = FloWritePrimary,
                        activeTrackColor = FloWritePrimary,
                        inactiveTrackColor = FloWriteBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Sensitivity threshold: $vadThreshold",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FloWriteOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = vadThreshold.toFloat(),
                    onValueChange = { viewModel.setVadThreshold(it.toInt()) },
                    valueRange = 100f..2000f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = FloWritePrimary,
                        activeTrackColor = FloWritePrimary,
                        inactiveTrackColor = FloWriteBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Automatic punctuation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FloWriteOnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = autoPunctuate,
                        onCheckedChange = { viewModel.setAutoPunctuate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = FloWritePrimary,
                            uncheckedThumbColor = Color(0xFF888888),
                            uncheckedTrackColor = Color(0xFF222222)
                        )
                    )
                }
            }

            // Appearance Section
            SettingsSection(title = "INTERFACE THEME") {
                ThemeSelector(
                    selectedTheme = theme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = FloWritePrimary.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FloWriteBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A0A0A)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ApiKeySection(
    hasApiKey: Boolean,
    apiKeyInput: String,
    onApiKeyInputChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    if (hasApiKey) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = FloWriteSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "API key secure & active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FloWriteSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onClearApiKey) {
                Icon(Icons.Default.Delete, "Remove", tint = FloWriteError)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Revoke", color = FloWriteError, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = onApiKeyInputChange,
            label = { Text("Groq API Key", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            placeholder = { Text("gsk_...", color = Color(0xFF444444)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = FloWritePrimary
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FloWritePrimary,
                unfocusedBorderColor = FloWriteBorder,
                focusedContainerColor = Color(0xFF000000),
                unfocusedContainerColor = Color(0xFF000000),
                cursorColor = FloWritePrimary,
                focusedTextColor = FloWriteOnSurface,
                unfocusedTextColor = FloWriteOnSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSaveApiKey,
            enabled = apiKeyInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = FloWritePrimary,
                disabledContainerColor = FloWriteBorder
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Authorize Key", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "auto" to "Automatic detection",
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "pt" to "Portuguese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "ar" to "Arabic",
        "hi" to "Hindi",
        "ru" to "Russian",
        "nl" to "Dutch",
        "sv" to "Swedish",
        "pl" to "Polish",
        "tr" to "Turkish"
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = languages.find { it.first == selectedLanguage }?.second ?: "Automatic detection"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FloWritePrimary,
                unfocusedBorderColor = FloWriteBorder,
                focusedContainerColor = Color(0xFF000000),
                unfocusedContainerColor = Color(0xFF000000),
                focusedTextColor = FloWriteOnSurface,
                unfocusedTextColor = FloWriteOnSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF111111),
            border = BorderStroke(1.dp, FloWriteBorder)
        ) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = FloWriteOnSurface) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val themes = listOf("dark" to "Obsidian Black", "light" to "Light", "system" to "System")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        themes.forEach { (value, label) ->
            Button(
                onClick = { onThemeSelected(value) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTheme == value) FloWritePrimary else Color(0xFF000000)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .border(1.dp, if (selectedTheme == value) Color.Transparent else FloWriteBorder, RoundedCornerShape(12.dp))
            ) {
                Text(
                    label,
                    color = if (selectedTheme == value) Color.Black else FloWriteOnSurface,
                    fontWeight = if (selectedTheme == value) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
