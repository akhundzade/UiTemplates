package com.azersun.uitemplates.demos.error2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val GlitchMillis = 650

/** The jitter changes in steps, like dropped video frames, rather than smoothly. */
private const val GlitchFrames = 16
private const val Slices = 7

private val CodeStyle = TextStyle(fontSize = 104.sp, fontWeight = FontWeight.Black, letterSpacing = (-3).sp)
private val SplitRed = Color(0xFFFF2A55)
private val SplitCyan = Color(0xFF00E5FF)

@Composable
fun GlitchDemo() {
    val measurer = rememberTextMeasurer()
    val okLayout = remember(measurer) { measurer.measure("200", CodeStyle) }
    val errorLayout = remember(measurer) { measurer.measure("500", CodeStyle) }
    var failed by remember { mutableStateOf(false) }
    val glitch = remember { Animatable(1f) }
    val showsFailure by remember { derivedStateOf { if (glitch.value < 0.5f) !failed else failed } }
    val scope = rememberCoroutineScope()
    val canvasHeight = with(LocalDensity.current) { okLayout.size.height.toDp() }

    fun toggle() {
        if (glitch.isRunning) return
        failed = !failed
        scope.launch {
            glitch.snapTo(0f)
            glitch.animateTo(1f, tween(GlitchMillis, easing = LinearEasing))
        }
    }

    ErrorStage(
        4,
        "Glitch",
        "\"200 OK\" glitches with RGB split and sliced lines into a red \"500\".",
        autoplay = { toggle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClickLabel = if (failed) "Retry request" else "Send request",
                    role = Role.Button,
                    onClick = ::toggle,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("GET /api/orders", color = ErrorColors.Muted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Canvas(Modifier.fillMaxWidth().height(canvasHeight)) {
                val p = glitch.value
                val target = if (failed) errorLayout else okLayout
                val targetColor = if (failed) ErrorColors.Red else ErrorColors.Text
                if (p >= 1f) {
                    drawText(target, targetColor, topLeft = centred(target))
                } else {
                    drawGlitch(p, previous = if (failed) okLayout else errorLayout, target = target)
                }
            }
            Text(
                if (showsFailure) "Server error" else "OK",
                color = if (showsFailure) ErrorColors.Red else ErrorColors.Cyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer {
                    val p = glitch.value
                    if (p < 1f) {
                        val frame = (p * GlitchFrames).toInt()
                        translationX = (noise(frame, 97) * 2f - 1f) * 10.dp.toPx() * intensity(p)
                        alpha = if (noise(frame, 41) < 0.2f) 0.4f else 1f
                    } else {
                        translationX = 0f
                        alpha = 1f
                    }
                },
            )
            Text(
                if (failed) "Tap to retry" else "Tap to send the request",
                color = ErrorColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 26.dp),
            )
        }
    }
}

private fun intensity(progress: Float): Float = 0.4f + 0.6f * sin(progress * PI.toFloat())

private fun DrawScope.centred(layout: TextLayoutResult): Offset =
    Offset((size.width - layout.size.width) / 2f, (size.height - layout.size.height) / 2f)

/**
 * Draws the code as horizontal slices that jump sideways, each a red and a cyan copy pulled apart and
 * added together (so they read white where they overlap). Slices switch from [previous] to [target]
 * as [progress] runs, so the new number shows through more and more of the bands.
 */
private fun DrawScope.drawGlitch(progress: Float, previous: TextLayoutResult, target: TextLayoutResult) {
    val frame = (progress * GlitchFrames).toInt()
    val strength = intensity(progress)
    val split = 6.dp.toPx() * strength * (0.5f + noise(frame, 3))
    val maxShift = 24.dp.toPx() * strength
    val band = size.height / Slices
    repeat(Slices) { k ->
        val shifted = noise(frame, k + 11) < 0.55f
        val dx = if (shifted) (noise(frame, k + 31) * 2f - 1f) * maxShift else 0f
        val layout = if (noise(frame, k + 67) < progress) target else previous
        val topLeft = centred(layout)
        clipRect(top = band * k, bottom = band * (k + 1)) {
            translate(left = dx) {
                drawText(layout, SplitRed, topLeft = topLeft - Offset(split, 0f), blendMode = BlendMode.Plus)
                drawText(layout, SplitCyan, topLeft = topLeft + Offset(split, 0f), blendMode = BlendMode.Plus)
            }
        }
    }
    repeat(2) { k ->
        if (noise(frame, k + 89) < 0.6f) {
            val y = noise(frame, k + 101) * size.height
            val width = size.width * (0.2f + 0.5f * noise(frame, k + 113))
            val x = noise(frame, k + 127) * (size.width - width)
            drawRect(
                if (k == 0) SplitCyan else SplitRed,
                topLeft = Offset(x, y),
                size = Size(width, 2.dp.toPx() + 3.dp.toPx() * noise(frame, k + 131)),
                alpha = 0.6f * strength,
            )
        }
    }
}

/** A stable pseudo-random value in [0, 1) for a frame and a channel, without allocating a Random. */
private fun noise(frame: Int, channel: Int): Float {
    var h = frame * 374761393 + channel * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777216f
}
