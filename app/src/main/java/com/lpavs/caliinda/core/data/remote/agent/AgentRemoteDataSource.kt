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

class AgentRemoteDataSource @Inject constructor(
    private val apiService: AgentApiService,
    private val authManager: AuthManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val TAG = "AgentRemoteDataSource"

    suspend fun runChat(message: String, context: UserContext): Result<Unit> {
        val token = authManager.getBackendAuthToken()
        if (token == null) {
            Log.e(TAG, "Could not get fresh token")
            return Result.failure(Exception("Authorization needed"))
        }

        return safeApiCall {
            apiService.run(
                token = "Bearer $token",
                request = ChatRequest(message, context)
            )
        }
    }

    private suspend fun safeApiCall(apiCall: suspend () -> Response<Unit>): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val response = apiCall()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "API call failed with code ${response.code()}: $errorBody")
                    Result.failure(ApiException(response.code(), "Server error: ${response.code()}. $errorBody"))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "API call exception: ${e.javaClass.simpleName}", e)
                when (e) {
                    is IOException -> Result.failure(NetworkException("Network issue: ${e.message}"))
                    is HttpException -> Result.failure(ApiException(e.code(), "Http error: ${e.message()}"))
                    else -> Result.failure(UnknownException("Unknown error: ${e.message}"))
                }
            }
        }
    }
}