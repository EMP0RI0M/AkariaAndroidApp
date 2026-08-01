package com.akaria.agent

import android.util.Log

class AkariaEngine {
    init {
        try {
            System.loadLibrary("akaria_engine")
            Log.i("AkariaEngine", "Successfully loaded native C++ engine!")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("AkariaEngine", "Failed to load native C++ engine.", e)
        }
    }

    /**
     * Initializes the backend, loads a GGUF model from the given path,
     * tokenizes the prompt, generates 10-20 tokens, and returns the output.
     */
    external fun testModelInference(modelPath: String, prompt: String): String
}
