package com.flowrite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flowrite.history.ui.HistoryScreen
import com.flowrite.onboarding.OnboardingScreen
import com.flowrite.onboarding.PermissionHelper
import com.flowrite.overlay.FloatingOverlayService
import com.flowrite.settings.SettingsScreen
import com.flowrite.settings.SettingsViewModel
import com.flowrite.ui.theme.FloWriteTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single entry-point Activity for FloWrite.
 * Hosts Compose navigation: Onboarding → Home → History → Settings.
 * Manages permission requests and starts the overlay service.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val theme by settingsViewModel.theme.collectAsState()
            val onboardingComplete by settingsViewModel.onboardingComplete.collectAsState()

            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> null // System default
            }

            FloWriteTheme(darkTheme = darkTheme ?: true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FloWriteApp(
                        onboardingComplete = onboardingComplete,
                        settingsViewModel = settingsViewModel,
                        activity = this@MainActivity
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from settings
    }
}

@Composable
private fun FloWriteApp(
    onboardingComplete: Boolean,
    settingsViewModel: SettingsViewModel,
    activity: MainActivity
) {
    val navController = rememberNavController()

    // Permission states
    var hasMicPermission by remember { mutableStateOf(PermissionHelper.hasRecordAudioPermission(activity)) }
    var hasOverlayPermission by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(activity)) }
    var hasAccessibilityEnabled by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(activity)) }

    // Refresh permission states periodically so UI updates when returning from settings
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            hasMicPermission = PermissionHelper.hasRecordAudioPermission(activity)
            hasOverlayPermission = PermissionHelper.hasOverlayPermission(activity)
            hasAccessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(activity)
        }
    }

    val startDestination = if (onboardingComplete) "home" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onApiKeySaved = { apiKey ->
                    if (apiKey.isNotBlank()) {
                        settingsViewModel.saveApiKey(apiKey)
                    }
                },
                onRequestMicPermission = {
                    PermissionHelper.requestRecordAudioPermission(activity)
                },
                onRequestOverlayPermission = {
                    PermissionHelper.requestOverlayPermission(activity)
                },
                onOpenAccessibilitySettings = {
                    PermissionHelper.openAccessibilitySettings(activity)
                },
                hasMicPermission = hasMicPermission,
                hasOverlayPermission = hasOverlayPermission,
                hasAccessibilityEnabled = hasAccessibilityEnabled,
                onComplete = {
                    settingsViewModel.setOnboardingComplete()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToSettings = { navController.navigate("settings") },
                onStartOverlay = {
                    if (PermissionHelper.hasOverlayPermission(activity)) {
                        FloatingOverlayService.startService(activity)
                    } else {
                        PermissionHelper.requestOverlayPermission(activity)
                    }
                },
                onStopOverlay = {
                    FloatingOverlayService.stopService(activity)
                },
                hasOverlayPermission = hasOverlayPermission
            )
        }

        composable("history") {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
