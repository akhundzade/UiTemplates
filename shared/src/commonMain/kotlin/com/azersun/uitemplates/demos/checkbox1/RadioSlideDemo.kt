package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private class Option(val title: String, val detail: String)

private val Options = listOf(Option("Standard", "3–5 days"), Option("Express", "Tomorrow"), Option("Pickup", "Today"))
private val RowHeight = 64.dp
private val RowGap = 10.dp
private val RadioInset = 18.dp
private val RingRadius = 11.dp
private val DotRadius = 6.dp

@Composable
fun RadioSlideDemo() {
    var selected by remember { mutableIntStateOf(0) }
    // Positions are in row units; the dot is drawn as a blob stretched between the two ends.
    val lead = remember { Animatable(0f) }
    val trail = remember { Animatable(0f) }
    val highlight = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        val target = selected.toFloat()
        launch { lead.animateTo(target, spring(dampingRatio = 0.62f, stiffness = 520f)) }
        launch {
            delay(60)
            trail.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 150f))
        }
        highlight.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 320f))
    }

    CheckboxStage(
        4,
        "Radio Slide",
        "The selected dot stretches like goo as it slides to the next option.",
        autoplay = {
            selected = 1
            delay(1200)
            selected = 2
        },
    ) {
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                val pitch = (RowHeight + RowGap).toPx()
                val rowSize = Size(size.width, RowHeight.toPx())
                val corner = CornerRadius(18.dp.toPx())
                Options.indices.forEach { i ->
                    drawRoundRect(CheckColors.Panel, Offset(0f, i * pitch), rowSize, corner)
                    drawRoundRect(CheckColors.PanelBorder, Offset(0f, i * pitch), rowSize, corner, style = Stroke(1.dp.toPx()))
                }
                val y = highlight.value * pitch
                drawRoundRect(CheckColors.Blue.copy(alpha = 0.12f), Offset(0f, y), rowSize, corner)
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    CheckColors.Blue,
                    Offset(stroke / 2, y + stroke / 2),
                    Size(rowSize.width - stroke, rowSize.height - stroke),
                    CornerRadius(corner.x - stroke / 2),
                    style = Stroke(stroke),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
                Options.forEachIndexed { i, option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .clickable(interactionSource = null, indication = null, role = Role.RadioButton) { selected = i }
                            .padding(start = RadioInset + RingRadius * 2 + 14.dp, end = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option.title, color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(option.detail, color = CheckColors.Muted, fontSize = 13.sp)
                    }
                }
            }
            val goo = remember { Path() }
            Canvas(Modifier.matchParentSize()) {
                val pitch = (RowHeight + RowGap).toPx()
                val half = RowHeight.toPx() / 2
                val cx = (RadioInset + RingRadius).toPx()
                Options.indices.forEach { i ->
                    val on = (1f - abs(highlight.value - i)).coerceIn(0f, 1f)
                    drawCircle(
                        lerp(CheckColors.Outline, CheckColors.Blue, on),
                        RingRadius.toPx() - 1.dp.toPx(),
                        Offset(cx, i * pitch + half),
                        style = Stroke(2.dp.toPx()),
                    )
                }
                val a = lead.value * pitch + half
                val b = trail.value * pitch + half
                val top = min(a, b)
                val bottom = max(a, b)
                val r = DotRadius.toPx()
                drawCircle(CheckColors.Blue, r, Offset(cx, top))
                drawCircle(CheckColors.Blue, r, Offset(cx, bottom))
                val distance = bottom - top
                if (distance > 0.5f) {
                    // The neck thins as the blob stretches; a quad's midpoint sits halfway to its control,
                    // so the control is pushed to 2·neck − r to make the waist exactly `neck` wide.
                    val neck = r * (1f - 0.5f * distance / (distance + pitch * 0.5f))
                    val control = 2 * neck - r
                    val mid = (top + bottom) / 2
                    goo.reset()
                    goo.moveTo(cx - r, top)
                    goo.quadraticTo(cx - control, mid, cx - r, bottom)
                    goo.lineTo(cx + r, bottom)
                    goo.quadraticTo(cx + control, mid, cx + r, top)
                    goo.close()
                    drawPath(goo, CheckColors.Blue)
                }
            }
        }
    }
}
