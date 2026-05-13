package com.flowrite.injection

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility service for injecting transcribed text into focused input fields.
 *
 * Works like Wispr Flow:
 * 1. Continuously tracks the last focused/clicked editable text field
 * 2. When injectText() is called, finds the active input field using multiple strategies
 * 3. Sets text via ACTION_SET_TEXT (primary) or clipboard paste (fallback)
 *
 * Uses companion object singleton since Android doesn't support binding to
 * accessibility services via ServiceConnection.
 */
class TextInjectionAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FloWriteInjection"

        /** Singleton reference — set when service connects, null when destroyed */
        var instance: TextInjectionAccessibilityService? = null
            private set
    }

    // Track the last known editable node (don't recycle — keep reference alive)
    private var lastEditableNodeInfo: NodeSnapshot? = null

    /**
     * Lightweight snapshot of an editable node's identity.
     * We store the package/class/viewId so we can re-find the node later
     * since AccessibilityNodeInfo references can become stale.
     */
    private data class NodeSnapshot(
        val packageName: String,
        val className: String,
        val viewId: String?,
        val windowId: Int
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "✅ Accessibility service CONNECTED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source ?: return
                if (isEditableTextField(source)) {
                    saveNodeSnapshot(source)
                    Log.d(TAG, "Tracked editable field: ${source.className} viewId=${source.viewIdResourceName} pkg=${source.packageName}")
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Text changed in a field — this field is definitely the active one
                val source = event.source ?: return
                if (isEditableTextField(source)) {
                    saveNodeSnapshot(source)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // New window — try to find an editable field
                findAndTrackEditableInActiveWindow()
            }
            else -> { }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        instance = null
        lastEditableNodeInfo = null
        super.onDestroy()
    }

    // ================================================================
    // Public API — called from FloatingOverlayService
    // ================================================================

    /**
     * Injects text into the currently focused/active input field.
     *
     * Strategy (multiple fallbacks like Wispr Flow):
     * 1. Try findFocus(FOCUS_INPUT) — the Android-recommended way
     * 2. Try finding editable node matching our last snapshot
     * 3. Try scanning the entire active window for any editable field
     * 4. Last resort: copy to clipboard and notify user
     */
    fun injectText(text: String) {
        Log.d(TAG, "injectText called: '${text.take(50)}...'")

        // Strategy 1: Use findFocus(FOCUS_INPUT) — most reliable
        var targetNode = findInputFocusedNode()

        // Strategy 2: Scan active window for editable nodes
        if (targetNode == null) {
            Log.d(TAG, "No input focus found, scanning window...")
            targetNode = findEditableNodeInActiveWindow()
        }

        // Strategy 3: Last resort — clipboard
        if (targetNode == null) {
            Log.w(TAG, "No editable field found — falling back to clipboard")
            copyToClipboard(text)
            return
        }

        Log.d(TAG, "Target field: ${targetNode.className}, viewId=${targetNode.viewIdResourceName}, editable=${targetNode.isEditable}")

        // Attempt injection
        val success = performTextInjection(targetNode, text)

        if (success) {
            Log.d(TAG, "✅ Text injected successfully")
        } else {
            Log.w(TAG, "ACTION_SET_TEXT failed, trying clipboard paste fallback")
            performClipboardPaste(targetNode, text)
        }
    }

    // ================================================================
    // Node Finding Strategies
    // ================================================================

    /**
     * Strategy 1: findFocus(FOCUS_INPUT) — returns the node that has input focus.
     * This is the most reliable method on modern Android.
     */
    private fun findInputFocusedNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && isEditableTextField(focused)) {
            Log.d(TAG, "Found input-focused editable: ${focused.className}")
            return focused
        }
        return null
    }

    /**
     * Strategy 2: Deep scan the active window for any editable field.
     * Prefers focused fields, but accepts any editable if none are focused.
     */
    private fun findEditableNodeInActiveWindow(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findEditableNode(rootNode)
    }

    /**
     * Recursively searches for editable text fields.
     * Priority: focused editable > any editable
     */
    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check this node
        if (isEditableTextField(node)) {
            if (node.isFocused) {
                return node // Best match — focused and editable
            }
            // Store as candidate but keep looking for a focused one
        }

        // Search children
        var candidate: AccessibilityNodeInfo? = null
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            if (result != null) {
                if (result.isFocused) return result // Found focused editable — use it
                if (candidate == null) candidate = result // Store first editable as fallback
            }
        }

        // Return this node if it's editable (as fallback), or the candidate from children
        if (isEditableTextField(node)) return node
        return candidate
    }

    private fun findAndTrackEditableInActiveWindow() {
        val node = findEditableNodeInActiveWindow()
        if (node != null) {
            saveNodeSnapshot(node)
        }
    }

    // ================================================================
    // Text Injection Methods
    // ================================================================

    /**
     * Primary injection: ACTION_SET_TEXT.
     * Sets transcribed text into the field. If the field already has real user text
     * (not just a hint/placeholder), appends with a space separator.
     *
     * Detecting real content vs placeholder on Samsung/OneUI:
     * - node.text returns the hint/placeholder (e.g. "Message") even when the field is empty
     * - node.hintText is often null on Samsung, so comparing against it is unreliable
     * - node.textSelectionStart is the reliable signal:
     *     -1 → field is empty (showing placeholder only)
     *     ≥0 → field has real user text with an active cursor
     */
    private fun performTextInjection(node: AccessibilityNodeInfo, text: String): Boolean {
        val rawExisting = node.text?.toString() ?: ""
        val hintText = node.hintText?.toString()
        val selectionStart = node.textSelectionStart

        // Determine if the field has real user-entered content:
        // 1. If selection cursor is -1, field is empty (Samsung returns hint via node.text)
        // 2. If text matches the hint, field is empty
        // 3. If text is blank, field is empty
        val hasRealContent = selectionStart >= 0
                && rawExisting.isNotEmpty()
                && (hintText == null || rawExisting != hintText)

        Log.d(TAG, "Field state: text='$rawExisting', hint='$hintText', selStart=$selectionStart, hasReal=$hasRealContent")

        val newText = if (hasRealContent) {
            if (rawExisting.endsWith(" ")) "$rawExisting$text" else "$rawExisting $text"
        } else {
            text
        }

        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }

        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_SET_TEXT exception", e)
            false
        }
    }

    /**
     * Fallback injection: Copy text to clipboard then paste.
     * Works with apps that block ACTION_SET_TEXT but support paste.
     */
    private fun performClipboardPaste(node: AccessibilityNodeInfo, text: String) {
        try {
            // First, focus the node
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            // Set clipboard
            copyToClipboard(text)

            // Small delay to ensure clipboard is set, then paste
            android.os.Handler(mainLooper).postDelayed({
                try {
                    node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    Log.d(TAG, "✅ Clipboard paste performed")
                } catch (e: Exception) {
                    Log.e(TAG, "ACTION_PASTE failed", e)
                }
            }, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard paste fallback failed", e)
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private fun isEditableTextField(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return true
        val className = node.className?.toString() ?: return false
        return className.contains("EditText") ||
                className.contains("AutoCompleteTextView") ||
                className.contains("SearchView") ||
                className.contains("TextInputEditText")
    }

    private fun saveNodeSnapshot(node: AccessibilityNodeInfo) {
        lastEditableNodeInfo = NodeSnapshot(
            packageName = node.packageName?.toString() ?: "",
            className = node.className?.toString() ?: "",
            viewId = node.viewIdResourceName,
            windowId = node.windowId
        )
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("", text))
    }
}
