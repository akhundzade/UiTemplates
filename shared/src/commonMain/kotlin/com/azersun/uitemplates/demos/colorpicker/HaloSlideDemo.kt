package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun HaloSlideDemo() {
    val state = rememberSwatchState()
    val halo = remember { Animatable(state.selected.toFloat()) }
    val tint = animateColorAsState(state.color, tween(420, easing = FastOutSlowInEasing), label = "tee")

    LaunchedEffect(state.selected) {
        halo.animateTo(state.selected.toFloat(), spring(dampingRatio = 0.55f, stiffness = 320f))
    }

    ColorPickerScene(
        state = state,
        number = 1,
        title = "Halo Slide",
        caption = "A white halo springs over to the tapped swatch while the tee cross-fades to its colour.",
        tee = { drawTee(it, tint.value) },
        swatch = { i, _ -> SwatchDot(i) },
        overlay = { layout -> drawHalo(layout, halo.value, halo.velocity) },
    )
}

/** The halo stretches along its path in proportion to its speed, like a ring sliding on a rail. */
private fun DrawScope.drawHalo(layout: CardLayout, position: Float, velocity: Float) {
    if (!layout.measured) return
    val center = layout.swatchCenter(position)
    val radius = RingRadius.toPx()
    val stretch = (abs(velocity) * 1.4.dp.toPx()).coerceAtMost(9.dp.toPx())
    val size = Size(radius * 2 + stretch, radius * 2 - stretch * 0.2f)
    val topLeft = Offset(center.x - size.width / 2, center.y - size.height / 2)
    drawOval(Color.White, topLeft, size, alpha = 0.14f, style = Stroke(7.dp.toPx()))
    drawOval(Color.White, topLeft, size, style = Stroke(RingStroke.toPx()))
}
