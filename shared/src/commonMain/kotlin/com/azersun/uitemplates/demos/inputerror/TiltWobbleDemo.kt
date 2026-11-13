package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val WobbleSeconds = 1.2f
private const val DecaySeconds = 0.3f
private const val TiltHz = 3.2f

// A frequency unrelated to the tilt, so the squash never lines up with it and reads as jelly.
private const val JellyHz = 4.8f

// Scaled so the first swing peaks near 4° and the first squash near 3%.
private const val TiltDegrees = 5f
private const val JellyScale = 0.036f

@Composable
fun TiltWobbleDemo() {
    val form = remember { EmailFormState() }
    val clock = remember { Animatable(WobbleSeconds) }
    LaunchedEffect(form.errorTick) {
        if (form.errorTick == 0) return@LaunchedEffect
        clock.snapTo(0f)
        clock.animateTo(WobbleSeconds, tween((WobbleSeconds * 1000).toInt(), easing = LinearEasing))
    }

    InputErrorStage(6, "Tilt Wobble", "The whole field rocks side to side like jelly, then settles.", form) {
        EmailField(
            form,
            Modifier.graphicsLayer {
                val t = clock.value
                // Closed form of an underdamped spring, tapered to exactly zero at the end.
                val envelope = exp(-t / DecaySeconds) * (1f - t / WobbleSeconds)
                rotationZ = TiltDegrees * envelope * sin(2f * PI.toFloat() * TiltHz * t)
                val jelly = JellyScale * envelope * sin(2f * PI.toFloat() * JellyHz * t)
                scaleX = 1f + jelly
                scaleY = 1f - jelly
            },
        )
    }
}
