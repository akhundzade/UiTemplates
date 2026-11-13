package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val GhostCount = 4

@Composable
fun BlurSlideDemo() {
    CounterScene(
        number = 3,
        title = "Blur Slide",
        caption = "The number slides down with motion blur while the new one slides in from the top.",
    ) { value ->
        BlurSlideNumber(value)
    }
}

@Composable
private fun BlurSlideNumber(value: Int) {
    var outgoing by remember { mutableStateOf(formatSteps(value)) }
    var incoming by remember { mutableStateOf(formatSteps(value)) }
    val slide = remember { Animatable(1f) }

    LaunchedEffect(value) {
        val next = formatSteps(value)
        if (next == incoming) return@LaunchedEffect
        // Interrupted mid-slide: whichever number is more visible is the one that leaves.
        if (slide.value >= 0.5f) outgoing = incoming
        incoming = next
        slide.snapTo(0f)
        slide.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }

    Box(Modifier.clipToBounds()) {
        Text(incoming, style = NumberStyle, modifier = Modifier.alpha(0f))
        MotionText(outgoing, entering = false, progress = { slide.value })
        MotionText(incoming, entering = true, progress = { slide.value })
    }
}

/**
 * Text moving down by one line height, with a trail of fading copies above it. The trail and
 * the blur follow sin(πp), which peaks with the eased slide's speed and is zero at rest.
 */
@Composable
private fun MotionText(text: String, entering: Boolean, progress: () -> Float) {
    Box(
        Modifier.graphicsLayer {
            val p = progress()
            translationY = if (entering) -(1f - p) * size.height else p * size.height
            alpha = if (entering) p else 1f - p
        },
    ) {
        for (k in GhostCount downTo 1) {
            Text(
                text,
                style = NumberStyle,
                modifier = Modifier.graphicsLayer {
                    val speed = sin(PI.toFloat() * progress())
                    translationY = -k * speed * 7.dp.toPx()
                    alpha = speed * 0.4f / k
                },
            )
        }
        Text(
            text,
            style = NumberStyle,
            modifier = Modifier.graphicsLayer {
                val blur = sin(PI.toFloat() * progress()) * 6.dp.toPx()
                // Android 11 and older ignore this; the ghost trail carries the effect there.
                renderEffect = if (blur >= 1f) BlurEffect(0.5f, blur, TileMode.Decal) else null
            },
        )
    }
}
