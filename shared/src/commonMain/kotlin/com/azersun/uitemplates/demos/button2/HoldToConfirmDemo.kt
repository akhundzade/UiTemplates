package com.azersun.uitemplates.demos.button2

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val HoldMillis = 1400
private const val DrainMillis = 450
private val ButtonSize = 96.dp
private val RingGap = 9.dp
private val RingWidth = 5.dp
private val ButtonDark = Color(0xFF2A2650)

@Composable
fun HoldToConfirmDemo() {
    // Kept apart so cancelling the autoplay never releases a finger that is really holding.
    var userHold by remember { mutableStateOf(false) }
    var autoHold by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    val holding = (userHold || autoHold) && !confirmed
    val progress = remember { Animatable(0f) }
    val done = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val press by animateFloatAsState(if (holding) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "press")
    val scope = rememberCoroutineScope()

    LaunchedEffect(holding) {
        if (holding) {
            // Resumes from wherever a drain left the ring, at the same fill rate.
            val remaining = ((1f - progress.value) * HoldMillis).roundToInt()
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            confirmed = true
        } else if (!confirmed) {
            progress.animateTo(0f, tween((progress.value * DrainMillis).roundToInt(), easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(confirmed) {
        if (!confirmed) return@LaunchedEffect
        launch { done.animateTo(1f, tween(520)) }
        pop.animateTo(1.12f, tween(130))
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }

    ButtonStage(
        1,
        "Hold to Confirm",
        "Hold it down: a ring fills around the button, then it turns into a ✓.",
        onReplay = {
            confirmed = false
            scope.launch {
                progress.snapTo(0f)
                pop.snapTo(1f)
                done.animateTo(0f, tween(300))
            }
        },
        autoplay = {
            try {
                autoHold = true
                delay(HoldMillis + 150L)
            } finally {
                autoHold = false
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(ButtonSize + (RingGap + RingWidth) * 2 + 8.dp)
                    .drawWithCache {
                        val width = RingWidth.toPx()
                        val baseRadius = (ButtonSize / 2 + RingGap).toPx() + width / 2
                        val track = Stroke(width)
                        val halo = Stroke(width * 2.6f, cap = StrokeCap.Round)
                        val arc = Stroke(width, cap = StrokeCap.Round)
                        onDrawBehind {
                            val d = done.value
                            val sweep = 360f * progress.value
                            val ringAlpha = 1f - smoothstep(0.25f, 0.75f, d)
                            if (ringAlpha <= 0f) return@onDrawBehind
                            val radius = baseRadius + 8.dp.toPx() * d
                            val topLeft = center - Offset(radius, radius)
                            val arcSize = Size(radius * 2, radius * 2)
                            drawCircle(Color.White, radius, alpha = 0.08f * ringAlpha, style = track)
                            if (sweep > 0f) {
                                drawArc(MenuColors.Blue, -90f, sweep, false, topLeft, arcSize, alpha = 0.25f * ringAlpha, style = halo)
                                drawArc(MenuColors.Blue, -90f, sweep, false, topLeft, arcSize, alpha = ringAlpha, style = arc)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(ButtonSize)
                        .graphicsLayer {
                            val s = pop.value * (1f - 0.06f * press)
                            scaleX = s
                            scaleY = s
                        }
                        .shadow(18.dp, CircleShape, ambientColor = MenuColors.Blue, spotColor = MenuColors.Blue)
                        .clip(CircleShape)
                        .drawBehind {
                            val fill = smoothstep(0f, 0.4f, done.value)
                            drawCircle(lerp(ButtonDark, MenuColors.Blue, fill))
                            val border = 1.dp.toPx()
                            drawCircle(MenuColors.CardBorder, size.minDimension / 2 - border / 2, alpha = 1f - fill, style = Stroke(border))
                        }
                        .semantics {
                            role = Role.Button
                            onClick(label = "Delete") {
                                confirmed = true
                                true
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown().consume()
                                try {
                                    userHold = true
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        if (event.changes.none { it.pressed }) break
                                    }
                                } finally {
                                    userHold = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(36.dp)) {
                        val d = done.value
                        val trash = 1f - smoothstep(0f, 0.3f, d)
                        if (trash > 0f) {
                            scale(0.6f + 0.4f * trash) { drawTrash(MenuColors.Text.copy(alpha = trash)) }
                        }
                        drawCheck(smoothstep(0.3f, 0.9f, d), Color.White, Offset.Zero, size.width, size.width * 0.11f)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Crossfade(confirmed, label = "label") { isDone ->
                Text(
                    if (isDone) "Deleted" else "Hold to delete",
                    color = if (isDone) MenuColors.Text else MenuColors.Muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun DrawScope.drawTrash(color: Color) {
    val w = size.width
    val stroke = w * 0.08f
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(w * x1, w * y1), Offset(w * x2, w * y2), stroke, StrokeCap.Round)
    line(0.16f, 0.26f, 0.84f, 0.26f)
    line(0.4f, 0.26f, 0.4f, 0.14f)
    line(0.4f, 0.14f, 0.6f, 0.14f)
    line(0.6f, 0.14f, 0.6f, 0.26f)
    line(0.25f, 0.36f, 0.3f, 0.86f)
    line(0.3f, 0.86f, 0.7f, 0.86f)
    line(0.7f, 0.86f, 0.75f, 0.36f)
    line(0.43f, 0.46f, 0.44f, 0.74f)
    line(0.57f, 0.46f, 0.56f, 0.74f)
}
