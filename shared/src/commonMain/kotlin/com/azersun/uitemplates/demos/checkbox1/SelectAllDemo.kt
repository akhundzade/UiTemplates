package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private class Item(val name: String, val size: String)

private val Items = listOf(Item("Photos", "1.2 GB"), Item("Videos", "3.4 GB"), Item("Documents", "0.8 GB"))
private const val StaggerMillis = 180L
private val ParentRowHeight = 44.dp
private val ParentBox = 26.dp
private val ChildRowHeight = 48.dp
private val ChildIndent = 40.dp
private val TreeGap = 4.dp

@Composable
fun SelectAllDemo() {
    val checked = remember { mutableStateListOf(false, false, false) }
    val count = checked.count { it }
    val mark by animateFloatAsState(
        when (count) {
            0 -> 0f
            Items.size -> 2f
            else -> 1f
        },
        tween(280, easing = FastOutSlowInEasing),
        label = "parentMark",
    )
    val fills = List(Items.size) { i -> animateFloatAsState(if (checked[i]) 1f else 0f, tween(200), label = "fill$i") }
    val ticks = List(Items.size) { i ->
        animateFloatAsState(if (checked[i]) 1f else 0f, tween(240, delayMillis = if (checked[i]) 60 else 0), label = "tick$i")
    }
    val scope = rememberCoroutineScope()
    var cascade by remember { mutableStateOf<Job?>(null) }

    fun toggleAll() {
        cascade?.cancel()
        val target = checked.any { !it }
        val order = if (target) Items.indices else Items.indices.reversed()
        cascade = scope.launch {
            order.filter { checked[it] != target }.forEachIndexed { step, i ->
                if (step > 0) delay(StaggerMillis)
                checked[i] = target
            }
        }
    }

    fun toggle(i: Int) {
        cascade?.cancel()
        checked[i] = !checked[i]
    }

    CheckboxStage(
        3,
        "Select All",
        "Tap \"Select all\": the items check one by one, the parent goes – then ✓.",
        autoplay = { toggleAll() },
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(ParentRowHeight)
                    .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { toggleAll() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    Modifier
                        .size(ParentBox)
                        .drawWithCache {
                            val path = Path()
                            onDrawBehind { drawTriStateBox(Offset.Zero, size.minDimension, mark, path) }
                        },
                )
                Spacer(Modifier.width(14.dp))
                Text("Select all", color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                CounterPill(count, Items.size)
            }
            Spacer(Modifier.height(TreeGap))
            Column(Modifier.fillMaxWidth().drawTree(fills)) {
                Items.forEachIndexed { i, item ->
                    val fill by fills[i]
                    val tick by ticks[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(ChildRowHeight)
                            .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { toggle(i) }
                            .padding(start = ChildIndent),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBoxMark(
                            { fill },
                            { tick },
                            Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    // A one-hump pop that plays both ways without its own animation.
                                    val pop = 1f + 0.14f * sin(fill * PI.toFloat())
                                    scaleX = pop
                                    scaleY = pop
                                },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(item.name, color = CheckColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(item.size, color = CheckColors.Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** The tree connecting the parent box to each child; a branch turns blue as its child fills. */
private fun Modifier.drawTree(fills: List<State<Float>>): Modifier = drawBehind {
    val stroke = 2.dp.toPx()
    val trunkX = ParentBox.toPx() / 2
    val rowHeight = ChildRowHeight.toPx()
    // Reach up past the gap and the lower half of the parent row to the bottom of the parent box.
    val top = -(TreeGap.toPx() + (ParentRowHeight.toPx() - ParentBox.toPx()) / 2)
    val lastY = rowHeight * (fills.size - 0.5f)
    val branchEnd = ChildIndent.toPx() - 6.dp.toPx()
    drawLine(CheckColors.Track, Offset(trunkX, top), Offset(trunkX, lastY), stroke)
    val radius = 8.dp.toPx()
    fills.forEachIndexed { i, fill ->
        val y = rowHeight * (i + 0.5f)
        val color = lerp(CheckColors.Track, CheckColors.Blue, fill.value)
        val elbow = Path().apply {
            moveTo(trunkX, y - radius)
            quadraticTo(trunkX, y, trunkX + radius, y)
            lineTo(branchEnd, y)
        }
        drawPath(elbow, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun CounterPill(count: Int, total: Int) {
    val all = count == total
    val background by animateColorAsState(
        if (all) CheckColors.Blue.copy(alpha = 0.28f) else CheckColors.Track,
        tween(300),
        label = "pill",
    )
    Row(
        Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            count,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn(tween(160))) togetherWith
                    (slideOutVertically { if (up) -it else it } + fadeOut(tween(160)))
            },
            label = "count",
        ) { value ->
            Text("$value", color = CheckColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text("/$total", color = if (all) CheckColors.Text else CheckColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
