package com.akaria.agent

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ApiService {
    
    private val executor = Executors.newSingleThreadExecutor()
    // Points to the Python FastAPI server running in Termux on the same device
    private const val BACKEND_URL = "http://localhost:9000/api/v1/step"

    /**
     * Sends the screenshot, XML, and goal to the Ubuntu backend.
     * In a production app, use Retrofit/OkHttp for multipart requests.
     */
    fun sendStepToBackend(
        screenshot: File, 
        xmlData: String, 
        goal: String, 
        onActionReceived: (action: String, x: Float, y: Float) -> Unit
    ) {
        executor.execute {
            try {
                // Mocking the network call for rapid development architecture.
                // Normally we'd build a Multipart Form request here with `screenshot` and `xmlData`.
                
                // Simulate network latency
                Thread.sleep(1500)
                
                // Simulate backend response
                val mockResponse = """{"action": "tap", "x": 195, "y": 245, "confidence": 0.95}"""
                
                // In production, parse JSON here
                onActionReceived("tap", 195f, 245f)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
