package com.akaria.agent.engine.models

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import android.util.Log

data class DownloadState(
    val id: String,
    val modelName: String,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val speedBytesPerSec: Long = 0,
    val status: Status = Status.IDLE,
    val error: String? = null
) {
    enum class Status { IDLE, FETCHING_METADATA, DOWNLOADING, PAUSED, VERIFYING, COMPLETED, ERROR }
}

class DownloadManager private constructor(private val context: Context) {

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    private val modelsDir = File(context.getExternalFilesDir(null), "models").apply { mkdirs() }
    
    // In-memory active connections so we can cancel/pause
    private val activeConnections = mutableMapOf<String, HttpURLConnection>()
    @Volatile private var isPaused = mutableSetOf<String>()

    suspend fun startDownload(id: String, modelName: String, urlString: String, expectedSha256: String? = null) {
        val outputFile = File(modelsDir, "$id.gguf.tmp")
        val finalFile = File(modelsDir, "$id.gguf")

        _downloads.value = _downloads.value.toMutableMap().apply {
            put(id, DownloadState(id = id, modelName = modelName, status = DownloadState.Status.FETCHING_METADATA))
        }
        
        isPaused.remove(id)

        withContext(Dispatchers.IO) {
            try {
                var currentUrl = urlString
                var connection: HttpURLConnection
                var redirectCount = 0
                
                Log.d("DownloadManager", "Request URL: $urlString")
                
                // Handle 302 redirects
                while (true) {
                    connection = URL(currentUrl).openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    // Support resuming
                    if (outputFile.exists() && outputFile.length() > 0) {
                        connection.setRequestProperty("Range", "bytes=${outputFile.length()}-")
                    }
                    
                    connection.connect()
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        val location = connection.getHeaderField("Location")
                        currentUrl = URL(URL(currentUrl), location).toString()
                        Log.d("DownloadManager", "Redirected to: $currentUrl")
                        redirectCount++
                        if (redirectCount > 5) throw Exception("Too many redirects")
                    } else if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                        break
                    } else {
                        throw Exception("HTTP Error $responseCode")
                    }
                }
                
                activeConnections[id] = connection
                
                val contentType = connection.contentType ?: ""
                val contentLength = connection.contentLengthLong
                val transferEncoding = connection.getHeaderField("Transfer-Encoding")
                
                Log.d("DownloadManager", "URL=$currentUrl")
                Log.d("DownloadManager", "Status=${connection.responseCode}")
                Log.d("DownloadManager", "Content-Type=$contentType")
                Log.d("DownloadManager", "Content-Length=$contentLength")
                Log.d("DownloadManager", "Transfer-Encoding=$transferEncoding")
                Log.d("DownloadManager", "Content-Disposition=${connection.getHeaderField("Content-Disposition")}")
                Log.d("DownloadManager", "Location=${connection.getHeaderField("Location")}")
                
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    val error = connection.inputStream.bufferedReader().readText()
                    Log.e("DownloadManager", "Error: Received HTML/JSON instead of file contents.")
                    Log.e("DownloadManager", error.take(500))
                    throw Exception("Invalid content type: $contentType. Received webpage instead of model.")
                }
                
                val totalBytes = if (connection.responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    contentLength + outputFile.length()
                } else {
                    contentLength
                }
                
                val append = connection.responseCode == HttpURLConnection.HTTP_PARTIAL
                val outputStream = FileOutputStream(outputFile, append)
                val inputStream: InputStream = connection.inputStream
                
                val header = ByteArray(16)
                var initialBytesRead = 0
                if (!append && inputStream.available() > 0 || contentLength != 0L) {
                    initialBytesRead = inputStream.read(header)
                    if (initialBytesRead > 0) {
                        Log.d("DownloadManager", "First 16 bytes: " + header.take(initialBytesRead).joinToString(" ") { "%02X".format(it) })
                        outputStream.write(header, 0, initialBytesRead)
                    }
                }
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloaded = if (append) outputFile.length() else initialBytesRead.toLong()
                var lastTime = System.currentTimeMillis()
                var bytesSinceLastCalc = initialBytesRead.toLong()

                // Emit initial downloading state with total bytes
                updateState(id) {
                    it.copy(
                        totalBytes = totalBytes,
                        bytesDownloaded = downloaded,
                        status = DownloadState.Status.DOWNLOADING
                    )
                }

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isPaused.contains(id)) {
                        outputStream.flush()
                        updateState(id) { it.copy(status = DownloadState.Status.PAUSED) }
                        return@withContext
                    }
                    
                    outputStream.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    bytesSinceLastCalc += bytesRead
                    
                    val now = System.currentTimeMillis()
                    if (now - lastTime >= 1000) {
                        val speed = bytesSinceLastCalc * 1000 / (now - lastTime)
                        updateState(id) { 
                            it.copy(
                                progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f,
                                bytesDownloaded = downloaded,
                                totalBytes = totalBytes,
                                speedBytesPerSec = speed,
                                status = DownloadState.Status.DOWNLOADING
                            )
                        }
                        bytesSinceLastCalc = 0
                        lastTime = now
                    }
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                activeConnections.remove(id)

                // Verify SHA256 if provided
                if (expectedSha256 != null) {
                    updateState(id) { it.copy(status = DownloadState.Status.VERIFYING) }
                    val actualSha = verifySha256(outputFile)
                    if (actualSha != expectedSha256) {
                        outputFile.delete()
                        throw Exception("SHA256 mismatch")
                    }
                }

                // Finalize
                outputFile.renameTo(finalFile)
                updateState(id) { 
                    it.copy(progress = 1f, status = DownloadState.Status.COMPLETED) 
                }
                Log.d("DownloadManager", "Downloaded bytes: ${finalFile.length()}")
            } catch (e: Exception) {
                Log.e("DownloadManager", "Exception: ${e.message}", e)
                activeConnections.remove(id)
                updateState(id) { 
                    it.copy(status = DownloadState.Status.ERROR, error = e.message) 
                }
            }
        }
    }

    fun pauseDownload(id: String) {
        isPaused.add(id)
        activeConnections[id]?.disconnect()
    }
    
    fun cancelDownload(id: String) {
        pauseDownload(id)
        File(modelsDir, "$id.gguf.tmp").delete()
        updateState(id) { it.copy(status = DownloadState.Status.IDLE, progress = 0f) }
    }

    private fun updateState(id: String, updater: (DownloadState) -> DownloadState) {
        _downloads.value = _downloads.value.toMutableMap().apply {
            get(id)?.let { put(id, updater(it)) }
        }
    }
    
    private fun verifySha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = file.inputStream()
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
