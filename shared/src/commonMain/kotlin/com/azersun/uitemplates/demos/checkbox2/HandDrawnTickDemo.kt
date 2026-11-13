package com.azersun.uitemplates.demos.checkbox2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckColors
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.PathTrim
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val Ink = Color(0xFF8FB3FF)
private val HandEasing = CubicBezierEasing(0.5f, 0f, 0.35f, 1f)
private val BoxSide = 34.dp
private const val PencilAngle = -55f

@Composable
fun HandDrawnTickDemo() {
    var done by remember { mutableStateOf(false) }
    val draw = remember { Animatable(0f) }
    val pencil = remember { Animatable(0f) }
    val away = remember { Animatable(0f) }
    val labelAlpha by animateFloatAsState(
        if (done) 0.4f else 1f,
        tween(420, delayMillis = if (done) 600 else 0),
        label = "label",
    )
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        done = !done
        job?.cancel()
        job = scope.launch {
            if (done) {
                away.snapTo(0f)
                pencil.animateTo(1f, tween(160))
                draw.animateTo(1f, tween(820, easing = HandEasing))
                launch { away.animateTo(1f, tween(360)) }
                pencil.animateTo(0f, tween(360))
            } else {
                pencil.snapTo(0f)
                draw.animateTo(0f, tween(260))
            }
        }
    }

    CheckboxStage(
        2,
        "Hand-drawn Tick",
        "A pencil scribbles a hand-drawn ✓ that overshoots the box.",
        autoplay = { toggle() },
    ) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { toggle() }
                .padding(start = 22.dp, end = 18.dp, top = 30.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(BoxSide)
                    .drawWithCache {
                        val s = size.minDimension
                        fun Path.at(x: Float, y: Float) = moveTo(x * s, y * s)
                        fun Path.curve(cx: Float, cy: Float, x: Float, y: Float) = quadraticTo(cx * s, cy * s, x * s, y * s)
                        // Two slightly different loops, like a box gone over twice by hand.
                        val box = Path().apply {
                            at(0.05f, 0.09f)
                            curve(0.5f, 0.01f, 0.97f, 0.05f)
                            curve(1.03f, 0.5f, 0.95f, 0.97f)
                            curve(0.5f, 1.03f, 0.04f, 0.94f)
                            curve(-0.03f, 0.5f, 0.08f, 0.02f)
                        }
                        val retrace = Path().apply {
                            at(0.02f, 0.05f)
                            curve(0.52f, -0.03f, 1.0f, 0.08f)
                            curve(0.98f, 0.55f, 0.99f, 1.0f)
                            curve(0.45f, 0.98f, 0.0f, 0.97f)
                            curve(0.03f, 0.45f, 0.01f, 0.1f)
                        }
                        // Overshoots well past the top-right corner, the way a quick hand flicks out.
                        val tick = PathTrim(
                            Path().apply {
                                at(0.16f, 0.46f)
                                curve(0.3f, 0.58f, 0.4f, 0.84f)
                                curve(0.66f, 0.26f, 1.34f, -0.36f)
                            },
                        )
                        val boxStroke = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val retraceStroke = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val tickStroke = Stroke(3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            val d = draw.value
                            val p = pencil.value
                            val lift = away.value
                            drawPath(box, CheckColors.Text, alpha = 0.9f, style = boxStroke)
                            drawPath(retrace, CheckColors.Text, alpha = 0.35f, style = retraceStroke)
                            if (d > 0f) drawPath(tick.fraction(0f, d), Ink, style = tickStroke)
                            if (p > 0f) {
                                val tip = tick.position(tick.length * d)
                                val up = 18.dp.toPx() * lift
                                drawPencil(tip + Offset(up * 0.57f, -up * 0.82f), p)
                            }
                        }
                    },
            )
            Spacer(Modifier.width(18.dp))
            Text(
                "Call the dentist",
                color = CheckColors.Text,
                fontSize = 23.sp,
                fontFamily = FontFamily.Cursive,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.graphicsLayer { alpha = labelAlpha },
            )
        }
    }
}

/** A small yellow pencil with its graphite point at [tip], leaning up and to the right like a held pencil. */
private fun DrawScope.drawPencil(tip: Offset, alpha: Float) {
    val u = 1.dp.toPx()
    translate(tip.x, tip.y) {
        rotate(PencilAngle, pivot = Offset.Zero) {
            val wood = Path().apply {
                moveTo(0f, 0f)
                lineTo(11 * u, -4 * u)
                lineTo(11 * u, 4 * u)
                close()
            }
            val lead = Path().apply {
                moveTo(0f, 0f)
                lineTo(4 * u, -1.5f * u)
                lineTo(4 * u, 1.5f * u)
                close()
            }
            drawPath(wood, Color(0xFFF2D3A0), alpha = alpha)
            drawPath(lead, Color(0xFF3A3A4A), alpha = alpha)
            drawRect(Color(0xFFFFC53D), Offset(11 * u, -4 * u), Size(27 * u, 8 * u), alpha = alpha)
            drawRect(Color(0xFFE0A42A), Offset(11 * u, 1.4f * u), Size(27 * u, 2.6f * u), alpha = alpha)
            drawRect(Color(0xFFC9CBD6), Offset(38 * u, -4 * u), Size(4 * u, 8 * u), alpha = alpha)
            drawRoundRect(Color(0xFFFF8FB1), Offset(41 * u, -4 * u), Size(7 * u, 8 * u), CornerRadius(2.5f * u), alpha = alpha)
        }
    }
}
