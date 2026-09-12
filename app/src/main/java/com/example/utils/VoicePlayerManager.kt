package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class VoicePlaybackState(
    val activeAudioUri: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    fun formatCurrentPosition(): String {
        val totalSec = currentPositionMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    fun formatDuration(): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}

object VoicePlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    fun togglePlay(context: Context, audioUri: String) {
        val current = _playbackState.value
        if (current.activeAudioUri == audioUri) {
            if (current.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            play(context, audioUri)
        }
    }

    fun play(context: Context, audioUri: String) {
        stop()

        try {
            val player = MediaPlayer()
            val uri = Uri.parse(audioUri)
            if (audioUri.startsWith("/")) {
                player.setDataSource(audioUri)
            } else {
                player.setDataSource(context, uri)
            }

            player.setOnPreparedListener { mp ->
                val duration = mp.duration
                _playbackState.value = VoicePlaybackState(
                    activeAudioUri = audioUri,
                    isPlaying = true,
                    currentPositionMs = 0,
                    durationMs = duration
                )
                mp.start()
                startTicker()
            }

            player.setOnCompletionListener {
                stopTicker()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = 0
                )
            }

            player.setOnErrorListener { _, _, _ ->
                stopTicker()
                _playbackState.value = VoicePlaybackState(error = "Error playing audio file")
                release()
                true
            }

            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = VoicePlaybackState(error = e.localizedMessage ?: "Failed to play audio")
            release()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
            stopTicker()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startTicker()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        stopTicker()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
        }
        _playbackState.value = VoicePlaybackState()
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive) {
                delay(150)
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = it.currentPosition
                        )
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun release() {
        stopTicker()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }
}
