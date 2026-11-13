package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val Slats = 10
private const val SlatStaggerMillis = 55L

@Composable
fun BlindsTurnFollowDemo() {
    val state = remember { FollowState() }
    val angles = remember { List(Slats) { Animatable(0f) } }
    LaunchedEffect(state.toggles) {
        val target = state.toggles * 180f
        val middle = (Slats - 1) / 2f
        angles.forEachIndexed { i, angle ->
            launch {
                delay(((abs(i - middle) - 0.5f) * SlatStaggerMillis).toLong())
                angle.animateTo(target, tween(420, easing = FastOutSlowInEasing))
            }
        }
    }
    // At rest the slats are drawn as one face so no seams can show between them.
    val turning by remember { derivedStateOf { angles.any { it.value != angles[0].value || it.isRunning } } }

    FollowScene(state, 2, "Blinds Turn", "Ten slats turn one by one from the middle outward, like blinds, to the other state.") {
        if (turning) {
            Box(Modifier.fillMaxSize()) {
                angles.forEachIndexed { i, angle -> Slat(i, angle) }
            }
        } else {
            FollowFace(faceAt(angles[0].value) == 1)
        }
    }
}

@Composable
private fun Slat(index: Int, angle: Animatable<Float, AnimationVector1D>) {
    val showingFollowing by remember { derivedStateOf { faceAt(angle.value) == 1 } }
    FollowFace(
        showingFollowing,
        Modifier
            .graphicsLayer {
                rotationY = tiltAt(angle.value)
                transformOrigin = TransformOrigin((index + 0.5f) / Slats, 0.5f)
                cameraDistance = 12f * density
                // Offscreen so the shade's SrcAtop only darkens the pill, not its clear corners.
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                val width = size.width / Slats
                val turn = abs(sin(tiltAt(angle.value) * PI.toFloat() / 180f))
                clipRect(left = index * width, right = (index + 1) * width) {
                    this@drawWithContent.drawContent()
                    if (turn > 0.01f) drawRect(Color.Black, alpha = 0.4f * turn, blendMode = BlendMode.SrcAtop)
                }
            },
    )
}
