package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val DipMillis = 1000
private const val WaveSegments = 28
private const val WaveCycles = 1.5f

@Composable
fun DyeDipDemo() {
    val state = rememberSwatchState()
    var base by remember { mutableStateOf(state.color) }
    var dye by remember { mutableStateOf(state.color) }
    val level = remember { Animatable(0f) }
    val body = remember { Path() }
    val crest = remember { Path() }

    LaunchedEffect(state.selected) {
        if (!state.changed) return@LaunchedEffect
        if (level.value > 0f) base = dye
        dye = state.color
        level.snapTo(0f)
        level.animateTo(1f, tween(DipMillis, easing = FastOutSlowInEasing))
        base = dye
        level.snapTo(0f)
    }

    ColorPickerScene(
        state = state,
        number = 4,
        title = "Dye Dip",
        caption = "A ring grows around the swatch and the new colour rises up the tee from the hem like dye.",
        tee = { art ->
            val l = level.value
            drawTee(art, base)
            if (l > 0f) {
                val w = art.size.width
                val surface = art.bottom + (art.top - art.bottom) * l
                // The surface is calm at both ends of the dip and choppiest halfway up.
                val amplitude = w * 0.035f * sin(PI.toFloat() * l)
                val phase = l * 4f * PI.toFloat()
                body.reset()
                crest.reset()
                for (k in 0..WaveSegments) {
                    val x = w * k / WaveSegments
                    val y = surface + amplitude * sin(2f * PI.toFloat() * WaveCycles * k / WaveSegments + phase)
                    if (k == 0) {
                        body.moveTo(x, y)
                        crest.moveTo(x, y)
                    } else {
                        body.lineTo(x, y)
                        crest.lineTo(x, y)
                    }
                }
                body.lineTo(w, art.size.height)
                body.lineTo(0f, art.size.height)
                body.close()
                clipPath(body) { drawTee(art, dye) }
                clipPath(art.outline) {
                    drawPath(crest, lerp(dye, Color.White, 0.45f), alpha = 0.8f, style = Stroke(2.dp.toPx()))
                }
            }
        },
        swatch = { i, selected -> GrowingRingSwatch(i, selected) },
    )
}

@Composable
private fun BoxScope.GrowingRingSwatch(index: Int, selected: Boolean) {
    val grow = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (selected) {
            grow.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f))
        } else {
            grow.animateTo(0f, tween(180))
        }
    }
    SwatchDot(index)
    Canvas(Modifier.matchParentSize()) {
        val g = grow.value
        if (g <= 0f) return@Canvas
        val dotRadius = DotSize.toPx() / 2
        val radius = dotRadius + (RingRadius.toPx() - dotRadius) * g
        val a = g.coerceAtMost(1f)
        drawCircle(Color.White, radius, alpha = a, style = Stroke(RingStroke.toPx() * a))
    }
}
