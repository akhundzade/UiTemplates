package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val EdgeWidth = 28.dp
private val FlingVelocity = 900.dp
private val EdgeShadow = 18.dp
private val FingerRadius = 18.dp
private const val CompleteFraction = 0.4f
private const val Parallax = 0.3f
private const val AutoplaySwipeTo = 0.42f
private val SwipeSpring = spring<Float>(dampingRatio = 1f, stiffness = 380f)

@Composable
fun SwipeBackTransitionDemo() {
    val nav = rememberNavigator(SwipeSpring)
    var width by remember { mutableFloatStateOf(1f) }
    var edgeDrag by remember { mutableStateOf(false) }
    // A ghost fingertip, shown only while the autoplay fakes the swipe.
    val finger = remember { Animatable(0f) }
    val density = LocalDensity.current
    val edge = with(density) { EdgeWidth.toPx() }
    val fling = with(density) { FlingVelocity.toPx() }

    TransitionScene(
        number = 1,
        title = "Swipe Back",
        caption = "Drag from the left edge: the page follows your finger while the list slides back in behind it, slower and dimmed. Let go past 40% to go back.",
        nav = nav,
        modifier = Modifier
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .draggable(
                state = rememberDraggableState { delta ->
                    if (edgeDrag) nav.snapTo((nav.progress.value - delta / width).coerceIn(0f, 1f))
                },
                orientation = Orientation.Horizontal,
                onDragStarted = { start ->
                    edgeDrag = start.x <= edge && !nav.progress.isRunning && nav.progress.value == 1f
                },
                onDragStopped = { velocity ->
                    if (edgeDrag) {
                        edgeDrag = false
                        val travelled = 1f - nav.progress.value
                        val goBack = velocity > fling || (travelled > CompleteFraction && velocity > -fling)
                        nav.animateTo(if (goBack) 0f else 1f, SwipeSpring, velocity = -velocity / width)
                    }
                },
            ),
        autoplayBack = {
            nav.launch {
                finger.animateTo(1f, tween(140))
                nav.progress.animateTo(AutoplaySwipeTo, tween(560, easing = FastOutSlowInEasing))
                launch { finger.animateTo(0f, tween(160)) }
                nav.progress.animateTo(0f, SwipeSpring)
            }.join()
        },
        overlay = {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val radius = FingerRadius.toPx()
                        val ring = Stroke(1.5.dp.toPx())
                        onDrawBehind {
                            val shown = finger.value
                            val pageEdge = (1f - nav.progress.value) * size.width
                            if (shown > 0f) {
                                val tip = Offset(pageEdge + radius + 4.dp.toPx(), size.height * 0.55f)
                                drawCircle(Color.White, radius, tip, alpha = 0.22f * shown)
                                drawCircle(Color.White, radius, tip, alpha = 0.6f * shown, style = ring)
                            }
                        }
                    },
            )
        },
    ) { explore, detail, progress ->
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -Parallax * size.width * progress() },
        ) {
            explore()
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress() }
                    .background(Color.Black.copy(alpha = 0.45f)),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = (1f - progress()) * size.width }
                .drawBehind {
                    // A soft shadow cast leftwards onto the list, just outside the page's edge.
                    val shadow = EdgeShadow.toPx()
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), -shadow, 0f),
                        topLeft = Offset(-shadow, 0f),
                        size = Size(shadow, size.height),
                    )
                },
        ) {
            detail()
        }
    }
}
