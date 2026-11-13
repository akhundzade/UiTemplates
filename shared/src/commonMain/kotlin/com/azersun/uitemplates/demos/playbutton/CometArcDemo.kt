package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val DegreesPerSecond = 300f
private const val MaxTail = 130f
private const val TailSegments = 24
private val OrbitGap = 9.dp

/** Room for the orbit and its glow around the 64 dp button. */
private val CometCanvas = 120.dp
private val HeadColor = Color(0xFFCFF6FF)

@Composable
fun CometArcDemo() {
    val state = rememberPlayerState()
    val speed = remember { Animatable(0f) }
    val presence = remember { Animatable(0f) }
    var angle by remember { mutableFloatStateOf(-90f) }

    LaunchedEffect(state.playing) {
        val playing = state.playing
        val target = if (playing) 1f else 0f
        launch {
            speed.animateTo(target, tween(if (playing) 600 else 1200, easing = if (playing) FastOutSlowInEasing else LinearOutSlowInEasing))
        }
        launch { presence.animateTo(target, tween(if (playing) 300 else 1200)) }
        var last = withFrameNanos { it }
        while (playing || presence.value > 0f) {
            withFrameNanos { now ->
                angle = (angle + DegreesPerSecond * speed.value * (now - last) / 1e9f) % 360f
                last = now
            }
        }
    }

    PlayerScene(
        state = state,
        number = 5,
        title = "Comet Arc",
        caption = "While playing a glowing comet circles the button; on pause it slows down and fades away.",
    ) {
        PlayButtonFace(state.playing)
        // Tail colours are mixed opaque against the card so overlapping segments don't show seams;
        // the whole comet then fades as one offscreen layer.
        Spacer(
            Modifier
                .align(Alignment.Center)
                .requiredSize(CometCanvas)
                .graphicsLayer {
                    alpha = presence.value
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val c = size.center
                    val radius = ButtonSize.toPx() / 2 + OrbitGap.toPx()
                    val topLeft = Offset(c.x - radius, c.y - radius)
                    val arc = Size(radius * 2, radius * 2)
                    val strokes = Array(TailSegments) { k ->
                        Stroke(3.2.dp.toPx() * (1f - k / TailSegments.toFloat()) + 0.6.dp.toPx(), cap = StrokeCap.Round)
                    }
                    val colors = Array(TailSegments) { k ->
                        val f = 1f - k / TailSegments.toFloat()
                        lerp(MenuColors.Tile, lerp(HeadColor, PlayViolet, 1f - f), f * f)
                    }
                    val glow = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                    val track = Stroke(1.dp.toPx())
                    val halo = 8.dp.toPx()
                    val head = 3.2.dp.toPx()
                    onDrawBehind {
                        val p = presence.value
                        if (p <= 0f) return@onDrawBehind
                        val a = angle
                        val sweep = MaxTail * (0.25f + 0.75f * speed.value)
                        val step = sweep / TailSegments
                        drawCircle(Color.White, radius, c, alpha = 0.06f, style = track)
                        drawArc(MenuColors.Cyan, a - sweep * 0.45f, sweep * 0.45f, false, topLeft, arc, alpha = 0.14f, style = glow)
                        for (k in TailSegments - 1 downTo 0) {
                            drawArc(colors[k], a - step * (k + 1), step, false, topLeft, arc, style = strokes[k])
                        }
                        val rad = a * PI.toFloat() / 180f
                        val tip = Offset(c.x + cos(rad) * radius, c.y + sin(rad) * radius)
                        drawCircle(MenuColors.Cyan, halo, tip, alpha = 0.32f)
                        drawCircle(Color.White, head, tip)
                    }
                },
        )
    }
}
