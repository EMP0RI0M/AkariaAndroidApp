package com.akaria.agent

import android.util.Log

/**
 * The Planner is responsible for executing tasks.
 * It first attempts to use the PhoneCLI macro graph (AppMap).
 * If the path is unknown, it falls back to waking up the VLM.
 */
class Planner(private val appMap: AppMap, private val engineScheduler: EngineScheduler) {

    fun executeGoal(startNodeId: String, goalNodeId: String, fallbackModelPath: String, onComplete: (Boolean) -> Unit) {
        Log.i("Planner", "Executing goal: Navigate to \$goalNodeId")
        
        // Step 1: Check deterministic graph (AppMap)
        val macroPath = appMap.findPath(startNodeId, goalNodeId)
        
        if (macroPath != null) {
            // Found a deterministic path! Replay macros without AI.
            Log.i("Planner", "Executing PhoneCLI Macro sequence...")
            for ((index, edge) in macroPath.withIndex()) {
                Log.i("Planner", "Step \${index + 1}: \${edge.actionType} at (\${edge.x}, \${edge.y}) to reach \${edge.toNodeId}")
                if (edge.actionType == "tap") {
                    AkariaAccessibilityService.instance?.performTap(edge.x, edge.y)
                }
            }
            Log.i("Planner", "Goal reached via AppMap macro!")
            onComplete(true)
        } else {
            // Step 2: Fallback to VLM Inference
            Log.w("Planner", "Path unknown. Waking up VLM inference engine...")
            
            // Generate a prompt for the VLM based on the goal
            val prompt = "Task: Navigate to \$goalNodeId. Analyze the current UI and output the next action."
            
            engineScheduler.testInferenceAsync(fallbackModelPath, prompt) { result ->
                Log.i("Planner", "VLM Decision: \$result")
                // In the future, we will parse the VLM output, execute it, and cache the successful path back into AppMap!
                onComplete(true)
            }
        }
    }
}
