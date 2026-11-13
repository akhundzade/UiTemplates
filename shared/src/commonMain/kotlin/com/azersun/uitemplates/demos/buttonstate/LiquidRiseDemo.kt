package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val RiseMillis = 1000
private val WaveHeight = 5.dp

@Composable
fun LiquidRiseDemo() {
    var base by remember { mutableStateOf(StartState) }
    var liquid by remember { mutableStateOf<PlayState?>(null) }
    var label by remember { mutableStateOf(StartState) }
    val level = remember { Animatable(0f) }
    val front = remember { Path() }
    val back = remember { Path() }

    ButtonStateScene(
        number = 1,
        title = "Liquid Rise",
        caption = "The next state's colour rises inside the pill as a wavy liquid; the label swaps at half full.",
        onToggle = { _, to, _ ->
            liquid = to
            level.snapTo(0f)
            level.animateTo(1f, tween(RiseMillis, easing = FastOutSlowInEasing)) {
                if (value >= 0.5f) label = to
            }
            base = to
            liquid = null
        },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clip(Pill)
                .drawBehind {
                    val t = level.value
                    drawRect(base.brush)
                    val fill = liquid ?: return@drawBehind
                    // The surface is calm at the ends and choppiest half way, like a poured fill.
                    val amplitude = WaveHeight.toPx() * sin(PI * t).toFloat()
                    val surface = size.height + amplitude - (size.height + 2 * amplitude) * t
                    val phase = t * 4 * PI.toFloat()
                    wave(back, surface - 3.dp.toPx(), amplitude, phase + PI.toFloat())
                    drawPath(back, fill.brush, alpha = 0.45f)
                    wave(front, surface, amplitude, phase)
                    drawPath(front, fill.brush)
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    (slideInVertically(tween(320)) { it } + fadeIn(tween(260))) togetherWith
                        (slideOutVertically(tween(260)) { -it } + fadeOut(tween(200)))
                },
                label = "label",
            ) { StateLabel(it) }
        }
    }
}

/** Rebuilds [path] as the liquid below a sine surface at [surface] (y, in px). */
private fun DrawScope.wave(path: Path, surface: Float, amplitude: Float, phase: Float) {
    val length = size.width / 1.4f
    val step = 6.dp.toPx()
    path.reset()
    path.moveTo(0f, surface + amplitude * sin(phase))
    var x = step
    while (x < size.width + step) {
        path.lineTo(x, surface + amplitude * sin(phase + x / length * 2 * PI.toFloat()))
        x += step
    }
    path.lineTo(size.width, size.height)
    path.lineTo(0f, size.height)
    path.close()
}
