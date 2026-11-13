package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val RunMillis = 1800

/** Where on the 0→1 run timeline the dropped dot lands in the cart. */
private const val CatchAt = 0.53f

private val RunCartSize = 18.dp

@Composable
fun CartRunDemo() {
    val scene = rememberCartScene()
    val scope = rememberCoroutineScope()
    val run = remember { Animatable(0f) }

    fun add() {
        if (run.isRunning) return
        scope.launch {
            run.snapTo(0f)
            run.animateTo(CatchAt, tween((RunMillis * CatchAt).toInt(), easing = LinearEasing))
            scene.count++
            run.animateTo(1f, tween((RunMillis * (1f - CatchAt)).toInt(), easing = LinearEasing))
            run.snapTo(0f)
        }
    }

    ProductScene(
        scene = scene,
        number = 4,
        title = "Cart Run",
        caption = "The label slides away, a little cart runs in, catches a dropped item, and the label returns.",
        onAutoplay = ::add,
        button = {
            PillButton(
                onClick = ::add,
                modifier = Modifier
                    .fillMaxWidth()
                    // Drawn outside the pill's clip so the item can fall in from above it.
                    .drawWithContent {
                        val t = run.value
                        drawContent()
                        drawDrop(t)
                    },
            ) {
                AddLabel(
                    Modifier.graphicsLayer {
                        val t = run.value
                        val out = phase(t, 0f, 0.11f)
                        val back = LinearOutSlowInEasing.transform(phase(t, 0.78f, 1f))
                        val slide = 28.dp.toPx()
                        translationX = if (t < 0.5f) out * slide else (back - 1f) * slide
                        alpha = if (t < 0.5f) 1f - out else back
                    },
                )
                Canvas(Modifier.fillMaxSize()) { drawRunner(run.value) }
            }
        },
    )
}

/** Horizontal centre of the running cart, from off the left edge to the middle and off the right. */
private fun DrawScope.runnerX(t: Float): Float {
    val half = RunCartSize.toPx() / 2
    val runIn = LinearOutSlowInEasing.transform(phase(t, 0.06f, 0.33f))
    val runOut = FastOutLinearInEasing.transform(phase(t, 0.64f, 0.84f))
    val enter = lerp(-half, size.width / 2, runIn)
    return lerp(enter, size.width + half, runOut)
}

private fun DrawScope.drawRunner(t: Float) {
    if (t <= 0.06f || t >= 0.84f) return
    val s = RunCartSize.toPx()
    val x = runnerX(t)
    val y = size.height / 2
    val runIn = phase(t, 0.06f, 0.33f)
    val runOut = phase(t, 0.64f, 0.84f)
    // Lines are strongest while the cart is fastest: entering, and accelerating away.
    val speed = if (t < 0.5f) 1f - LinearOutSlowInEasing.transform(runIn) else FastOutLinearInEasing.transform(runOut)
    if (speed > 0.02f) {
        val stroke = 1.2.dp.toPx()
        listOf(-4f to 8f, 0f to 12f, 4f to 6f).forEach { (dy, length) ->
            val start = x - s * 0.6f - 3.dp.toPx()
            drawLine(
                CartColors.Ink,
                Offset(start, y + dy.dp.toPx()),
                Offset(start - length.dp.toPx() * speed, y + dy.dp.toPx()),
                stroke,
                StrokeCap.Round,
                alpha = speed,
            )
        }
    }
    val rock = sin(2 * PI.toFloat() * phase(t, 0.33f, 0.42f)) * 7f * (1f - phase(t, 0.33f, 0.42f))
    val squash = sin(PI.toFloat() * phase(t, CatchAt, 0.62f)) * 0.18f
    translate(x - s / 2, y - s / 2) {
        rotate(rock, pivot = Offset(s / 2, s * 0.85f)) {
            scale(1f + squash * 0.6f, 1f - squash, pivot = Offset(s / 2, s * 0.9f)) {
                drawCart(CartColors.Ink, IconStroke.toPx(), side = s)
                if (t >= CatchAt) drawCircle(CartColors.Ink, 2.4.dp.toPx(), Offset(s * BasketCenter.x, s * BasketCenter.y))
            }
        }
    }
}

/** The item falling from above the button into the stopped cart. */
private fun DrawScope.drawDrop(t: Float) {
    val fall = phase(t, 0.4f, CatchAt)
    if (fall <= 0f || fall >= 1f) return
    val s = RunCartSize.toPx()
    val target = Offset(size.width / 2 - s / 2 + s * BasketCenter.x, size.height / 2 - s / 2 + s * BasketCenter.y)
    val y = lerp(-18.dp.toPx(), target.y, fall * fall)
    drawCircle(CartColors.Ink, 3.dp.toPx(), Offset(target.x, y), alpha = phase(fall, 0f, 0.15f))
}
