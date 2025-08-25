package com.lpavs.caliinda.core.data.remote.agent

import android.util.Log
import com.lpavs.caliinda.app.di.IoDispatcher
import com.lpavs.caliinda.core.common.ApiException
import com.lpavs.caliinda.core.common.NetworkException
import com.lpavs.caliinda.core.common.UnknownException
import com.lpavs.caliinda.core.data.auth.AuthManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject


interface AgentRepository {
    suspend fun sendMessage(message: String): Result<Unit> // Можно изменить <Unit> на модель ответа, если она появится
}

class AgentRemoteDataSource
@Inject
constructor(
    private val apiService: AgentApiService,
    private val authManager: AuthManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val TAG = "CalendarRemoteDataSource"
  private suspend inline fun <T> authenticatedApiCall(
      crossinline apiCall: suspend (String) -> T
  ): Result<T> {
    val token = authManager.getBackendAuthToken()
    return if (token != null) {
      safeApiCall { apiCall("Bearer $token") }
    } else {
      Log.e(TAG, "Could not get fresh token")
      Result.failure(Exception("Authorization needed"))
    }
  }

  suspend fun run_chat(message: String, context: UserContext): Result<Unit> {
    return authenticatedApiCall { token ->
      apiService.run(
          token = token,
          request = ChatRequest(
              message,
              context
          )
      )
    }
  }

  private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    Log.d(TAG, "safeApiCall started")
    return withContext(ioDispatcher) {
        try {
            Log.d(TAG, "safeApiCall executing apiCall")
            val result = apiCall()
            Log.d(TAG, "safeApiCall apiCall successful, result: $result")
            Result.success(result)
        } catch (e: Throwable) {
            Log.e(TAG, "safeApiCall caught exception: ${e.javaClass.simpleName}", e)
            when (e) {
                is IOException -> {
                    Log.e(TAG, "safeApiCall: IOException - Network issue", e)
                    Result.failure(NetworkException("Network issue: ${e.message}"))
                }

                is HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e(
                        TAG,
                        "safeApiCall: HttpException - code: ${e.code()}, errorBody: $errorBody",
                        e
                    )
                    Result.failure(ApiException(e.code(), "Server error: ${e.code()}. $errorBody"))
                }

                is ApiException -> {
                    Log.e(TAG, "safeApiCall: ApiException - code: ${e}, message: ${e}", e)
                    Result.failure(e)
                }

                else -> {
                    Log.e(TAG, "safeApiCall: UnknownException - message: ${e.message}", e)
                    Result.failure(UnknownException("Unknown error: ${e.message}"))
                }
            }
        }
    }
  }
}