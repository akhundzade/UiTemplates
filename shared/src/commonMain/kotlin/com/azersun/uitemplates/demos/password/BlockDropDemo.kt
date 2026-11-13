package com.azersun.uitemplates.demos.password

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FallMillis = 240
private const val DropStaggerMillis = 90L
private val FallHeight = 24.dp

@Composable
fun BlockDropDemo() {
    val form = remember { PasswordFormState() }
    // presence: 1 while the segment is part of the fill. drop: 0 hovering under the field, 1 landed.
    // squash: 1 at the moment of impact, springing back through a slight stretch to 0.
    val presence = remember { List(SegmentCount) { Animatable(0f) } }
    val drop = remember { List(SegmentCount) { Animatable(0f) } }
    val squash = remember { List(SegmentCount) { Animatable(0f) } }
    val color by animateLevelColor(form.level)

    LaunchedEffect(form.level) {
        val level = form.level
        var order = 0
        repeat(SegmentCount) { i ->
            if (i < level) {
                if (presence[i].value == 1f && drop[i].value == 1f) {
                    if (squash[i].value != 0f) launch { squash[i].animateTo(0f, LandingSpring) }
                    return@repeat
                }
                val wait = order++ * DropStaggerMillis
                launch {
                    delay(wait)
                    if (presence[i].value == 0f) drop[i].snapTo(0f)
                    presence[i].snapTo(1f)
                    drop[i].animateTo(1f, tween((FallMillis * (1f - drop[i].value)).toInt(), easing = FastOutLinearInEasing))
                    squash[i].snapTo(1f)
                    squash[i].animateTo(0f, LandingSpring)
                }
            } else if (presence[i].value != 0f) {
                launch {
                    presence[i].animateTo(0f, tween(180))
                    drop[i].snapTo(0f)
                    squash[i].snapTo(0f)
                }
            }
        }
    }

    PasswordStage(2, "Block Drop", "Each new segment drops out of the field and lands with a squash.", form) {
        Canvas(Modifier.fillMaxSize()) {
            drawTrack()
            repeat(SegmentCount) { i ->
                val p = presence[i].value
                if (p <= 0f) return@repeat
                val s = squash[i].value
                val shrink = 0.6f + 0.4f * p
                val pivot = Offset(segmentLeft(i) + segmentWidth() / 2, size.height)
                val lift = (1f - drop[i].value) * FallHeight.toPx()
                withTransform({
                    translate(top = -lift)
                    scale((1f + 0.15f * s) * shrink, (1f - 0.4f * s) * shrink, pivot)
                }) {
                    drawSegment(i, color, alpha = p)
                }
            }
        }
    }
}

private val LandingSpring = spring<Float>(dampingRatio = 0.35f, stiffness = 600f)
