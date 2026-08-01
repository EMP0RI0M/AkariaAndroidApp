package com.akaria.agent

import android.util.Log

/**
 * Represents a node in the App Map graph (e.g., "Settings -> Bluetooth").
 */
data class AppNode(
    val id: String,
    val description: String,
    val uiHash: String? = null
)

/**
 * Represents a macro edge between two nodes (e.g., tapping a specific button).
 */
data class MacroEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val actionType: String, // "tap", "swipe", "input"
    val x: Float = 0f,
    val y: Float = 0f,
    val text: String? = null
)

/**
 * The App Map maintains a deterministic graph of the Android UI.
 * This allows Akaria to navigate instantly without waking up the VLM.
 */
class AppMap {
    private val nodes = mutableMapOf<String, AppNode>()
    private val edges = mutableListOf<MacroEdge>()

    fun addNode(node: AppNode) {
        nodes[node.id] = node
    }

    fun addEdge(edge: MacroEdge) {
        edges.add(edge)
    }

    fun findPath(startNodeId: String, targetNodeId: String): List<MacroEdge>? {
        Log.i("AppMap", "Searching graph for path from \$startNodeId to \$targetNodeId...")
        
        // Simple BFS to find the shortest macro path
        val queue = ArrayDeque<List<MacroEdge>>()
        val visited = mutableSetOf<String>()
        
        // Initialize queue with outgoing edges from the start node
        edges.filter { it.fromNodeId == startNodeId }.forEach {
            queue.add(listOf(it))
        }
        visited.add(startNodeId)

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val lastEdge = path.last()
            
            if (lastEdge.toNodeId == targetNodeId) {
                Log.i("AppMap", "Path found! Macro sequence length: \${path.size}")
                return path
            }
            
            if (!visited.contains(lastEdge.toNodeId)) {
                visited.add(lastEdge.toNodeId)
                edges.filter { it.fromNodeId == lastEdge.toNodeId }.forEach {
                    val newPath = path.toMutableList()
                    newPath.add(it)
                    queue.add(newPath)
                }
            }
        }
        
        Log.w("AppMap", "No deterministic path found. AI fallback required.")
        return null
    }
}
