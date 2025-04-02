package com.example.caliindar

import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val cacheDir: File) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording() {
        try {
            outputFile = File.createTempFile("audio", ".ogg", cacheDir)
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            releaseMediaRecorder()
            throw e
        }
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.stop()
            return outputFile
        } finally {
            releaseMediaRecorder()
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
