package com.azersun.uitemplates.demos.switch1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sqrt

private val LiquidFill = Brush.horizontalGradient(listOf(SwitchColors.Violet, SwitchColors.Blue))

@Composable
fun LiquidToggleDemo() {
    var on by remember { mutableStateOf(false) }
    // The knob's two edges as fractions of its travel; they move on different curves so the knob stretches.
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        on = !on
        val target = if (on) 1f else 0f
        val lead = if (on) right else left
        val trail = if (on) left else right
        job?.cancel()
        job = scope.launch {
            launch { lead.animateTo(target, tween(260, easing = FastOutSlowInEasing)) }
            delay(70)
            // Underdamped, so the trailing edge overshoots into the knob and squishes it against the end.
            trail.animateTo(target, spring(dampingRatio = 0.55f, stiffness = 220f))
        }
    }

    CheckboxStage(
        1,
        "Liquid Toggle",
        "Knob stretches like a liquid drop as it slides, then squishes at the edge.",
        autoplay = { toggle() },
    ) {
        Spacer(
            Modifier
                .size(TrackWidth, TrackHeight)
                .clickable(interactionSource = null, indication = null, role = Role.Switch) { toggle() }
                .drawWithCache {
                    val inset = KnobInset.toPx()
                    val knob = size.height - inset * 2
                    val travel = size.width - inset * 2 - knob
                    val centerY = size.height / 2
                    val maxHeight = size.height - inset * 0.6f
                    val shadowDrop = 2.dp.toPx()
                    onDrawBehind {
                        val l = left.value
                        val r = right.value
                        val x0 = inset + l * travel
                        val x1 = inset + r * travel + knob
                        val width = (x1 - x0).coerceAtLeast(knob * 0.7f)
                        val centerX = (x0 + x1) / 2
                        // Roughly keeps the drop's volume: thinner while stretched, taller while squished.
                        val height = (knob * sqrt(knob / width)).coerceAtMost(maxHeight)
                        drawTrack(((l + r) / 2).coerceIn(0f, 1f), LiquidFill)
                        val topLeft = Offset(centerX - width / 2, centerY - height / 2)
                        val blob = Size(width, height)
                        val corner = CornerRadius(min(width, height) / 2)
                        drawRoundRect(Color.Black, topLeft + Offset(0f, shadowDrop), blob, corner, alpha = 0.22f)
                        drawRoundRect(SwitchColors.Knob, topLeft, blob, corner)
                    }
                },
        )
    }
}
