package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Seconds per beat (130 BPM). */
private const val BeatSeconds = 60f / 130f

private val JitterFrequency = Random(7).let { r -> FloatArray(BarHeights.size) { 9f + r.nextFloat() * 8f } }
private val JitterPhase = Random(11).let { r -> FloatArray(BarHeights.size) { r.nextFloat() * 6.28f } }

@Composable
fun DancingWaveDemo() {
    val state = rememberPlayerState()
    val energy = remember { Animatable(0f) }
    var clock by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.playing) {
        val playing = state.playing
        val settle = launch {
            if (playing) {
                energy.animateTo(1f, tween(300))
            } else {
                energy.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 180f))
            }
        }
        var last = withFrameNanos { it }
        while (playing || settle.isActive) {
            withFrameNanos { now ->
                clock += (now - last) / 1e9f
                last = now
            }
        }
    }

    PlayerScene(
        state = state,
        number = 4,
        title = "Dancing Wave",
        caption = "While playing the waveform bars dance to the beat in a travelling wave, then settle back on pause.",
        barScale = { i ->
            val e = energy.value
            if (e == 0f) {
                1f
            } else {
                val t = clock
                val travel = sin(t * 7f - i * 0.55f)
                val jitter = sin(t * JitterFrequency[i] + JitterPhase[i])
                val beat = exp(-(t % BeatSeconds) / 0.12f)
                1f + e * (0.42f * travel + 0.18f * jitter + 0.3f * beat - 0.1f)
            }
        },
    ) {
        PlayButtonFace(state.playing)
    }
}
