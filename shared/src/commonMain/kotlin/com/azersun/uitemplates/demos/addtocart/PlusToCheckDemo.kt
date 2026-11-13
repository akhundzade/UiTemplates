package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

private const val ForwardMillis = 900
private const val ReverseMillis = 520
private const val HoldMillis = 2000L

/** Timeline marks: + grows a disc until [DiscEnd], morphs into ✓ until [CheckAt], then the fill. */
private const val DiscEnd = 0.3f
private const val CheckAt = 0.62f

private val GlyphInset = 19.dp
private val DiscRadius = 13.dp

/** Plus (horizontal, vertical) and tick (short, long) strokes, in units of the glyph's arm. */
private val PlusStrokes = listOf(Offset(-1f, 0f) to Offset(1f, 0f), Offset(0f, 1f) to Offset(0f, -1f))
private val CheckStrokes = listOf(Offset(-0.7f, 0f) to Offset(-0.25f, 0.5f), Offset(-0.25f, 0.5f) to Offset(0.8f, -0.55f))

@Composable
fun PlusToCheckDemo() {
    val scene = rememberCartScene()
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }

    fun add() {
        if (busy) return
        busy = true
        scope.launch {
            progress.animateTo(CheckAt, tween((ForwardMillis * CheckAt).toInt(), easing = LinearEasing))
            scene.count++
            progress.animateTo(1f, tween((ForwardMillis * (1f - CheckAt)).toInt(), easing = LinearEasing))
            delay(HoldMillis)
            progress.animateTo(0f, tween(ReverseMillis, easing = FastOutSlowInEasing))
            busy = false
        }
    }

    ProductScene(
        scene = scene,
        number = 6,
        title = "Plus to Check",
        caption = "The + grows into a black disc and turns into a ✓, then the button fills: ADDED. It reverts after 2 s.",
        onAutoplay = ::add,
        button = {
            PillButton(onClick = ::add, modifier = Modifier.fillMaxWidth()) {
                Canvas(Modifier.fillMaxSize()) { drawDiscAndGlyph(progress.value) }
                Box(Modifier.fillMaxSize().padding(start = 14.dp, end = 38.dp), contentAlignment = Alignment.Center) {
                    Caps(
                        "ADD TO CART",
                        Modifier.graphicsLayer { alpha = 1f - phase(progress.value, CheckAt, 0.8f) },
                    )
                    Caps(
                        "ADDED",
                        Modifier.graphicsLayer {
                            val shown = phase(progress.value, 0.75f, 1f)
                            alpha = shown
                            translationY = (1f - shown) * 6.dp.toPx()
                        },
                        color = Color.White,
                    )
                }
            }
        },
    )
}

private fun DrawScope.drawDiscAndGlyph(p: Float) {
    val disc = FastOutSlowInEasing.transform(phase(p, 0f, DiscEnd))
    val morph = FastOutSlowInEasing.transform(phase(p, DiscEnd, CheckAt))
    val fill = FastOutSlowInEasing.transform(phase(p, CheckAt, 1f))
    val center = Offset(size.width - GlyphInset.toPx(), size.height / 2)
    val discRadius = DiscRadius.toPx()
    // Far enough to cover the pill's opposite end, which the pill's clip then trims.
    val coverRadius = hypot(center.x, size.height / 2)
    val radius = discRadius * disc + (coverRadius - discRadius) * fill
    if (radius > 0f) drawCircle(CartColors.Ink, radius, center)

    val arm = lerp(4.5.dp.toPx(), 6.dp.toPx(), disc)
    val stroke = lerp(1.3.dp.toPx(), 1.8.dp.toPx(), disc)
    rotate(90f * (1f - morph), pivot = center) {
        PlusStrokes.zip(CheckStrokes).forEach { (plus, check) ->
            val start = center + lerpOffset(plus.first, check.first, morph) * arm
            val end = center + lerpOffset(plus.second, check.second, morph) * arm
            if (disc < 1f) drawLine(CartColors.Ink, start, end, stroke, StrokeCap.Round, alpha = 1f - disc)
            if (disc > 0f) drawLine(Color.White, start, end, stroke, StrokeCap.Round, alpha = disc)
        }
    }
}

private fun lerpOffset(from: Offset, to: Offset, fraction: Float): Offset =
    Offset(lerp(from.x, to.x, fraction), lerp(from.y, to.y, fraction))
