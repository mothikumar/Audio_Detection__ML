package com.addev.listaspam.data

import com.google.gson.annotations.SerializedName

data class AudioAnalysisResponse(
    @SerializedName("prediction") val prediction: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("riskLevel") val riskLevel: String // <-- Changed from "risk_level"
)