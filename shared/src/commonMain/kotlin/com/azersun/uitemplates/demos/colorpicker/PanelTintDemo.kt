package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private val LiftHeight = 4.dp
private val GlowRadius = 17.dp
private val GlowDrop = 4.dp

@Composable
fun PanelTintDemo() {
    val state = rememberSwatchState()
    val tint = animateColorAsState(state.color, tween(700, easing = FastOutSlowInEasing), label = "panel")
    val teeColor = animateColorAsState(state.color, tween(450, easing = FastOutSlowInEasing), label = "tee")

    ColorPickerScene(
        state = state,
        number = 5,
        title = "Panel Tint",
        caption = "The swatch lifts on a glow of its colour and the whole image panel takes on a tint of it.",
        tee = { drawTee(it, teeColor.value) },
        panel = { drawTintedPanel(tint.value) },
        swatch = { i, selected -> LiftSwatch(i, selected) },
    )
}

private fun DrawScope.drawTintedPanel(tint: Color) {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.linearGradient(
            listOf(lerp(PanelColor, tint, 0.55f), lerp(PanelColor, tint, 0.12f)),
            start = Offset.Zero,
            end = Offset(w, h),
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(tint.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.48f),
            radius = w * 0.55f,
        ),
    )
}

@Composable
private fun BoxScope.LiftSwatch(index: Int, selected: Boolean) {
    val lift = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (selected) {
            lift.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
        } else {
            lift.animateTo(0f, tween(200))
        }
    }
    val color = Swatches[index].color
    // The glow stays on the ground while the dot rises, so the gap between them reads as height.
    Spacer(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val radius = GlowRadius.toPx()
                val glowCenter = size.center + Offset(0f, GlowDrop.toPx())
                val glow = Brush.radialGradient(listOf(color.copy(alpha = 0.75f), color.copy(alpha = 0f)), glowCenter, radius)
                onDrawBehind {
                    val l = lift.value
                    if (l <= 0f) return@onDrawBehind
                    drawCircle(glow, radius, glowCenter, alpha = l.coerceAtMost(1f))
                }
            },
    )
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer { translationY = -LiftHeight.toPx() * lift.value },
        contentAlignment = Alignment.Center,
    ) {
        SwatchDot(index)
        SelectionRing({ lift.value.coerceIn(0f, 1f) })
    }
}
