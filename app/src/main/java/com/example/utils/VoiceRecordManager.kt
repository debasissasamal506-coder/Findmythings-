package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class VoiceRecordState(
    val isRecording: Boolean = false,
    val durationSec: Int = 0,
    val currentAmplitudeFraction: Float = 0f,
    val error: String? = null
) {
    fun formatDuration(): String {
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

data class VoiceRecordResult(
    val fileUri: String,
    val durationSec: Int
)

class VoiceRecordManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var tickerJob: Job? = null
    private var startTimeMs: Long = 0L

    private val _recordState = MutableStateFlow(VoiceRecordState())
    val recordState: StateFlow<VoiceRecordState> = _recordState.asStateFlow()

    fun startRecording(context: Context): Boolean {
        try {
            stopRecordingInternal(discard = true)

            val voiceDir = File(context.filesDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            _recordState.value = VoiceRecordState(isRecording = true, durationSec = 0)

            tickerJob = scope.launch {
                while (isActive && _recordState.value.isRecording) {
                    delay(100)
                    val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val ampFraction = (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
                    _recordState.value = _recordState.value.copy(
                        durationSec = elapsedSec,
                        currentAmplitudeFraction = ampFraction
                    )
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _recordState.value = VoiceRecordState(error = e.localizedMessage ?: "Failed to start recording")
            cleanup()
            return false
        }
    }

    fun stopRecording(): VoiceRecordResult? {
        val file = currentOutputFile
        val duration = _recordState.value.durationSec.coerceAtLeast(1)
        val success = stopRecordingInternal(discard = false)
        return if (success && file != null && file.exists() && file.length() > 0) {
            VoiceRecordResult(
                fileUri = Uri.fromFile(file).toString(),
                durationSec = duration
            )
        } else {
            null
        }
    }

    fun cancelRecording() {
        stopRecordingInternal(discard = true)
    }

    private fun stopRecordingInternal(discard: Boolean): Boolean {
        tickerJob?.cancel()
        tickerJob = null

        var clean = true
        try {
            mediaRecorder?.let {
                it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            clean = false
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        if (discard || !clean) {
            currentOutputFile?.let {
                if (it.exists()) it.delete()
            }
            currentOutputFile = null
        }

        _recordState.value = VoiceRecordState(isRecording = false)
        return clean
    }

    private fun cleanup() {
        tickerJob?.cancel()
        tickerJob = null
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaRecorder = null
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
    }

    companion object {
        fun deleteVoiceFile(uriString: String?) {
            if (uriString.isNullOrBlank()) return
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: return)
                    if (file.exists()) {
                        file.delete()
                    }
                } else {
                    val file = File(uriString)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
