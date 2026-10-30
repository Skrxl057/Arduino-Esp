package com.embedhub.app.core.ota

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.*
import java.io.File
import java.io.IOException

sealed class OtaProgress {
    data class Progress(val bytesSent: Long, val totalBytes: Long, val percentage: Int) : OtaProgress()
    data class Success(val responseMessage: String) : OtaProgress()
    data class Failure(val error: String) : OtaProgress()
}

class EspOtaUpdater(private val okHttpClient: OkHttpClient = OkHttpClient()) {

    /**
     * Streams progress of binary firmware upload to ESP HTTP OTA endpoint
     * e.g. http://192.168.4.1/update or http://esp-ip:80/update
     */
    fun uploadFirmware(espIp: String, espPort: Int = 80, binaryFile: File): Flow<OtaProgress> = flow {
        val totalBytes = binaryFile.length()
        if (totalBytes <= 0) {
            emit(OtaProgress.Failure("Invalid or empty binary file"))
            return@flow
        }

        val url = "http://$espIp:$espPort/update"

        val countingBody = object : RequestBody() {
            override fun contentType(): MediaType? = "application/octet-stream".toMediaTypeOrNull()

            override fun contentLength(): Long = totalBytes

            override fun writeTo(sink: BufferedSink) {
                val source = binaryFile.source()
                val buffer = okio.Buffer()
                var bytesSent = 0L

                while (true) {
                    val read = source.read(buffer, 4096)
                    if (read == -1L) break
                    sink.write(buffer, read)
                    bytesSent += read
                    // In-stream progress tracking
                }
            }
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "update",
                binaryFile.name,
                countingBody
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()

        try {
            // Emitting initial progress
            emit(OtaProgress.Progress(0, totalBytes, 0))

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                emit(OtaProgress.Progress(totalBytes, totalBytes, 100))
                val message = response.body?.string() ?: "Flashing successful"
                emit(OtaProgress.Success(message))
            } else {
                emit(OtaProgress.Failure("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: IOException) {
            emit(OtaProgress.Failure("Network error during OTA: ${e.message}"))
        } catch (e: Exception) {
            emit(OtaProgress.Failure("OTA update error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
