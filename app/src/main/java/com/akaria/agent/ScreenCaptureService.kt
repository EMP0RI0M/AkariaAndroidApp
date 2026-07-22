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
import android.graphics.PixelFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

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
            //.setSmallIcon(R.mipmap.ic_launcher) // Excluded to prevent crash
            .build()

        startForeground(1, notification)
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
                // In a real app: Convert image plane to Bitmap, save to file
                // File tempFile = File(cacheDir, "current_screen.png")
                // saveBitmap(bitmap, tempFile)
                
                Log.i("Akaria", "Captured frame! Sending to Termux Backend...")
                
                // Mock sending to backend
                ApiService.sendStepToBackend(File(""), "<hierarchy/>", "Auto-goal") { action, x, y ->
                    Log.i("Akaria", "Received Action: $action at ($x, $y)")
                }
                
                image.close()
            }
            handler.postDelayed(this, 3000) // Loop every 3 seconds
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        handler.removeCallbacks(captureRunnable)
    }
}
