package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val PopScale = 1.25f
private const val SquashX = 0.05f
private const val SquashY = 0.08f

@Composable
fun CheckPopDemo() {
    val state = rememberSwatchState()
    val squash = remember { Animatable(0f) }
    val tint = animateColorAsState(state.color, tween(320, easing = FastOutSlowInEasing), label = "tee")

    LaunchedEffect(state.selected) {
        if (!state.changed) return@LaunchedEffect
        squash.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
        squash.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 420f))
    }

    ColorPickerScene(
        state = state,
        number = 2,
        title = "Check Pop",
        caption = "The tapped swatch pops and draws a check; the tee squashes and springs back in the new colour.",
        tee = { drawTee(it, tint.value) },
        teeModifier = Modifier.graphicsLayer {
            val s = squash.value
            scaleX = 1f + SquashX * s
            scaleY = 1f - SquashY * s
            transformOrigin = TransformOrigin(0.5f, 0.9f)
        },
        swatch = { i, selected -> CheckSwatch(i, selected) },
    )
}

@Composable
private fun BoxScope.CheckSwatch(index: Int, selected: Boolean) {
    val pop = remember { Animatable(1f) }
    val check = remember { Animatable(if (selected) 1f else 0f) }
    val ring = animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "ring")

    LaunchedEffect(selected) {
        if (!selected) {
            check.animateTo(0f, tween(150))
            return@LaunchedEffect
        }
        if (check.value == 1f) return@LaunchedEffect
        launch {
            pop.animateTo(PopScale, tween(110, easing = FastOutSlowInEasing))
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
        check.snapTo(0f)
        check.animateTo(1f, tween(280, delayMillis = 70, easing = FastOutSlowInEasing))
    }

    val color = Swatches[index].color
    val checkColor = if (color.luminance() > 0.5f) Color(0xFF14122B) else Color.White
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
    ) {
        SwatchDot(index, Modifier.align(Alignment.Center))
        SelectionRing({ ring.value })
        Box(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val u = DotSize.toPx() / 2
                    val c = size.center
                    val tick = Path().apply {
                        moveTo(c.x - u * 0.42f, c.y + u * 0.02f)
                        lineTo(c.x - u * 0.12f, c.y + u * 0.32f)
                        lineTo(c.x + u * 0.44f, c.y - u * 0.3f)
                    }
                    val measure = PathMeasure().apply { setPath(tick, false) }
                    val segment = Path()
                    val stroke = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    onDrawBehind {
                        val t = check.value
                        if (t <= 0f) return@onDrawBehind
                        segment.reset()
                        measure.getSegment(0f, measure.length * t, segment, true)
                        drawPath(segment, checkColor, style = stroke)
                    }
                },
        )
    }
}
