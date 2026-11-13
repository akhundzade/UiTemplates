package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val FallMillis = 440
private const val AfterMillis = 900
private val BackOut = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Sparkle positions around the cart's centre, in dp. */
private val Sparkles = listOf(Offset(-22f, -16f), Offset(20f, -22f), Offset(-26f, 10f), Offset(26f, 6f))

/** progress runs 0→1 for the fall and 1→2 for the sparkles and tick after the catch. */
private class Drop(val cart: Rect) {
    val progress = Animatable(0f)
}

@Composable
fun IntoTheBagDemo() {
    val scene = rememberCartScene()
    val scope = rememberCoroutineScope()
    val drops = remember { mutableStateListOf<Drop>() }
    val squash = remember { Animatable(0f) }

    fun add() {
        val drop = Drop(scene.bounds(scene.cart))
        drops += drop
        scope.launch {
            drop.progress.animateTo(1f, tween(FallMillis, easing = LinearEasing))
            scene.count++
            launch {
                squash.animateTo(1f, tween(70))
                squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 600f))
            }
            drop.progress.animateTo(2f, tween(AfterMillis, easing = LinearEasing))
            drops -= drop
        }
    }

    ProductScene(
        scene = scene,
        number = 5,
        title = "Into the Bag",
        caption = "A tiny mug drops into the cart, the cart squashes as it catches it, and sparkles and a tick pop.",
        onAutoplay = ::add,
        button = { AddButton(::add) },
        cart = {
            CartWithBadge(
                scene,
                cartModifier = Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleX = 1f + 0.1f * squash.value
                    scaleY = 1f - 0.16f * squash.value
                },
            )
        },
        overlay = {
            Canvas(Modifier.fillMaxSize()) {
                drops.forEach { drop ->
                    val t = drop.progress.value
                    if (t < 1f) drawFalling(drop.cart, t) else drawCelebration(drop.cart, t - 1f)
                }
            }
        },
    )
}

private fun DrawScope.drawFalling(cart: Rect, fall: Float) {
    val basket = Offset(cart.left + cart.width * BasketCenter.x, cart.top + cart.height * BasketCenter.y)
    // Quadratic in time, like a dropped object under gravity.
    val y = lerp(cart.top - 90.dp.toPx(), basket.y, fall * fall)
    val side = 22.dp.toPx() * lerp(1f, 0.6f, phase(fall, 0.75f, 1f))
    val tilt = sin(fall * PI.toFloat() * 2) * 10f * (1f - fall)
    val at = Offset(basket.x, y)
    rotate(tilt, pivot = at) {
        drawMug(
            CartColors.Ink.copy(alpha = phase(fall, 0f, 0.15f)),
            IconStroke.toPx(),
            side = side,
            topLeft = at - Offset(side / 2, side / 2),
        )
    }
}

private fun DrawScope.drawCelebration(cart: Rect, after: Float) {
    val center = cart.center
    val stroke = IconStroke.toPx()
    Sparkles.forEachIndexed { i, offset ->
        val local = phase(after, i * 0.06f, i * 0.06f + 0.6f)
        val arm = 4.dp.toPx() * sin(PI.toFloat() * local)
        if (arm <= 0f) return@forEachIndexed
        val at = center + Offset(offset.x.dp.toPx(), offset.y.dp.toPx())
        rotate(45f * local, pivot = at) {
            drawLine(CartColors.Ink, at - Offset(arm, 0f), at + Offset(arm, 0f), stroke, StrokeCap.Round)
            drawLine(CartColors.Ink, at - Offset(0f, arm), at + Offset(0f, arm), stroke, StrokeCap.Round)
        }
    }
    val pop = BackOut.transform(phase(after, 0.05f, 0.4f))
    val fade = 1f - phase(after, 0.7f, 1f)
    if (pop <= 0f || fade <= 0f) return
    val tick = Offset(center.x, cart.top - 16.dp.toPx())
    val radius = 9.dp.toPx()
    scale(pop, pivot = tick) {
        drawCircle(CartColors.Ink, radius, tick, alpha = fade)
        val side = radius * 1.1f
        translate(tick.x - side / 2, tick.y - side / 2) {
            drawCheck(Color.White.copy(alpha = fade), stroke * 1.2f, side)
        }
    }
}
