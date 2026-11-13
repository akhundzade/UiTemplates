package com.azersun.uitemplates.demos.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun LineUnfoldDemo() {
    val state = rememberDialogState()
    val line = remember { Animatable(0f) }
    val unfold = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            line.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            unfold.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 360f))
        } else if (shown) {
            unfold.animateTo(0f, tween(200, easing = FastOutLinearInEasing))
            line.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            shown = false
        }
    }

    DialogScene(
        state = state,
        number = 4,
        title = "Line Unfold",
        caption = "A glowing line opens wide, then unfolds tall into the dialog.",
        shown = shown,
        dim = { line.value * 0.35f + unfold.value * 0.65f },
        dialogModifier = Modifier.graphicsLayer {
            val u = unfold.value
            scaleY = u.coerceAtLeast(0f)
            alpha = (u * 4f).coerceIn(0f, 1f)
        },
        decoration = {
            val width = line.value
            val glow = 1f - unfold.value.coerceIn(0f, 1f)
            if (width > 0f && glow > 0f) drawGlowLine(width, glow)
        },
    )
}

private fun DrawScope.drawGlowLine(widthFraction: Float, alpha: Float) {
    val half = size.width / 2 * widthFraction
    val y = size.height / 2
    val start = Offset(size.width / 2 - half, y)
    val end = Offset(size.width / 2 + half, y)
    drawLine(DialogPink, start, end, 14.dp.toPx(), StrokeCap.Round, alpha = 0.14f * alpha)
    drawLine(DialogPink, start, end, 6.dp.toPx(), StrokeCap.Round, alpha = 0.4f * alpha)
    drawLine(Color.White, start, end, 2.dp.toPx(), StrokeCap.Round, alpha = alpha)
}
