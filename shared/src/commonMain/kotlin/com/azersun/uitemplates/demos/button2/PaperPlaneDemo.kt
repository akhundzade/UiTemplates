package com.azersun.uitemplates.demos.button2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

private val PillWidth = 172.dp
private val PillHeight = 58.dp
private val PlaneSize = 22.dp

// From the pill's right edge to the plane's resting centre.
private val SlotInset = 30.dp
private const val TrailDots = 24
private const val FlightMillis = 780
private const val ReturnMillis = 650
private const val SentHoldMillis = 2000L
private val SendGradient = Brush.horizontalGradient(listOf(MenuColors.Blue, MenuColors.Violet))

// Dips below zero first, so the plane pulls back a touch before it launches.
private val TakeoffEasing = CubicBezierEasing(0.45f, -0.35f, 0.75f, 1f)

// Cubic flight curve up and to the right, in dp from the resting slot.
private val FlightX = floatArrayOf(0f, 40f, 120f, 250f)
private val FlightY = floatArrayOf(0f, -24f, -110f, -280f)

// Quadratic return curve in from the left, landing in the slot.
private val ReturnX = floatArrayOf(-210f, -95f, 0f)
private val ReturnY = floatArrayOf(50f, -75f, 0f)

private val RestHeading = degrees(atan2(FlightY[1] - FlightY[0], FlightX[1] - FlightX[0]))

@Composable
fun PaperPlaneDemo() {
    val fly = remember { Animatable(0f) }
    val back = remember { Animatable(1f) }
    val trail = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    var returning by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val showSent by remember { derivedStateOf { flip.value >= 0.5f } }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 700f), label = "press")
    val scope = rememberCoroutineScope()

    fun send() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                returning = false
                fly.snapTo(0f)
                trail.snapTo(1f)
                launch {
                    delay(140)
                    flip.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                }
                fly.animateTo(1f, tween(FlightMillis, easing = TakeoffEasing))
                trail.animateTo(0f, tween(450))
                delay(SentHoldMillis - FlightMillis - 450)
                back.snapTo(0f)
                returning = true
                launch {
                    delay(250)
                    flip.animateTo(0f, tween(460, easing = FastOutSlowInEasing))
                }
                back.animateTo(1f, tween(ReturnMillis, easing = FastOutSlowInEasing))
                returning = false
                fly.snapTo(0f)
            } finally {
                busy = false
            }
        }
    }

    ButtonStage(
        2,
        "Paper Plane",
        "The paper plane flies off with a trail and the label flips to \"Sent!\".",
        autoplay = { send() },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val u = 1.dp.toPx()
                    val half = PlaneSize.toPx() / 2
                    // Nose along +x; the two wings are shaded apart like folded paper.
                    val upperWing = Path().apply {
                        moveTo(half, 0f)
                        lineTo(-half, -half * 0.88f)
                        lineTo(-half * 0.42f, 0f)
                        close()
                    }
                    val lowerWing = Path().apply {
                        moveTo(half, 0f)
                        lineTo(-half * 0.42f, 0f)
                        lineTo(-half, half * 0.88f)
                        close()
                    }
                    val slot = Offset(size.width / 2 + PillWidth.toPx() / 2 - SlotInset.toPx(), size.height / 2)
                    val dot = 1.8f * u
                    onDrawWithContent {
                        drawContent()
                        val f = fly.value
                        val tr = trail.value
                        if (tr > 0f) {
                            for (i in 1..TrailDots) {
                                val s = i / TrailDots.toFloat()
                                if (s > f) break
                                val alpha = tr * (1f - (f - s) / 0.55f).coerceIn(0f, 1f)
                                if (alpha <= 0f) continue
                                val at = slot + Offset(cubic(FlightX, s), cubic(FlightY, s)) * u
                                drawCircle(Color.White, dot, at, alpha = 0.75f * alpha)
                            }
                        }
                        val position: Offset
                        val heading: Float
                        val scale: Float
                        val alpha: Float
                        if (returning) {
                            val b = back.value
                            position = slot + Offset(quadratic(ReturnX, b), quadratic(ReturnY, b)) * u
                            val travel = degrees(atan2(quadraticSlope(ReturnY, b), quadraticSlope(ReturnX, b)))
                            heading = mix(travel, RestHeading, smoothstep(0.7f, 1f, b))
                            scale = 0.8f + 0.2f * b
                            alpha = smoothstep(0f, 0.2f, b)
                        } else {
                            position = slot + Offset(cubic(FlightX, f), cubic(FlightY, f)) * u
                            heading = degrees(atan2(cubicSlope(FlightY, f), cubicSlope(FlightX, f)))
                            scale = 1f - 0.35f * f.coerceIn(0f, 1f)
                            alpha = 1f - smoothstep(0.75f, 1f, f)
                        }
                        if (alpha > 0f) {
                            translate(position.x, position.y) {
                                rotate(heading, Offset.Zero) {
                                    scale(scale, scale, Offset.Zero) {
                                        drawPath(upperWing, Color.White, alpha = alpha)
                                        drawPath(lowerWing, Color.White, alpha = 0.72f * alpha)
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(PillWidth, PillHeight)
                    .graphicsLayer {
                        scaleX = press
                        scaleY = press
                    }
                    .shadow(16.dp, CircleShape, ambientColor = MenuColors.Violet, spotColor = MenuColors.Violet)
                    .clip(CircleShape)
                    .drawWithCache { onDrawBehind { drawRect(SendGradient) } }
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClickLabel = "Send",
                        role = Role.Button,
                        onClick = { send() },
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxHeight().width(PillWidth - SlotInset * 2 + 6.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (showSent) "Sent!" else "Send",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer {
                            val v = flip.value
                            rotationX = if (v < 0.5f) v * 180f else v * 180f - 180f
                            cameraDistance = 12f * density
                        },
                    )
                }
            }
        }
    }
}

private fun degrees(radians: Float): Float = radians * 180f / PI.toFloat()

private fun cubic(p: FloatArray, t: Float): Float {
    val m = 1f - t
    return m * m * m * p[0] + 3f * m * m * t * p[1] + 3f * m * t * t * p[2] + t * t * t * p[3]
}

private fun cubicSlope(p: FloatArray, t: Float): Float {
    val m = 1f - t
    return 3f * m * m * (p[1] - p[0]) + 6f * m * t * (p[2] - p[1]) + 3f * t * t * (p[3] - p[2])
}

private fun quadratic(p: FloatArray, t: Float): Float {
    val m = 1f - t
    return m * m * p[0] + 2f * m * t * p[1] + t * t * p[2]
}

private fun quadraticSlope(p: FloatArray, t: Float): Float = 2f * (1f - t) * (p[1] - p[0]) + 2f * t * (p[2] - p[1])
