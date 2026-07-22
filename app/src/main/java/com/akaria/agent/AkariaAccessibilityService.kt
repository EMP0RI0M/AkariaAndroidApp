package com.akaria.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AkariaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("Akaria", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We do not act on every event to avoid spamming the backend.
        // Screen capture and analysis is triggered by the user's intent.
    }

    override fun onInterrupt() {
        Log.w("Akaria", "Accessibility Service Interrupted")
    }

    /**
     * Called by the Native App UI when a response from the Ubuntu Backend dictates a tap action.
     */
    fun performTap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        // Tap gesture: duration 100ms
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        
        val success = dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i("Akaria", "Tap executed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.e("Akaria", "Tap cancelled at ($x, $y)")
            }
        }, null)

        if (!success) {
            Log.e("Akaria", "Failed to dispatch tap gesture")
        }
    }

    /**
     * Dumps the current Window Hierarchy to XML string.
     */
    fun extractUIHierarchy(): String {
        val root = rootInActiveWindow ?: return "<error>No active window</error>"
        // In a complete implementation, this traverses the AccessibilityNodeInfo tree
        // and returns a UIAutomator-style XML string representing the UI.
        return "<hierarchy>... Mocked UI Tree ...</hierarchy>"
    }
}
