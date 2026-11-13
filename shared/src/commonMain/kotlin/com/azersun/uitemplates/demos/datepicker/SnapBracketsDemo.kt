package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TravelMillis = 300
private const val OpenMillis = 110

/** How far the brackets have opened by the time they arrive, before the wide overshoot. */
private const val TravelSpread = 0.45f

@Composable
fun SnapBracketsDemo() {
    val state = rememberDatePickerState()
    val position = remember { Animatable((state.selected - FirstDay).toFloat()) }
    val spread = remember { Animatable(0f) }

    LaunchedEffect(state.selected) {
        val target = (state.selected - FirstDay).toFloat()
        if (position.value == target) return@LaunchedEffect
        coroutineScope {
            launch { spread.animateTo(TravelSpread, tween(TravelMillis, easing = FastOutSlowInEasing)) }
            position.animateTo(target, tween(TravelMillis, easing = FastOutSlowInEasing))
        }
        spread.animateTo(1f, tween(OpenMillis, easing = LinearOutSlowInEasing))
        spread.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 1400f))
    }

    DatePickerScene(
        state = state,
        number = 2,
        title = "Snap Brackets",
        caption = "Four corner brackets fly to the tapped day, open wide, then snap shut onto it.",
        overlay = { drawBrackets(cellRect(position.value), spread.value) },
    ) { _, selected -> SnapCapsule(selected) }
}

@Composable
private fun BoxScope.SnapCapsule(selected: Boolean) {
    val shown = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (selected) {
            if (shown.value == 1f) return@LaunchedEffect
            // Waits for the brackets to arrive and open, so the capsule lands as they snap shut.
            delay((TravelMillis + OpenMillis).toLong())
            shown.animateTo(1f, tween(180))
        } else {
            shown.animateTo(0f, tween(200))
        }
    }
    Capsule(
        Modifier.graphicsLayer {
            val s = shown.value
            alpha = s
            scaleX = 0.94f + 0.06f * s
            scaleY = 0.94f + 0.06f * s
        },
    )
}

private fun DrawScope.drawBrackets(cell: Rect, spread: Float) {
    val out = 4.dp.toPx() + spread * 10.dp.toPx()
    val r = Rect(cell.left - out, cell.top - out, cell.right + out, cell.bottom + out)
    val arm = 9.dp.toPx()
    val corners = listOf(
        Triple(r.topLeft, 1f, 1f),
        Triple(r.topRight, -1f, 1f),
        Triple(r.bottomLeft, 1f, -1f),
        Triple(r.bottomRight, -1f, -1f),
    )
    listOf(MenuColors.Cyan.copy(alpha = 0.25f) to 6.dp.toPx(), Color.White to 2.4.dp.toPx()).forEach { (color, stroke) ->
        corners.forEach { (corner, dx, dy) ->
            drawLine(color, corner, corner + Offset(arm * dx, 0f), stroke, StrokeCap.Round)
            drawLine(color, corner, corner + Offset(0f, arm * dy), stroke, StrokeCap.Round)
        }
    }
}
