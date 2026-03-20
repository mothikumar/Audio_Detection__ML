package com.addev.listaspam.data

import com.google.gson.annotations.SerializedName

data class UploadResponse(
    val prediction: String,
    val confidence: Double,
    @SerializedName("riskLevel") val riskLevel: String? = null // <-- Added annotation here
)