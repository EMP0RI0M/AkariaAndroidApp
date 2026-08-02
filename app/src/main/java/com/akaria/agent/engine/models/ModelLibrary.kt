package com.akaria.agent.engine.models

data class ModelInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val contextWindow: Int,
    val quantization: String,
    val requiredRamMb: Long,
    val speedEstimate: String,
    val downloadUrl: String,
    val hfRating: Float = 0f,
    val isRecommended: Boolean = false,
    val sha256: String? = null
)

object ModelLibrary {
    // Initial recommended models. In a full app, this would be fetched from a server or HF API.
    val recommendedModels = listOf(
        ModelInfo(
            id = "gemma-4b-q4",
            name = "Gemma 4B",
            sizeBytes = 637699456L, // True size of the dummy payload to avoid UI confusion
            contextWindow = 8192,
            quantization = "Q4_K_M",
            requiredRamMb = 4500,
            speedEstimate = "Fast",
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_0.gguf", // Dummy link that actually works (public)
            hfRating = 4.8f,
            isRecommended = true
        ),
        ModelInfo(
            id = "tiny-llama-1b",
            name = "TinyLlama 1.1B",
            sizeBytes = 630_000_000L,
            contextWindow = 2048,
            quantization = "Q4_0",
            requiredRamMb = 1024,
            speedEstimate = "Very Fast",
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_0.gguf",
            hfRating = 4.2f,
            isRecommended = false
        )
    )
    
    // Stub for searching HF directly
    suspend fun searchHuggingFace(query: String): List<ModelInfo> {
        // Here we would make a REST call to https://huggingface.co/api/models?search=$query&filter=gguf
        // For the sake of the vertical slice, return a stub.
        return emptyList()
    }
}
