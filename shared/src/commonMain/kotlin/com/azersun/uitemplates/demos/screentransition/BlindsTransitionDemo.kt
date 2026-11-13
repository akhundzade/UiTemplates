package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private const val Slats = 6
private const val SlatMillis = 420
private const val SlatStaggerMillis = 60
private const val TotalMillis = SlatMillis + SlatStaggerMillis * (Slats - 1)
private val SlatShapes = List(Slats) { SlatShape(it) }

@Composable
fun BlindsTransitionDemo() {
    // Linear overall; each slat eases its own turn inside its slice of the timeline.
    val nav = rememberNavigator(tween(TotalMillis, easing = LinearEasing))
    TransitionScene(
        number = 3,
        title = "Blinds",
        caption = "The new screen turns in on six horizontal slats, one after another from the top down.",
        nav = nav,
    ) { explore, detail, progress ->
        Box(Modifier.fillMaxSize()) {
            explore()
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.4f * progress() }
                    .background(Color.Black),
            )
        }
        repeat(Slats) { i ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val turn = slatTurn(progress(), i)
                        rotationX = -90f * (1f - turn)
                        cameraDistance = 12f * density
                        transformOrigin = TransformOrigin(0.5f, (i + 0.5f) / Slats)
                        shape = SlatShapes[i]
                        clip = true
                        alpha = if (turn > 0f) 1f else 0f
                    },
            ) {
                detail()
                // Darker while the slat still faces away, so each one seems to catch the light.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.55f * (1f - slatTurn(progress(), i)) }
                        .background(Color.Black),
                )
            }
        }
    }
}

private fun slatTurn(progress: Float, index: Int): Float {
    val elapsed = progress * TotalMillis - index * SlatStaggerMillis
    return FastOutSlowInEasing.transform((elapsed / SlatMillis).coerceIn(0f, 1f))
}

/** The [index]th horizontal band of the screen. */
private class SlatShape(private val index: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val height = size.height / Slats
        // Half a pixel of overlap hides hairline seams between neighbouring slats.
        return Outline.Rectangle(Rect(0f, height * index - 0.5f, size.width, height * (index + 1) + 0.5f))
    }
}
