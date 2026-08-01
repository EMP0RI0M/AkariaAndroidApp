package com.akaria.agent

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Abstracts the native C++ engine from the UI and executes inference
 * on a background worker thread to prevent UI freezing.
 */
class EngineScheduler {
    private val engine = AkariaEngine()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun testInferenceAsync(modelPath: String, prompt: String, onComplete: (String) -> Unit) {
        Log.i("EngineScheduler", "Dispatching inference task to native worker thread...")
        scope.launch {
            // Run on a background thread
            val result = withContext(Dispatchers.IO) {
                engine.testModelInference(modelPath, prompt)
            }
            
            // Switch back to Main thread to deliver the result
            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }
}
