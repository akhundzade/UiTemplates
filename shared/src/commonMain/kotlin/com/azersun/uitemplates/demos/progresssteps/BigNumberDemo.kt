package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors

private val PanelSize = 96.dp
private val PanelShape = RoundedCornerShape(18.dp)
private val FlapUpper = Color(0xFF242149)
private val FlapLower = Color(0xFF1C1A3B)
private val Hinge = Color(0xFF0B0A18)
private const val MaxShade = 0.45f

@Composable
fun BigNumberDemo() {
    StepsScene(
        number = 2,
        title = "Big Number",
        caption = "The giant step number flips like a split-flap board while the step name rolls and the bar fills.",
        controls = { RoundControls(it) },
    ) { state ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlapNumber(state.step)
            Spacer(Modifier.width(18.dp))
            Column {
                Text("STEP", color = MenuColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(2.dp))
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val d = state.direction
                        (slideInVertically(tween(360)) { it * d } + fadeIn(tween(300))) togetherWith
                            (slideOutVertically(tween(300)) { -it * d } + fadeOut(tween(220)))
                    },
                    label = "name",
                ) { step ->
                    Text(StepNames[step], color = MenuColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SegmentBar(state.step)
    }
}

/**
 * Four stacked halves: the new top waiting behind, the old bottom, the old top that falls
 * forward around the hinge, then the new bottom that falls into place over the old one.
 */
@Composable
private fun FlapNumber(step: Int) {
    var from by remember { mutableIntStateOf(step) }
    var to by remember { mutableIntStateOf(step) }
    // 0..90 the old top falls, 90..180 the new bottom lands; 180 is at rest.
    val flip = remember { Animatable(180f) }

    LaunchedEffect(step) {
        if (step == to) return@LaunchedEffect
        from = to
        to = step
        flip.snapTo(0f)
        flip.animateTo(90f, tween(240, easing = FastOutLinearInEasing))
        flip.animateTo(180f, spring(dampingRatio = 0.45f, stiffness = 650f))
    }

    Box(
        Modifier
            .size(PanelSize)
            .clip(PanelShape)
            .background(Hinge)
            .border(1.dp, MenuColors.CardBorder, PanelShape),
    ) {
        FlapHalf(to, upper = true)
        FlapHalf(from, upper = false)
        FlapHalf(
            from,
            upper = true,
            shade = { flip.value / 90f * MaxShade },
            modifier = Modifier.graphicsLayer {
                val a = flip.value
                rotationX = -a.coerceAtMost(90f)
                cameraDistance = 12f * density
                alpha = if (a < 90f) 1f else 0f
            },
        )
        FlapHalf(
            to,
            upper = false,
            shade = { ((180f - flip.value) / 90f * MaxShade).coerceIn(0f, MaxShade) },
            modifier = Modifier.graphicsLayer {
                val a = flip.value
                rotationX = 180f - a
                cameraDistance = 12f * density
                alpha = if (a >= 90f) 1f else 0f
            },
        )
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(2.dp).background(Hinge))
    }
}

/** One half of the panel showing step [value], hinged on the panel's centre line. */
@Composable
private fun FlapHalf(
    value: Int,
    upper: Boolean,
    modifier: Modifier = Modifier,
    shade: () -> Float = { 0f },
) {
    Box(
        modifier
            .fillMaxSize()
            .drawWithContent {
                val top = if (upper) 0f else size.height / 2
                clipRect(top = top, bottom = top + size.height / 2) {
                    this@drawWithContent.drawContent()
                    drawRect(Color.Black, alpha = shade())
                }
            }
            .background(if (upper) FlapUpper else FlapLower),
        contentAlignment = Alignment.Center,
    ) {
        Text("${value + 1}", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentBar(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepNames.indices.forEach { i ->
            val fill by animateFloatAsState(
                if (i <= step) 1f else 0f,
                tween(420, delayMillis = 120, easing = FastOutSlowInEasing),
                label = "segment",
            )
            Box(Modifier.weight(1f).height(6.dp).clip(Pill).background(StepColors.Track)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(StepColors.Fill))
            }
        }
    }
}

/** A quiet "‹ Back" link and the round blue "Next →" button. */
@Composable
private fun RoundControls(state: StepsState) {
    val backAlpha by animateFloatAsState(if (state.canBack) 1f else 0.4f, tween(200), label = "back")
    val nextAlpha by animateFloatAsState(if (state.canNext) 1f else 0.4f, tween(200), label = "next")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .graphicsLayer { alpha = backAlpha }
                .clip(Pill)
                .clickable(enabled = state.canBack, role = Role.Button, onClick = state::back)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chevron(pointsRight = false, color = MenuColors.Muted)
            Spacer(Modifier.width(6.dp))
            Text("Back", color = MenuColors.Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(52.dp)
                .graphicsLayer { alpha = nextAlpha }
                .clip(CircleShape)
                .background(StepColors.Fill)
                .clickable(enabled = state.canNext, onClickLabel = "Next", role = Role.Button, onClick = state::next),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val stroke = 2.2.dp.toPx()
                val y = size.height / 2
                val tip = Offset(size.width * 0.86f, y)
                drawLine(Color.White, Offset(size.width * 0.14f, y), tip, stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width * 0.56f, size.height * 0.2f), tip, stroke, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width * 0.56f, size.height * 0.8f), tip, stroke, StrokeCap.Round)
            }
        }
    }
}
