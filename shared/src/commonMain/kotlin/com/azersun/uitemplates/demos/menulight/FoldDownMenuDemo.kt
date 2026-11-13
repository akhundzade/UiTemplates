package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

private const val FoldedAngle = -90f
private const val UnfoldStaggerMillis = 110L
private const val FoldStaggerMillis = 50L

@Composable
fun FoldDownMenuDemo() {
    val state = remember { MinimalMenuState() }
    val scope = rememberCoroutineScope()
    val edge = remember { Animatable(0f) }
    LaunchedEffect(state.open) {
        if (state.open) {
            edge.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        } else {
            delay(FoldStaggerMillis * MenuOption.entries.size + 120)
            edge.animateTo(0f, tween(160, easing = FastOutLinearInEasing))
        }
    }

    MinimalMenuScene(state, 4, "Fold Down", "The menu unfolds downward under the ⋮ like folded paper, one panel at a time.") {
        MinimalPage(state) { KebabButton(state.open, onClick = { state.open = !state.open }) }
        if (state.open) {
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { state.open = false })
        }
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = PagePadding + KebabSize + 10.dp, end = PagePadding)
                .width(184.dp),
        ) {
            MenuOption.entries.forEachIndexed { i, option ->
                FoldPanel(index = i, open = state.open) {
                    OptionRow(
                        option,
                        active = option == state.current,
                        enabled = state.open,
                        onClick = { scope.launch { state.pick(option) } },
                        modifier = Modifier.height(44.dp),
                    ) {
                        Caps("0${i + 1}", color = LightColors.Hairline, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Normal)
                    }
                }
            }
            // The reel's black bottom edge rides along under the last unfolded panel.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer {
                        scaleX = edge.value
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
                    .background(LightColors.Ink),
            )
        }
    }
}

/**
 * One paper panel hinged on its top edge. Its layout height follows the projected height
 * (h·cos θ) so the panels below stay attached to its bottom edge while it turns.
 */
@Composable
private fun FoldPanel(index: Int, open: Boolean, content: @Composable () -> Unit) {
    val angle = remember { Animatable(FoldedAngle) }
    LaunchedEffect(open) {
        if (open) {
            delay(80L + index * UnfoldStaggerMillis)
            angle.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f))
        } else {
            delay((MenuOption.entries.lastIndex - index) * FoldStaggerMillis)
            angle.animateTo(FoldedAngle, tween(170, easing = FastOutLinearInEasing))
        }
    }
    Box(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val projected = cos(angle.value * PI.toFloat() / 180f).coerceAtLeast(0f)
                layout(placeable.width, (placeable.height * projected).roundToInt().coerceAtLeast(0)) {
                    placeable.place(0, 0)
                }
            }
            .graphicsLayer {
                rotationX = angle.value
                transformOrigin = TransformOrigin(0.5f, 0f)
                cameraDistance = 12f * density
                alpha = if (angle.value <= FoldedAngle + 0.5f) 0f else 1f
            }
            .drawWithContent {
                drawContent()
                val shade = 1f - cos(angle.value * PI.toFloat() / 180f)
                if (shade > 0.001f) drawRect(Color.Black, alpha = 0.28f * shade.coerceIn(0f, 1f))
            }
            .background(LightColors.Page)
            .drawBehind {
                val line = 1.dp.toPx()
                val half = line / 2
                if (index == 0) drawLine(LightColors.Hairline, Offset(0f, half), Offset(size.width, half), line)
                drawLine(LightColors.Hairline, Offset(half, 0f), Offset(half, size.height), line)
                drawLine(LightColors.Hairline, Offset(size.width - half, 0f), Offset(size.width - half, size.height), line)
                drawLine(LightColors.Faint, Offset(0f, size.height - half), Offset(size.width, size.height - half), line)
            },
    ) { content() }
}
