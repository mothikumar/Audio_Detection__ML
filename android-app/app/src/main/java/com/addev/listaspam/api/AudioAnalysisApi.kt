package com.addev.listaspam.api

import com.addev.listaspam.data.UploadResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AudioAnalysisApi {
    @Multipart
    @POST("upload") // Your endpoint
    fun uploadAudio(@Part audio: MultipartBody.Part): Call<UploadResponse>
}