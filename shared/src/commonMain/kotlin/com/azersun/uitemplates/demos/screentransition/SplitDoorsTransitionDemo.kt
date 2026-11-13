package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val LeftHalf = GenericShape { size, _ -> addRect(Rect(0f, 0f, size.width / 2, size.height)) }
private val RightHalf = GenericShape { size, _ -> addRect(Rect(size.width / 2, 0f, size.width, size.height)) }
private val SeamShade = 22.dp
private val DoorEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

@Composable
fun SplitDoorsTransitionDemo() {
    val nav = rememberNavigator(tween(760, easing = DoorEasing))
    TransitionScene(
        number = 2,
        title = "Split Doors",
        caption = "The old screen splits down the middle and both halves slide apart like doors, revealing the new one behind.",
        nav = nav,
    ) { explore, detail, progress ->
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 0.96f + 0.04f * progress()
                    scaleX = s
                    scaleY = s
                },
        ) {
            detail()
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - progress() }
                    .background(Color.Black.copy(alpha = 0.5f)),
            )
        }
        Door(LeftHalf, direction = -1f, progress = progress, content = explore)
        Door(RightHalf, direction = 1f, progress = progress, content = explore)
    }
}

/** One half of [content], clipped to [half] and slid outwards by up to half the width. */
@Composable
private fun Door(half: Shape, direction: Float, progress: () -> Float, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = direction * progress() * size.width / 2
                shape = half
                clip = true
            },
    ) {
        content()
        // Shade on the door's inner edge, faded in quickly so the closed screen shows no seam.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (progress() * 4f).coerceAtMost(1f) }
                .drawWithCache {
                    val seam = SeamShade.toPx()
                    val mid = size.width / 2
                    val shadeLeft = if (direction < 0f) mid - seam else mid
                    val shade = if (direction < 0f) {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)), mid - seam, mid)
                    } else {
                        Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent), mid, mid + seam)
                    }
                    onDrawBehind { drawRect(shade, Offset(shadeLeft, 0f), Size(seam, size.height)) }
                },
        )
    }
}
