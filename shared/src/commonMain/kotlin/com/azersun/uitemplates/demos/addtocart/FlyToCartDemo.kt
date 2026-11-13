package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.min

private const val FlightMillis = 720

private class Flight(val from: Offset, val to: Offset) {
    val progress = Animatable(0f)
}

@Composable
fun FlyToCartDemo() {
    val scene = rememberCartScene()
    val scope = rememberCoroutineScope()
    val flights = remember { mutableStateListOf<Flight>() }
    val dip = remember { Animatable(0f) }

    fun add() {
        val from = scene.bounds(scene.thumb).center
        val cart = scene.bounds(scene.cart)
        val to = Offset(cart.left + cart.width * BasketCenter.x, cart.top + cart.height * BasketCenter.y)
        val flight = Flight(from, to)
        flights += flight
        scope.launch {
            flight.progress.animateTo(1f, tween(FlightMillis, easing = FastOutSlowInEasing))
            flights -= flight
            scene.count++
            dip.animateTo(1f, tween(90))
            dip.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }

    ProductScene(
        scene = scene,
        number = 1,
        title = "Fly to Cart",
        caption = "A mini thumbnail jumps from the product and arcs into the cart; the cart dips and the badge ticks up.",
        onAutoplay = ::add,
        button = { AddButton(::add) },
        cart = {
            CartWithBadge(
                scene,
                cartModifier = Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationY = dip.value * 5.dp.toPx()
                    rotationZ = dip.value * -6f
                },
            )
        },
        overlay = {
            Canvas(Modifier.fillMaxSize()) {
                flights.forEach { flight ->
                    val t = flight.progress.value
                    // Peaks well above both ends so it reads as a jump, not a slide.
                    val control = Offset((flight.from.x + flight.to.x) / 2, min(flight.from.y, flight.to.y) - 110.dp.toPx())
                    val at = quadratic(flight.from, control, flight.to, t)
                    val side = lerp(30.dp.toPx(), 9.dp.toPx(), t) * (1f - 0.4f * phase(t, 0.9f, 1f))
                    rotate(lerp(0f, 200f, t), pivot = at) {
                        drawRoundRect(
                            CartColors.Ink,
                            topLeft = at - Offset(side / 2, side / 2),
                            size = Size(side, side),
                            cornerRadius = CornerRadius(side * 0.22f),
                        )
                    }
                }
            }
        },
    )
}
