package com.azersun.uitemplates.demos.checkbox2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckColors
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.PathTrim
import com.azersun.uitemplates.demos.checkbox1.checkMark
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import com.azersun.uitemplates.demos.checkbox1.checkPop
import com.azersun.uitemplates.demos.checkbox1.checkStroke
import com.azersun.uitemplates.demos.checkbox1.drawBoxShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val DotCount = 8
private val DotColors = listOf(CheckColors.Cyan, CheckColors.Violet, CheckColors.Blue, CheckColors.Pink)
private val BoxSide = 32.dp

@Composable
fun PopBurstDemo() {
    var on by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val fill = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    // Ring and burst run 0 → 1 per check and are hidden at 1.
    val ring = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        on = !on
        if (on) {
            scope.launch { scale.checkPop(dip = 0.85f, peak = 1.15f) }
            scope.launch {
                delay(100)
                fill.animateTo(1f, tween(140))
                tick.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
            scope.launch {
                ring.snapTo(0f)
                delay(110)
                ring.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
            }
            scope.launch {
                burst.snapTo(0f)
                delay(130)
                burst.animateTo(1f, tween(680, easing = LinearOutSlowInEasing))
            }
        } else {
            scope.launch {
                scale.animateTo(0.9f, tween(100))
                scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f))
            }
            scope.launch {
                tick.animateTo(0f, tween(120))
                fill.animateTo(0f, tween(180))
            }
        }
    }

    CheckboxStage(
        6,
        "Pop Burst",
        "Tap: the box squishes, then pops a ✓ with a burst of dots and a ring.",
        autoplay = { toggle() },
    ) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { toggle() }
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(BoxSide)
                    .drawWithCache {
                        val side = size.minDimension
                        val center = size.center
                        val trim = PathTrim(Path().apply { checkMark(Offset.Zero, side) })
                        val checkStyle = checkStroke(side)
                        onDrawBehind {
                            val s = scale.value
                            val f = fill.value
                            val t = tick.value
                            val r = ring.value
                            val b = burst.value
                            if (r < 1f) {
                                drawCircle(
                                    CheckColors.Cyan,
                                    side * (0.55f + 0.75f * r),
                                    center,
                                    alpha = 1f - r,
                                    style = Stroke(3.dp.toPx() * (1f - r) + 0.5f),
                                )
                            }
                            if (b < 1f) {
                                repeat(DotCount) { i ->
                                    val angle = (i * 360f / DotCount - 90f) * PI.toFloat() / 180f
                                    val distance = side * (0.6f + 0.85f * b)
                                    drawCircle(
                                        DotColors[i % DotColors.size],
                                        2.8.dp.toPx() * (1f - b * 0.75f),
                                        center + Offset(cos(angle), sin(angle)) * distance,
                                        alpha = ((1f - b) * 1.4f).coerceAtMost(1f),
                                    )
                                }
                            }
                            scale(s, center) {
                                drawBoxShell(Offset.Zero, side, f)
                                if (t > 0f) drawPath(trim.fraction(0f, t), Color.White, style = checkStyle)
                            }
                        }
                    },
            )
            Spacer(Modifier.width(18.dp))
            Column {
                Text("Join the beta", color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                AnimatedContent(
                    on,
                    transitionSpec = {
                        (slideInVertically(tween(260, delayMillis = 180)) { it / 2 } + fadeIn(tween(260, delayMillis = 180))) togetherWith
                            (slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(160)))
                    },
                    label = "status",
                ) { isOn ->
                    Text(
                        if (isOn) "You're in!" else "Early access",
                        color = if (isOn) CheckColors.Cyan else CheckColors.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
