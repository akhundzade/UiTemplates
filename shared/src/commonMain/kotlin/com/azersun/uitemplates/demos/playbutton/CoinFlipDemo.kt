package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val LiftScale = 0.12f
private val Thickness = 6.dp
private val EdgeBrush = Brush.verticalGradient(listOf(Color(0xFF5B4FD6), Color(0xFF2B2878)))

@Composable
fun CoinFlipDemo() {
    val state = rememberPlayerState()
    val flip = remember { Animatable(0f) }

    // Every tap adds half a turn, so the coin keeps flipping the same way and lands on its other face.
    LaunchedEffect(state.toggles) {
        flip.animateTo(state.toggles * 180f, spring(dampingRatio = 0.6f, stiffness = 120f))
    }

    PlayerScene(
        state = state,
        number = 2,
        title = "Coin Flip",
        caption = "The button flips like a coin, edge-on for a moment, and lands showing the new icon on its back.",
    ) {
        PlayGlow()
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val s = 1f + LiftScale * sin(PI.toFloat() * (flip.value % 180f) / 180f)
                    scaleX = s
                    scaleY = s
                },
        ) {
            // The rim: the coin's full silhouette, of which the face covers all but a sliver.
            Spacer(
                Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val r = size.minDimension / 2
                        val t = Thickness.toPx()
                        onDrawBehind {
                            val rad = flip.value * PI.toFloat() / 180f
                            // Face-on the rim would only show as an anti-aliased fringe around the disc.
                            if (t * abs(sin(rad)) < 0.5f) return@onDrawBehind
                            val width = 2 * r * abs(cos(rad)) + t * abs(sin(rad))
                            drawOval(EdgeBrush, Offset(size.width / 2 - width / 2, 0f), Size(width, size.height))
                        }
                    },
            )
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val rad = flip.value * PI.toFloat() / 180f
                        val facing = if (cos(rad) >= 0f) 1f else -1f
                        rotationY = flip.value
                        cameraDistance = 12f * density
                        translationX = Thickness.toPx() / 2 * sin(rad) * facing
                    },
            ) {
                // Seen from behind the layer is mirrored; flip the face back so it reads correctly.
                Box(Modifier.matchParentSize().graphicsLayer { scaleX = if (showsBack(flip.value)) -1f else 1f }) {
                    PlayDisc()
                    Spacer(
                        Modifier
                            .matchParentSize()
                            .drawWithCache {
                                val path = Path()
                                onDrawBehind { drawPlayPause(path, if (showsBack(flip.value)) 1f else 0f) }
                            },
                    )
                }
            }
        }
    }
}

/** The back (pause) face is towards the viewer between 90° and 270°, modulo a full turn. */
private fun showsBack(degrees: Float): Boolean = floor((degrees + 90f) / 180f).toInt() % 2 != 0
