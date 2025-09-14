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
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AgentRemoteDataSource
@Inject
constructor(
    private val apiService: AgentApiService,
    private val authManager: AuthManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val TAG = "AgentRemoteDataSource"

  suspend fun runChat(message: String, context: UserContext): Result<ChatApiResponse> {
    val token = authManager.getBackendAuthToken()
    if (token == null) {
      Log.e(TAG, "Could not get fresh token")
      return Result.failure(Exception("Authorization needed"))
    }

    return safeApiCall {
      apiService.run(token = "Bearer $token", request = ChatRequest(message, context))
    }
  }

  suspend fun deleteChat(): Result<Unit> {
    val token = authManager.getBackendAuthToken()
    if (token == null) {
      Log.e(TAG, "Could not get fresh token")
      return Result.failure(Exception("Authorization needed"))
    }

    return safeApiCall {
      apiService.delete(
          token = "Bearer $token",
      )
    }
  }

  private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
    return withContext(ioDispatcher) {
      try {
        val response = apiCall()
        if (response.isSuccessful) {
          val body = response.body()
          if (body != null) {
            Result.success(body)
          } else if (response.code() == 204) {
            Result.success(Unit as T)
          } else {
            Result.failure(
                Exception(
                    "My brother Server said nothing, well continue with oldschool mmethods or try later"))
          }
        } else {
          val errorBody = response.errorBody()?.string()
          Log.e(TAG, "API call failed with code ${response.code()}: $errorBody")
          Result.failure(
              ApiException(
                  response.code(), "Sorry cannot help you now, my brother Server is in trouble"))
        }
      } catch (e: Throwable) {
        Log.e(TAG, "API call exception: ${e.javaClass.simpleName}", e)
        when (e) {
          is IOException ->
              Result.failure(
                  NetworkException("Some problems with the connection to great network I see"))
          is HttpException -> Result.failure(ApiException(e.code(), "Some troubles :("))
          else -> Result.failure(UnknownException("idk what happened but I cannot help you now :("))
        }
      }
    }
  }
}
