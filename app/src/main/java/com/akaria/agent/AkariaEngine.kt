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
     * Loads a GGUF model into memory.
     * @param modelPath The absolute path to the .gguf file
     * @param contextSize The context window size (e.g., 2048)
     */
    external fun loadModel(modelPath: String, contextSize: Int): Boolean

    /**
     * Unloads the model and frees RAM.
     */
    external fun unloadModel()

    /**
     * Streams inference tokens synchronously. Call this from a background thread!
     * @param prompt The prompt to feed to the model
     * @param maxTokens Maximum number of tokens to generate
     * @param callback Callback invoked every time a token is generated
     */
    external fun generate(prompt: String, maxTokens: Int, callback: TokenCallback)

    /**
     * Stops an ongoing generation loop.
     */
    external fun stopGeneration()

    interface TokenCallback {
        fun onToken(token: String)
    }
}
