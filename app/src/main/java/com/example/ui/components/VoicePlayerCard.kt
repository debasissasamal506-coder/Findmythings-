package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.VoicePlaybackState
import com.example.utils.VoicePlayerManager
import kotlin.math.sin

@Composable
fun VoicePlayerCard(
    audioUri: String,
    totalDurationSec: Int,
    title: String = "Voice Note Instructions",
    subtitle: String? = "Listen to audio instructions to find this item",
    onDelete: (() -> Unit)? = null,
    onReRecord: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by VoicePlayerManager.playbackState.collectAsState()
    val isCurrentAudio = playbackState.activeAudioUri == audioUri
    val isPlaying = isCurrentAudio && playbackState.isPlaying

    val durationText = if (isCurrentAudio && playbackState.durationMs > 0) {
        playbackState.formatDuration()
    } else {
        val min = totalDurationSec / 60
        val sec = totalDurationSec % 60
        String.format("%02d:%02d", min, sec)
    }

    val currentPositionText = if (isCurrentAudio) {
        playbackState.formatCurrentPosition()
    } else {
        "00:00"
    }

    val cardBg = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (isPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("voice_player_card"),
        shape = RoundedCornerShape(16.dp),
        color = cardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onReRecord != null) {
                        IconButton(
                            onClick = onReRecord,
                            modifier = Modifier.size(36.dp).testTag("voice_re_record_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-record voice note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp).testTag("voice_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete voice note",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Waveform / Audio Visualizer Row
            AnimatedAudioWaveform(
                isPlaying = isPlaying,
                progress = if (isCurrentAudio) playbackState.progress else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )

            // Scrubber Slider
            Slider(
                value = if (isCurrentAudio) playbackState.progress else 0f,
                onValueChange = { fraction ->
                    if (isCurrentAudio && playbackState.durationMs > 0) {
                        val seekMs = (fraction * playbackState.durationMs).toInt()
                        VoicePlayerManager.seekTo(seekMs)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("voice_player_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Playback Controls Row: Play/Pause Button, Current Time, Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = {
                        VoicePlayerManager.togglePlay(context, audioUri)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("voice_play_pause_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isPlaying) "Pause" else "Play Recording",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    text = "$currentPositionText / $durationText",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AnimatedAudioWaveform(
    isPlaying: Boolean,
    progress: Float,
    barCount: Int = 32,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeBarIndex = (progress * barCount).toInt()

        for (i in 0 until barCount) {
            val baseHeight = 0.25f + 0.55f * ((sin(i * 0.45f) + 1f) / 2f)
            val dynamicHeight = if (isPlaying) {
                val wave = (sin(phase + i * 0.5f) + 1f) / 2f
                (baseHeight * 0.4f + wave * 0.6f).coerceIn(0.15f, 1f)
            } else {
                baseHeight.coerceIn(0.15f, 1f)
            }

            val isBarPassed = i <= activeBarIndex
            val barColor = when {
                isBarPassed -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .height((32 * dynamicHeight).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun VoiceQuickPlayButton(
    audioUri: String,
    durationSec: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by VoicePlayerManager.playbackState.collectAsState()
    val isCurrentAudio = playbackState.activeAudioUri == audioUri
    val isPlaying = isCurrentAudio && playbackState.isPlaying

    val durationStr = if (durationSec > 0) {
        val m = durationSec / 60
        val s = durationSec % 60
        String.format("%d:%02d", m, s)
    } else ""

    val pillColor = if (isPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }

    val contentColor = if (isPlaying) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        onClick = {
            VoicePlayerManager.togglePlay(context, audioUri)
        },
        shape = RoundedCornerShape(10.dp),
        color = pillColor,
        modifier = modifier.testTag("voice_quick_play_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.VolumeUp,
                contentDescription = if (isPlaying) "Pause audio" else "Listen to voice instructions",
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isPlaying) "Playing..." else if (durationStr.isNotEmpty()) "Voice ($durationStr)" else "Voice Note",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
