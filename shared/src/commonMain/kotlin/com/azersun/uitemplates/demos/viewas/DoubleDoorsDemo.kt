package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

private const val OpenAngle = 90f

// Margins keep the panel's shadow; the right half reaches 1px past the middle so no seam shows.
private const val ShadowMargin = 80f
private val LeftHalf = GenericShape { size, _ ->
    addRect(Rect(-ShadowMargin, -ShadowMargin, size.width / 2, size.height + ShadowMargin))
}
private val RightHalf = GenericShape { size, _ ->
    addRect(Rect(size.width / 2 - 1f, -ShadowMargin, size.width + ShadowMargin, size.height + ShadowMargin))
}

@Composable
fun DoubleDoorsDemo() {
    val state = rememberViewAsState()
    val angle = remember { Animatable(OpenAngle) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            angle.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 180f))
        } else if (shown) {
            angle.animateTo(OpenAngle, tween(320, easing = FastOutLinearInEasing))
            shown = false
        }
    }

    ViewAsScene(state, 2, "Double Doors", "Two halves swing shut like doors; picking swings them open.") {
        if (shown) {
            Box {
                ViewAsMenu(state, Modifier.door(left = true) { angle.value })
                ViewAsMenu(state, Modifier.door(left = false) { angle.value })
            }
        }
    }
}

/** One half of the panel, hinged at its outer edge, darkening as it turns away from the viewer. */
private fun Modifier.door(left: Boolean, angle: () -> Float): Modifier = graphicsLayer {
    val a = angle()
    transformOrigin = TransformOrigin(if (left) 0f else 1f, 0.5f)
    rotationY = if (left) a else -a
    cameraDistance = 12f * density
    shape = if (left) LeftHalf else RightHalf
    clip = true
    alpha = ((OpenAngle - a) / 25f).coerceIn(0f, 1f)
}.drawWithContent {
    drawContent()
    val shade = (angle() / OpenAngle).coerceIn(0f, 1f) * 0.55f
    if (shade > 0f) drawRoundRect(Color.Black, cornerRadius = CornerRadius(MenuCorner.toPx()), alpha = shade)
}
