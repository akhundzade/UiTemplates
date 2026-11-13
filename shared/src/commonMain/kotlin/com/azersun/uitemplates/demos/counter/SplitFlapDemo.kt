package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val FlapStaggerMillis = 70L
private val FlapShape = RoundedCornerShape(8.dp)
private val FlapFace = Color(0xFF14122B)
private val Hinge = Color(0xFF0B0A18)

@Composable
fun SplitFlapDemo() {
    CounterScene(
        number = 2,
        title = "Split Flap",
        caption = "Each digit flaps down in two halves like an airport board, left to right.",
    ) { value ->
        val digits = digitsOf(value)
        DigitRow { index ->
            if (index > 0) Spacer(Modifier.width(4.dp))
            FlapDigit(digits[index], index)
        }
    }
}

/**
 * Four stacked half-cards: the new top waiting behind, the old bottom, the old top that falls
 * forward around the hinge, then the new bottom that falls into place over the old one.
 */
@Composable
private fun FlapDigit(digit: Int, index: Int) {
    var front by remember { mutableIntStateOf(digit) }
    var back by remember { mutableIntStateOf(digit) }
    val topFall = remember { Animatable(0f) }
    val bottomFall = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        // A change mid-flip snaps the running flip to its end before starting the next one.
        front = back
        topFall.snapTo(0f)
        bottomFall.snapTo(0f)
        if (digit == front) return@LaunchedEffect
        back = digit
        bottomFall.snapTo(90f)
        delay(index * FlapStaggerMillis)
        topFall.animateTo(90f, tween(170, easing = FastOutLinearInEasing))
        bottomFall.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 900f))
        front = back
    }

    Box {
        FlapHalf(back, upper = true, angle = { 0f }, modifier = Modifier)
        FlapHalf(front, upper = false, angle = { 0f }, modifier = Modifier.matchParentSize())
        FlapHalf(front, upper = true, angle = { -topFall.value }, modifier = Modifier.matchParentSize())
        FlapHalf(back, upper = false, angle = { bottomFall.value }, modifier = Modifier.matchParentSize())
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent {
                    val y = size.height / 2
                    drawLine(Hinge, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx())
                },
        )
    }
}

/** One half of a flap card, rotated around the card's middle by [angle] degrees and shaded as it turns. */
@Composable
private fun FlapHalf(digit: Int, upper: Boolean, angle: () -> Float, modifier: Modifier) {
    Box(
        modifier
            .graphicsLayer {
                val a = angle()
                rotationX = a
                cameraDistance = 10f * density
                alpha = if (abs(a) >= 89.5f) 0f else 1f
            }
            .drawWithContent {
                val half = size.height / 2
                if (upper) {
                    clipRect(bottom = half) { this@drawWithContent.drawContent() }
                } else {
                    clipRect(top = half) { this@drawWithContent.drawContent() }
                }
                val shade = (abs(angle()) / 90f).coerceIn(0f, 1f) * 0.5f
                if (shade > 0f) {
                    clipRect(top = if (upper) 0f else half, bottom = if (upper) half else size.height) {
                        drawRect(Color.Black.copy(alpha = shade))
                    }
                }
            }
            .background(FlapFace, FlapShape)
            .border(1.dp, MenuColors.CardBorder, FlapShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        DigitCell { Text(digit.toString(), style = NumberStyle) }
    }
}
