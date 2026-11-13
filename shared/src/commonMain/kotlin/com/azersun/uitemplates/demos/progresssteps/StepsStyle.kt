package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal val StepNames = listOf("Account", "Profile", "Plan", "Done")
internal const val StartStep = 1
internal val LastStep = StepNames.lastIndex

internal object StepColors {
    val Blue = Color(0xFF3B82F6)
    val Cyan = Color(0xFF22D3EE)
    val Track = MenuColors.TileLight
    val Fill = Brush.horizontalGradient(listOf(Blue, Cyan))

    /** The fill gradient's colour at step [index], so lit dots match the line under them. */
    fun at(index: Float): Color = lerp(Blue, Cyan, (index / LastStep).coerceIn(0f, 1f))
}

internal val Pill = RoundedCornerShape(50)

private const val AutoplayDelayMillis = 700L
private const val AutoplayGapMillis = 1400L

@Stable
internal class StepsState {
    var step by mutableIntStateOf(StartStep)
        private set

    /** +1 after Next, −1 after Back, so transitions can pick their direction. */
    var direction by mutableIntStateOf(1)
        private set

    var interacted by mutableStateOf(false)

    val canBack: Boolean get() = step > 0
    val canNext: Boolean get() = step < LastStep

    fun next() {
        if (!canNext) return
        direction = 1
        step++
    }

    fun back() {
        if (!canBack) return
        direction = -1
        step--
    }
}

/**
 * The card all six progress demos share. It owns the step and the reel's autoplay (Next,
 * Next, Back, 1.4 s apart), which stops at the first real touch. [content] draws the
 * indicator; [controls] defaults to the Back / Next pair.
 */
@Composable
internal fun StepsScene(
    number: Int,
    title: String,
    caption: String,
    controls: @Composable (StepsState) -> Unit = { StepControls(it) },
    content: @Composable ColumnScope.(StepsState) -> Unit,
) {
    val state = remember { StepsState() }

    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        state.next()
        delay(AutoplayGapMillis)
        state.next()
        delay(AutoplayGapMillis)
        state.back()
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.interacted = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .background(MenuColors.Tile, RoundedCornerShape(24.dp))
                    .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                content(state)
                Spacer(Modifier.height(24.dp))
                controls(state)
            }
        }
    }
}

/** An outlined "‹ Back" and a filled "Next ›", each dimmed at its end of the steps. */
@Composable
internal fun StepControls(state: StepsState, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton("Back", filled = false, enabled = state.canBack, onClick = state::back, Modifier.weight(1f))
        StepButton("Next", filled = true, enabled = state.canNext, onClick = state::next, Modifier.weight(1f))
    }
}

@Composable
private fun StepButton(label: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, tween(200), label = "enabled")
    Row(
        modifier
            .height(44.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(Pill)
            .then(if (filled) Modifier.background(StepColors.Fill) else Modifier.border(1.dp, MenuColors.Muted.copy(alpha = 0.5f), Pill))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!filled) Chevron(pointsRight = false, color = MenuColors.Text)
        Text(
            label,
            color = MenuColors.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (filled) Chevron(pointsRight = true, color = Color.White)
    }
}

@Composable
internal fun Chevron(pointsRight: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(10.dp)) {
        val stroke = 1.8.dp.toPx()
        val tip = if (pointsRight) size.width * 0.72f else size.width * 0.28f
        val tail = if (pointsRight) size.width * 0.3f else size.width * 0.7f
        drawLine(color, Offset(tail, size.height * 0.12f), Offset(tip, size.height / 2), stroke, StrokeCap.Round)
        drawLine(color, Offset(tip, size.height / 2), Offset(tail, size.height * 0.88f), stroke, StrokeCap.Round)
    }
}

/** A step's name under its marker, brighter for the current step and the ones already done. */
@Composable
internal fun StepLabel(index: Int, step: Int, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        when {
            index == step -> MenuColors.Text
            index < step -> MenuColors.Text.copy(alpha = 0.7f)
            else -> MenuColors.Muted
        },
        tween(300),
        label = "label",
    )
    Text(
        StepNames[index],
        modifier,
        color = color,
        fontSize = 12.sp,
        fontWeight = if (index == step) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
    )
}

/** Back-out easing: overshoots by a touch before settling, for pops. */
internal fun backOut(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}
