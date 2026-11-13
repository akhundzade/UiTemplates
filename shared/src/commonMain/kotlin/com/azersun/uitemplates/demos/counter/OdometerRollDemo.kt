package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.floor
import kotlin.math.roundToInt

/** Whole extra revolutions per digit, so the digits further right roll further. */
private val ExtraTurns = intArrayOf(0, 0, 1, 2)
private val RollEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

private val EdgeFade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.2f to Color.Black,
    0.8f to Color.Black,
    1f to Color.Transparent,
)

@Composable
fun OdometerRollDemo() {
    CounterScene(
        number = 1,
        title = "Odometer Roll",
        caption = "Each digit rolls through the numbers like a car odometer; the right-hand digits roll furthest.",
    ) { value ->
        DigitRow { index -> OdometerDigit(value, index) }
    }
}

@Composable
private fun OdometerDigit(value: Int, index: Int) {
    // Unbounded position in digit steps; the shown digit is the position mod 10.
    val position = remember { Animatable(digitsOf(value)[index].toFloat()) }
    var lastValue by remember { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        if (value == lastValue) return@LaunchedEffect
        val rising = value > lastValue
        lastValue = value
        val start = position.targetValue.roundToInt()
        val from = start.mod(10)
        val to = digitsOf(value)[index]
        val steps = if (rising) (to - from).mod(10) + 10 * ExtraTurns[index] else -((from - to).mod(10) + 10 * ExtraTurns[index])
        position.animateTo((start + steps).toFloat(), tween(1000 + 120 * index, easing = RollEasing))
    }

    val base by remember { derivedStateOf { floor(position.value).toInt() } }
    DigitCell(
        Modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                clip = true
            }
            .drawWithContent {
                drawContent()
                drawRect(EdgeFade, blendMode = BlendMode.DstIn)
            },
    ) {
        Text(
            base.mod(10).toString(),
            style = NumberStyle,
            modifier = Modifier.graphicsLayer { translationY = -(position.value - base) * size.height },
        )
        Text(
            (base + 1).mod(10).toString(),
            style = NumberStyle,
            modifier = Modifier.graphicsLayer { translationY = (1f - (position.value - base)) * size.height },
        )
    }
}
