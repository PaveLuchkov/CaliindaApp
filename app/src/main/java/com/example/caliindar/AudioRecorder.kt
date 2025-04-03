package com.example.caliindar

import android.media.MediaRecorder
import android.util.Log // Добавьте импорт Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val cacheDir: File) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val TAG = "AudioRecorder" // Добавьте TAG для логов

    // Ленивая инициализация стала не нужна с изменениями
    // private val recorder: MediaRecorder ...

    @Throws(IOException::class, IllegalStateException::class)
    fun startRecording() {
        // Создаем новый экземпляр MediaRecorder КАЖДЫЙ раз перед записью
        // т.к. после stop() или reset() его нужно настраивать заново
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.OGG)
            setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
        }

        outputFile = File.createTempFile("audio", ".ogg", cacheDir).also {
            Log.d(TAG, "Created temp file: ${it.absolutePath}")
            // Устанавливаем файл ПЕРЕД prepare()
            mediaRecorder?.setOutputFile(it.absolutePath)
        }

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            Log.i(TAG, "Recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Error during prepare/start", e)
            // Важно освободить ресурсы и удалить файл при ошибке старта
            outputFile?.delete()
            outputFile = null
            releaseMediaRecorder() // Очищаем mediaRecorder
            throw e // Пробрасываем исключение дальше
        }
    }


    fun stopRecording(): File? {
        // Проверяем, был ли mediaRecorder инициализирован и запись начата
        if (mediaRecorder == null) {
            Log.w(TAG, "stopRecording called but recorder is not initialized.")
            return null
        }

        return try {
            Log.i(TAG, "Attempting to stop recording...")
            mediaRecorder?.stop() // Останавливаем запись
            Log.i(TAG, "Recording stopped.")
            val recordedFile = outputFile // Сохраняем ссылку на файл
            outputFile = null // Сбрасываем ссылку на файл здесь, т.к. он больше не нужен рекордеру
            recordedFile // Возвращаем файл
        } catch (e: RuntimeException) {
            // Обработка возможных ошибок при остановке
            Log.e(TAG, "Error stopping recorder: ${e.message}", e)
            outputFile?.delete() // Удаляем файл, если остановка неудачна
            outputFile = null
            null // Возвращаем null в случае ошибки
        } finally {
            releaseMediaRecorder() // Освобождаем ресурсы ПОСЛЕ остановки
        }
    }

    // Метод для освобождения ресурсов MediaRecorder
    private fun releaseMediaRecorder() {
        if (mediaRecorder != null) {
            Log.d(TAG, "Releasing MediaRecorder...")
            try {
                // reset() может быть полезен, чтобы сбросить состояние перед release
                // но stop() уже должен был перевести рекордер в состояние Initial или Idle
                // mediaRecorder?.reset()
                mediaRecorder?.release() // Освобождаем ресурсы
                Log.d(TAG, "MediaRecorder released.")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaRecorder: ${e.message}", e)
            } finally {
                mediaRecorder = null // Обнуляем ссылку в любом случае
            }
        } else {
            Log.d(TAG, "releaseMediaRecorder called but recorder is already null.")
        }
    }

    // ---> ДОБАВЛЕННЫЙ МЕТОД <---
    fun cancelRecording() {
        Log.w(TAG, "Cancelling recording...")
        // Останавливаем и освобождаем рекордер, если он активен
        releaseMediaRecorder()
        // Пытаемся удалить временный файл, если он существует
        if (outputFile != null) {
            val deleted = outputFile?.delete()
            Log.w(TAG, "Deleting temp file ${outputFile?.name} on cancel: $deleted")
            outputFile = null // Сбрасываем ссылку на файл
        }
    }
}