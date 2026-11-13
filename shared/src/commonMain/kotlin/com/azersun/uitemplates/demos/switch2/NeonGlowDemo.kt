package com.azersun.uitemplates.demos.switch2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LapMillis = 700
private const val TrailSegments = 8
private const val TrailFraction = 0.3f
private val NeonOff = Color(0xFF4A4675)
private val NeonCore = Color(0xFFB8F4FF)

@Composable
fun NeonGlowDemo() {
    var on by remember { mutableStateOf(false) }
    // The spark is hidden at 1 and runs 0 → 1 once per switch-on.
    val spark = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    val knob = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        on = !on
        job?.cancel()
        job = scope.launch {
            if (on) {
                spark.snapTo(0f)
                launch { spark.animateTo(1f, tween(LapMillis, easing = FastOutSlowInEasing)) }
                // Light up as the spark closes the loop.
                delay((LapMillis * 0.8f).toLong())
                launch { glow.animateTo(1f, tween(320)) }
                knob.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 420f))
            } else {
                spark.snapTo(1f)
                launch { glow.animateTo(0f, tween(240)) }
                knob.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 420f))
            }
        }
    }

    SwitchStage(
        2,
        "Neon Glow",
        "A spark races around the outline, then the toggle glows in neon.",
        autoplay = { if (!on) toggle() },
    ) {
        Box(
            Modifier
                .size(TrackWidth, TrackHeight)
                .drawWithCache {
                    val line = 2.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val r = h / 2 - line / 2
                    val left = Rect(Offset(line / 2, line / 2), Size(r * 2, r * 2))
                    val right = left.translate(w - h, 0f)
                    // Starts at the left end, by the knob, and runs clockwise over the top.
                    val outline = Path().apply {
                        arcTo(left, 180f, 90f, forceMoveTo = true)
                        lineTo(w - h / 2, line / 2)
                        arcTo(right, -90f, 180f, forceMoveTo = false)
                        lineTo(h / 2, h - line / 2)
                        arcTo(left, 90f, 90f, forceMoveTo = false)
                    }
                    val measure = PathMeasure().apply { setPath(outline, false) }
                    val length = measure.length
                    val segment = Path()
                    val knobRadius = KnobSize.toPx() / 2
                    val inset = KnobInset.toPx()
                    val travel = KnobTravel.toPx()
                    val dot = 3.dp.toPx()
                    val lineStroke = Stroke(line)
                    val haloWide = Stroke(line * 8f)
                    val haloMid = Stroke(line * 4.5f)
                    val haloNarrow = Stroke(line * 2.2f)
                    val trailStrokes = Array(TrailSegments) { i ->
                        Stroke(line * (1f + 1.5f * (1f - i / TrailSegments.toFloat())), cap = StrokeCap.Round)
                    }
                    onDrawBehind {
                        val g = glow.value
                        val k = knob.value
                        val s = spark.value
                        val color = lerp(NeonOff, MenuColors.Cyan, g)
                        if (g > 0f) {
                            drawPath(outline, MenuColors.Cyan, alpha = 0.06f * g)
                            drawPath(outline, MenuColors.Cyan, alpha = 0.08f * g, style = haloWide)
                            drawPath(outline, MenuColors.Cyan, alpha = 0.16f * g, style = haloMid)
                            drawPath(outline, MenuColors.Cyan, alpha = 0.35f * g, style = haloNarrow)
                        }
                        drawPath(outline, color, style = lineStroke)

                        val knobCenter = Offset(inset + knobRadius + travel * k, h / 2)
                        if (g > 0f) {
                            drawCircle(MenuColors.Cyan, knobRadius * 1.5f, knobCenter, alpha = 0.1f * g)
                            drawCircle(MenuColors.Cyan, knobRadius * 1.22f, knobCenter, alpha = 0.18f * g)
                            drawCircle(MenuColors.Cyan, knobRadius, knobCenter, alpha = g)
                            drawCircle(NeonCore, knobRadius * 0.55f, knobCenter, alpha = 0.8f * g)
                        }
                        drawCircle(color, knobRadius - line / 2, knobCenter, style = lineStroke)

                        if (s < 1f) {
                            val fade = 1f - smoothstep(0.85f, 1f, s)
                            val head = s * length
                            val tail = length * TrailFraction
                            for (i in 0 until TrailSegments) {
                                val stop = head - tail * i / TrailSegments
                                val start = (head - tail * (i + 1) / TrailSegments).coerceAtLeast(0f)
                                if (stop <= 0f) break
                                segment.reset()
                                measure.getSegment(start, stop, segment, true)
                                val strength = 1f - i / TrailSegments.toFloat()
                                drawPath(segment, MenuColors.Cyan, alpha = fade * strength, style = trailStrokes[i])
                            }
                            val at = measure.getPosition(head)
                            drawCircle(MenuColors.Cyan, dot * 4.5f, at, alpha = 0.12f * fade)
                            drawCircle(MenuColors.Cyan, dot * 2.5f, at, alpha = 0.35f * fade)
                            drawCircle(Color.White, dot, at, alpha = fade)
                        }
                    }
                }
                .toggleable(on, interactionSource = null, indication = null, role = Role.Switch) { toggle() },
        )
    }
}
