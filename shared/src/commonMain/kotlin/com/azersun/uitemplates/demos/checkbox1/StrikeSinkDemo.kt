package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Tasks = listOf("Buy milk", "Call mom", "Book flights")
private val InitialOrder = Tasks.indices.toList()

@Composable
fun StrikeSinkDemo() {
    // Ids in the order they were checked, so the latest one always sinks to the very bottom.
    var checked by remember { mutableStateOf(emptyList<Int>()) }
    var order by remember { mutableStateOf(InitialOrder) }
    val scope = rememberCoroutineScope()

    fun toggle(id: Int) {
        val nowChecked = id !in checked
        checked = if (nowChecked) checked + id else checked - id
        scope.launch {
            // Let the fill and strike play in place before the row moves.
            delay(if (nowChecked) 560 else 240)
            order = InitialOrder.filter { it !in checked } + checked
        }
    }

    CheckboxStage(
        2,
        "Strike & Sink",
        "Tap: the box fills, a line strikes the task, the row sinks to the bottom.",
        onReplay = {
            checked = emptyList()
            order = InitialOrder
        },
        autoplay = {
            toggle(0)
            delay(1400)
            toggle(2)
        },
    ) {
        LazyColumn(
            Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
        ) {
            items(order, key = { it }) { id ->
                TaskRow(
                    title = Tasks[id],
                    done = id in checked,
                    onToggle = { toggle(id) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = spring(dampingRatio = 0.78f, stiffness = 260f, visibilityThreshold = IntOffset(1, 1)),
                    ),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(title: String, done: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(if (done) 1f else 0f, tween(180), label = "fill")
    val tick by animateFloatAsState(
        if (done) 1f else 0f,
        tween(if (done) 260 else 140, delayMillis = if (done) 90 else 0, easing = FastOutSlowInEasing),
        label = "tick",
    )
    val strike by animateFloatAsState(
        if (done) 1f else 0f,
        tween(if (done) 320 else 200, delayMillis = if (done) 200 else 0, easing = FastOutSlowInEasing),
        label = "strike",
    )
    val dim by animateFloatAsState(if (done) 0.5f else 1f, tween(300, delayMillis = if (done) 380 else 0), label = "dim")

    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = dim }
            .checkPanel()
            .clickable(interactionSource = null, indication = null, role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBoxMark({ fill }, { tick }, Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            color = CheckColors.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.drawWithContent {
                drawContent()
                val progress = strike
                if (progress > 0f) {
                    val y = size.height * 0.54f
                    drawLine(
                        CheckColors.Text,
                        Offset(-2.dp.toPx(), y),
                        Offset(-2.dp.toPx() + (size.width + 4.dp.toPx()) * progress, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            },
        )
    }
}
