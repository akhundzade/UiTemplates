package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors

private val TopHalf = GenericShape { size, _ -> addRect(Rect(0f, 0f, size.width, size.height / 2)) }
private val BottomHalf = GenericShape { size, _ -> addRect(Rect(0f, size.height / 2, size.width, size.height)) }

@Composable
fun BladeSliceDemo() {
    SwipeDeleteScene(
        number = 3,
        title = "Blade Slice",
        caption = "A blade line follows the finger and the row splits in two.",
        spec = SwipeSpec(exitMillis = 700, collapseAt = 0.62f),
    ) { message, state -> BladeRow(message, state) }
}

@Composable
private fun BladeRow(message: Message, state: SwipeRowState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawWithContent {
                val e = state.exit.value
                val drag = state.drag.value
                drawContent()
                val w = size.width
                val tip = if (state.deleting) {
                    lerp(w - state.releasedDrag, 0f, LinearOutSlowInEasing.transform(phase(e, 0f, 0.22f)))
                } else {
                    w - drag
                }
                val alpha = 1f - phase(e, 0.24f, 0.46f)
                if (tip < w && alpha > 0f) drawBlade(tip.coerceAtLeast(0f), w, RowHeight.toPx() / 2, alpha)
            },
    ) {
        if (!state.deleting) {
            MessageRow(message)
        } else {
            MessageRow(message, Modifier.half(state, top = true))
            MessageRow(message, Modifier.half(state, top = false))
        }
    }
}

private fun Modifier.half(state: SwipeRowState, top: Boolean): Modifier = graphicsLayer {
    val e = state.exit.value
    val apart = FastOutSlowInEasing.transform(phase(e, 0.2f, 1f))
    val side = if (top) -1f else 1f
    translationX = -size.width * (if (top) 0.16f else 0.1f) * apart
    translationY = side * size.height * 0.6f * apart
    rotationZ = side * 4f * apart
    alpha = 1f - phase(e, 0.35f, 1f)
    shape = if (top) TopHalf else BottomHalf
    clip = true
}

/** A glowing pink cut from [tipX] to the row's right edge, brightest at the finger. */
private fun DrawScope.drawBlade(tipX: Float, endX: Float, y: Float, alpha: Float) {
    val start = Offset(tipX, y)
    val end = Offset(endX, y)
    drawLine(MenuColors.Pink, start, end, 7.dp.toPx(), StrokeCap.Round, alpha = 0.22f * alpha)
    drawLine(MenuColors.Pink, start, end, 2.5.dp.toPx(), StrokeCap.Round, alpha = alpha)
    drawLine(Color.White, start, end, 1.dp.toPx(), StrokeCap.Round, alpha = alpha)
    drawCircle(MenuColors.Pink, 7.dp.toPx(), start, alpha = 0.3f * alpha)
    drawCircle(Color.White, 2.5.dp.toPx(), start, alpha = alpha)
}
