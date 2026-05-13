package com.flowrite.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.flowrite.overlay.FloatingOverlayService

/**
 * BroadcastReceiver that auto-starts the floating overlay service on device boot.
 * Only starts if overlay permission is granted.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Settings.canDrawOverlays(context)) {
                FloatingOverlayService.startService(context)
            }
        }
    }
}
