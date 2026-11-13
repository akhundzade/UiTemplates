package com.azersun.uitemplates.demos.password

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FlapWidth = 84.dp
private val FlapHeight = 28.dp
private val FlapTile = Color(0xFF2B2752)
private const val SegmentStaggerMillis = 70L

@Composable
fun FlapLabelDemo() {
    val form = remember { PasswordFormState() }
    // 90° is folded edge-on (invisible), 0° is open.
    val flap = remember { List(SegmentCount) { Animatable(90f) } }
    val color by animateLevelColor(form.level)

    LaunchedEffect(form.level) {
        val level = form.level
        var order = 0
        repeat(SegmentCount) { i ->
            if (i < level && flap[i].value != 0f) {
                val wait = order++ * SegmentStaggerMillis
                launch {
                    delay(wait)
                    flap[i].animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 380f))
                }
            } else if (i >= level && flap[i].value != 90f) {
                launch { flap[i].animateTo(90f, tween(150)) }
            }
        }
    }

    PasswordStage(
        5,
        "Flap Label",
        "The strength word flips like a split-flap board; segments flap open.",
        form,
        label = { StrengthLabel(form.level, word = { SplitFlapWord(form.level) }) },
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(SegmentGap)) {
            repeat(SegmentCount) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind { drawRoundRect(PasswordColors.Track, cornerRadius = CornerRadius(size.height / 2)) },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                rotationX = flap[i].value
                                cameraDistance = 6f * density
                                alpha = if (flap[i].value >= 89.5f) 0f else 1f
                            }
                            .drawBehind { drawRoundRect(color, cornerRadius = CornerRadius(size.height / 2)) },
                    )
                }
            }
        }
    }
}

/**
 * Classic two-stage flap: the old word's top half falls away revealing the new top half, then the new
 * bottom half drops down over the old one, bouncing a little as it lands.
 */
@Composable
private fun SplitFlapWord(level: Int) {
    var old by remember { mutableIntStateOf(level) }
    var new by remember { mutableIntStateOf(level) }
    val top = remember { Animatable(1f) }
    val bottom = remember { Animatable(1f) }
    LaunchedEffect(level) {
        if (level == new) return@LaunchedEffect
        old = new
        new = level
        top.snapTo(0f)
        bottom.snapTo(0f)
        top.animateTo(1f, tween(170, easing = FastOutLinearInEasing))
        bottom.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f))
    }

    Box(Modifier.size(FlapWidth, FlapHeight)) {
        FlapHalf(new, top = true, Modifier.align(Alignment.TopStart))
        FlapHalf(old, top = false, Modifier.align(Alignment.BottomStart))
        if (top.value < 1f) {
            FlapHalf(
                old,
                top = true,
                Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        rotationX = -90f * top.value
                        cameraDistance = 12f * density
                    },
                shade = { top.value },
            )
        } else {
            FlapHalf(
                new,
                top = false,
                Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        rotationX = 90f * (1f - bottom.value)
                        cameraDistance = 12f * density
                    },
                shade = { (1f - bottom.value).coerceAtLeast(0f) },
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(PasswordColors.Panel),
        )
    }
}

@Composable
private fun FlapHalf(level: Int, top: Boolean, modifier: Modifier = Modifier, shade: () -> Float = { 0f }) {
    val shape = if (top) {
        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    } else {
        RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
    }
    val strength = StrengthLevels[level]
    Box(
        modifier
            .width(FlapWidth)
            .clip(shape)
            .background(FlapTile)
            .drawWithContent {
                drawContent()
                drawRect(Color.Black, alpha = 0.5f * shade())
            }
            .halfWindow(top),
    ) {
        Text(
            strength.word,
            color = strength.color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
        )
    }
}

/** Lays the content out at the full flap height but reports only its top or bottom half. */
private fun Modifier.halfWindow(top: Boolean): Modifier = layout { measurable, constraints ->
    val full = FlapHeight.roundToPx()
    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, full))
    val upper = full / 2
    val height = if (top) upper else full - upper
    layout(placeable.width, height) { placeable.place(0, if (top) 0 else -upper) }
}
