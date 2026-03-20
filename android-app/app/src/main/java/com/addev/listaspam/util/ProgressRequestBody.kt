package com.addev.listaspam.util

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val listener: UploadProgressListener
) : RequestBody() {

    interface UploadProgressListener {
        fun onProgressUpdate(percentage: Int)
    }

    override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val inputStream = FileInputStream(file)
        var uploaded: Long = 0
        val handler = Handler(Looper.getMainLooper())

        try {
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                val progress = (uploaded * 100 / fileLength).toInt()
                handler.post { listener.onProgressUpdate(progress) }
            }
        } finally {
            inputStream.close()
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}
