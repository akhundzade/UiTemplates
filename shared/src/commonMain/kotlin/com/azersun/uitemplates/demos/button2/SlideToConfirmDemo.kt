package com.azersun.uitemplates.demos.button2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val SlideTrackHeight = 64.dp
private val SlideKnob = 52.dp
private val SlideKnobPad = 6.dp

// How far along a release still counts as "made it to the end".
private const val ConfirmAt = 0.85f
private val SlideFill = Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Blue))

@Composable
fun SlideToConfirmDemo() {
    var trackWidth by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    val done = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val knobSpan = with(LocalDensity.current) { (SlideKnob + SlideKnobPad * 2).toPx() }
    val maxX = (trackWidth - knobSpan).coerceAtLeast(0f)

    fun confirm() {
        confirmed = true
        scope.launch { done.animateTo(1f, tween(480, easing = FastOutSlowInEasing)) }
    }

    fun settle() {
        settleJob = scope.launch {
            if (maxX > 0f && dragX >= maxX * ConfirmAt) {
                animate(dragX, maxX, animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f)) { v, _ -> dragX = v }
                confirm()
            } else {
                animate(dragX, 0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 380f)) { v, _ -> dragX = v }
            }
        }
    }

    ButtonStage(
        6,
        "Slide to Confirm",
        "Drag the knob to the end: the track fills and the knob turns into a ✓.",
        onReplay = {
            settleJob?.cancel()
            confirmed = false
            scope.launch { done.animateTo(0f, tween(200)) }
            settleJob = scope.launch {
                animate(dragX, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) { v, _ -> dragX = v }
            }
        },
        autoplay = {
            settleJob?.cancel()
            try {
                animate(dragX, maxX, animationSpec = tween(1000, easing = FastOutSlowInEasing)) { v, _ -> dragX = v }
            } finally {
                // A touch that grabbed the knob owns it now; any other touch sends it back.
                if (!dragging && !confirmed) settle()
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 330.dp)
                .height(SlideTrackHeight)
                .onSizeChanged { trackWidth = it.width }
                .clip(CircleShape)
                .background(MenuColors.Tile)
                .border(1.dp, MenuColors.CardBorder, CircleShape)
                .drawWithCache {
                    val radius = CornerRadius(size.height / 2)
                    onDrawBehind {
                        // Spring overshoot is clamped so the knob stops at the track's ends.
                        val x = dragX.coerceIn(0f, maxX)
                        drawRoundRect(SlideFill, size = Size(knobSpan + x, size.height), cornerRadius = radius)
                    }
                }
                .semantics {
                    role = Role.Button
                    onClick(label = "Confirm") {
                        if (!confirmed) {
                            dragX = maxX
                            settle()
                        }
                        true
                    }
                }
                .pointerInput(maxX) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (confirmed) return@awaitEachGesture
                        // Only the knob (plus a little slack) can be grabbed, not the empty track.
                        val knobRight = dragX.coerceIn(0f, maxX) + knobSpan + 12.dp.toPx()
                        if (down.position.x > knobRight) return@awaitEachGesture
                        settleJob?.cancel()
                        dragging = true
                        down.consume()
                        try {
                            horizontalDrag(down.id) { change ->
                                dragX = (dragX.coerceIn(0f, maxX) + change.positionChange().x).coerceIn(0f, maxX)
                                change.consume()
                            }
                        } finally {
                            dragging = false
                            settle()
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "Slide to confirm",
                color = MenuColors.Muted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val progress = if (maxX > 0f) dragX.coerceIn(0f, maxX) / maxX else 0f
                        alpha = (1f - progress * 1.8f).coerceIn(0f, 1f)
                        translationX = progress * 24.dp.toPx()
                    },
            )
            Text(
                "Confirmed",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val d = done.value
                        alpha = smoothstep(0.2f, 0.8f, d)
                        translationX = -(1f - smoothstep(0.2f, 1f, d)) * 12.dp.toPx() - knobSpan / 2
                    },
            )
            Box(
                Modifier
                    .offset { IntOffset((SlideKnobPad.toPx() + dragX.coerceIn(0f, maxX)).roundToInt(), 0) }
                    .size(SlideKnob)
                    .graphicsLayer {
                        val s = 1f + 0.12f * sin(PI.toFloat() * smoothstep(0f, 0.6f, done.value))
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(24.dp)) {
                    val d = done.value
                    val chevrons = 1f - smoothstep(0f, 0.35f, d)
                    if (chevrons > 0f) {
                        val w = size.width
                        val stroke = w * 0.11f
                        val color = MenuColors.Blue.copy(alpha = chevrons)
                        repeat(2) { i ->
                            val tipX = w * (0.46f + 0.28f * i)
                            drawLine(color, Offset(tipX - w * 0.17f, w * 0.28f), Offset(tipX, w * 0.5f), stroke, StrokeCap.Round)
                            drawLine(color, Offset(tipX, w * 0.5f), Offset(tipX - w * 0.17f, w * 0.72f), stroke, StrokeCap.Round)
                        }
                    }
                    drawCheck(smoothstep(0.3f, 1f, d), MenuColors.Blue, Offset.Zero, size.width, size.width * 0.12f)
                }
            }
        }
    }
}
