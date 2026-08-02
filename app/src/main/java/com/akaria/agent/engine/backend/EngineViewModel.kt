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

import com.akaria.agent.engine.models.DownloadManager
import com.akaria.agent.engine.models.ModelLibrary
import com.akaria.agent.engine.AgentLoop
import com.akaria.agent.engine.planner.Planner

class EngineViewModel(application: Application) : AndroidViewModel(application) {
    
    private val core = AkariaCore.getInstance(application)
    val downloadManager = DownloadManager.getInstance(application)
    
    // We instantiate AgentLoop here for now. Planner and AccessibilityService can be null/stubbed initially.
    private val agentLoop = AgentLoop(core, Planner(), null)
    
    val agentTimeline = agentLoop.timeline
    
    // UI exposes the core state directly
    val coreState = core.coreState
    val telemetry = core.telemetry
    
    val activeDownloads = downloadManager.downloads

    init {
        // Kick off telemetry polling
        viewModelScope.launch {
            while (true) {
                core.updateTelemetry()
                kotlinx.coroutines.delay(2000) // update every 2 seconds
            }
        }
    }

    fun startModelDownload(modelId: String) {
        viewModelScope.launch {
            val model = ModelLibrary.recommendedModels.find { it.id == modelId } ?: return@launch
            downloadManager.startDownload(model.id, model.name, model.downloadUrl, model.sha256)
            core.reboot() // Force core to rescan when done
        }
    }

    fun pauseModelDownload(modelId: String) {
        downloadManager.pauseDownload(modelId)
    }

    fun cancelModelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
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

    fun startAgentTask(goal: String) {
        viewModelScope.launch {
            try {
                agentLoop.startTask(goal)
            } catch (e: Exception) {
                Log.e("EngineViewModel", "Agent task failed", e)
            }
        }
    }
}
