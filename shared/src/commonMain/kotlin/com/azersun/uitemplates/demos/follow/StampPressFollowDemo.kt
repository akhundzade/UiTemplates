package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch

// Starts slow and hits hard, like a stamp coming down.
private val SlamEasing = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)

@Composable
fun StampPressFollowDemo() {
    val state = remember { FollowState() }
    val stamp = remember { Animatable(1f) }
    val ink = remember { Animatable(1f) }
    val shock = remember { Animatable(1f) }
    // The face being replaced stays under the stamp until the new one lands on it.
    var underneath by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state.toggles) {
        if (state.toggles == 0) return@LaunchedEffect
        underneath = !state.following
        stamp.snapTo(1.4f)
        ink.snapTo(0f)
        launch { ink.animateTo(1f, tween(130)) }
        stamp.animateTo(0.95f, tween(170, easing = SlamEasing))
        underneath = null
        launch {
            shock.snapTo(0f)
            shock.animateTo(1f, tween(480, easing = LinearOutSlowInEasing))
        }
        stamp.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f))
    }

    FollowScene(state, 6, "Stamp Press", "The new state slams down like a stamp and a shock ring ripples out from the button.") {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val s = shock.value
                    if (s >= 1f) return@drawWithContent
                    val inflate = 22.dp.toPx() * s
                    val stroke = (3f - 2.5f * s).dp.toPx()
                    drawRoundRect(
                        if (state.following) MenuColors.Violet else MenuColors.Muted,
                        topLeft = Offset(-inflate, -inflate),
                        size = Size(size.width + inflate * 2, size.height + inflate * 2),
                        cornerRadius = CornerRadius(size.height / 2 + inflate),
                        alpha = 1f - s,
                        style = Stroke(stroke),
                    )
                },
        ) {
            underneath?.let { FollowFace(it) }
            FollowFace(
                state.following,
                Modifier.graphicsLayer {
                    scaleX = stamp.value
                    scaleY = stamp.value
                    alpha = ink.value
                },
            )
        }
    }
}
