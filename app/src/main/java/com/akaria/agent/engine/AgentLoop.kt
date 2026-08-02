package com.akaria.agent.engine

import android.util.Log
import com.akaria.agent.engine.accessibility.AkariaAccessibilityService
import com.akaria.agent.engine.planner.Planner
import com.akaria.agent.engine.planner.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException

enum class AgentState {
    IDLE, OBSERVE, BUILD_CONTEXT, THINK, VALIDATE, EXECUTE, DONE, FAILED
}

class AgentLoop(
    private val core: AkariaCore,
    private val planner: Planner,
    private val accessibilityService: AkariaAccessibilityService?
) {

    private var currentState = AgentState.IDLE
    private var iterations = 0
    private val maxIterations = 15

    enum class StepStatus { PENDING, RUNNING, SUCCESS, FAILED }

    data class AgentStep(
        val timestamp: Long,
        val description: String,
        val status: StepStatus
    )

    private val _timeline = MutableStateFlow<List<AgentStep>>(emptyList())
    val timeline: StateFlow<List<AgentStep>> = _timeline.asStateFlow()

    private fun addTimelineEvent(desc: String, status: StepStatus = StepStatus.PENDING) {
        _timeline.value = _timeline.value + AgentStep(System.currentTimeMillis(), desc, status)
    }

    private fun updateLastTimelineEvent(status: StepStatus, appendedText: String = "") {
        val current = _timeline.value
        if (current.isNotEmpty()) {
            val last = current.last()
            val newDesc = if (appendedText.isNotEmpty()) "${last.description} $appendedText" else last.description
            val updated = current.toMutableList().apply {
                this[size - 1] = last.copy(status = status, description = newDesc)
            }
            _timeline.value = updated
        }
    }

    // Task Memory
    private val history = mutableListOf<String>()

    suspend fun startTask(goal: String) {
        currentState = AgentState.IDLE
        iterations = 0
        history.clear()
        _timeline.value = emptyList()
        
        Log.i("AgentLoop", "Starting new task: $goal")
        addTimelineEvent("Goal: $goal", StepStatus.SUCCESS)
        
        // Initial feedback context
        var lastExecutionResult = "Task Started"
        var lastPrediction = "None"

        while (iterations < maxIterations) {
            iterations++
            Log.i("AgentLoop", "--- Iteration $iterations ---")

            // OBSERVE & BUILD_CONTEXT
            currentState = AgentState.OBSERVE
            val screenContext = accessibilityService?.captureScreenContext() ?: "[]"
            
            currentState = AgentState.BUILD_CONTEXT
            val prompt = buildAgentPrompt(goal, screenContext, lastExecutionResult, lastPrediction)
            
            // THINK
            currentState = AgentState.THINK
            addTimelineEvent("Thinking...", StepStatus.RUNNING)
            val rawResponse = core.runInference(prompt)
            
            // VALIDATE
            currentState = AgentState.VALIDATE
            val parsedAction = validateAndParse(rawResponse)
            
            if (parsedAction == null) {
                lastExecutionResult = "Error: Invalid JSON output. Please strictly follow the tool schema."
                updateLastTimelineEvent(StepStatus.FAILED, "(JSON Error)")
                Log.w("AgentLoop", "Validation failed. Retrying.")
                continue // Repair loop
            }
            
            updateLastTimelineEvent(StepStatus.SUCCESS) // Finished thinking
            
            lastPrediction = parsedAction.predictedOutcome
            
            if (parsedAction.tool == "sys.done") {
                currentState = AgentState.DONE
                addTimelineEvent("Task Complete: ${parsedAction.reasoning}", StepStatus.SUCCESS)
                Log.i("AgentLoop", "Task marked as complete by agent: ${parsedAction.reasoning}")
                break
            }

            // EXECUTE
            currentState = AgentState.EXECUTE
            addTimelineEvent("Executing: ${parsedAction.tool}", StepStatus.RUNNING)
            lastExecutionResult = executeToolCall(parsedAction)
            
            if (lastExecutionResult.startsWith("Success")) {
                updateLastTimelineEvent(StepStatus.SUCCESS)
            } else {
                updateLastTimelineEvent(StepStatus.FAILED)
            }
            
            // Record memory
            history.add("Action: ${parsedAction.tool}, Result: $lastExecutionResult")
        }

        if (iterations >= maxIterations) {
            currentState = AgentState.FAILED
            addTimelineEvent("Task failed: Exceeded max iterations", StepStatus.FAILED)
            Log.w("AgentLoop", "Task failed: Exceeded maximum iterations ($maxIterations)")
        }
    }

    private fun buildAgentPrompt(goal: String, screenContext: String, lastResult: String, lastPrediction: String): String {
        return """
            You are an autonomous AI agent operating an Android device.
            Current Goal: $goal
            
            Past Actions Memory:
            ${history.takeLast(5).joinToString("\n")}
            
            Last Execution Feedback:
            $lastResult
            
            Your Last Prediction (Before Action):
            $lastPrediction
            Did the current screen meet this expectation? If not, replan.
            
            Current Screen Elements:
            $screenContext
            
            Respond STRICTLY with a JSON object containing your reasoning, a tool_calls array, and a predicted_outcome string.
            Available Tools:
            - ui.click (arguments: id)
            - ui.scroll (arguments: direction="up"|"down")
            - sys.done (arguments: message)
            
            Format:
            {
              "reasoning": "I need to tap the download button...",
              "tool_calls": [
                {
                  "tool": "ui.click",
                  "arguments": { "id": 42 }
                }
              ],
              "predicted_outcome": "The screen should change to show the download progress bar."
            }
        """.trimIndent()
    }

    data class ToolCall(val reasoning: String, val tool: String, val arguments: JSONObject, val predictedOutcome: String)

    private fun validateAndParse(rawJson: String): ToolCall? {
        try {
            val start = rawJson.indexOf('{')
            val end = rawJson.lastIndexOf('}')
            if (start == -1 || end == -1) return null
            
            val json = JSONObject(rawJson.substring(start, end + 1))
            val reasoning = json.optString("reasoning", "No reasoning provided")
            val predictedOutcome = json.optString("predicted_outcome", "No prediction")
            val toolCalls = json.optJSONArray("tool_calls")
            
            if (toolCalls != null && toolCalls.length() > 0) {
                val call = toolCalls.getJSONObject(0)
                return ToolCall(
                    reasoning = reasoning,
                    tool = call.getString("tool"),
                    arguments = call.optJSONObject("arguments") ?: JSONObject(),
                    predictedOutcome = predictedOutcome
                )
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    private fun executeToolCall(action: ToolCall): String {
        Log.i("AgentLoop", "Executing: ${action.tool} with args ${action.arguments}")
        
        return when (action.tool) {
            "ui.click" -> {
                val id = action.arguments.optInt("id", -1)
                if (id == -1) return "Execution Failed: missing id argument"
                
                val success = accessibilityService?.clickNodeById(id) ?: false
                if (success) "Success: clicked element $id" else "Failed: Element $id not found or not clickable"
            }
            "ui.scroll" -> {
                val dir = action.arguments.optString("direction", "down")
                val success = accessibilityService?.scroll(dir) ?: false
                if (success) "Success: scrolled $dir" else "Failed: could not scroll $dir"
            }
            else -> "Execution Failed: Unknown tool ${action.tool}"
        }
    }
}
