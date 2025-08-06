package com.lpavs.caliinda.core.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.services.calendar.CalendarScopes
import com.lpavs.caliinda.core.data.auth.AuthEvent
import com.lpavs.caliinda.core.data.di.BackendUrl
import com.lpavs.caliinda.core.data.di.WebClientId
import com.lpavs.caliinda.core.data.repository.CalendarRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @BackendUrl private val backendBaseUrl: String,
    @WebClientId private val webClientId: String,
    private val sharedPreferences: SharedPreferences
) {
  private val TAG = "AuthManager"

  private companion object {
    const val BACKEND_TOKEN_KEY = "backend_auth_token"
    const val USER_EMAIL_KEY = "user_email"
    const val USER_DISPLAY_NAME_KEY = "user_displayName"
    const val USER_PHOTO_URL_KEY = "user_photoUrl"
  }

  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _authState = MutableStateFlow(AuthState())
  val authState: StateFlow<AuthState> = _authState.asStateFlow()

  private val _authEvents = MutableSharedFlow<AuthEvent>()
  val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

  // --- Google Sign-In Клиент ---
  private val credentialManager: CredentialManager = CredentialManager.create(context)
  private val authorizationClient: AuthorizationClient = Identity.getAuthorizationClient(context)

  private var pendingIdToken: String? = null

  init {
    Log.d(TAG, "Initializing AuthManager with Credential Manager...")
    restoreStateFromStorage()
  }

  private fun restoreStateFromStorage() {
    try {
      Log.d(TAG, "Starting state restoration from EncryptedSharedPreferences...")
      Log.d(TAG, "Process ID: ${android.os.Process.myPid()}")

      val token = getBackendAuthToken()
      Log.d(
          TAG,
          "Token restoration result: ${if (token != null) "FOUND token" else "NO token found"}")

      if (token != null) {
        val email = sharedPreferences.getString(USER_EMAIL_KEY, null)
        val displayName = sharedPreferences.getString(USER_DISPLAY_NAME_KEY, null)
        val photoUrlString = sharedPreferences.getString(USER_PHOTO_URL_KEY, null)

        Log.d(TAG, "Restoring user data: email=$email, displayName=$displayName")

        val photoUri =
            if (!photoUrlString.isNullOrEmpty()) {
              photoUrlString.toUri()
            } else {
              null
            }

        _authState.value =
            AuthState(
                isSignedIn = true,
                userEmail = email,
                displayName = displayName,
                photoUrl = photoUri,
                isLoading = false,
                authError = null)
        Log.i(TAG, "✅ Successfully restored session for $email from EncryptedSharedPreferences")
      } else {
        Log.d(TAG, "❌ No token found, setting signed out state")
        _authState.value = AuthState(isLoading = false)
      }
    } catch (e: Exception) {
      Log.e(TAG, "❌ Exception during state restoration from EncryptedSharedPreferences", e)
      _authState.value = AuthState(isLoading = false, authError = "Failed to restore session")
    }
  }

  fun signIn(activity: Activity) {
    managerScope.launch {
      _authState.update { it.copy(isLoading = true, authError = null) }
      try {
        val googleIdOption = buildGoogleIdOption(filterByAuthorizedAccounts = false)
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val result = credentialManager.getCredential(activity, request)
        handleAuthenticationSuccess(result)
      } catch (e: GetCredentialException) {
        Log.w(TAG, "GetCredentialException: ${e.message}", e)
        _authState.update { it.copy(isLoading = false, authError = "Вход был отменен.") }
      } catch (e: Exception) {
        Log.e(TAG, "Unknown error during sign-in", e)
        _authState.update {
          it.copy(isLoading = false, authError = "Произошла неизвестная ошибка.")
        }
      }
    }
  }

  private suspend fun handleAuthenticationSuccess(result: GetCredentialResponse) {
    val credential = result.credential
    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      try {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleIdTokenCredential.idToken
        val userEmail = googleIdTokenCredential.id
        val displayName = googleIdTokenCredential.displayName
        val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
        saveUserInfo(userEmail, displayName, photoUrl)
        Log.i(TAG, "Authentication Success! Email: ${googleIdTokenCredential.id}")

        this.pendingIdToken = idToken
        _authState.update {
          it.copy(
              isLoading = true,
              userEmail = googleIdTokenCredential.id,
              displayName = googleIdTokenCredential.displayName,
              photoUrl = googleIdTokenCredential.profilePictureUri,
          )
        }
        requestCalendarAuthorization()
      } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Error parsing Google ID token", e)
        signOutInternally("Failed to process Google token")
      }
    } else {
      Log.w(TAG, "Received an unexpected credential type: ${credential.type}")
      signOutInternally("Unsupported credential type.")
    }
  }

  private suspend fun requestCalendarAuthorization() {
    val requiredScopes = Scope(CalendarScopes.CALENDAR)
    val authRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(requiredScopes))
            .requestOfflineAccess(webClientId)
            .build()
    try {
      val result = authorizationClient.authorize(authRequest).await()
      if (result.hasResolution()) {
        Log.d(TAG, "Authorization requires user consent.")
        _authState.update { it.copy(authorizationIntent = result.pendingIntent) }
      } else {
        Log.d(TAG, "Authorization succeeded without user consent.")
        val authCode = result.serverAuthCode
        if (authCode != null) {
          handleAuthorizationSuccess(authCode)
        } else {
          Log.e(TAG, "Authorization failed with no serverAuthCode.")
          signOutInternally("Authorization failed with no serverAuthCode.")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error during authorization", e)
      signOutInternally("Error during authorization")
    }
  }

  fun handleAuthorizationResult(intent: Intent) {
    managerScope.launch {
      val authorizationResult = authorizationClient.getAuthorizationResultFromIntent(intent)
      val authCode = authorizationResult.serverAuthCode
      if (authCode != null) {
        Log.i(TAG, "User granted permissions successfully.")
        handleAuthorizationSuccess(authCode)
      } else {
        Log.w(TAG, "User denied permissions or an error occurred.")
        signOutInternally("Calendar permission is required to continue.")
      }
      _authState.update { it.copy(authorizationIntent = null) }
    }
  }

  private suspend fun handleAuthorizationSuccess(authCode: String) {
    val idToken = pendingIdToken
    if (idToken == null) {
      Log.e(TAG, "No pending ID token found for authorization.")
      signOutInternally("No pending ID token found for authorization.")
      return
    }
    Log.d(TAG, "Sending authorization code to backend...")
    val exchangeSuccess = sendAuthInfoToBackend(idToken, authCode)

    if (exchangeSuccess) {
      Log.i(TAG, "Successfully exchanged tokens with backend.")
      _authState.update {
        it.copy(
            isSignedIn = true,
            isLoading = false,
            authError = null,
        )
      }
      pendingIdToken = null
    } else {
      Log.e(TAG, "Failed to exchange tokens with backend.")
      signOutInternally("Server could not verify your session.")
    }
  }

  fun signOut() {
    managerScope.launch {
      _authState.update { it.copy(isLoading = true) }
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        Log.i(TAG, "Successfully cleared credentials.")
      } catch (e: Exception) {
        Log.e(TAG, "Error clearing credentials", e)
      } finally {
        _authEvents.emit(AuthEvent.SignedOut)
        signOutInternally("You have been signed out.")
      }
    }
  }

  private fun signOutInternally(error: String?) {
    clearBackendToken()
    clearUserInfo()
    pendingIdToken = null
    _authState.value = AuthState(isSignedIn = false, isLoading = false, authError = error)
    Log.i(TAG, "Internal sign out completed. Error: $error")
  }

  private fun buildGoogleIdOption(
      filterByAuthorizedAccounts: Boolean,
      autoSelect: Boolean = false,
  ): GetGoogleIdOption {
    return GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(autoSelect)
        .setNonce(generateNonce())
        .build()
  }

  private fun generateNonce(): String {
    return UUID.randomUUID().toString() // В проде можно другой
  }

  fun clearAuthError() {
    _authState.update { it.copy(authError = null) }
  }

  fun clearAuthorizationIntent() {
    _authState.update { it.copy(authorizationIntent = null) }
  }

  @WorkerThread
  private suspend fun sendAuthInfoToBackend(idToken: String, authCode: String): Boolean =
      withContext(Dispatchers.IO) {
        Log.i(TAG, "Sending Auth Info (JSON) to /auth/google/exchange")
        val jsonObject =
            JSONObject().apply {
              put("id_token", idToken)
              put("auth_code", authCode)
            }
        val requestBody =
            jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request =
            Request.Builder().url("$backendBaseUrl/auth/google/exchange").post(requestBody).build()

        try {
          okHttpClient.newCall(request).execute().use { response ->
            val responseBodyString = response.body?.string()
            if (!response.isSuccessful) {
              Log.e(TAG, "Backend error exchanging code: ${response.code} - $responseBodyString")
              false
            } else {
              Log.i(TAG, "Backend successfully exchanged tokens. Response: $responseBodyString")
              // ----- НОВАЯ ЛОГИКА: ИЗВЛЕКАЕМ И СОХРАНЯЕМ ТОКЕН -----
              try {
                val jsonResponse = responseBodyString?.let { JSONObject(it) }
                val backendToken = jsonResponse?.optString("token", null.toString())
                if (backendToken != null) { // Доп. проверка на строку "null" на всякий случай
                  saveBackendToken(backendToken)
                  true // Успех
                } else {
                  Log.e(
                      TAG, "Backend response is successful, but 'token' field is missing or null.")
                  false
                }
              } catch (e: JSONException) {
                Log.e(TAG, "Failed to parse backend token response.", e)
                false
              }
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error processing backend response for auth exchange", e)
          false
        }
      }

  fun getBackendAuthToken(): String? {
    return try {
      Log.d(TAG, "Attempting to retrieve backend token with key: '$BACKEND_TOKEN_KEY'")

      val token = sharedPreferences.getString(BACKEND_TOKEN_KEY, null)

      if (token != null) {
        Log.d(TAG, "✅ Backend token FOUND in EncryptedSharedPreferences (length: ${token.length})")
        Log.d(TAG, "Token preview: ${token.take(20)}...")
      } else {
        Log.w(TAG, "❌ Backend token NOT FOUND in EncryptedSharedPreferences")

        // Дополнительная диагностика
        val hasKey = sharedPreferences.contains(BACKEND_TOKEN_KEY)
        Log.d(TAG, "Key '$BACKEND_TOKEN_KEY' exists in preferences: $hasKey")

        // Проверим все ключи
        val allKeys = sharedPreferences.all.keys
        Log.d(TAG, "All keys in EncryptedSharedPreferences: $allKeys")
      }

      token
    } catch (e: Exception) {
      Log.e(TAG, "❌ Exception while retrieving backend token from EncryptedSharedPreferences", e)
      null
    }
  }

  private fun saveBackendToken(token: String) {
    try {
      Log.d(TAG, "Attempting to save backend token with key: '$BACKEND_TOKEN_KEY'")

      val success = sharedPreferences.edit().putString(BACKEND_TOKEN_KEY, token).commit()

      if (success) {
        Log.d(TAG, "✅ Backend token saved successfully to EncryptedSharedPreferences")

        // Проверяем, что токен действительно сохранился
        val savedToken = sharedPreferences.getString(BACKEND_TOKEN_KEY, null)
        if (savedToken == token) {
          Log.d(TAG, "✅ Token verification successful - token matches")
        } else {
          Log.e(
              TAG,
              "❌ Token verification FAILED! Expected: ${token.take(20)}..., Got: ${savedToken?.take(20) ?: "null"}")
        }
      } else {
        Log.e(TAG, "❌ Failed to save backend token - commit() returned false")
      }
    } catch (e: Exception) {
      Log.e(TAG, "❌ Exception while saving backend token to EncryptedSharedPreferences", e)
    }
  }

  private fun clearBackendToken() {
    try {
      Log.d(TAG, "Clearing backend token with key: '$BACKEND_TOKEN_KEY'")
      val success = sharedPreferences.edit().remove(BACKEND_TOKEN_KEY).commit()

      if (success) {
        Log.d(TAG, "✅ Backend token cleared successfully")
      } else {
        Log.e(TAG, "❌ Failed to clear backend token")
      }
    } catch (e: Exception) {
      Log.e(TAG, "❌ Exception while clearing backend token", e)
    }
  }

  private fun saveUserInfo(email: String?, displayName: String?, photoUrl: String?) {
    try {
      Log.d(TAG, "Saving user info to EncryptedSharedPreferences...")

      val success =
          sharedPreferences
              .edit()
              .putString(USER_EMAIL_KEY, email)
              .putString(USER_DISPLAY_NAME_KEY, displayName)
              .putString(USER_PHOTO_URL_KEY, photoUrl)
              .commit()

      if (success) {
        Log.d(TAG, "✅ User info saved successfully")
      } else {
        Log.e(TAG, "❌ Failed to save user info")
      }
    } catch (e: Exception) {
      Log.e(TAG, "❌ Exception while saving user info to EncryptedSharedPreferences", e)
    }
  }

  private fun clearUserInfo() {
    try {
      val success =
          sharedPreferences
              .edit()
              .remove(USER_EMAIL_KEY)
              .remove(USER_DISPLAY_NAME_KEY)
              .remove(USER_PHOTO_URL_KEY)
              .commit()

      if (success) {
        Log.d(TAG, "✅ User info cleared successfully")
      } else {
        Log.e(TAG, "❌ Failed to clear user info")
      }
    } catch (e: Exception) {
      Log.e(TAG, "❌ Exception while clearing user info", e)
    }
  }
}


sealed class AuthEvent {
  object SignedOut : AuthEvent()
}