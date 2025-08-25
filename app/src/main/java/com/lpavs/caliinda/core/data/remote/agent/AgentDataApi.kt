package com.lpavs.caliinda.core.data.remote.agent

import com.google.gson.annotations.SerializedName

data class UserContext(
    @SerializedName("user:timezone") val timezone: String,
    @SerializedName("user:timezone_offset") val timezoneOffset: String,
    @SerializedName("user:glance_date") val glanceDate: String, // "YYYY-MM-DD"
    @SerializedName("user:language") val language: String? = null // Необязательное поле
)

data class ChatRequest(
    val message: String,
    val context: UserContext
)