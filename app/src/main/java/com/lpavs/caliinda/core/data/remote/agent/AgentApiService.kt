package com.lpavs.caliinda.core.data.remote.agent

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AgentApiService {
  @POST("agent/chat/start")
  suspend fun run(
      @Header("Authorization") token: String,
      @Body request: ChatRequest
  ): Response<Unit>
}