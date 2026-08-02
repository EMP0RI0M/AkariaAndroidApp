package com.akaria.agent.engine.planner

import org.json.JSONObject
import org.json.JSONException

/**
 * Represents a structured intent produced by the Planner.
 */
sealed class Intent {
    data class UIMacro(val action: String, val targetDescription: String) : Intent()
    data class DeviceAction(val function: String, val parameters: Map<String, String>) : Intent()
    data class Conversational(val text: String) : Intent()
    object Unknown : Intent()
}

/**
 * The Planner takes natural language input and queries the local LLM to output a JSON intent.
 */
class Planner {
    
    // System prompt instructing the LLM to act as a structured router
    private val systemPrompt = """
        You are the Akaria Intent Planner. Your job is to parse the user's request and output a STRICT JSON object representing the action to take.
        Do NOT output markdown. Do NOT output conversational text. Output ONLY JSON.
        
        Valid Intent Types:
        1. "UI_MACRO" - When the user asks to interact with an app UI (e.g., "click the heart", "scroll down")
        2. "DEVICE_ACTION" - When the user asks for a device function (e.g., "turn on flashlight")
        3. "CONVERSATIONAL" - When the user is just chatting.
        
        Example outputs:
        {"intent": "UI_MACRO", "action": "CLICK", "target_description": "Submit button"}
        {"intent": "DEVICE_ACTION", "function": "FLASHLIGHT", "parameters": {"state": "ON"}}
        {"intent": "CONVERSATIONAL", "text": "Hello! How can I help you today?"}
    """.trimIndent()

    /**
     * Parses the LLM's raw text response into a structured Intent object.
     */
    fun parseIntent(rawLlmResponse: String): Intent {
        try {
            // Find JSON boundaries in case the LLM ignored instructions and wrapped in markdown
            val jsonStart = rawLlmResponse.indexOf('{')
            val jsonEnd = rawLlmResponse.lastIndexOf('}')
            
            if (jsonStart == -1 || jsonEnd == -1) {
                return Intent.Unknown
            }
            
            val jsonString = rawLlmResponse.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonString)
            
            return when (json.optString("intent").uppercase()) {
                "UI_MACRO" -> {
                    Intent.UIMacro(
                        action = json.optString("action"),
                        targetDescription = json.optString("target_description")
                    )
                }
                "DEVICE_ACTION" -> {
                    val paramsJson = json.optJSONObject("parameters")
                    val params = mutableMapOf<String, String>()
                    paramsJson?.let {
                        val keys = it.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            params[key] = it.getString(key)
                        }
                    }
                    Intent.DeviceAction(
                        function = json.optString("function"),
                        parameters = params
                    )
                }
                "CONVERSATIONAL" -> {
                    Intent.Conversational(text = json.optString("text"))
                }
                else -> Intent.Unknown
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            return Intent.Unknown
        }
    }
    
    /**
     * Formats the prompt to send to the LLM, optionally including the screen context.
     */
    fun buildPrompt(userMessage: String, screenContextJson: String? = null): String {
        var prompt = "$systemPrompt\n\n"
        if (screenContextJson != null && screenContextJson.isNotBlank()) {
            prompt += "Current Screen Context:\n$screenContextJson\n\n"
        }
        prompt += "User Request: $userMessage\n\nJSON Output:"
        return prompt
    }
}
