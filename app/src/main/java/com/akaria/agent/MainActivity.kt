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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.akaria.agent.ui.AkariaApp
import java.io.File

class MainActivity : ComponentActivity() {

    private val SCREEN_CAPTURE_REQUEST_CODE = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure models directory exists
        val modelsDir = File(getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        // Set up the modern Compose UI
        setContent {
            AkariaApp()
        }

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
                
                // Start the Compose Floating Chat UI
                val floatingIntent = Intent(this, FloatingService::class.java)
                startService(floatingIntent)
                
            } else {
                Log.e("Akaria", "Screen capture permission denied.")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
