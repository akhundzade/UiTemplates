package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DotSize = 6.dp

/** Distance of the dot's centre above the cell's bottom edge: just under the date. */
private val DotLift = 9.dp

@Composable
fun DotSwellDemo() {
    val state = rememberDatePickerState()
    DatePickerScene(
        state = state,
        number = 5,
        title = "Dot Swell",
        caption = "A tiny dot appears under the tapped date and swells up into the full capsule.",
    ) { _, selected -> SwellCapsule(selected) }
}

@Composable
private fun BoxScope.SwellCapsule(selected: Boolean) {
    val start = if (selected) 1f else 0f
    val dot = remember { Animatable(start) }
    val tall = remember { Animatable(start) }
    val wide = remember { Animatable(start) }

    LaunchedEffect(selected) {
        if (selected) {
            if (wide.value == 1f && tall.value == 1f) return@LaunchedEffect
            dot.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 900f))
            delay(60)
            launch { tall.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f)) }
            delay(50)
            wide.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 360f))
        } else {
            launch { wide.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
            tall.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            dot.animateTo(0f, tween(120))
        }
    }

    Canvas(Modifier.matchParentSize()) {
        val d = dot.value
        val v = tall.value
        val h = wide.value
        if (d <= 0f) return@Canvas
        val dotPx = DotSize.toPx() * d.coerceAtLeast(0f)
        val width = (dotPx + (size.width - dotPx) * h).coerceAtLeast(0f)
        val height = (dotPx + (size.height - dotPx) * v).coerceAtLeast(0f)
        val dotY = size.height - DotLift.toPx()
        val centerY = dotY + (size.height / 2 - dotY) * v
        drawRoundRect(
            CapsuleBrush,
            topLeft = Offset((size.width - width) / 2, centerY - height / 2),
            size = Size(width, height),
            cornerRadius = CornerRadius(minOf(width, height) / 2),
        )
    }
}
