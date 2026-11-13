package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

private const val SpinMillis = 950
private const val DashCount = 12
private const val DriftMillis = 9000

/** Inset of the tee box inside the panel, matching the scene's padding. */
private val TeeInset = 10.dp

@Composable
fun TurntableSpinDemo() {
    val state = rememberSwatchState()
    val spin = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(state.color) }
    val drift = rememberInfiniteTransition(label = "dash drift").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(DriftMillis, easing = LinearEasing)),
        label = "drift",
    )

    LaunchedEffect(state.selected) {
        if (!state.changed) return@LaunchedEffect
        val target = state.color
        // Recolour at the next edge-on moment (90° + k·180°) and always finish facing front.
        val from = spin.value
        val end = if (from < 270f) 360f else 720f
        val edge = when {
            from < 90f -> 90f
            from < 270f -> 270f
            else -> 450f
        }
        spin.animateTo(end, tween(SpinMillis, easing = FastOutSlowInEasing)) {
            if (value >= edge) shown = target
        }
        shown = target
        spin.snapTo(0f)
    }

    ColorPickerScene(
        state = state,
        number = 6,
        title = "Turntable Spin",
        caption = "The tee spins once on its axis, swapping colour edge-on, while a dashed ring turns in around the swatch.",
        tee = { art ->
            val a = spin.value % 360f
            val back = a > 90f && a < 270f
            drawTee(art, shown, showLogo = !back)
            val edgeOn = 1f - abs(cos(a * PI.toFloat() / 180f))
            if (edgeOn > 0f) drawPath(art.outline, Color.Black, alpha = edgeOn * 0.35f)
        },
        teeModifier = Modifier.graphicsLayer {
            rotationY = spin.value
            cameraDistance = 12f * density
        },
        panel = {
            drawDefaultPanel()
            drawTurntable()
        },
        swatch = { i, selected -> DashedRingSwatch(i, selected, drift) },
    )
}

private fun DrawScope.drawTurntable() {
    val inset = TeeInset.toPx()
    val hem = inset + (size.height - inset * 2) * 0.9f
    val platform = Size(size.width * 0.66f, size.width * 0.11f)
    val topLeft = Offset((size.width - platform.width) / 2, hem - platform.height * 0.45f)
    drawOval(Color.Black, topLeft + Offset(0f, 4.dp.toPx()), platform, alpha = 0.35f)
    drawOval(Color(0xFF242150), topLeft, platform)
    drawOval(Color.White, topLeft, platform, alpha = 0.1f, style = Stroke(1.dp.toPx()))
}

@Composable
private fun BoxScope.DashedRingSwatch(index: Int, selected: Boolean, drift: State<Float>) {
    val turn = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (selected) {
            turn.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        } else {
            turn.animateTo(0f, tween(200))
        }
    }
    SwatchDot(index)
    Spacer(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val radius = RingRadius.toPx()
                val period = 2f * PI.toFloat() * radius / DashCount
                val stroke = Stroke(
                    RingStroke.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(period * 0.35f, period * 0.65f)),
                )
                onDrawBehind {
                    val t = turn.value
                    if (t <= 0f) return@onDrawBehind
                    val angle = drift.value - 150f * (1f - t)
                    val scale = 1.35f - 0.35f * t
                    withTransform({
                        rotate(angle)
                        scale(scale, scale)
                    }) {
                        drawCircle(Color.White, radius, alpha = t, style = stroke)
                    }
                }
            },
    )
}
