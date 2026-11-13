package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// Matches the phone's own corners, so the risen sheet lines up with them exactly at rest.
private val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private val RiseEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
private const val TiltDegrees = 10f
private const val BackScale = 0.9f

@Composable
fun DepthStackTransitionDemo() {
    val nav = rememberNavigator(tween(640, easing = RiseEasing))
    TransitionScene(
        number = 4,
        title = "Depth Stack",
        caption = "Screens stack in 3D: the old one tilts back, shrinks and darkens while the new one rises up from the bottom.",
        nav = nav,
    ) { explore, detail, progress ->
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress()
                    // Positive rotationX tips the top edge away from the viewer.
                    rotationX = TiltDegrees * p
                    val s = 1f - (1f - BackScale) * p
                    scaleX = s
                    scaleY = s
                    cameraDistance = 12f * density
                    shape = PhoneShape
                    clip = true
                },
        ) {
            explore()
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.5f * progress() }
                    .background(Color.Black),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = (1f - progress()) * size.height
                    shadowElevation = 24.dp.toPx()
                    shape = SheetShape
                    clip = true
                },
        ) {
            detail()
        }
    }
}
