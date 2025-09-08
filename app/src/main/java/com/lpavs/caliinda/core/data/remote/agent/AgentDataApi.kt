package com.lpavs.caliinda.core.data.remote.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserContext(
    @SerialName("user:timezone") val timezone: String,
    @SerialName("user:timezone_offset") val timezoneOffset: String,
    @SerialName("user:glance_date") val glanceDate: String,
    @SerialName("user:language") val language: String? = null
)

@Serializable data class ChatRequest(val message: String, val context: UserContext)
