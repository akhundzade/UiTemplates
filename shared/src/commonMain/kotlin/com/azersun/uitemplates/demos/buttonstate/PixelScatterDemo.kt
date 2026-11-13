package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val ScatterMillis = 1100
private val PixelSize = 4.dp

/** Share of the timeline the break-up takes to spread from the tap point to the far end. */
private const val SpreadSpan = 0.4f

/** Share of the timeline each pixel spends flying away. */
private const val PixelLife = 0.55f

@Composable
fun PixelScatterDemo() {
    var face by remember { mutableStateOf(StartState) }
    var shatter by remember { mutableStateOf<Shatter?>(null) }
    val progress = remember { Animatable(0f) }
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current

    ButtonStateScene(
        number = 4,
        title = "Pixel Scatter",
        caption = "The old state breaks into tiny pixels that fly off from your tap and fade, revealing the new state underneath.",
        onToggle = { _, to, tap ->
            val image = layer.toImageBitmap()
            shatter = Shatter(image, tap, with(density) { PixelSize.roundToPx() }.coerceAtLeast(1), density.density)
            face = to
            progress.snapTo(0f)
            progress.animateTo(1f, tween(ScatterMillis, easing = LinearEasing))
            shatter = null
        },
    ) {
        PillFace(
            face,
            Modifier.drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            },
        )
        Canvas(Modifier.matchParentSize()) {
            val t = progress.value
            shatter?.let { drawShatter(it, t) }
        }
    }
}

/** Per-pixel flight, fixed once per tap so drawing allocates nothing per frame. */
private class Shatter(val image: ImageBitmap, tap: Offset, val cell: Int, density: Float) {
    val cols = (image.width + cell - 1) / cell
    val rows = (image.height + cell - 1) / cell
    val start = FloatArray(cols * rows)
    val driftX = FloatArray(cols * rows)
    val driftY = FloatArray(cols * rows)

    init {
        val random = Random(tap.x.toInt() * 31 + tap.y.toInt())
        val far = maxOf(
            tap.getDistance(),
            (tap - Offset(image.width.toFloat(), 0f)).getDistance(),
            (tap - Offset(0f, image.height.toFloat())).getDistance(),
            (tap - Offset(image.width.toFloat(), image.height.toFloat())).getDistance(),
        )
        for (i in start.indices) {
            val dx = ((i % cols) + 0.5f) * cell - tap.x
            val dy = ((i / cols) + 0.5f) * cell - tap.y
            val distance = sqrt(dx * dx + dy * dy)
            start[i] = distance / far * SpreadSpan + random.nextFloat() * 0.05f
            // Away from the tap, fanned out a little so the pixels don't fly in neat rays.
            val base = if (distance < 1f) random.nextFloat() * 6.28f else atan2(dy, dx)
            val angle = base + (random.nextFloat() - 0.5f) * 0.9f
            val speed = (26f + random.nextFloat() * 54f) * density
            driftX[i] = cos(angle) * speed
            driftY[i] = sin(angle) * speed
        }
    }
}

private fun DrawScope.drawShatter(shatter: Shatter, t: Float) {
    val g = shatter.cell
    val gravity = 34.dp.toPx()
    for (i in shatter.start.indices) {
        val p = ((t - shatter.start[i]) / PixelLife).coerceIn(0f, 1f)
        if (p >= 1f) continue
        val sx = (i % shatter.cols) * g
        val sy = (i / shatter.cols) * g
        val w = min(g, shatter.image.width - sx)
        val h = min(g, shatter.image.height - sy)
        val ease = 1f - (1f - p) * (1f - p)
        val x = sx + shatter.driftX[i] * ease
        val y = sy + shatter.driftY[i] * ease + gravity * p * p
        val scale = 1f - 0.45f * p
        drawImage(
            shatter.image,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(w, h),
            dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
            dstSize = IntSize(max(1, (w * scale).roundToInt()), max(1, (h * scale).roundToInt())),
            alpha = 1f - p,
        )
    }
}
