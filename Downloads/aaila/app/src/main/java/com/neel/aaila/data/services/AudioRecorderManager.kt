package com.neel.aaila.data.services

import android.content.Context
import android.media.MediaRecorder
import android.os.Build

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    @Suppress("DEPRECATION")
    fun startRecording(outputFile: java.io.File): Boolean {
        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (_: Exception) {
            isRecording = false
            mediaRecorder = null
            false
        }
    }

    fun stopRecording(): Boolean {
        if (!isRecording) return false
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            true
        } catch (_: Exception) {
            mediaRecorder = null
            isRecording = false
            false
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
