package com.azersun.uitemplates.demos.checkbox2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckBoxMark
import com.azersun.uitemplates.demos.checkbox1.CheckColors
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import com.azersun.uitemplates.demos.checkbox1.checkPop

private val Days = listOf("Mon" to "Gym", "Tue" to "Run", "Wed" to "Yoga", "Thu" to "Swim")
private val RowHeight = 58.dp
private val BoxSize = 26.dp

@Composable
fun DragSelectDemo() {
    val checked = remember { mutableStateListOf(false, false, false, false) }
    // The scripted finger, in row units (0.5 is the centre of the first row).
    val ghostY = remember { Animatable(0.5f) }
    val ghostAlpha = remember { Animatable(0f) }
    var ghostVisible by remember { mutableStateOf(false) }

    fun set(row: Int, value: Boolean) {
        if (checked[row] != value) checked[row] = value
    }

    CheckboxStage(
        1,
        "Drag Select",
        "Press and drag down the list: every box you pass checks in turn.",
        onReplay = { checked.indices.forEach { checked[it] = false } },
        autoplay = {
            try {
                ghostVisible = true
                ghostY.snapTo(0.5f)
                ghostAlpha.snapTo(0f)
                ghostAlpha.animateTo(1f, tween(200))
                set(0, true)
                ghostY.animateTo(Days.size - 0.5f, tween(1300, delayMillis = 150, easing = FastOutSlowInEasing)) {
                    set(value.toInt().coerceIn(0, Days.lastIndex), true)
                }
                ghostAlpha.animateTo(0f, tween(260, delayMillis = 120))
            } finally {
                ghostVisible = false
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Text("This week", color = CheckColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .checkPanel()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .pointerInput(Unit) {
                        val rowPx = RowHeight.toPx()
                        fun rowAt(y: Float) = (y / rowPx).toInt().coerceIn(0, Days.lastIndex)
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            var last = rowAt(down.position.y)
                            // The first box decides the mode: start on an empty one to check, a checked one to clear.
                            val target = !checked[last]
                            set(last, target)
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                val row = rowAt(change.position.y)
                                // A fast flick can jump several rows in one event; fill in the ones it skipped.
                                for (i in minOf(row, last)..maxOf(row, last)) set(i, target)
                                last = row
                            }
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (ghostVisible) {
                            val alpha = ghostAlpha.value
                            val center = Offset(BoxSize.toPx() / 2, ghostY.value * RowHeight.toPx())
                            drawCircle(Color.White, 22.dp.toPx(), center, alpha = 0.14f * alpha)
                            drawCircle(Color.White, 13.dp.toPx(), center, alpha = 0.3f * alpha)
                        }
                    },
            ) {
                Days.forEachIndexed { i, (day, activity) ->
                    DayRow(day, activity, checked[i], divider = i > 0)
                }
            }
            Text(
                "Press a box and drag down",
                color = CheckColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun DayRow(day: String, activity: String, checked: Boolean, divider: Boolean) {
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(160), label = "fill")
    val tick by animateFloatAsState(
        if (checked) 1f else 0f,
        tween(if (checked) 240 else 120, delayMillis = if (checked) 60 else 0),
        label = "tick",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        if (checked) pop.checkPop(dip = 0.9f, peak = 1.18f) else pop.animateTo(1f, spring())
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawBehind {
                if (divider) drawLine(CheckColors.PanelBorder, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBoxMark(
            { fill },
            { tick },
            Modifier
                .size(BoxSize)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
        )
        Spacer(Modifier.width(16.dp))
        Text(day, color = CheckColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(40.dp))
        Text(activity, color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
