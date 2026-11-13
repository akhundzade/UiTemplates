package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val FlightMillis = 520
private const val FloatMillis = 800

private class Hop(val from: Offset, val to: Offset) {
    val progress = Animatable(0f)
}

private class PlusOne(val anchor: Offset) {
    val progress = Animatable(0f)
}

@Composable
fun BadgeHopDemo() {
    val scene = rememberCartScene()
    val scope = rememberCoroutineScope()
    val hops = remember { mutableStateListOf<Hop>() }
    val plusOnes = remember { mutableStateListOf<PlusOne>() }
    val jump = remember { Animatable(0f) }

    fun add() {
        val button = scene.bounds(scene.button)
        val badge = scene.bounds(scene.badge)
        val hop = Hop(button.center, badge.center)
        hops += hop
        scope.launch {
            hop.progress.animateTo(1f, tween(FlightMillis, easing = FastOutSlowInEasing))
            hops -= hop
            scene.count++
            val plusOne = PlusOne(Offset(badge.right, badge.top))
            plusOnes += plusOne
            launch {
                plusOne.progress.animateTo(1f, tween(FloatMillis, easing = LinearEasing))
                plusOnes -= plusOne
            }
            jump.animateTo(1f, tween(130, easing = LinearOutSlowInEasing))
            // Under-damped so the badge lands with a couple of small bounces.
            jump.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 650f))
        }
    }

    ProductScene(
        scene = scene,
        number = 3,
        title = "Badge Hop",
        caption = "A dot flies from the button to the cart; the badge hops and a +1 floats up.",
        onAutoplay = ::add,
        button = { AddButton(::add) },
        cart = {
            CartWithBadge(
                scene,
                badgeModifier = Modifier.graphicsLayer {
                    translationY = -jump.value * 12.dp.toPx()
                    val squash = jump.value.coerceAtMost(0f)
                    scaleX = 1f - squash * 0.25f
                    scaleY = 1f + squash * 0.25f
                },
            )
        },
        overlay = {
            Canvas(Modifier.fillMaxSize()) {
                hops.forEach { hop ->
                    val t = hop.progress.value
                    val control = Offset(lerp(hop.from.x, hop.to.x, 0.35f), hop.to.y - 60.dp.toPx())
                    // A short fading trail sells the speed of such a small dot.
                    for (i in 3 downTo 0) {
                        val at = quadratic(hop.from, control, hop.to, (t - i * 0.035f).coerceAtLeast(0f))
                        drawCircle(CartColors.Ink, 4.dp.toPx() * (1f - i * 0.18f), at, alpha = 1f - i * 0.25f)
                    }
                }
            }
            plusOnes.forEach { plusOne ->
                key(plusOne) {
                    Text(
                        "+1",
                        color = CartColors.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                            val t = plusOne.progress.value
                            translationX = plusOne.anchor.x + 3.dp.toPx()
                            translationY = plusOne.anchor.y - size.height - 26.dp.toPx() * LinearOutSlowInEasing.transform(t)
                            alpha = 1f - phase(t, 0.45f, 1f)
                        },
                    )
                }
            }
        },
    )
}
