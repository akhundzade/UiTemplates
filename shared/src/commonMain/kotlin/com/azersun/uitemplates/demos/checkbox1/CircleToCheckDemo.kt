package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val FlowMillis = 750

@Composable
fun CircleToCheckDemo() {
    var done by remember { mutableStateOf(false) }
    // Linear on purpose: the head and tail of the stroke each apply their own easing over a sub-range.
    val flow by animateFloatAsState(if (done) 1f else 0f, tween(FlowMillis, easing = LinearEasing), label = "flow")
    val labelColor by animateColorAsState(
        if (done) CheckColors.Muted.copy(alpha = 0.7f) else CheckColors.Text,
        tween(400, delayMillis = if (done) 250 else 0),
        label = "label",
    )

    CheckboxStage(
        1,
        "Circle to Check",
        "The ring unwinds and its stroke flows into a ✓. The label fades to gray.",
        autoplay = { done = true },
    ) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { done = !done }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(30.dp)
                    .drawWithCache {
                        val stroke = 2.6.dp.toPx()
                        val side = size.minDimension
                        val radius = side / 2 - stroke / 2
                        val ring = Rect(size.center, radius)
                        // One stroke: counter-clockwise round the ring from 9 o'clock, then straight on into the ✓,
                        // so sliding a fixed-width window along it hands the ring's ink over to the check.
                        fun Path.ring() {
                            moveTo(ring.left, size.center.y)
                            arcTo(ring, 180f, -180f, false)
                            arcTo(ring, 0f, -180f, false)
                        }
                        val checkFrom = Offset(side * 0.27f, side * 0.53f)
                        val path = Path().apply {
                            ring()
                            lineTo(checkFrom.x, checkFrom.y)
                            lineTo(side * 0.44f, side * 0.69f)
                            lineTo(side * 0.76f, side * 0.35f)
                        }
                        val ringLength = PathMeasure().apply { setPath(Path().apply { ring() }, false) }.length
                        val trim = PathTrim(path)
                        val checkStart = ringLength + (checkFrom - Offset(ring.left, size.center.y)).getDistance()
                        val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            val t = flow
                            val head = FastOutSlowInEasing.transform((t / 0.7f).coerceIn(0f, 1f))
                            val tail = FastOutSlowInEasing.transform(((t - 0.18f) / 0.82f).coerceIn(0f, 1f))
                            drawPath(
                                trim.between(mix(0f, checkStart, tail), mix(ringLength, trim.length, head)),
                                lerp(CheckColors.Outline, CheckColors.Cyan, t),
                                style = style,
                            )
                        }
                    },
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Water the plants", color = labelColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Today · 9:00", color = CheckColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
