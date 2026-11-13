package com.azersun.uitemplates.demos.checkbox2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckBoxMark
import com.azersun.uitemplates.demos.checkbox1.CheckColors
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.PathTrim
import com.azersun.uitemplates.demos.checkbox1.checkMark
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import com.azersun.uitemplates.demos.checkbox1.checkStroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Tasks = listOf("Stretch", "Read 10 pages", "Drink water")
private val InitialChecked = listOf(true, false, false)
private val RingSize = 100.dp
private val RingStroke = 9.dp
private const val SegmentGap = 16f
private val SparkColors = listOf(CheckColors.Cyan, CheckColors.Violet, CheckColors.Yellow, CheckColors.Pink)

@Composable
fun ProgressRingDemo() {
    val checked = remember { mutableStateListOf(*InitialChecked.toTypedArray()) }
    val count = checked.count { it }
    val all = count == Tasks.size
    val progress by animateFloatAsState(count.toFloat(), tween(520, easing = FastOutSlowInEasing), label = "progress")
    val complete = remember { Animatable(if (all) 1f else 0f) }
    val tick = remember { Animatable(if (all) 1f else 0f) }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(all) {
        if (all) {
            // Let the last segment finish filling before the ring turns into the badge.
            delay(380)
            launch {
                burst.snapTo(0f)
                burst.animateTo(1f, tween(760, easing = LinearOutSlowInEasing))
            }
            launch {
                delay(160)
                tick.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            }
            complete.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
        } else {
            tick.animateTo(0f, tween(120))
            complete.animateTo(0f, tween(240))
        }
    }

    CheckboxStage(
        4,
        "Progress Ring",
        "Each checked task fills the ring. At 3/3 it turns into a glowing ✓.",
        onReplay = { InitialChecked.forEachIndexed { i, value -> checked[i] = value } },
        autoplay = {
            checked[1] = true
            delay(1000)
            checked[2] = true
        },
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .checkPanel()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(RingSize), contentAlignment = Alignment.Center) {
                Spacer(
                    Modifier
                        .size(RingSize)
                        .drawWithCache {
                            val stroke = RingStroke.toPx()
                            val radius = size.minDimension / 2 - stroke / 2
                            val center = size.center
                            val arcRect = Rect(center, radius)
                            val segment = 360f / Tasks.size
                            val ink = Brush.linearGradient(listOf(CheckColors.Violet, CheckColors.Cyan), Offset.Zero, Offset(size.width, size.height))
                            val badge = Brush.linearGradient(listOf(Color(0xFF6E9BFF), CheckColors.Violet), Offset.Zero, Offset(size.width, size.height))
                            val outer = radius + stroke / 2
                            val glow = Brush.radialGradient(
                                listOf(CheckColors.Blue.copy(alpha = 0.5f), Color.Transparent),
                                center,
                                outer * 1.6f,
                            )
                            val checkSide = outer * 1.1f
                            val trim = PathTrim(Path().apply { checkMark(center - Offset(checkSide / 2, checkSide / 2), checkSide) })
                            val checkStyle = checkStroke(checkSide * 0.85f)
                            val arcStyle = Stroke(stroke, cap = StrokeCap.Round)
                            onDrawBehind {
                                val p = progress
                                val c = complete.value
                                val t = tick.value
                                val b = burst.value
                                for (i in Tasks.indices) {
                                    val start = -90f + i * segment + SegmentGap / 2
                                    val sweep = segment - SegmentGap
                                    drawArc(CheckColors.Track, start, sweep, false, arcRect.topLeft, arcRect.size, style = arcStyle)
                                    val f = (p - i).coerceIn(0f, 1f)
                                    if (f > 0f) drawArc(ink, start, sweep * f, false, arcRect.topLeft, arcRect.size, style = arcStyle)
                                }
                                if (c > 0f) {
                                    drawCircle(glow, outer * 1.6f * c, center, alpha = c.coerceAtMost(1f))
                                    drawCircle(badge, outer * c, center)
                                }
                                if (t > 0f) drawPath(trim.fraction(0f, t), Color.White, style = checkStyle)
                                if (b < 1f) {
                                    val eased = 1f - (1f - b) * (1f - b)
                                    repeat(10) { i ->
                                        val angle = (i * 36f + 18f) * PI.toFloat() / 180f
                                        val distance = outer + 6.dp.toPx() + 30.dp.toPx() * eased
                                        drawCircle(
                                            SparkColors[i % SparkColors.size],
                                            (if (i % 2 == 0) 3.2f else 2.2f).dp.toPx() * (1f - b * 0.7f),
                                            center + Offset(cos(angle), sin(angle)) * distance,
                                            alpha = 1f - b,
                                        )
                                    }
                                }
                            }
                        },
                )
                AnimatedContent(
                    count,
                    Modifier.graphicsLayer { alpha = (1f - complete.value * 1.5f).coerceIn(0f, 1f) },
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                            (slideOutVertically { if (up) -it else it } + fadeOut())
                    },
                    label = "count",
                ) { value ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$value", color = CheckColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "/${Tasks.size}",
                            color = CheckColors.Muted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Today", color = CheckColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Tasks.forEachIndexed { i, task ->
                    TaskRow(task, checked[i]) { checked[i] = !checked[i] }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "fill")
    val tick by animateFloatAsState(
        if (checked) 1f else 0f,
        tween(if (checked) 240 else 120, delayMillis = if (checked) 70 else 0),
        label = "tick",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, role = Role.Checkbox, onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBoxMark({ fill }, { tick }, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            color = if (checked) CheckColors.Muted else CheckColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
