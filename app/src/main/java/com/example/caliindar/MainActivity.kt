package com.example.caliindar

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.caliindar.ui.theme.CaliindarTheme
import android.media.MediaRecorder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import android.Manifest
import android.R.attr.level
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var textView: TextView
    private lateinit var recordButton: Button
    private lateinit var signInButton: Button
    private val REQUEST_RECORD_AUDIO_PERMISSION = 123
    private lateinit var googleAuthHelper: GoogleAuthHelper
    private var googleToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов
        textView = findViewById(R.id.resultText)
        recordButton = findViewById(R.id.recordButton)
        signInButton = findViewById(R.id.signInButton)
        audioRecorder = AudioRecorder(cacheDir)
        googleAuthHelper = GoogleAuthHelper(this)

        setupClickListeners()
        checkAuthState()
    }

    private fun setupClickListeners() {
        recordButton.setOnClickListener {
            if (recordButton.text == "Записать") {
                checkPermissionsAndRecord()
            } else {
                stopRecordingAndSend()
            }
        }

        signInButton.setOnClickListener {
            lifecycleScope.launch {
                val result = googleAuthHelper.signIn()
                if (result.isSuccess) {
                    val credential = result.getOrNull()
                    googleToken = credential?.idToken
                    updateUI("Вход выполнен: ${credential?.id}")
                } else {
                    val exception = result.exceptionOrNull()
                    showError("Ошибка входа: ${exception?.message}")
                }
            }
        }
    }

    private fun checkAuthState() {
        lifecycleScope.launch {
            try {
                val account = googleAuthHelper.getLastSignedInAccount()
                account?.let {
                    googleToken = it.idToken
                    updateUI("Добро пожаловать, ${it.id}")
                }
            } catch (e: Exception) {
                Log.e("Auth", "Error checking auth state", e)
            }
        }
    }

    private fun checkPermissionsAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
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
        try {
            audioRecorder.startRecording()
            updateUI("Запись началась...", "Стоп")  // Меняем на "Стоп"
        } catch (e: Exception) {
            showError("Ошибка записи: ${e.message}")
        }
    }

    private fun stopRecordingAndSend() {
        lifecycleScope.launch {
            try {
                val audioFile = audioRecorder.stopRecording()
                updateUI("Отправка на сервер...", "Записать")

                googleToken?.let { token ->
                    audioFile?.let { sendAudioToBackend(it, token) }
                } ?: showError("Требуется вход в аккаунт")

            } catch (e: Exception) {
                showError("Ошибка остановки: ${e.message}")
            }
        }
    }

    private fun sendAudioToBackend(audioFile: File, token: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Log request/response
            })
            .build()

        // 1. Create multipart request
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/ogg".toMediaTypeOrNull()) // Verify media type matches backend
            )
            .addFormDataPart("google_token", token) // Ensure field name matches backend
            .build()

        // 2. Send request
        val request = Request.Builder()
            .url("http://172.23.35.166:8000/process_audio") // Confirm URL/port
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showError("Network error: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        showError("Server error: ${response.code} - ${response.body?.string()}")
                    }
                } else {
                    runOnUiThread {
                        updateUI("Upload successful!", "Записать")
                    }
                }
            }
        })
    }

    private fun handleBackendResponse(response: String) {
        try {
            val json = JSONObject(response)
            when (json.getString("status")) {
                "success" -> showEventDetails(json.getJSONObject("event"))
                else -> showError(json.getString("detail"))
            }
        } catch (e: Exception) {
            showError("Ошибка обработки ответа")
        }
    }

    private fun updateUI(message: String, buttonText: String = recordButton.text.toString()) {
        runOnUiThread {
            textView.text = message
            recordButton.text = buttonText
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            textView.text = "❌ $message"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showEventDetails(event: JSONObject) {
        val eventDetails = """
            Событие создано!
            Название: ${event.getString("event_name")}
            Дата: ${event.getString("date")}
            Время: ${event.getString("time")}
        """.trimIndent()
        updateUI(eventDetails)
    }
}

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