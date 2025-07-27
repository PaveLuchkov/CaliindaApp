package com.lpavs.caliinda.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.services.calendar.CalendarScopes
import com.lpavs.caliinda.data.calendar.CalendarDataManager
import com.lpavs.caliinda.di.BackendUrl
import com.lpavs.caliinda.di.WebClientId
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit


@Singleton
class AuthManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @BackendUrl private val backendBaseUrl: String,
    @WebClientId private val webClientId: String,
    private val calendarDataManager: Lazy<CalendarDataManager>
) {
  private val TAG = "AuthManagerV2"

  private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _authState = MutableStateFlow(AuthState())
  val authState: StateFlow<AuthState> = _authState.asStateFlow()

  // --- Google Sign-In Клиент ---
  private val credentialManager: CredentialManager = CredentialManager.create(context)
  private val authorizationClient: AuthorizationClient = Identity.getAuthorizationClient(context)

  private var pendingIdToken: String? = null

  init {
    Log.d(TAG, "Initializing AuthManager with Credential Manager...")
    checkInitialAuthState()
  }

  fun signIn(activity: Activity) {
    managerScope.launch {
      _authState.update { it.copy(isLoading = true, authError = null) }
      try {
        val googleIdOption = buildGoogleIdOption(filterByAuthorizedAccounts = true)
        val request = GetCredentialRequest.Builder()
          .addCredentialOption(googleIdOption)
          .build()
        val result = credentialManager.getCredential(activity, request)
        handleAuthenticationSuccess(result)
      } catch (e: GetCredentialException) {
        Log.w(TAG, "GetCredentialException: ${e.message}", e)
        _authState.update { it.copy(isLoading = false, authError = "Вход был отменен.") }
      } catch (e: Exception) {
        Log.e(TAG, "Unknown error during sign-in", e)
        _authState.update {
          it.copy(
            isLoading = false,
            authError = "Произошла неизвестная ошибка."
          )
        }
      }
    }
  }

  private suspend fun handleAuthenticationSuccess(result: GetCredentialResponse) {
    val credential = result.credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      try {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleIdTokenCredential.idToken
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
    val authRequest = AuthorizationRequest.builder()
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
        calendarDataManager.get().clearLocalDataOnSignOut()
      } catch (e: Exception) {
        Log.e(TAG, "Error clearing credentials", e)
      } finally {
        signOutInternally("You have been signed out.")
      }
    }
  }

  private fun checkInitialAuthState() {
    managerScope.launch {
      _authState.update { it.copy(isLoading = true) }
      val googleIdOption = buildGoogleIdOption(filterByAuthorizedAccounts = true, autoSelect = true)
      val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
      try {
        val result = credentialManager.getCredential(context, request)
        Log.i(TAG, "Silent sign-in successful on init.")
        handleAuthenticationSuccess(result)
      } catch (e: GetCredentialException) {
        Log.w(TAG, "Silent sign-in failed on init: ${e.message}", e)
        _authState.update { it.copy(isLoading = false) }
        signOutInternally(null)
      }
    }
  }

  private fun signOutInternally(error: String?) {
    clearBackendToken()
    pendingIdToken = null
    _authState.update {
      it.copy(
        isSignedIn = false,
        isLoading = false,
        authError = error
      )
    }
    Log.i(TAG, "Internal sign out completed. Error: $error")
  }

  private fun buildGoogleIdOption(
    filterByAuthorizedAccounts: Boolean,
    autoSelect: Boolean = false
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

  @Suppress("DEPRECATION")
  private val sharedPreferences: SharedPreferences by lazy {
    val masterKey = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

    EncryptedSharedPreferences.create(
      context,
      "secret_shared_prefs",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  private val BACKEND_TOKEN_KEY = "backend_auth_token"

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
              val jsonResponse = JSONObject(responseBodyString)
              val backendToken = jsonResponse.optString("token", null.toString())
              run {
                saveBackendToken(backendToken)
                true // Успех
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
    val token = sharedPreferences.getString(BACKEND_TOKEN_KEY, null)
    if (token == null) {
      Log.w(TAG, "Backend token not found in storage.")
    }
    return token
  }
  private fun saveBackendToken(token: String) {
    sharedPreferences.edit { putString(BACKEND_TOKEN_KEY, token) }
    Log.d(TAG, "Backend token saved securely.")
  }
  private fun clearBackendToken() {
    sharedPreferences.edit { remove(BACKEND_TOKEN_KEY) }
    Log.d(TAG, "Backend token cleared.")
  }
}
