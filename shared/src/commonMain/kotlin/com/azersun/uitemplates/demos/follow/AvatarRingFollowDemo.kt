package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val RingGap = 5.dp
private val RingStroke = 3.dp
private val RingBrush = Brush.sweepGradient(listOf(MenuColors.Violet, MenuColors.Cyan, MenuColors.Violet))

@Composable
fun AvatarRingFollowDemo() {
    val state = remember { FollowState() }
    val sweep = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(state.toggles) {
        if (!state.following) {
            ring.animateTo(0f, tween(150))
            return@LaunchedEffect
        }
        sweep.snapTo(0f)
        ring.snapTo(1f)
        sweep.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        launch {
            pop.animateTo(1.08f, tween(110))
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
        delay(250)
        ring.animateTo(0f, tween(450))
    }

    FollowScene(
        state,
        5,
        "Avatar Ring",
        "A violet-to-cyan ring draws itself around the avatar, then fades out.",
        avatar = {
            Box(contentAlignment = Alignment.Center) {
                Avatar(
                    Modifier.graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    },
                )
                // Drawn past its own bounds so the ring doesn't widen the avatar's slot in the row.
                Canvas(Modifier.matchParentSize()) {
                    val s = sweep.value
                    val alpha = ring.value
                    if (alpha <= 0f || s <= 0f) return@Canvas
                    val stroke = RingStroke.toPx()
                    // Once drawn, the ring widens a little as it fades.
                    val grow = if (s >= 1f) (1f - alpha) * 4.dp.toPx() else 0f
                    val radius = size.minDimension / 2 + RingGap.toPx() + stroke / 2 + grow
                    rotate(-90f) {
                        drawArc(
                            RingBrush,
                            startAngle = 0f,
                            sweepAngle = 360f * s,
                            useCenter = false,
                            topLeft = center - Offset(radius, radius),
                            size = Size(radius * 2, radius * 2),
                            alpha = alpha,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    if (s < 1f) {
                        val tip = (360f * s - 90f) * PI.toFloat() / 180f
                        drawCircle(MenuColors.Text, stroke * 0.9f, center + Offset(cos(tip), sin(tip)) * radius, alpha = alpha)
                    }
                }
            }
        },
    ) {
        FadingFollowButton(state.following)
    }
}
