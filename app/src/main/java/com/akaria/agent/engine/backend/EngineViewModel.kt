package com.akaria.agent.engine.backend

import android.app.Application
import com.akaria.agent.AkariaEngine
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
import com.akaria.agent.engine.AkariaCore
import com.akaria.agent.engine.CoreState

class EngineViewModel(application: Application) : AndroidViewModel(application) {
    
    private val core = AkariaCore.getInstance(application)
    
    // UI exposes the core state directly
    val coreState = core.coreState
    val telemetry = core.telemetry
    
    // Keep local state for downloading since it's a specific UI operation, 
    // or we could move this to a separate ModelManager later.
    private val modelsDir = File(application.getExternalFilesDir(null), "models").apply { mkdirs() }
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    init {
        // Kick off telemetry polling
        viewModelScope.launch {
            while (true) {
                core.updateTelemetry()
                kotlinx.coroutines.delay(2000) // update every 2 seconds
            }
        }
    }

    fun downloadTinyModel() {
        _downloadProgress.value = 0f
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var connection: HttpURLConnection
                var redirectUrl = "https://huggingface.co/ikawrakow/various-2b-sota-gguf/resolve/main/smollm-135m-instruct-add-bos-q8_0.gguf"
                
                var redirectCount = 0
                while (true) {
                    val url = URL(redirectUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connect()
                    
                    val status = connection.responseCode
                    if (status != HttpURLConnection.HTTP_OK && (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)) {
                        redirectUrl = connection.getHeaderField("Location")
                        redirectCount++
                        if (redirectCount > 5) throw Exception("Too many redirects")
                    } else {
                        break
                    }
                }
                
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
                        _downloadProgress.value = total.toFloat() / fileLength
                    }
                    output.write(data, 0, count)
                }
                
                output.flush()
                output.close()
                input.close()
                
                _downloadProgress.value = null
                
                // Reboot core to detect the new model
                core.reboot()
            } catch (e: Exception) {
                Log.e("EngineViewModel", "Download failed", e)
                _downloadProgress.value = null
            }
        }
    }

    private val _inferenceResult = MutableStateFlow<String?>(null)
    val inferenceResult = _inferenceResult.asStateFlow()

    fun runInference(prompt: String) {
        viewModelScope.launch {
            _inferenceResult.value = null
            try {
                val result = core.runInference(prompt)
                _inferenceResult.value = result
            } catch (e: Exception) {
                _inferenceResult.value = "Error: ${e.message}"
            }
        }
    }
}
