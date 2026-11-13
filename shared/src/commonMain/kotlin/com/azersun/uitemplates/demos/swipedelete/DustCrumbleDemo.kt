package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val GrainSize = 4.dp

/** Share of the timeline each grain spends drifting away. */
private const val GrainLife = 0.42f

/** Share of the timeline the crumbling front takes to sweep from the right edge to the left. */
private const val SweepSpan = 0.5f
private const val SweepJitter = 0.08f

/** The row only follows the finger part-way, so the crumble starts close to where it sat. */
private const val Resistance = 0.4f

@Composable
fun DustCrumbleDemo() {
    SwipeDeleteScene(
        number = 4,
        title = "Dust Crumble",
        caption = "The row crumbles into sand from right to left and blows away.",
        spec = SwipeSpec(exitMillis = 1500, collapseAt = 0.82f),
    ) { message, state -> DustRow(message, state) }
}

@Composable
private fun DustRow(message: Message, state: SwipeRowState) {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val grainPx = with(density) { GrainSize.roundToPx() }.coerceAtLeast(1)
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    val sand = remember(snapshot) { snapshot?.let { Sand(it.width, it.height, grainPx, density.density) } }

    LaunchedEffect(state.deleting) {
        if (state.deleting) snapshot = layer.toImageBitmap()
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .graphicsLayer { translationX = -rowOffset(state) }
            .drawWithContent {
                val e = state.exit.value
                val image = snapshot
                if (image != null && sand != null) {
                    drawSand(image, sand, e)
                } else {
                    // Keep the last recording intact once deleting starts, while it is being snapshotted.
                    if (!state.deleting) layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
            },
    ) {
        MessageRow(message)
    }
}

private fun rowOffset(state: SwipeRowState): Float {
    if (!state.deleting) return state.drag.value * Resistance
    val back = FastOutSlowInEasing.transform(phase(state.exit.value, 0f, 0.2f))
    return state.releasedDrag * Resistance * (1f - back)
}

/** Per-grain timing and drift, fixed once per snapshot so drawing allocates nothing per frame. */
private class Sand(val width: Int, val height: Int, val grain: Int, density: Float) {
    val cols = (width + grain - 1) / grain
    val rows = (height + grain - 1) / grain
    val start = FloatArray(cols * rows)
    val driftX = FloatArray(cols * rows)
    val driftY = FloatArray(cols * rows)
    val wobble = FloatArray(cols * rows)

    init {
        val random = Random(7)
        for (i in start.indices) {
            val fromRight = 1f - ((i % cols) + 0.5f) / cols
            start[i] = fromRight * SweepSpan + random.nextFloat() * SweepJitter
            driftX[i] = (14f + random.nextFloat() * 40f) * density
            driftY[i] = (20f + random.nextFloat() * 56f) * density
            wobble[i] = random.nextFloat() * 6.28f
        }
    }
}

private fun DrawScope.drawSand(image: ImageBitmap, sand: Sand, e: Float) {
    val g = sand.grain
    for (i in sand.start.indices) {
        val p = ((e - sand.start[i]) / GrainLife).coerceIn(0f, 1f)
        if (p >= 1f) continue
        val sx = (i % sand.cols) * g
        val sy = (i / sand.cols) * g
        val w = min(g, sand.width - sx)
        val h = min(g, sand.height - sy)
        val ease = 1f - (1f - p) * (1f - p)
        val x = sx + sand.driftX[i] * ease + sin(sand.wobble[i] + p * 6f) * g * 0.6f * p
        val y = sy - sand.driftY[i] * ease
        val scale = 1f - 0.5f * p
        drawImage(
            image,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(w, h),
            dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
            dstSize = IntSize(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt())),
            alpha = 1f - p,
        )
    }
}
