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
import android.view.WindowManager
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.widget.Toast
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
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
            val data = intent?.getParcelableExtra<Intent>("DATA")

            if (resultCode != 0 && data != null) {
                startForegroundNotification()
                startCapture(resultCode, data)
            }
        } catch (e: Exception) {
            Log.e("Akaria", "Crash in onStartCommand", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Crash: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
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
        
        showFloatingIcon()
        
        // Setup capture loop
        handler.postDelayed(captureRunnable, 1500)
    }

    private fun showFloatingIcon() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            val tv = TextView(this).apply {
                text = "👁 Akaria"
                textSize = 14f
                setBackgroundColor(Color.parseColor("#99000000")) // Semi-transparent black
                setPadding(30, 20, 30, 20)
                setTextColor(Color.WHITE)
            }
            floatingView = tv
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = 100 // Slightly below the top edge
            
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e("Akaria", "Crash in showFloatingIcon", e)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Orb Crash: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val captureRunnable = object : Runnable {
        override fun run() {
            try {
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
            } catch (e: Exception) {
                Log.e("Akaria", "Crash in captureRunnable", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@ScreenCaptureService, "Capture Crash: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
        floatingView?.let { windowManager?.removeView(it) }
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        handler.removeCallbacks(captureRunnable)
    }
}
