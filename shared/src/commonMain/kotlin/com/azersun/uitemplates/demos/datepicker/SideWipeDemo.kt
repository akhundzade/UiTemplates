package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect

private const val WipeOutMillis = 260
private const val WipeInMillis = 320
private const val WipeInDelayMillis = 60

@Composable
fun SideWipeDemo() {
    val state = rememberDatePickerState()
    DatePickerScene(
        state = state,
        number = 4,
        title = "Side Wipe",
        caption = "Color wipes out of the old day toward the new one, then wipes into it from the same side.",
    ) { day, selected -> WipeCapsule(state, day, selected) }
}

@Composable
private fun BoxScope.WipeCapsule(state: DatePickerState, day: Int, selected: Boolean) {
    val reveal = remember { Animatable(if (selected) 1f else 0f) }
    // Which edge the visible part of the capsule stays pinned to while it grows or shrinks.
    var pinnedLeft by remember { mutableStateOf(true) }

    LaunchedEffect(selected) {
        if (selected) {
            if (reveal.value == 1f) return@LaunchedEffect
            pinnedLeft = state.previous < day
            reveal.animateTo(1f, tween(WipeInMillis, WipeInDelayMillis, FastOutSlowInEasing))
        } else {
            pinnedLeft = state.selected < day
            reveal.animateTo(0f, tween(WipeOutMillis, easing = FastOutSlowInEasing))
        }
    }

    Canvas(Modifier.matchParentSize()) {
        val r = reveal.value
        val left = pinnedLeft
        if (r <= 0f) return@Canvas
        val width = size.width * r
        val start = if (left) 0f else size.width - width
        clipRect(start, 0f, start + width, size.height) {
            drawRoundRect(CapsuleBrush, cornerRadius = CornerRadius(size.width / 2))
        }
    }
}
