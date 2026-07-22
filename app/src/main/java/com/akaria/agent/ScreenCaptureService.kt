package com.akaria.agent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.security.MessageDigest
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastFrameHash: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("DATA")

        if (resultCode != 0 && data != null) {
            startForeground()
            startCapture(resultCode, data)
        }
        return START_NOT_STICKY
    }

    private fun startForeground() {
        val channelId = "AkariaServiceChannel"
        val channel = NotificationChannel(channelId, "Akaria Capture Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Akaria Engine Running")
            .setContentText("Capturing screen for AI analysis...")
            .setSmallIcon(android.R.drawable.ic_menu_camera) // REQUIRED by Android
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "AkariaCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        Log.i("Akaria", "Screen capture virtual display created.")
        
        // Setup capture loop (e.g. 1 frame every 3 seconds)
        handler.postDelayed(captureRunnable, 3000)
    }

    private val captureRunnable = object : Runnable {
        override fun run() {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val currentHash = computeHash(buffer)
                
                if (currentHash != lastFrameHash) {
                    lastFrameHash = currentHash
                    Log.i("Akaria", "New frame detected! Hash: $currentHash. Sending to Backend...")
                    
                    // Mock sending to backend
                    ApiService.sendStepToBackend(File(""), "<hierarchy/>", "Auto-goal") { action, x, y ->
                        Log.i("Akaria", "Received Action: $action at ($x, $y)")
                    }
                } else {
                    Log.d("Akaria", "Screen unchanged. Skipping inference.")
                }
                
                image.close()
            }
            handler.postDelayed(this, 1500) // Sped up to 1.5 seconds since we are skipping redundant frames
        }
    }

    private fun computeHash(buffer: ByteBuffer): String {
        buffer.rewind()
        val md = MessageDigest.getInstance("MD5")
        // To save CPU, we could hash just a subset of the buffer, 
        // but modern phones can hash a screen buffer extremely fast.
        md.update(buffer)
        buffer.rewind()
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        handler.removeCallbacks(captureRunnable)
    }
}
