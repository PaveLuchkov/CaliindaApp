package com.lpavs.caliinda.core.data.auth

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
  @POST("auth/google/exchange")
  suspend fun exchangeAuthTokens(@Body authBody: AuthBody): Response<AuthResponse>
}

@Serializable data class AuthBody(val idToken: String, val authCode: String)

@Serializable
data class AuthResponse(
    val status: String,
    val message: String,
    val token: String?
)
