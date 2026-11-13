package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val MaxLean = 8.dp
private const val MaxTiltDegrees = 3f

@Composable
fun MagneticPullDemo() {
    val press = remember { PressState() }
    val lean = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    // 0 = label at rest, 1 = label fully sucked into the touch point.
    val suck = remember { Animatable(0f) }
    val tint = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(StartState) }
    var behind by remember { mutableStateOf(StartState) }
    var ahead by remember { mutableStateOf(StartState) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val half = with(density) { Offset(PillWidth.toPx() / 2, PillHeight.toPx() / 2) }
    val maxLean = with(density) { MaxLean.toPx() }

    LaunchedEffect(press.pressed, press.position) {
        if (press.pressed) {
            val pull = press.position - half
            val target = Offset(
                (pull.x / half.x).coerceIn(-1f, 1f) * maxLean,
                (pull.y / half.y).coerceIn(-1f, 1f) * maxLean * 0.5f,
            )
            lean.animateTo(target, spring(dampingRatio = 0.75f, stiffness = 500f))
        } else {
            // Under-damped so the pill wobbles back to centre once let go.
            lean.animateTo(Offset.Zero, spring(dampingRatio = 0.32f, stiffness = 320f))
        }
    }

    ButtonStateScene(
        number = 2,
        title = "Magnetic Pull",
        caption = "While pressed the pill leans toward your finger; on release the label is sucked in and the new one springs out.",
        press = press,
        onToggle = { from, to, tap ->
            origin = tap - half
            behind = from
            ahead = to
            tint.snapTo(0f)
            coroutineScope {
                launch { tint.animateTo(1f, tween(420)) }
                suck.animateTo(1f, tween(190, easing = FastOutLinearInEasing))
                shown = to
                suck.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 380f))
            }
            behind = to
            tint.snapTo(0f)
        },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = lean.value.x
                    translationY = lean.value.y
                    rotationZ = lean.value.x / maxLean * MaxTiltDegrees
                }
                .clip(Pill)
                .drawBehind {
                    val t = tint.value
                    drawRect(behind.brush)
                    if (t > 0f) drawRect(ahead.brush, alpha = t)
                },
            contentAlignment = Alignment.Center,
        ) {
            StateLabel(
                shown,
                Modifier.graphicsLayer {
                    val g = suck.value
                    translationX = origin.x * g
                    translationY = origin.y * g
                    val scale = (1f - g).coerceAtLeast(0f)
                    scaleX = scale
                    scaleY = scale
                    alpha = (1f - g * 1.2f).coerceIn(0f, 1f)
                },
            )
        }
    }
}
