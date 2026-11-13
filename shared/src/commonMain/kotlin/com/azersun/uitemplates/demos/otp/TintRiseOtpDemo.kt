package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch

private val TintColors = listOf(MenuColors.Violet, MenuColors.Blue)
private val SuccessColors = listOf(Color(0xFF14B8A6), Color(0xFF0D9488))

@Composable
fun TintRiseOtpDemo() {
    val state = remember { OtpState() }
    OtpScene(6, "Tint Rise", "Each box fills with color from the bottom as its digit lands.", state) { metrics ->
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap)) {
            repeat(CodeLength) { TintBox(it, state, metrics) }
        }
    }
}

@Composable
private fun TintBox(index: Int, state: OtpState, metrics: OtpMetrics) {
    val digit = state.code.getOrNull(index)
    val level = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(digit != null) {
        if (digit == null) {
            level.snapTo(0f)
            reveal.snapTo(0f)
            return@LaunchedEffect
        }
        launch { reveal.animateTo(1f, tween(220, delayMillis = 60)) }
        level.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }
    val success by animateFloatAsState(
        if (state.verified) 1f else 0f,
        tween(420, delayMillis = if (state.verified) index * 50 else 0),
        label = "success",
    )

    OtpBox(metrics, active = index == state.activeIndex, success = { success }) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    if (level.value <= 0f) return@drawBehind
                    val top = size.height * (1f - level.value)
                    val shape = Path().apply {
                        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(BoxCorner.toPx())))
                    }
                    clipPath(shape) {
                        // Gradients span the whole box so the colours stay put while the level rises.
                        val tint = Brush.verticalGradient(TintColors, startY = 0f, endY = size.height)
                        val done = Brush.verticalGradient(SuccessColors, startY = 0f, endY = size.height)
                        drawRect(tint, Offset(0f, top), size.copy(height = size.height - top), alpha = 1f - success)
                        drawRect(done, Offset(0f, top), size.copy(height = size.height - top), alpha = success)
                        if (level.value < 1f) {
                            drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, top), Offset(size.width, top), 1.5.dp.toPx())
                        }
                    }
                },
        )
        if (digit != null) {
            OtpDigit(
                digit,
                Modifier.graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 6.dp.toPx()
                },
            )
        }
    }
}
