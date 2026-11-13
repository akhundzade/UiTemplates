package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Node centres as fractions of the canvas: low, up, low, up-right. */
private val NodeSpots = listOf(Offset(0.13f, 0.6f), Offset(0.38f, 0.24f), Offset(0.62f, 0.6f), Offset(0.87f, 0.24f))
private val NodeRadius = 22.dp
private val HopHeight = 9.dp
private const val HopMillis = 1100
private const val FillMillis = 650

private val Gold = MenuColors.Yellow
private val GoldRim = Color(0xFFC98A12)
private val BlueFace = Color(0xFF3B82F6)
private val BlueRim = Color(0xFF1E4FB8)
private val LockedFace = MenuColors.TileLight
private val LockedRim = Color(0xFF1C1A38)
private val LabelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)

private enum class NodeStatus { Done, Current, Locked }

/** A node counts as current only while the path progress sits right on it. */
private fun statusOf(index: Int, progress: Float): NodeStatus = when {
    index < progress - 0.05f -> NodeStatus.Done
    index > progress + 0.05f -> NodeStatus.Locked
    else -> NodeStatus.Current
}

@Composable
fun SkillPathDemo() {
    StepsScene(
        number = 4,
        title = "Skill Path",
        caption = "Done nodes turn gold with a tick, the current one hops; advancing fills the dotted path and unlocks the next node with a pop.",
    ) { state ->
        val progress = remember { Animatable(StartStep.toFloat()) }
        val pops = remember { List(StepNames.size) { Animatable(1f) } }
        val hop = rememberInfiniteTransition(label = "hop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(HopMillis, easing = LinearEasing)),
            label = "hop",
        )
        val measurer = rememberTextMeasurer()
        // Pops outlive a restarted effect, so a quick Back never leaves a node half-popped.
        val popScope = rememberCoroutineScope()

        LaunchedEffect(state.step) {
            val from = progress.value.roundToInt()
            if (from == state.step) return@LaunchedEffect
            popScope.launch { pops[from].pop() }
            progress.animateTo(state.step.toFloat(), tween(FillMillis, easing = FastOutSlowInEasing))
            popScope.launch { pops[state.step].pop() }
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .drawWithCache {
                    val r = NodeRadius.toPx()
                    val centers = NodeSpots.map { Offset(it.x * size.width, it.y * size.height) }
                    val labels = StepNames.map { measurer.measure(it, LabelStyle) }
                    val icons = NodeIcons()
                    val dotGap = 9.dp.toPx()
                    val dotRadius = 2.5.dp.toPx()
                    val inset = r + 7.dp.toPx()
                    onDrawBehind {
                        val p = progress.value
                        val h = hop.value
                        for (i in 0 until LastStep) {
                            val a = centers[i]
                            val b = centers[i + 1]
                            val dir = (b - a) / (b - a).getDistance()
                            val start = a + dir * inset
                            val end = b - dir * inset
                            val count = ((end - start).getDistance() / dotGap).toInt().coerceAtLeast(1)
                            val filled = (p - i).coerceIn(0f, 1f)
                            for (j in 0..count) {
                                val f = j / count.toFloat()
                                drawCircle(if (f <= filled) Gold else StepColors.Track, dotRadius, start + (end - start) * f)
                            }
                        }
                        for (i in centers.indices) {
                            val status = statusOf(i, p)
                            val c = centers[i]
                            val lift = if (status == NodeStatus.Current) hopLift(h) * HopHeight.toPx() else 0f
                            val shadow = 1f - 0.35f * lift / HopHeight.toPx()
                            drawOval(
                                Color.Black,
                                topLeft = Offset(c.x - r * shadow, c.y + r + 2.dp.toPx()),
                                size = Size(2 * r * shadow, 6.dp.toPx()),
                                alpha = 0.25f,
                            )
                            val scale = pops[i].value
                            withTransform({
                                translate(c.x, c.y - lift)
                                scale(scale, scale, Offset.Zero)
                            }) { drawNode(status, r, icons) }
                            val label = labels[i]
                            drawText(
                                label,
                                color = when (status) {
                                    NodeStatus.Done -> Gold
                                    NodeStatus.Current -> MenuColors.Text
                                    NodeStatus.Locked -> MenuColors.Muted
                                },
                                topLeft = Offset(c.x - label.size.width / 2f, c.y + r + 12.dp.toPx()),
                            )
                        }
                    }
                },
        )
    }
}

private suspend fun Animatable<Float, AnimationVector1D>.pop() {
    snapTo(0.7f)
    animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 520f))
}

/** A quick hop at the start of each cycle, then a rest on the ground. */
private fun hopLift(t: Float): Float {
    val s = t / 0.45f
    return if (s >= 1f) 0f else 4f * s * (1f - s)
}

/** Icon paths in a unit space (±1 around the node centre), built once per size. */
private class NodeIcons {
    val star = Path().apply {
        for (k in 0 until 10) {
            val angle = -PI / 2 + k * PI / 5
            val radius = if (k % 2 == 0) 1f else 0.45f
            val x = (cos(angle) * radius).toFloat()
            val y = (sin(angle) * radius).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    val check = Path().apply {
        moveTo(-0.55f, 0.02f)
        lineTo(-0.15f, 0.42f)
        lineTo(0.6f, -0.38f)
    }
    val checkStroke = Stroke(width = 0.24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val shackleStroke = Stroke(width = 0.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawNode(status: NodeStatus, r: Float, icons: NodeIcons) {
    val face = when (status) {
        NodeStatus.Done -> Gold
        NodeStatus.Current -> BlueFace
        NodeStatus.Locked -> LockedFace
    }
    val rim = when (status) {
        NodeStatus.Done -> GoldRim
        NodeStatus.Current -> BlueRim
        NodeStatus.Locked -> LockedRim
    }
    if (status == NodeStatus.Current) drawCircle(StepColors.Cyan, r * 1.35f, Offset.Zero, alpha = 0.18f)
    drawCircle(rim, r, Offset(0f, 3.dp.toPx()))
    drawCircle(face, r, Offset.Zero)
    val icon = r * 0.48f
    withTransform({ scale(icon, icon, Offset.Zero) }) {
        when (status) {
            NodeStatus.Done -> drawPath(icons.check, Color.White, style = icons.checkStroke)
            NodeStatus.Current -> drawPath(icons.star, Color.White)
            NodeStatus.Locked -> {
                drawArc(
                    MenuColors.Muted,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(-0.4f, -0.85f),
                    size = Size(0.8f, 0.9f),
                    style = icons.shackleStroke,
                )
                drawLine(MenuColors.Muted, Offset(-0.4f, -0.4f), Offset(-0.4f, -0.1f), 0.2f)
                drawLine(MenuColors.Muted, Offset(0.4f, -0.4f), Offset(0.4f, -0.1f), 0.2f)
                drawRoundRect(MenuColors.Muted, Offset(-0.62f, -0.12f), Size(1.24f, 0.92f), CornerRadius(0.2f))
            }
        }
    }
}
