package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val LineCore = Color(0xFFB5E0FF)

@Composable
fun LineExpandDemo() {
    val state = rememberViewAsState()
    val line = remember { Animatable(0f) }
    val expand = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            line.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            expand.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f))
        } else if (shown) {
            expand.animateTo(0f, tween(220, easing = FastOutLinearInEasing))
            line.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
            shown = false
        }
    }

    ViewAsScene(state, 6, "Line Expand", "A line shoots out of the button, then opens into the menu.") {
        if (shown) {
            ViewAsMenu(
                state,
                Modifier.graphicsLayer {
                    // Opens up and down from the line, which runs level with the button's centre.
                    transformOrigin = TransformOrigin(0.5f, (ButtonHeight / 2).toPx() / size.height)
                    scaleY = expand.value.coerceAtLeast(0.001f)
                    alpha = if (expand.value > 0f) 1f else 0f
                },
                row = { i ->
                    Modifier.graphicsLayer { alpha = ((expand.value - 0.45f - i * 0.1f) / 0.3f).coerceIn(0f, 1f) }
                },
            )
            Canvas(Modifier.width(MenuWidth).height(ButtonHeight)) {
                val alpha = (1f - expand.value).coerceIn(0f, 1f)
                if (alpha <= 0f || line.value <= 0f) return@Canvas
                val y = size.height / 2
                val start = Offset(-MenuGap.toPx(), y)
                val end = Offset(start.x + (size.width + MenuGap.toPx()) * line.value, y)
                drawLine(WaveBlue, start, end, 7.dp.toPx(), StrokeCap.Round, alpha = alpha * 0.25f)
                drawLine(WaveBlue, start, end, 4.dp.toPx(), StrokeCap.Round, alpha = alpha * 0.45f)
                drawLine(LineCore, start, end, 2.dp.toPx(), StrokeCap.Round, alpha = alpha)
                if (line.value < 1f) drawCircle(Color.White, 2.5.dp.toPx(), end, alpha = alpha)
            }
        }
    }
}
