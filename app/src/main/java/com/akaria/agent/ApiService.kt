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
                // Create HTTP connection to Termux backend
                val url = URL(BACKEND_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // Send simple JSON payload
                val payload = """{"goal": "$goal", "xmlData": "$xmlData", "status": "screenshot_ready"}"""
                connection.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // Check response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // For now, we hardcode the tap parsed response to prove the loop
                    onActionReceived("tap", 195f, 245f)
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
