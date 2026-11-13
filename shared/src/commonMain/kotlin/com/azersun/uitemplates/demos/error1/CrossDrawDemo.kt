package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CrossDrawDemo() {
    val ring = remember { Animatable(0f) }
    val red = remember { Animatable(0f) }
    // 0 → 1 draws the first stroke of the ✕, 1 → 2 the second.
    val cross = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun play() {
        job?.cancel()
        job = scope.launch {
            ring.snapTo(0f)
            red.snapTo(0f)
            cross.snapTo(0f)
            shake.snapTo(0f)
            ring.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            red.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
            cross.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            cross.animateTo(2f, tween(150, easing = FastOutSlowInEasing))
            shake.shake(9f, swings = 5, stepMillis = 40)
        }
    }

    fun reset() {
        job?.cancel()
        job = scope.launch {
            ring.snapTo(0f)
            red.snapTo(0f)
            cross.snapTo(0f)
            shake.snapTo(0f)
        }
    }

    CheckboxStage(
        2,
        "Cross Draw",
        "Ring draws in, turns red, then a white ✕ strokes in with a quick shake.",
        onReplay = { reset() },
        autoplay = { play() },
    ) {
        Spacer(
            Modifier
                .size(120.dp)
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .clickable(interactionSource = null, indication = null) { play() }
                .drawWithCache {
                    val center = size.center
                    val stroke = 6.dp.toPx()
                    val radius = size.minDimension / 2 - stroke / 2
                    val arcTopLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)
                    val ringStyle = Stroke(stroke, cap = StrokeCap.Round)
                    val crossWidth = 7.dp.toPx()
                    val arm = radius * 0.36f
                    onDrawBehind {
                        val g = ring.value
                        val f = red.value
                        val x = cross.value
                        if (f > 0f) drawCircle(ErrorColors.Red, radius * 1.4f, center, alpha = 0.14f * f)
                        if (g > 0f) {
                            drawArc(lerp(ErrorColors.Blue, ErrorColors.Red, f), -90f, 360f * g, false, arcTopLeft, arcSize, style = ringStyle)
                        }
                        // The red disc grows out of the centre until it covers the ring.
                        if (f > 0f) drawCircle(ErrorColors.Red, (radius + stroke / 2) * f, center)
                        val first = x.coerceIn(0f, 1f)
                        val second = (x - 1f).coerceIn(0f, 1f)
                        if (first > 0f) {
                            drawLine(
                                Color.White,
                                center + Offset(-arm, -arm),
                                center + Offset(-arm + 2 * arm * first, -arm + 2 * arm * first),
                                crossWidth,
                                StrokeCap.Round,
                            )
                        }
                        if (second > 0f) {
                            drawLine(
                                Color.White,
                                center + Offset(arm, -arm),
                                center + Offset(arm - 2 * arm * second, -arm + 2 * arm * second),
                                crossWidth,
                                StrokeCap.Round,
                            )
                        }
                    }
                },
        )
    }
}
