package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SubmitBrush = Brush.horizontalGradient(listOf(ErrorColors.Violet, ErrorColors.Blue))
private val PillWidth = 220.dp
private val PillHeight = 58.dp
private const val Submit = "Submit"

@Composable
fun ButtonFailDemo() {
    var label by remember { mutableStateOf(Submit) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // 1 is the full pill, 0 a circle as wide as it is tall.
    val expand = remember { Animatable(1f) }
    val red = remember { Animatable(0f) }
    val cross = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val labelAlpha = remember { Animatable(1f) }
    val spinnerAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun submit() {
        if (busy) return
        busy = true
        job = scope.launch {
            launch { labelAlpha.animateTo(0f, tween(120)) }
            launch { red.animateTo(0f, tween(240)) }
            expand.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
            loading = true
            spinnerAlpha.animateTo(1f, tween(150))
            delay(1100)
            spinnerAlpha.animateTo(0f, tween(100))
            loading = false
            launch { red.animateTo(1f, tween(220)) }
            cross.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            shake.shake(10f)
            delay(380)
            label = "Try again"
            launch { cross.animateTo(0f, tween(140)) }
            launch {
                delay(140)
                labelAlpha.animateTo(1f, tween(220))
            }
            expand.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 380f))
            busy = false
        }
    }

    fun reset() {
        job?.cancel()
        busy = false
        loading = false
        label = Submit
        job = scope.launch {
            expand.snapTo(1f)
            red.snapTo(0f)
            cross.snapTo(0f)
            shake.snapTo(0f)
            labelAlpha.snapTo(1f)
            spinnerAlpha.snapTo(0f)
        }
    }

    CheckboxStage(
        3,
        "Button Fail",
        "Button spins, turns red with an ✕ and shakes, then reads \"Try again\".",
        onReplay = { reset() },
        autoplay = { submit() },
    ) {
        Box(
            Modifier
                .layout { measurable, _ ->
                    val height = PillHeight.roundToPx()
                    val full = PillWidth.roundToPx()
                    val width = (height + (full - height) * expand.value).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .clip(RoundedCornerShape(50))
                .drawBehind {
                    drawRect(SubmitBrush)
                    val r = red.value
                    if (r > 0f) drawRect(ErrorColors.Red, alpha = r)
                }
                .clickable(enabled = !busy, interactionSource = null, indication = null, role = Role.Button) { submit() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .wrapContentWidth(unbounded = true)
                    .graphicsLayer { alpha = labelAlpha.value },
            )
            if (loading) {
                Spinner(Color.White, Modifier.size(26.dp).graphicsLayer { alpha = spinnerAlpha.value })
            }
            Spacer(
                Modifier
                    .size(22.dp)
                    .drawBehind {
                        val c = cross.value
                        if (c <= 0f) return@drawBehind
                        val width = 3.dp.toPx()
                        val inset = size.width * 0.2f
                        val reach = (size.width - inset * 2) * c
                        drawLine(Color.White, Offset(inset, inset), Offset(inset + reach, inset + reach), width, StrokeCap.Round)
                        drawLine(
                            Color.White,
                            Offset(size.width - inset, inset),
                            Offset(size.width - inset - reach, inset + reach),
                            width,
                            StrokeCap.Round,
                        )
                    },
            )
        }
    }
}
