package com.example.caliindar

// Стандартные импорты Android
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Импорты Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

// Импорты OkHttp (или вашей сетевой библиотеки)
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType // Добавьте этот импорт
import okhttp3.RequestBody.Companion.toRequestBody // Добавьте этот импорт

// Импорты JSON (если нужны для обработки ответа)
import org.json.JSONObject

// Импорт вашего класса AudioRecorder
import com.example.caliindar.AudioRecorder // Убедитесь, что путь правильный

// Импорт ваших тем (если используется Jetpack Compose Preview)
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.caliindar.ui.theme.CaliindarTheme // Убедитесь, что путь правильный


class MainActivity : AppCompatActivity() {

    // UI Элементы
    private lateinit var textView: TextView
    private lateinit var recordButton: Button
    private lateinit var signInButton: Button

    // Аудио
    private lateinit var audioRecorder: AudioRecorder
    private val REQUEST_RECORD_AUDIO_PERMISSION = 123

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var currentIdToken: String? = null // Храним ID токен для /process_audio

    // Константы
    private val TAG = "MainActivityAuth"
    // !!! ЗАМЕНИТЕ НА ВАШ WEB CLIENT ID С БЭКЕНДА !!!
    private val BACKEND_WEB_CLIENT_ID = "835523232919-o0ilepmg8ev25bu3ve78kdg0smuqp9i8.apps.googleusercontent.com"
    // !!! ЗАМЕНИТЕ НА АДРЕС ВАШЕГО БЭКЕНДА !!!
    private val BACKEND_BASE_URL = "http://172.23.35.166:8000" // Пример

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Убедитесь, что используете правильный layout

        // Инициализация UI
        textView = findViewById(R.id.resultText)
        recordButton = findViewById(R.id.recordButton)
        signInButton = findViewById(R.id.signInButton)

        // Инициализация Аудио
        audioRecorder = AudioRecorder(cacheDir) // Убедитесь, что класс AudioRecorder доступен

        // Инициализация Google Sign-In Client и Result Launcher
        configureGoogleSignIn()
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                // Обработка отмены входа пользователем или другой ошибки
                Log.w(TAG, "Sign-in flow cancelled or failed with resultCode: ${result.resultCode}")
                showError("Вход отменен или произошла ошибка")
            }
        }

        // Настройка слушателей кнопок
        setupClickListeners()

        // Проверка состояния аутентификации при запуске (опционально)
        checkAuthState()
    }

    // --- Google Sign-In Конфигурация и Флоу ---

    private fun configureGoogleSignIn() {
        Log.d(TAG, "Configuring Google Sign-In...")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Запрашиваем права на календарь
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar.events"))
            // Запрашиваем Auth Code для бэкенда (используем WEB client ID бэкенда)
            .requestServerAuthCode(BACKEND_WEB_CLIENT_ID)
            // Запрашиваем ID Token (также используем WEB client ID бэкенда)
            // Бэкенд будет его проверять для идентификации пользователя
            .requestIdToken(BACKEND_WEB_CLIENT_ID)
            // Запрашиваем email для отображения и идентификации
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "GoogleSignInClient configured.")
    }

    private fun startSignInFlow() {
        Log.i(TAG, "Starting Google Sign-In flow...")
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Вход успешен, получаем данные
            val idToken = account?.idToken
            val serverAuthCode = account?.serverAuthCode
            val userEmail = account?.email

            Log.i(TAG, "Sign-In Success! Email: $userEmail")
            Log.d(TAG, "ID Token received: ${idToken != null}")
            Log.d(TAG, "Server Auth Code received: ${serverAuthCode != null}")

            // Сохраняем ID токен для последующих запросов к /process_audio
            currentIdToken = idToken

            if (idToken != null && serverAuthCode != null) {
                // Отправляем ID Token и Auth Code на бэкенд для обмена
                sendAuthInfoToBackend(idToken, serverAuthCode)
                updateUI("Вход выполнен: $userEmail. Идет авторизация календаря...")
            } else {
                showError("Не удалось получить токен или код авторизации от Google.")
                Log.w(TAG, "ID Token or Server Auth Code is null after sign-in.")
                currentIdToken = null // Сбрасываем токен
            }

        } catch (e: ApiException) {
            // Ошибка входа
            Log.w(TAG, "signInResult:failed code=" + e.statusCode, e)
            showError("Ошибка входа Google: ${e.statusCode}")
            currentIdToken = null // Сбрасываем токен
        }
    }

    // Проверка состояния при запуске (адаптировано)
    private fun checkAuthState() {
        Log.d(TAG, "Checking auth state...")
        // Используем GoogleSignIn.getLastSignedInAccount для проверки предыдущего входа
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope("https://www.googleapis.com/auth/calendar.events"))) {
            // Пользователь уже вошел и дал разрешение
            currentIdToken = account.idToken // Получаем ID токен
            val userEmail = account.email
            Log.i(TAG, "User already signed in: $userEmail")
            updateUI("Аккаунт: $userEmail (Авторизован)")
            // Важно: Даже если пользователь вошел, ID токен может быть устаревшим.
            // Бэкенд должен уметь обрабатывать устаревший токен при вызове /process_audio.
            // Либо можно инициировать silentSignIn для обновления токена.
            // googleSignInClient.silentSignIn().addOnCompleteListener { task -> handleSignInResult(task) }
        } else {
            Log.i(TAG, "User not signed in or permissions missing.")
            updateUI("Требуется вход и авторизация")
            currentIdToken = null
        }
    }


    // --- Слушатели Кнопок ---

    private fun setupClickListeners() {
        recordButton.setOnClickListener {
            if (recordButton.text == "Записать") {
                checkPermissionsAndRecord()
            } else {
                stopRecordingAndSend()
            }
        }

        // Кнопка входа теперь запускает новый флоу
        signInButton.setOnClickListener {
            startSignInFlow()
        }

        // TODO: Добавить кнопку выхода (Sign Out)
        // signOutButton.setOnClickListener { signOut() }
    }

    // --- Логика Записи Аудио ---

    private fun checkPermissionsAndRecord() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            // Разрешение есть, начинаем запись
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                showError("Нет разрешения на запись аудио")
            }
        }
    }

    private fun startRecording() {
        if (currentIdToken == null) {
            showError("Сначала войдите в аккаунт Google")
            return
        }
        try {
            Log.i(TAG, "Starting audio recording...")
            audioRecorder.startRecording() // Убедитесь, что метод существует
            updateUI("Запись началась...", "Стоп")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            showError("Ошибка начала записи: ${e.message}")
        }
    }

    private fun stopRecordingAndSend() {
        lifecycleScope.launch { // Используем корутины для асинхронности
            try {
                Log.i(TAG, "Stopping audio recording...")
                val audioFile = audioRecorder.stopRecording() // Убедитесь, что метод существует
                if (audioFile != null) {
                    updateUI("Отправка аудио на сервер...", "Записать")
                    // Отправляем аудио и ТЕКУЩИЙ ID токен на эндпоинт /process_audio
                    currentIdToken?.let { token ->
                        sendAudioToBackend(audioFile, token)
                    } ?: showError("Ошибка: ID токен отсутствует. Попробуйте войти снова.")
                } else {
                    showError("Не удалось получить файл записи.")
                    updateUI("Ошибка записи", "Записать")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping/sending recording", e)
                showError("Ошибка остановки/отправки записи: ${e.message}")
                updateUI("Ошибка", "Записать") // Сбрасываем UI
            }
        }
    }

    // --- Сетевые Запросы ---

    // Отправка Auth Code и ID Token на бэкенд для обмена
    private fun sendAuthInfoToBackend(idToken: String, authCode: String) {
        Log.i(TAG, "Sending Auth Code and ID Token as JSON to backend endpoint: /auth/google/exchange")
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        // 1. Создаем JSON объект
        val jsonObject = JSONObject()
        jsonObject.put("id_token", idToken)
        jsonObject.put("auth_code", authCode)

        // 2. Создаем RequestBody из JSON строки
        val jsonBody = jsonObject.toString()
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()) // Указываем JSON media type

        // 3. Строим POST запрос с JSON телом
        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/auth/google/exchange") // Убедитесь, что URL правильный
            .post(requestBody) // Отправляем JSON RequestBody
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network error sending auth info (JSON)", e)
                runOnUiThread { showError("Сетевая ошибка обмена токенов: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                // Важно: response.body?.string() можно вызвать только один раз!
                val responseBodyString = response.body?.string()
                // Важно закрыть тело ответа, даже если оно пустое или прочитано
                response.body?.close()

                try { // Добавим try-catch для JSON парсинга ответа, если нужно
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Backend error exchanging code (JSON): ${response.code} - $responseBodyString")
                        runOnUiThread { showError("Ошибка бэкенда при обмене токенов: ${response.code}") }
                    } else {
                        Log.i(TAG, "Backend successfully exchanged tokens (JSON). Response: $responseBodyString")
                        runOnUiThread {
                            // Обработка успешного ответа (можно парсить JSON ответа, если он есть)
                            updateUI("Авторизация календаря успешна! Готово к созданию событий.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing backend response (JSON)", e)
                    runOnUiThread { showError("Ошибка обработки ответа сервера: ${e.message}") }
                }
            }
        })
    }

    // Отправка аудио и ID токена на бэкенд для обработки
    private fun sendAudioToBackend(audioFile: File, idToken: String) {
        Log.i(TAG, "Sending audio and ID token to backend endpoint: /process_audio")
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Увеличим таймаут для загрузки файла
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/ogg".toMediaTypeOrNull()) // Проверьте media type
            )
            // Теперь отправляем только ID токен для идентификации
            .addFormDataPart("id_token_str", idToken) // Имя поля должно совпадать с FastAPI
            .build()

        val request = Request.Builder()
            .url("$BACKEND_BASE_URL/process_audio") // Эндпоинт обработки аудио
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network error sending audio", e)
                runOnUiThread { showError("Сетевая ошибка отправки аудио: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() // Читаем тело ответа ОДИН РАЗ
                if (!response.isSuccessful) {
                    Log.e(TAG, "Server error processing audio: ${response.code} - $responseBody")
                    runOnUiThread { showError("Ошибка сервера при обработке аудио: ${response.code}") }
                } else {
                    Log.i(TAG, "Audio processed successfully by backend. Response: $responseBody")
                    runOnUiThread {
                        // Обрабатываем успешный ответ от /process_audio
                        handleProcessAudioResponse(responseBody)
                        // Можно вернуть кнопку записи в исходное состояние
                        // updateUI("Аудио обработано", "Записать") // Или показать детали события
                    }
                }
                response.body?.close() // Важно закрыть тело ответа
            }
        })
    }

    // --- Обработка Ответов и Обновление UI ---

    // Обработка ответа от /process_audio
    private fun handleProcessAudioResponse(responseBody: String?) {
        if (responseBody == null) {
            showError("Пустой ответ от сервера после обработки аудио.")
            return
        }
        try {
            val json = JSONObject(responseBody)
            if (json.optString("status") == "success") {
                // Попытаемся получить детали события, если они есть
                val event = json.optJSONObject("event")
                val eventLink = json.optString("event_link", null) // Ссылка на событие

                if (event != null) {
                    showEventDetails(event, eventLink)
                } else {
                    // Если деталей события нет, просто показываем успех
                    val recognizedText = json.optString("recognized_text", "")
                    updateUI("Аудио успешно обработано. Текст: '$recognizedText'", "Записать")
                }
            } else {
                // Если статус не success, показываем ошибку из detail
                val detail = json.optString("detail", "Неизвестная ошибка")
                showError("Ошибка обработки аудио: $detail")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing /process_audio response", e)
            showError("Ошибка обработки ответа сервера: ${e.message}")
        }
    }


    // Обновление UI (убедитесь, что элементы существуют в layout)
    private fun updateUI(message: String, buttonText: String = recordButton.text.toString()) {
        // Выполняем обновление UI в главном потоке
        runOnUiThread {
            textView.text = message
            recordButton.text = buttonText
        }
    }

    // Показ ошибок (Toast и TextView)
    private fun showError(message: String) {
        runOnUiThread {
            textView.text = "❌ $message" // Показываем ошибку в TextView
            Toast.makeText(this, message, Toast.LENGTH_LONG).show() // Показываем Toast
            // Можно сбросить состояние кнопки записи при ошибке
            if (recordButton.text != "Записать") {
                recordButton.text = "Записать"
            }
        }
    }

    // Показ деталей созданного события
    private fun showEventDetails(event: JSONObject, eventLink: String?) {
        val eventName = event.optString("event_name", "Без названия")
        val eventDate = event.optString("date", "Не указана")
        val eventTime = event.optString("time", "Не указано")

        var eventDetails = """
             Событие создано!
             Название: $eventName
             Дата: $eventDate
             Время: $eventTime
         """.trimIndent()

        if (eventLink != null) {
            eventDetails += "\nСсылка: $eventLink"
        }

        updateUI(eventDetails, "Записать") // Обновляем UI и сбрасываем кнопку
    }


    // --- Выход из аккаунта (TODO) ---
    /*
    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            Log.i(TAG, "User signed out.")
            currentIdToken = null
            updateUI("Выход выполнен. Войдите снова.")
            // TODO: Возможно, нужно вызвать эндпоинт на бэкенде для инвалидации сессии/токена
            // Например, googleSignInClient.revokeAccess() для отзыва разрешений
        }
    }
    */  // Add this closing */
}

// --- Jetpack Compose Preview (если используется) ---
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hi $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CaliindarTheme {
        Greeting("Android")
    }
}