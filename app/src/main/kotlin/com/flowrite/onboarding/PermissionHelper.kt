package com.flowrite.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.flowrite.injection.TextInjectionAccessibilityService

/**
 * Utility class for checking and requesting permissions needed by FloWrite.
 */
object PermissionHelper {

    const val REQUEST_CODE_RECORD_AUDIO = 100
    const val REQUEST_CODE_OVERLAY = 101
    const val REQUEST_CODE_NOTIFICATIONS = 102

    /**
     * Checks if RECORD_AUDIO permission is granted.
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if SYSTEM_ALERT_WINDOW (overlay) permission is granted.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if the Accessibility Service is enabled.
     * Uses both the singleton instance AND the system Settings API.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Fast path: singleton is set
        if (TextInjectionAccessibilityService.instance != null) return true

        // Fallback: check Settings.Secure (authoritative source)
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val serviceName = "${context.packageName}/${TextInjectionAccessibilityService::class.java.canonicalName}"
            enabledServices.contains(serviceName)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Requests RECORD_AUDIO permission.
     */
    fun requestRecordAudioPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_CODE_RECORD_AUDIO
        )
    }

    /**
     * Opens the overlay permission settings page.
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Opens the Accessibility Settings page.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Requests POST_NOTIFICATIONS permission (Android 13+).
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATIONS
            )
        }
    }

    /**
     * Returns true if all required permissions are granted.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasRecordAudioPermission(context) &&
                hasOverlayPermission(context) &&
                isAccessibilityServiceEnabled(context)
    }
}
