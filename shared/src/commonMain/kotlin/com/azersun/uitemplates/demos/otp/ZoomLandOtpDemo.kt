package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

private const val StartScale = 2.4f

@Composable
fun ZoomLandOtpDemo() {
    val state = remember { OtpState() }
    OtpScene(5, "Zoom Land", "Each digit zooms in from big and lands; a shine turns them mint.", state) { metrics ->
        val shine = remember { Animatable(0f) }
        LaunchedEffect(state.verified) {
            if (state.verified) shine.animateTo(1f, tween(720, easing = FastOutSlowInEasing)) else shine.snapTo(0f)
        }
        // Band centre and half-width in dp; it starts and ends fully outside the row.
        val bandHalf = metrics.boxWidth.value * 0.9f
        fun bandCenter(): Float = -bandHalf + (metrics.rowWidth.value + bandHalf * 2) * shine.value

        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap)) {
                repeat(CodeLength) { i ->
                    val digit = state.code.getOrNull(i)
                    OtpBox(
                        metrics,
                        active = i == state.activeIndex,
                        // Each box turns mint while the band's centre crosses it.
                        success = { (bandCenter() - metrics.boxStart(i).value) / metrics.boxWidth.value },
                    ) {
                        if (digit != null) ZoomDigit(digit)
                    }
                }
            }
            Canvas(Modifier.matchParentSize()) {
                val progress = shine.value
                if (progress <= 0f || progress >= 1f) return@Canvas
                val boxes = Path()
                repeat(CodeLength) { i ->
                    val x = metrics.boxStart(i).toPx()
                    boxes.addRoundRect(
                        RoundRect(x, 0f, x + metrics.boxWidth.toPx(), size.height, CornerRadius(BoxCorner.toPx())),
                    )
                }
                val center = bandCenter() * density
                val half = bandHalf * density
                clipPath(boxes) {
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                            start = Offset(center - half, 0f),
                            end = Offset(center + half, size.height),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomDigit(digit: Char) {
    val scale = remember { Animatable(StartScale) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(160)) }
        // Damped just enough that it dips slightly below 1 on landing, then settles.
        scale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 380f))
    }
    OtpDigit(
        digit,
        Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
    )
}
