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

class MainActivity : Activity() {

    private val SCREEN_CAPTURE_REQUEST_CODE = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
