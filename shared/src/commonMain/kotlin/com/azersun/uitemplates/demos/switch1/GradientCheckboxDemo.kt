package com.azersun.uitemplates.demos.switch1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.PathTrim
import com.azersun.uitemplates.demos.checkbox1.checkMark
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import com.azersun.uitemplates.demos.checkbox1.checkStroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SparkCount = 8
private val BoxSide = 36.dp

@Composable
fun GradientCheckboxDemo() {
    var on by remember { mutableStateOf(false) }
    // 1 is fully squashed; the underdamped return also stretches it tall for a moment.
    val squish = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    // Runs 0 → 1 per check and is hidden at 1.
    val sparks = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        on = !on
        job?.cancel()
        job = scope.launch {
            launch {
                squish.animateTo(1f, tween(90))
                squish.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 520f))
            }
            if (on) {
                delay(70)
                fill.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                tick.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
            } else {
                tick.animateTo(0f, tween(120))
                fill.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
            }
        }
        // Outside the job, so a quick second tap cannot freeze the sparks mid-flight.
        if (on) {
            scope.launch {
                sparks.snapTo(0f)
                delay(330)
                sparks.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
            }
        }
    }

    CheckboxStage(
        5,
        "Checkbox",
        "Box squishes on tap, fills violet→cyan from the center, check draws in.",
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
                        val corner = CornerRadius(side * 0.28f)
                        val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, side, side, corner)) }
                        val brush = Brush.linearGradient(listOf(SwitchColors.Violet, SwitchColors.Cyan), Offset.Zero, Offset(side, side))
                        val trim = PathTrim(Path().apply { checkMark(Offset.Zero, side) })
                        val checkStyle = checkStroke(side)
                        val outline = side * 0.075f
                        val outlineStyle = Stroke(outline)
                        // Just past the half-diagonal, so the circle covers the corners when full.
                        val fullRadius = side * 0.72f
                        val sparkWidth = 2.dp.toPx()
                        onDrawBehind {
                            val s = squish.value
                            val f = fill.value
                            val t = tick.value
                            val k = sparks.value
                            if (k > 0f && k < 1f) {
                                repeat(SparkCount) { i ->
                                    val angle = (i * 360f / SparkCount + 22.5f) * PI.toFloat() / 180f
                                    val dir = Offset(cos(angle), sin(angle))
                                    val inner = side * (0.62f + 0.45f * k)
                                    val outer = inner + side * 0.22f * (1f - k)
                                    drawLine(
                                        if (i % 2 == 0) SwitchColors.Violet else SwitchColors.Cyan,
                                        center + dir * inner,
                                        center + dir * outer,
                                        sparkWidth,
                                        StrokeCap.Round,
                                        alpha = 1f - k,
                                    )
                                }
                            }
                            scale(1f + 0.14f * s, 1f - 0.16f * s, center) {
                                drawRoundRect(
                                    lerp(SwitchColors.Outline, SwitchColors.Violet, f),
                                    Offset(outline / 2, outline / 2),
                                    Size(side - outline, side - outline),
                                    CornerRadius(corner.x - outline / 2),
                                    style = outlineStyle,
                                )
                                if (f > 0f) clipPath(clip) { drawCircle(brush, fullRadius * f, center) }
                                if (t > 0f) drawPath(trim.fraction(0f, t), Color.White, style = checkStyle)
                            }
                        }
                    },
            )
            Spacer(Modifier.width(16.dp))
            Text("Remember me", color = SwitchColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
