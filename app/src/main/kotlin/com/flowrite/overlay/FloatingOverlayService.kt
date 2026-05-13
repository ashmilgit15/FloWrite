package com.flowrite.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.flowrite.app.MainActivity
import com.flowrite.audio.AudioCaptureManager
import com.flowrite.audio.AudioCaptureState
import com.flowrite.history.data.TranscriptionDao
import com.flowrite.history.data.TranscriptionEntity
import com.flowrite.injection.TextInjectionAccessibilityService
import com.flowrite.overlay.ui.FloatingButton
import com.flowrite.overlay.ui.ResultCard
import com.flowrite.transcription.domain.TranscribeUseCase
import com.flowrite.transcription.domain.TranscriptionResult
import com.flowrite.ui.theme.FloWriteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Foreground service that manages the floating overlay button.
 * Handles the complete lifecycle: recording → transcription → auto-inject → clipboard sync.
 *
 * Wispr Flow-inspired features:
 * - Auto-injects text into focused field immediately on transcription success
 * - Clipboard listener syncs edits back to history library
 * - Result card shown for copy/edit after auto-injection
 */
@AndroidEntryPoint
class FloatingOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val TAG = "FloWriteOverlay"
        private const val NOTIFICATION_CHANNEL_ID = "flowrite_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.flowrite.STOP_OVERLAY"

        private val KEY_OVERLAY_X = floatPreferencesKey("overlay_x")
        private val KEY_OVERLAY_Y = floatPreferencesKey("overlay_y")

        fun startService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }

    @Inject lateinit var audioCaptureManager: AudioCaptureManager
    @Inject lateinit var transcribeUseCase: TranscribeUseCase
    @Inject lateinit var transcriptionDao: TranscriptionDao
    @Inject lateinit var dataStore: DataStore<Preferences>

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recordingJob: Job? = null

    // PCM accumulator — so we can process data even when manually stopped
    private val pcmAccumulator = ByteArrayOutputStream()

    private lateinit var windowManager: WindowManager
    private var overlayView: android.view.View? = null
    private lateinit var layoutParams_field: WindowManager.LayoutParams

    // Lifecycle management for ComposeView
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStoreField = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStoreField

    // UI State
    private var uiState by mutableStateOf<OverlayUiState>(OverlayUiState.Idle)
    private var lastTranscription by mutableStateOf<String?>(null)

    // Track last inserted history entry ID for clipboard sync
    private var lastInsertedEntityId: Long? = null
    private var lastCopiedText: String? = null

    // Clipboard listener for syncing edits back to history
    private lateinit var clipboardManager: ClipboardManager
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        onClipboardChanged()
    }

    // Overlay position
    private var overlayX = 0
    private var overlayY = 200

    // Drag tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // Throttle UI updates to avoid frame drops
    private var lastUiUpdateTime = 0L
    private val UI_UPDATE_INTERVAL_MS = 100L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        createNotificationChannel()

        // Register clipboard listener for history sync
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        Log.d(TAG, "Clipboard listener registered")

        // Load saved position and create overlay
        serviceScope.launch {
            try {
                val prefs = dataStore.data.first()
                overlayX = prefs[KEY_OVERLAY_X]?.toInt() ?: 0
                overlayY = prefs[KEY_OVERLAY_Y]?.toInt() ?: 200
                createOverlay()
                Log.d(TAG, "Overlay created at ($overlayX, $overlayY)")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating overlay", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stop action received")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // Unregister clipboard listener
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)

        recordingJob?.cancel()
        serviceScope.cancel()

        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
        viewModelStoreField.clear()

        super.onDestroy()
    }

    // ================================================================
    // Clipboard Sync — Updates history when user edits copied text
    // ================================================================

    /**
     * Called when clipboard content changes.
     * If the user copied text from FloWrite (via the "Copy" button) and then
     * edits it in another app, the history entry is updated to match.
     */
    private fun onClipboardChanged() {
        val entityId = lastInsertedEntityId ?: return
        val previousText = lastCopiedText ?: return

        val currentClip = clipboardManager.primaryClip ?: return
        if (currentClip.itemCount == 0) return

        val newText = currentClip.getItemAt(0)?.text?.toString() ?: return

        // Only update if the clipboard text differs from what we originally set
        // and is not completely different content (basic similarity check)
        if (newText != previousText && isSimilarEnough(previousText, newText)) {
            Log.d(TAG, "Clipboard changed — updating history entry $entityId")
            Log.d(TAG, "  Old: '$previousText'")
            Log.d(TAG, "  New: '$newText'")

            serviceScope.launch {
                try {
                    val wordCount = newText.split("\\s+".toRegex()).size
                    transcriptionDao.updateText(entityId, newText, wordCount)
                    lastTranscription = newText
                    lastCopiedText = newText

                    // Also update the UI if result card is showing
                    if (uiState is OverlayUiState.Result) {
                        uiState = OverlayUiState.Result(newText)
                    }

                    Log.d(TAG, "History entry $entityId updated successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update history entry", e)
                }
            }
        }
    }

    /**
     * Checks if two strings are "similar enough" to be considered edits of each other.
     * Uses a simple length-ratio check — if the new text is within 3x the length
     * of the original, it's likely an edit rather than completely new content.
     */
    private fun isSimilarEnough(original: String, modified: String): Boolean {
        if (original.isBlank() || modified.isBlank()) return false
        val ratio = modified.length.toFloat() / original.length.toFloat()
        // Accept if new text is between 10% and 300% of original length
        return ratio in 0.1f..3.0f
    }

    // ================================================================
    // Overlay UI
    // ================================================================

    /**
     * Custom FrameLayout that intercepts drag gestures via onInterceptTouchEvent.
     * This is the proper Android pattern for parent-child touch negotiation:
     * - onInterceptTouchEvent monitors the gesture and steals it when drag is detected
     * - Once intercepted, onTouchEvent handles the drag movement
     * - Taps (no drag) flow through to the ComposeView child for button clicks
     */
    private inner class DraggableFrameLayout(context: Context) : android.widget.FrameLayout(context) {

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams_field.x
                    initialY = layoutParams_field.y
                    initialTouchX = ev.rawX
                    initialTouchY = ev.rawY
                    isDragging = false
                    return false // Let child (ComposeView) get the DOWN
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - initialTouchX
                    val dy = ev.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        return true // Intercept! Steal touch from child
                    }
                }
            }
            return false
        }

        @Suppress("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        layoutParams_field.x = initialX + dx.toInt()
                        layoutParams_field.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(this, layoutParams_field)
                        } catch (_: Exception) {}
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        overlayX = layoutParams_field.x
                        overlayY = layoutParams_field.y
                        serviceScope.launch {
                            dataStore.edit { prefs ->
                                prefs[KEY_OVERLAY_X] = overlayX.toFloat()
                                prefs[KEY_OVERLAY_Y] = overlayY.toFloat()
                            }
                        }
                        isDragging = false
                        return true
                    }
                }
            }
            return super.onTouchEvent(event)
        }
    }

    private fun createOverlay() {
        layoutParams_field = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = overlayX
            y = overlayY
        }

        val wrapper = DraggableFrameLayout(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)

            setContent {
                FloWriteTheme(darkTheme = true) {
                    OverlayContent(
                        state = uiState,
                        lastTranscription = lastTranscription,
                        onMicTap = ::onMicTap,
                        onInject = ::onInjectText,
                        onCopy = ::onCopyText,
                        onDismiss = ::onDismiss
                    )
                }
            }
        }

        wrapper.addView(
            composeView,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        try {
            windowManager.addView(wrapper, layoutParams_field)
            overlayView = wrapper
            Log.d(TAG, "Overlay view added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create overlay", e)
            Toast.makeText(this, "Failed to create overlay: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun OverlayContent(
        state: OverlayUiState,
        lastTranscription: String?,
        onMicTap: () -> Unit,
        onInject: () -> Unit,
        onCopy: () -> Unit,
        onDismiss: () -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FloatingButton(
                state = state,
                onTap = onMicTap
            )

            Spacer(modifier = Modifier.height(8.dp))

            val showCard = state is OverlayUiState.Result || state is OverlayUiState.Error
            val cardText = when (state) {
                is OverlayUiState.Result -> state.text
                is OverlayUiState.Error -> state.message
                else -> lastTranscription ?: ""
            }

            ResultCard(
                text = cardText,
                isError = state is OverlayUiState.Error,
                visible = showCard,
                onInject = onInject,
                onCopy = onCopy,
                onDismiss = onDismiss
            )
        }
    }

    // ================================================================
    // Recording & Transcription
    // ================================================================

    private fun onMicTap() {
        Log.d(TAG, "onMicTap — current state: ${uiState::class.simpleName}")
        when (uiState) {
            is OverlayUiState.Idle, is OverlayUiState.Result, is OverlayUiState.Error -> startRecording()
            is OverlayUiState.Recording -> stopRecording()
            is OverlayUiState.Processing -> { /* Ignore taps while processing */ }
        }
    }

    @Suppress("MissingPermission")
    private fun startRecording() {
        Log.d(TAG, "Starting recording...")
        pcmAccumulator.reset()
        uiState = OverlayUiState.Recording()

        recordingJob = serviceScope.launch {
            try {
                audioCaptureManager.startRecording().collect { state ->
                    when (state) {
                        is AudioCaptureState.Recording -> {
                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdateTime > UI_UPDATE_INTERVAL_MS) {
                                uiState = OverlayUiState.Recording(state.amplitudeDb)
                                lastUiUpdateTime = now
                            }
                        }
                        is AudioCaptureState.SilenceDetected -> {
                            Log.d(TAG, "Silence detected, stopping recording...")
                        }
                        is AudioCaptureState.Stopped -> {
                            Log.d(TAG, "Recording stopped, PCM data size: ${state.pcmData.size} bytes")
                            uiState = OverlayUiState.Processing
                            processTranscription(state.pcmData)
                        }
                        is AudioCaptureState.Error -> {
                            Log.e(TAG, "Recording error: ${state.message}")
                            uiState = OverlayUiState.Error(state.message)
                        }
                        is AudioCaptureState.Idle -> { }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording coroutine error", e)
                val accumulated = pcmAccumulator.toByteArray()
                if (accumulated.isNotEmpty() && uiState !is OverlayUiState.Processing) {
                    Log.d(TAG, "Processing accumulated PCM data: ${accumulated.size} bytes")
                    uiState = OverlayUiState.Processing
                    processTranscription(accumulated)
                }
            }
        }
    }

    private fun stopRecording() {
        Log.d(TAG, "Manual stop recording")
        audioCaptureManager.stopRequested = true
    }

    /**
     * Processes transcription and AUTO-INJECTS into focused text field.
     * This is the Wispr Flow behavior — text appears instantly in the active field.
     * Result card is still shown so user can copy or re-inject if needed.
     */
    private fun processTranscription(pcmData: ByteArray) {
        if (pcmData.isEmpty()) {
            Log.w(TAG, "Empty PCM data, nothing to transcribe")
            uiState = OverlayUiState.Error("No audio recorded")
            return
        }

        Log.d(TAG, "Processing transcription: ${pcmData.size} bytes (${pcmData.size / 32000.0}s)")
        serviceScope.launch {
            try {
                val result = transcribeUseCase.execute(pcmData)

                when (result) {
                    is TranscriptionResult.Success -> {
                        Log.d(TAG, "Transcription success: '${result.text}'")
                        lastTranscription = result.text

                        // *** AUTO-INJECT: Immediately inject text into focused field ***
                        autoInjectText(result.text)

                        // Show result card (user can still copy/dismiss/re-inject)
                        uiState = OverlayUiState.Result(result.text)

                        // Save to history and track the entry ID for clipboard sync
                        val entity = TranscriptionEntity(
                            text = result.text,
                            languageCode = null,
                            durationMs = result.durationMs,
                            createdAt = System.currentTimeMillis(),
                            wordCount = result.text.split("\\s+".toRegex()).size
                        )
                        lastInsertedEntityId = transcriptionDao.insert(entity)
                        lastCopiedText = result.text
                        Log.d(TAG, "Saved to history: id=${lastInsertedEntityId}")

                        // Limit history to 100 entries
                        val count = transcriptionDao.getCount()
                        if (count > 100) {
                            transcriptionDao.deleteOldest(count - 100)
                        }
                    }
                    is TranscriptionResult.Error -> {
                        Log.e(TAG, "Transcription error: ${result.message} (code: ${result.code})")
                        uiState = OverlayUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "processTranscription exception", e)
                uiState = OverlayUiState.Error("Transcription failed: ${e.message}")
            }
        }
    }

    /**
     * Auto-injects text into the currently focused text field.
     * If accessibility service isn't available, copies to clipboard as fallback.
     */
    private fun autoInjectText(text: String) {
        val injectionService = TextInjectionAccessibilityService.instance
        if (injectionService != null) {
            Log.d(TAG, "Auto-injecting text into focused field")
            injectionService.injectText(text)
        } else {
            Log.d(TAG, "Accessibility service not available — copying to clipboard")
            copyToClipboard(text)
        }
    }

    // ================================================================
    // User Actions (from Result Card)
    // ================================================================

    private fun onInjectText() {
        val text = (uiState as? OverlayUiState.Result)?.text ?: lastTranscription ?: return
        Log.d(TAG, "Manual inject: '$text'")

        val injectionService = TextInjectionAccessibilityService.instance
        if (injectionService != null) {
            injectionService.injectText(text)
            uiState = OverlayUiState.Idle
        } else {
            Toast.makeText(this, "Enable FloWrite in Accessibility Settings to inject text", Toast.LENGTH_LONG).show()
            copyToClipboard(text)
        }
    }

    private fun onCopyText() {
        val text = (uiState as? OverlayUiState.Result)?.text ?: lastTranscription ?: return
        lastCopiedText = text  // Track what we copied for clipboard sync
        copyToClipboard(text)
        uiState = OverlayUiState.Idle
    }

    private fun copyToClipboard(text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", text))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun onDismiss() {
        uiState = OverlayUiState.Idle
    }

    // ================================================================
    // Notification
    // ================================================================

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "FloWrite Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Floating overlay for speech-to-text"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FloWrite Active")
            .setContentText("Tap the floating mic to dictate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    stopPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }
}
