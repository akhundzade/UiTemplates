package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.sin

private val BinX = 22.dp
private val BinSize = 28.dp
private val LidRed = Color(0xFFDC2626)

/** How far the row is already sucked in when the finger reaches the threshold. */
private const val PreviewWarp = 0.3f

@Composable
fun GenieBinDemo() {
    SwipeDeleteScene(
        number = 6,
        title = "Genie Bin",
        caption = "The row is sucked into a little red bin like a genie.",
        spec = SwipeSpec(exitMillis = 820, collapseAt = 0.72f),
    ) { message, state -> GenieRow(message, state) }
}

@Composable
private fun GenieRow(message: Message, state: SwipeRowState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawWithContent {
                val e = state.exit.value
                val g = warp(state)
                val pull = state.fraction
                val w = size.width
                val h = RowHeight.toPx()
                val bin = Offset(BinX.toPx(), h / 2)
                if (g < 1f) {
                    withTransform({
                        scale(1f - 0.96f * g, 1f - 0.7f * g, bin)
                        clipPath(geniePath(w, h, g))
                    }) { this@drawWithContent.drawContent() }
                }
                val appear = if (state.deleting) 1f else LinearOutSlowInEasing.transform((pull / 0.1f).coerceIn(0f, 1f))
                val bump = 1f + 0.3f * sin(PI.toFloat() * phase(e, 0.62f, 0.84f))
                val vanish = 1f - FastOutSlowInEasing.transform(phase(e, 0.86f, 1f))
                val lid = if (state.deleting) 1f - phase(e, 0.58f, 0.68f) else (pull / DeleteThreshold).coerceIn(0f, 1f)
                drawBin(bin, BinSize.toPx() * appear * bump * vanish, lid)
            },
    ) {
        MessageRow(message, Modifier.graphicsLayer { alpha = 1f - phase(warp(state), 0.8f, 1f) })
    }
}

/** 0 = untouched row, 1 = fully swallowed by the bin. */
private fun warp(state: SwipeRowState): Float {
    if (state.width <= 0f) return 0f
    fun preview(drag: Float) = (drag / state.width / DeleteThreshold * PreviewWarp).coerceIn(0f, PreviewWarp * 1.25f)
    if (!state.deleting) return preview(state.drag.value)
    return lerp(preview(state.releasedDrag), 1f, FastOutLinearInEasing.transform(phase(state.exit.value, 0f, 0.62f)))
}

/** The row's outline pinched towards the bin: the left end narrows first, like a genie's tail. */
private fun geniePath(w: Float, h: Float, g: Float): Path {
    val cy = h / 2
    val left = h / 2 * (1f - 0.8f * (g * 2.2f).coerceAtMost(1f))
    val right = h / 2 * (1f - 0.45f * g)
    return Path().apply {
        moveTo(0f, cy - left)
        cubicTo(w * 0.45f, cy - left, w * 0.55f, cy - right, w, cy - right)
        lineTo(w, cy + right)
        cubicTo(w * 0.55f, cy + right, w * 0.45f, cy + left, 0f, cy + left)
        close()
    }
}

/** A filled red bin of [size] centred on [center]; [lid] 0→1 lifts the lid on its left hinge. */
private fun DrawScope.drawBin(center: Offset, size: Float, lid: Float) {
    if (size <= 0f) return
    val left = center.x - size / 2
    val top = center.y - size / 2
    fun at(x: Float, y: Float) = Offset(left + size * x, top + size * y)
    drawCircle(MenuColors.Screen, size * 0.78f, center, alpha = 0.85f)
    val body = Path().apply {
        moveTo(left + size * 0.2f, top + size * 0.36f)
        lineTo(left + size * 0.27f, top + size * 0.92f)
        lineTo(left + size * 0.73f, top + size * 0.92f)
        lineTo(left + size * 0.8f, top + size * 0.36f)
        close()
    }
    drawPath(body, DeleteRed)
    val rib = size * 0.06f
    drawLine(Color.White.copy(alpha = 0.75f), at(0.42f, 0.5f), at(0.43f, 0.8f), rib, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.75f), at(0.58f, 0.5f), at(0.57f, 0.8f), rib, StrokeCap.Round)
    rotate(-50f * lid, pivot = at(0.12f, 0.3f)) {
        drawRoundRect(LidRed, at(0.4f, 0.13f), Size(size * 0.2f, size * 0.12f), CornerRadius(size * 0.04f))
        drawRoundRect(DeleteRedLight, at(0.12f, 0.22f), Size(size * 0.76f, size * 0.1f), CornerRadius(size * 0.05f))
    }
}
