package com.akaria.agent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.File

class MainActivity : Activity() {

    private val SCREEN_CAPTURE_REQUEST_CODE = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure models directory exists
        val modelsDir = File(getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        val scheduler = EngineScheduler()
        
        // Initialize OpenPhone Architecture (App Map)
        val appMap = AppMap().apply {
            addNode(AppNode("home_screen", "The Android Home Screen"))
            addNode(AppNode("settings_main", "The main settings menu"))
            addNode(AppNode("settings_bluetooth", "The Bluetooth settings page"))
            
            // Add a known deterministic macro path
            addEdge(MacroEdge("home_screen", "settings_main", "tap", 100f, 200f))
            addEdge(MacroEdge("settings_main", "settings_bluetooth", "tap", 150f, 300f))
        }
        
        val planner = Planner(appMap, scheduler)

        // Simple programmatic UI for testing
        val captureButton = Button(this).apply {
            text = "START AKARIA SCREEN CAPTURE"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Please grant 'Display over other apps' permission for the floating icon.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                    return@setOnClickListener
                }
                // Test PhoneCLI / AppMap logic
                val modelPath = File(modelsDir, "tiny.gguf").absolutePath
                
                // Test 1: A known path (should execute instantly without AI)
                planner.executeGoal("home_screen", "settings_bluetooth", modelPath) {
                    // Test 2: An unknown path (should fallback to VLM inference)
                    planner.executeGoal("home_screen", "unknown_app", modelPath) {
                        Toast.makeText(this@MainActivity, "Planner tests complete. Check Logcat.", Toast.LENGTH_LONG).show()
                    }
                }
                
                startScreenCapture()
            }
        }
        setContentView(captureButton)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun startScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.i("Akaria", "Screen capture permission granted.")
                Toast.makeText(this, "Akaria Engine Started", Toast.LENGTH_SHORT).show()
                
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("RESULT_CODE", resultCode)
                    putExtra("DATA", data)
                }
                startForegroundService(serviceIntent)
            } else {
                Log.e("Akaria", "Screen capture permission denied.")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
