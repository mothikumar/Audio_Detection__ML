package com.addev.listaspam

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.addev.listaspam.api.ApiClient
import com.addev.listaspam.databinding.ActivityUploadBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private var audioUri: Uri? = null
    private val TAG = "UploadActivity"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val apiService = ApiClient.getService()

    private val selectAudioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                audioUri = result.data?.data
                if (audioUri != null) {
                    binding.tvSelectedFile.text = "Audio selected"
                    binding.btnAnalyze.visibility = View.VISIBLE
                    binding.btnAnalyze.isEnabled = true
                }
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) selectAudio()
            else Toast.makeText(this, "Permission required", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AI Voice Analysis"

        binding.btnAnalyze.visibility = View.GONE

        binding.btnSelectFile.setOnClickListener {
            if (hasPermission()) selectAudio()
            else requestPermission()
        }

        binding.btnAnalyze.setOnClickListener {
            analyzeCall()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun selectAudio() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        selectAudioLauncher.launch(Intent.createChooser(intent, "Select Audio"))
    }

    private fun analyzeCall() {
        if (audioUri == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE
        binding.btnAnalyze.isEnabled = false

        scope.launch {
            val file = withContext(Dispatchers.IO) { getFileFromUri(audioUri!!) }

            if (file == null) {
                resetUI("File read error")
                return@launch
            }

            val requestBody = file.asRequestBody("audio/*".toMediaType())
            val part = MultipartBody.Part.createFormData("audio", file.name, requestBody)

            apiService.uploadAudio(part).enqueue(object :
                Callback<com.addev.listaspam.data.UploadResponse> {

                override fun onResponse(
                    call: Call<com.addev.listaspam.data.UploadResponse>,
                    response: Response<com.addev.listaspam.data.UploadResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    binding.btnAnalyze.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        binding.resultCard.visibility = View.VISIBLE
                        binding.tvPrediction.text = "Prediction: ${res.prediction}"
                        binding.tvConfidence.text =
                            "Confidence: ${(res.confidence * 100).toInt()}%"
                        binding.confidenceProgress.progress =
                            (res.confidence * 100).toInt()
                        binding.tvRiskLevel.text =
                            "Risk Level: ${res.riskLevel ?: "Unknown"}" // <-- Capital L here!
                    } else {
                        resetUI("Analysis failed")
                    }
                }

                override fun onFailure(call: Call<com.addev.listaspam.data.UploadResponse>, t: Throwable) {
                    resetUI("Server error")
                }
            })
        }
    }

    private fun resetUI(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE
        binding.btnAnalyze.isEnabled = true
        binding.tvSelectedFile.text = msg
    }

    private fun hasPermission(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        return ContextCompat.checkSelfPermission(this, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        requestPermissionLauncher.launch(permission)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "audio_${System.currentTimeMillis()}.wav")

            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "File error", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
