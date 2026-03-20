package com.addev.listaspam.data

import com.google.gson.annotations.SerializedName

data class UserBalanceResponse(
    @SerializedName("balance") val balance: Int,
    @SerializedName("currency") val currency: String? = "credits"
)
