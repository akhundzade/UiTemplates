package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

private const val MaxShade = 0.5f

@Composable
fun BarrelRollDemo() {
    var front by remember { mutableStateOf(StartState) }
    var incoming by remember { mutableStateOf<PlayState?>(null) }
    val roll = remember { Animatable(0f) }

    ButtonStateScene(
        number = 3,
        title = "Barrel Roll",
        caption = "The pill rolls up like a barrel: the old face tips back over the top as the new one comes up from below.",
        onToggle = { _, to, _ ->
            incoming = to
            roll.snapTo(0f)
            roll.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f))
            front = to
            incoming = null
            roll.snapTo(0f)
        },
    ) {
        // Square faces turn behind a pill-shaped window, so the rounded ends never distort.
        Box(Modifier.matchParentSize().clip(Pill)) {
            PillFace(
                front,
                shape = RectangleShape,
                modifier = Modifier
                    .graphicsLayer {
                        val p = roll.value
                        // The two faces share the edge between them, which slides from the bottom to the top.
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        translationY = -size.height * p
                        rotationX = 90f * p
                        cameraDistance = 12f * density
                        alpha = if (p >= 0.99f) 0f else 1f
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(Color.Black, alpha = MaxShade * roll.value.coerceIn(0f, 1f))
                    },
            )
            incoming?.let { next ->
                PillFace(
                    next,
                    shape = RectangleShape,
                    modifier = Modifier
                        .graphicsLayer {
                            val p = roll.value
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            translationY = size.height * (1f - p)
                            rotationX = -90f * (1f - p)
                            cameraDistance = 12f * density
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(Color.Black, alpha = MaxShade * (1f - roll.value).coerceIn(0f, 1f))
                        },
                )
            }
        }
    }
}
