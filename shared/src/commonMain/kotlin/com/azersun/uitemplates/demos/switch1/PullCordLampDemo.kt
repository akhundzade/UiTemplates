package com.azersun.uitemplates.demos.switch1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Lengths in dp.
private const val CordRest = 84f
private const val MaxPull = 110f
private const val PullThreshold = 28f
private const val ScriptedPull = 64f
private const val GrabRadius = 48f
// How far the lamp tilts, in degrees, when the cord is fully pulled.
private const val MaxTilt = 6f

private val Warm = Color(0xFFFFD37A)
private val BulbOff = Color(0xFF4A4670)
private val BulbOn = Color(0xFFFFEDB0)
private val Wire = Color(0xFF5A5590)
private val Cord = Color(0xFFB9B4E0)
private val Pull = Color(0xFFE8B95A)
private val ShadeBrushColors = listOf(Color(0xFF4A4590), Color(0xFF2B2760))

/** Where everything hangs for a canvas of the given size; shared by drawing and hit-testing. */
private class LampGeometry(width: Float, height: Float, private val dp: Float) {
    val pivot = Offset(width / 2, 0f)
    val shadeTop = min(height * 0.2f, 130f * dp)
    val rim = shadeTop + 66f * dp
    val bulb = Offset(width / 2, rim)
    private val anchor = Offset(width / 2 + 42f * dp, rim - 2f * dp)

    /** The cord's top end once the lamp has swung by [tilt] degrees around the ceiling. */
    fun anchorAt(tilt: Float): Offset {
        val angle = tilt * PI.toFloat() / 180f
        val dx = anchor.x - pivot.x
        val dy = anchor.y - pivot.y
        return Offset(pivot.x + dx * cos(angle) - dy * sin(angle), pivot.y + dx * sin(angle) + dy * cos(angle))
    }

    /** The top of the pull: the cord hangs straight down from its anchor, whatever the lamp's tilt. */
    fun pullAt(tilt: Float, pull: Float): Offset = anchorAt(tilt) + Offset(0f, (CordRest + pull).coerceAtLeast(12f) * dp)
}

@Composable
fun PullCordLampDemo() {
    var on by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    val pull = remember { Animatable(0f) }
    val tilt = remember { Animatable(0f) }
    val light = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var lightJob by remember { mutableStateOf<Job?>(null) }

    fun hitsPull(position: Offset, width: Float, height: Float, density: Float): Boolean {
        val tip = LampGeometry(width, height, density).pullAt(tilt.value, pull.value)
        return (position - (tip + Offset(0f, 12f * density))).getDistance() < GrabRadius * density
    }

    fun release(pulled: Float) {
        if (pulled >= PullThreshold) {
            on = !on
            lightJob?.cancel()
            lightJob = scope.launch {
                if (on) {
                    delay(90)
                    light.animateTo(0.85f, tween(40))
                    light.animateTo(0.1f, tween(70))
                    light.animateTo(0.9f, tween(50))
                    light.animateTo(0.2f, tween(90))
                    light.animateTo(1f, tween(240))
                } else {
                    delay(60)
                    light.animateTo(0f, tween(160))
                }
            }
        }
        scope.launch { pull.animateTo(0f, spring(dampingRatio = 0.28f, stiffness = 520f)) }
        scope.launch {
            tilt.animateTo(
                0f,
                spring(dampingRatio = 0.13f, stiffness = 42f),
                initialVelocity = -pulled / MaxPull * 30f,
            )
        }
    }

    suspend fun scriptedPull() {
        coroutineScope {
            launch { tilt.animateTo(ScriptedPull / MaxPull * MaxTilt, tween(420, easing = FastOutSlowInEasing)) }
            pull.animateTo(ScriptedPull, tween(420, easing = FastOutSlowInEasing))
        }
        delay(120)
        // A finger may have grabbed the cord meanwhile; its own release decides then.
        if (!holding) release(ScriptedPull)
    }

    CheckboxStage(
        4,
        "Pull Cord Lamp",
        "Pull the cord: it springs back, the bulb flickers on and the lamp sways.",
        autoplay = { scope.launch { scriptedPull() } },
    ) {
        Box(Modifier.fillMaxSize()) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { start ->
                                holding = hitsPull(start, size.width.toFloat(), size.height.toFloat(), density)
                                dragged = pull.value
                            },
                            onDragEnd = {
                                if (holding) release(dragged)
                                holding = false
                            },
                            onDragCancel = {
                                if (holding) release(dragged)
                                holding = false
                            },
                        ) { change, dy ->
                            if (!holding) return@detectVerticalDragGestures
                            change.consume()
                            dragged = (dragged + dy / density).coerceIn(0f, MaxPull)
                            val next = dragged
                            scope.launch {
                                pull.snapTo(next)
                                tilt.snapTo(next / MaxPull * MaxTilt)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { position ->
                            if (hitsPull(position, size.width.toFloat(), size.height.toFloat(), density)) {
                                scope.launch { scriptedPull() }
                            }
                        }
                    }
                    .drawWithCache {
                        val dp = density
                        val lamp = LampGeometry(size.width, size.height, dp)
                        val cx = size.width / 2
                        val top = lamp.shadeTop
                        val rim = lamp.rim
                        val shade = Path().apply {
                            moveTo(cx - 15f * dp, top)
                            lineTo(cx + 15f * dp, top)
                            cubicTo(cx + 38f * dp, top + 6f * dp, cx + 70f * dp, top + 34f * dp, cx + 74f * dp, rim)
                            lineTo(cx - 74f * dp, rim)
                            cubicTo(cx - 70f * dp, top + 34f * dp, cx - 38f * dp, top + 6f * dp, cx - 15f * dp, top)
                            close()
                        }
                        val shadeBrush = Brush.verticalGradient(ShadeBrushColors, top, rim)
                        val cone = Path().apply {
                            moveTo(cx - 66f * dp, rim)
                            lineTo(cx + 66f * dp, rim)
                            lineTo(cx + size.width * 0.62f, size.height)
                            lineTo(cx - size.width * 0.62f, size.height)
                            close()
                        }
                        val coneBrush = Brush.verticalGradient(listOf(Warm.copy(alpha = 0.42f), Warm.copy(alpha = 0f)), rim, size.height)
                        val glowRadius = 120f * dp
                        val glowBrush = Brush.radialGradient(listOf(Warm.copy(alpha = 0.55f), Warm.copy(alpha = 0f)), lamp.bulb, glowRadius)
                        // A small bulb-shaped pull, its tip at the origin.
                        val drop = Path().apply {
                            moveTo(0f, 0f)
                            cubicTo(3f * dp, 5f * dp, 8f * dp, 9f * dp, 8f * dp, 15f * dp)
                            cubicTo(8f * dp, 19.5f * dp, 4.4f * dp, 23f * dp, 0f, 23f * dp)
                            cubicTo(-4.4f * dp, 23f * dp, -8f * dp, 19.5f * dp, -8f * dp, 15f * dp)
                            cubicTo(-8f * dp, 9f * dp, -3f * dp, 5f * dp, 0f, 0f)
                            close()
                        }
                        onDrawBehind {
                            val angle = tilt.value
                            val stretch = pull.value
                            val l = light.value
                            drawRoundRect(Wire, Offset(cx - 26f * dp, -6f * dp), Size(52f * dp, 14f * dp), CornerRadius(6f * dp))
                            rotate(angle, lamp.pivot) {
                                if (l > 0f) {
                                    drawPath(cone, coneBrush, alpha = l)
                                    drawCircle(glowBrush, glowRadius, lamp.bulb, alpha = l)
                                }
                                drawLine(Wire, lamp.pivot, Offset(cx, top), 2f * dp)
                                drawCircle(lerp(BulbOff, BulbOn, l), 16f * dp, lamp.bulb)
                                drawPath(shade, shadeBrush)
                                drawRoundRect(Color(0xFF22204A), Offset(cx - 12f * dp, top - 8f * dp), Size(24f * dp, 10f * dp), CornerRadius(3f * dp))
                                drawLine(Color(0xFF6B64B8), Offset(cx - 73f * dp, rim), Offset(cx + 73f * dp, rim), 3f * dp, StrokeCap.Round)
                                if (l > 0f) drawLine(Warm, Offset(cx - 70f * dp, rim), Offset(cx + 70f * dp, rim), 2f * dp, StrokeCap.Round, alpha = l)
                            }
                            val anchor = lamp.anchorAt(angle)
                            val tip = lamp.pullAt(angle, stretch)
                            drawLine(Cord, anchor, tip, 1.5f * dp)
                            translate(tip.x, tip.y) {
                                drawPath(drop, Pull)
                                drawCircle(Color.White, 2.5f * dp, Offset(-3f * dp, 14f * dp), alpha = 0.35f)
                            }
                        }
                    },
            )
            Text(
                "Pull the cord",
                color = SwitchColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            )
        }
    }
}
