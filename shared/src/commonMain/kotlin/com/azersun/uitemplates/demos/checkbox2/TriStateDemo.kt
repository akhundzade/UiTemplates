package com.azersun.uitemplates.demos.checkbox2

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckColors
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import com.azersun.uitemplates.demos.checkbox1.drawTriStateBox
import kotlinx.coroutines.delay

private val States = listOf("None", "Some", "All")
private val SegmentWidth = 54.dp

@Composable
fun TriStateDemo() {
    var state by remember { mutableIntStateOf(0) }
    var changes by remember { mutableIntStateOf(0) }
    // 0 → 1 grows the dash, 1 → 2 bends it into the ✓; going 2 → 0 passes back through the dash.
    val mark by animateFloatAsState(state.toFloat(), tween(340, easing = FastOutSlowInEasing), label = "mark")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(changes) {
        if (changes == 0) return@LaunchedEffect
        pop.animateTo(0.88f, tween(90))
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
    }

    fun select(next: Int) {
        if (next == state) return
        state = next
        changes++
    }

    CheckboxStage(
        5,
        "Tri-state",
        "Tap cycles: empty → – (some) → ✓ (all). The dash bends into a check.",
        autoplay = {
            select(1)
            delay(1100)
            select(2)
        },
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { select((state + 1) % States.size) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        }
                        .drawWithCache {
                            val path = Path()
                            onDrawBehind { drawTriStateBox(Offset.Zero, size.minDimension, mark, path) }
                        },
                )
                Spacer(Modifier.width(16.dp))
                Text("Sync folders", color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            StatePill(state, ::select, Modifier.padding(start = 46.dp, top = 12.dp))
        }
    }
}

@Composable
private fun StatePill(state: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val indicator by animateDpAsState(
        SegmentWidth * state,
        spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "indicator",
    )
    val tint by animateColorAsState(
        if (state == 0) CheckColors.Outline.copy(alpha = 0.45f) else CheckColors.Blue.copy(alpha = 0.32f),
        tween(240),
        label = "tint",
    )
    Box(
        modifier
            .background(CheckColors.Track, RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = indicator)
                .width(SegmentWidth)
                .height(26.dp)
                .background(tint, RoundedCornerShape(50)),
        )
        Row {
            States.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .width(SegmentWidth)
                        .height(26.dp)
                        .clickable(interactionSource = null, indication = null, role = Role.RadioButton) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (i == state) CheckColors.Text else CheckColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
