package com.example.caliindar.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.caliindar.di.BackendUrl
import com.example.caliindar.di.WebClientId
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
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
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @BackendUrl private val backendBaseUrl: String,
    @WebClientId private val webClientId: String
) {
    private val TAG = "AuthManager"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // --- Google Sign-In Клиент ---
    private val gso: GoogleSignInOptions
    val googleSignInClient: GoogleSignInClient

    private var currentIdToken: String? = null

    init {
        Log.d(TAG, "Initializing AuthManager...")
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
            .requestServerAuthCode(webClientId)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Запускаем начальную проверку состояния при инициализации
        checkInitialAuthState()
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        managerScope.launch { // Используем scope менеджера
            try {
                val account = completedTask.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val serverAuthCode = account?.serverAuthCode
                val userEmail = account?.email

                Log.i(TAG, "Sign-In Success! Email: $userEmail")
                Log.d(TAG, "ID Token received: ${idToken != null}")
                Log.d(TAG, "Server Auth Code received: ${serverAuthCode != null}")

                if (idToken != null && serverAuthCode != null && userEmail != null) {
                    currentIdToken = idToken // Сохраняем токен
                    _authState.update {
                        it.copy(
                            isLoading = true, // Начинаем обмен с бэкендом
                            authError = null,
                            userEmail = userEmail // Можно показать email сразу
                        )
                    }

                    // Отправляем данные на бэкенд
                    val exchangeSuccess = sendAuthInfoToBackend(idToken, serverAuthCode)

                    if (exchangeSuccess) {
                        _authState.update {
                            it.copy(
                                isSignedIn = true,
                                userEmail = userEmail,
                                isLoading = false,
                                authError = null
                            )
                        }
                        Log.i(TAG, "Auth successful for $userEmail")
                        // Тут можно уведомить другие части системы через Flow/Callback, если нужно
                    } else {
                        // Ошибка обмена, сбрасываем состояние
                        signOutInternally("Ошибка авторизации на сервере.")
                    }
                } else {
                    Log.w(TAG, "ID Token, Server Auth Code or Email is null after sign-in.")
                    signOutInternally("Не удалось получить данные от Google.")
                }

            } catch (e: ApiException) {
                Log.w(TAG, "signInResult:failed code=" + e.statusCode, e)
                signOutInternally("Ошибка входа Google: ${e.statusCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sign in result", e)
                signOutInternally("Неизвестная ошибка при входе: ${e.message}")
            }
        }
    }
    suspend fun getFreshIdToken(): String? = withContext(Dispatchers.IO)
    {
        if (!_authState.value.isSignedIn) {
            Log.w(TAG, "Cannot get fresh token: User not signed in.")
            return@withContext null
        }
        Log.d(TAG, "Attempting to get fresh ID token via silent sign-in...")
        try {
            val account = googleSignInClient.silentSignIn().await()
            val freshToken = account?.idToken
            if (freshToken != null) {
                Log.i(TAG, "Silent sign-in successful, got fresh token for: ${account.email}")
                if (currentIdToken != freshToken) {
                    Log.d(TAG, "ID Token was refreshed.")
                    currentIdToken = freshToken // Обновляем сохраненный токен
                }
                // Убедимся, что состояние актуально
                if (!_authState.value.isSignedIn || _authState.value.userEmail != account.email) {
                    _authState.update {
                        it.copy(
                            isSignedIn = true,
                            userEmail = account.email,
                            isLoading = false,
                            authError = null
                        )
                    }
                }
                freshToken
            } else {
                Log.w(TAG, "Silent sign-in successful but ID token is null.")
                // Не делаем signOutInternally здесь сразу, т.к. это может быть временная проблема
                // Вызывающий код должен обработать null
                _authState.update { it.copy(authError = "Не удалось обновить токен.") }
                null
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Silent sign-in failed: ${e.statusCode}", e)
            // SIGN_IN_REQUIRED - точно разлогиниваем
            if (e.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                signOutInternally("Сессия истекла или отозвана. Требуется вход.")
            } else {
                // Другие ошибки могут быть временными
                _authState.update { it.copy(authError = "Ошибка обновления сессии (${e.statusCode}).") }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in failed with generic exception", e)
            _authState.update { it.copy(authError = "Ошибка обновления сессии: ${e.message}") }
            null
        }
    }

    fun signOut() {
        managerScope.launch {
            _authState.update { it.copy(isLoading = true) } // Показываем процесс выхода
            try {
                googleSignInClient.signOut().await() // Ждем завершения выхода из Google
                Log.i(TAG, "User signed out successfully from Google.")
                // Опционально: Уведомить бэкенд о выходе (отдельный запрос)
                // revokeAccess() - если нужно отозвать доступ полностью

            } catch (e: Exception) {
                Log.e(TAG, "Error during Google sign out", e)
                // Все равно выполняем внутренний выход
            } finally {
                signOutInternally("Вы успешно вышли.") // Всегда сбрасываем состояние
            }
        }
    }

    fun clearAuthError() {
        _authState.update { it.copy(authError = null) }
    }

    private fun checkInitialAuthState() {
        managerScope.launch {
            _authState.update { it.copy(isLoading = true) }
            Log.d(TAG, "Checking initial auth state via silent sign-in...")
            try {
                val account = googleSignInClient.silentSignIn().await()
                val idToken = account?.idToken
                val userEmail = account?.email

                if (idToken != null && userEmail != null) {
                    Log.i(TAG, "Silent sign-in success on init for: $userEmail")
                    currentIdToken = idToken
                    _authState.update {
                        AuthState( // Полностью перезаписываем состояние
                            isSignedIn = true,
                            userEmail = userEmail,
                            isLoading = false,
                            authError = null
                        )
                    }
                    // НЕ НУЖНО снова вызывать sendAuthInfoToBackend здесь
                } else {
                    Log.i(TAG, "Silent sign-in did not return a valid account/token on init.")
                    signOutInternally(null) // Просто сбрасываем в неавторизованное состояние без ошибки
                }
            } catch (e: ApiException) {
                // Не показываем ошибку при старте, если просто не вошел
                if (e.statusCode != com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                    Log.w(TAG, "Silent sign-in failed on init: ${e.statusCode}", e)
                } else {
                    Log.d(TAG, "Silent sign-in failed on init: SIGN_IN_REQUIRED")
                }
                signOutInternally(null) // Считаем не вошедшим
            } catch (e: Exception) {
                Log.e(TAG, "Error checking initial auth state", e)
                signOutInternally("Ошибка проверки аккаунта: ${e.message}") // Считаем не вошедшим
            } finally {
                // Убедимся что isLoading сброшен в любом случае
                if (_authState.value.isLoading) {
                    _authState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    private fun signOutInternally(error: String?) {
        currentIdToken = null
        _authState.update {
            AuthState( // Полностью сбрасываем состояние
                isSignedIn = false,
                userEmail = null,
                authError = error,
                isLoading = false // Убедимся, что загрузка выключена
            )
        }
        Log.i(TAG, "Internal sign out completed. Error: $error")
    }
    @WorkerThread // Явно указываем, что метод блокирующий и должен быть в IO
    private suspend fun sendAuthInfoToBackend(idToken: String, authCode: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Sending Auth Info (JSON) to /auth/google/exchange")
        val jsonObject = JSONObject().apply {
            put("id_token", idToken)
            put("auth_code", authCode)
        }
        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$backendBaseUrl/auth/google/exchange")
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Backend error exchanging code (JSON): ${response.code} - $responseBodyString")
                    // Ошибка будет установлена в signOutInternally ниже
                    false // Неудача
                } else {
                    Log.i(TAG, "Backend successfully exchanged tokens (JSON). Response: $responseBodyString")
                    true // Успех
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending auth info (JSON)", e)
            false // Неудача
        } catch (e: Exception) {
            Log.e(TAG, "Error processing backend response for auth exchange", e)
            false // Неудача
        }
        // Результат (true/false) будет использован в handleSignInResult
    }
}