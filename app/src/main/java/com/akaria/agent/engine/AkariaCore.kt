package com.akaria.agent.engine

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.akaria.agent.AkariaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The central brain of the Akaria Assistant.
 * Manages all subsystems (Planner, Models, Inference, Accessibility, Vision).
 * Acts as the single source of truth for the entire application.
 */
class AkariaCore private constructor(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jniEngine = AkariaEngine()
    
    // Core state exposed to the rest of the application
    private val _coreState = MutableStateFlow<CoreState>(CoreState.Booting)
    val coreState: StateFlow<CoreState> = _coreState.asStateFlow()

    private val modelsDir = File(context.getExternalFilesDir(null), "models").apply { mkdirs() }
    
    // Telemetry state
    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    init {
        reboot()
    }

    /**
     * Strict startup lifecycle. 
     * Validates all subsystems before reporting READY.
     */
    fun reboot() {
        scope.launch {
            _coreState.value = CoreState.Booting
            
            // 1. Check directories & storage
            updateTelemetry()
            
            // 2. Detect Models
            val ggufFiles = detectModels()
            if (ggufFiles.isEmpty()) {
                _coreState.value = CoreState.Error("No GGUF models found. Please download one.")
                return@launch
            }
            
            // 3. Initialize subsystems (stubs for now, to be expanded)
            _coreState.value = CoreState.InitializingSubsystems
            
            // 4. Warm up model
            val defaultModel = ggufFiles.first()
            _coreState.value = CoreState.WarmingUp(defaultModel.name)
            
            // (In a real implementation, we would load the model into RAM here via JNI,
            // but currently our JNI bridge loads and unloads per-request. 
            // We will refactor JNI later to keep the context persistent.)
            
            // 5. System Ready
            _coreState.value = CoreState.Ready(defaultModel)
        }
    }

    private fun detectModels(): List<File> {
        return modelsDir.listFiles { _, name -> name.endsWith(".gguf") }?.toList() ?: emptyList()
    }

    /**
     * Executes a prompt through the inference engine.
     */
    suspend fun runInference(prompt: String): String {
        val state = _coreState.value
        if (state !is CoreState.Ready) {
            throw IllegalStateException("Akaria is not ready. Current state: $state")
        }

        _coreState.value = CoreState.Inferencing
        
        return withContext(Dispatchers.IO) {
            try {
                // Call down to JNI
                val response = jniEngine.testModelInference(state.activeModel.absolutePath, prompt)
                response
            } finally {
                // Return to ready state
                _coreState.value = CoreState.Ready(state.activeModel)
            }
        }
    }

    /**
     * Collects live telemetry from the Android device.
     */
    fun updateTelemetry() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val freeStorageMb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024 * 1024)

        // Basic RAM (requires ActivityManager, keeping simple here)
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)

        _telemetry.value = TelemetryData(
            batteryLevel = batteryLevel,
            freeStorageMb = freeStorageMb,
            usedRamMb = usedMem,
            maxRamMb = maxMem
        )
    }

    companion object {
        @Volatile
        private var instance: AkariaCore? = null

        fun getInstance(context: Context): AkariaCore {
            return instance ?: synchronized(this) {
                instance ?: AkariaCore(context.applicationContext).also { instance = it }
            }
        }
    }
}

sealed class CoreState {
    object Booting : CoreState()
    object InitializingSubsystems : CoreState()
    data class WarmingUp(val modelName: String) : CoreState()
    data class Ready(val activeModel: File) : CoreState()
    object Inferencing : CoreState()
    data class Error(val message: String) : CoreState()
}

data class TelemetryData(
    val batteryLevel: Int = -1,
    val freeStorageMb: Long = -1,
    val usedRamMb: Long = -1,
    val maxRamMb: Long = -1,
    val accessibilityEnabled: Boolean = false,
    val mediaProjectionEnabled: Boolean = false
)
