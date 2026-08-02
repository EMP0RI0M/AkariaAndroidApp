package com.akaria.agent.engine.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class AkariaAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        // The service is now connected. We can configure it via service_config.xml 
        // or programmatically here if needed.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We can capture screen state here to pass to the Vision model or Planner
    }

    override fun onInterrupt() {
        // Service interrupted
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Executes a UI macro such as clicking a specific node or performing a gesture.
     */
    fun executeMacro(action: String, targetDescription: String) {
        val rootNode = rootInActiveWindow ?: return
        
        when (action.uppercase()) {
            "CLICK" -> {
                val node = findNodeByTextOrContentDescription(rootNode, targetDescription)
                node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            "SCROLL_DOWN" -> {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) // Just a placeholder example
            }
            "SWIPE_UP" -> {
                // Example programmatic gesture
                val path = Path()
                path.moveTo(500f, 1500f)
                path.lineTo(500f, 500f)
                
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                    .build()
                    
                dispatchGesture(gesture, null, null)
            }
        }
    }

    /**
     * Recursively searches the node tree for a node matching the description.
     */
    private fun findNodeByTextOrContentDescription(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val q = query.lowercase()
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (text.contains(q) || contentDesc.contains(q)) {
            if (node.isClickable) return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findNodeByTextOrContentDescription(child, query)
                if (found != null) return found
            }
        }
        return null
    }
}
