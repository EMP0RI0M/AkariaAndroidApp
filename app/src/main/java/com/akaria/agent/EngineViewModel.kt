package com.akaria.agent

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class EngineViewModel(application: Application) : AndroidViewModel(application) {
    private val modelsDir = File(application.getExternalFilesDir(null), "models").apply { mkdirs() }
    
    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val akariaEngine = AkariaEngine()

    fun checkModels() {
        val ggufFiles = modelsDir.listFiles { _, name -> name.endsWith(".gguf") }
        if (ggufFiles != null && ggufFiles.isNotEmpty()) {
            _engineState.value = EngineState.ModelReady(ggufFiles.first())
        } else {
            _engineState.value = EngineState.ModelMissing
        }
    }

    fun downloadTinyModel() {
        _engineState.value = EngineState.Downloading(0f)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tiny stories model (around ~25MB) for quick testing
                val url = URL("https://huggingface.co/karpathy/tinyllamas/resolve/main/stories15M.bin") 
                // wait, stories15M is bin, we need a GGUF.
                // Qwen1.5-0.5B-Chat-Q4_K_M.gguf is ~350MB, might be too big for a quick test.
                // Let's use a known tiny model GGUF URL if possible, or just mock the download if the user wants to push it themselves.
                // For now, let's just create a dummy file if the user hasn't provided a URL.
                // Wait, if it's a real vertical slice, we need a real model.
                // Let's use a very small model URL: 
                val modelUrl = URL("https://huggingface.co/ikawrakow/various-2b-sota-gguf/resolve/main/smollm-135m-instruct-add-bos-q8_0.gguf") // 144MB
                
                val connection = modelUrl.openConnection() as HttpURLConnection
                connection.connect()
                
                val fileLength = connection.contentLength
                val outputFile = File(modelsDir, "smollm-135m.gguf")
                
                val input = connection.inputStream
                val output = FileOutputStream(outputFile)
                
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        _engineState.value = EngineState.Downloading(total.toFloat() / fileLength)
                    }
                    output.write(data, 0, count)
                }
                
                output.flush()
                output.close()
                input.close()
                
                _engineState.value = EngineState.ModelReady(outputFile)
            } catch (e: Exception) {
                Log.e("EngineViewModel", "Download failed", e)
                _engineState.value = EngineState.Error("Download failed: ${e.message}")
            }
        }
    }

    fun runInference(prompt: String) {
        val currentState = _engineState.value
        if (currentState is EngineState.ModelReady) {
            _engineState.value = EngineState.Inferencing
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = akariaEngine.testModelInference(currentState.modelFile.absolutePath, prompt)
                    _engineState.value = EngineState.InferenceComplete(result, currentState.modelFile)
                } catch (e: Exception) {
                    _engineState.value = EngineState.Error("Inference failed: ${e.message}")
                }
            }
        }
    }
}

sealed class EngineState {
    object Idle : EngineState()
    object ModelMissing : EngineState()
    data class Downloading(val progress: Float) : EngineState()
    data class ModelReady(val modelFile: File) : EngineState()
    object Inferencing : EngineState()
    data class InferenceComplete(val result: String, val modelFile: File) : EngineState()
    data class Error(val message: String) : EngineState()
}
