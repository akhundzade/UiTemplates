package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class Message(
    val name: String,
    val preview: String,
    val time: String,
    val color: Color,
    val ink: Color = Color.White,
)

private fun inbox() = listOf(
    Message("Theo Banks", "Meeting moved to 3 PM", "9:20", MenuColors.Violet),
    Message("Ivy Moss", "Dinner on Friday?", "8:55", MenuColors.Pink),
    Message("Owen Hale", "Draft v2 attached", "8:02", MenuColors.Blue),
    Message("Luna Reyes", "Call me when you can", "Tue", MenuColors.Yellow, Color(0xFF2A2140)),
)

internal val RowHeight = 64.dp
internal val RowShape = RoundedCornerShape(16.dp)
internal const val DeleteThreshold = 0.4f
internal val DeleteRed = Color(0xFFEF4444)
internal val DeleteRedLight = Color(0xFFF87171)

private val RowGap = 10.dp
private val FlingVelocity = 900.dp
private const val InboxSize = 4
private const val AutoplayDelayMillis = 700L
private const val AutoplayHoldMillis = 160L
private const val AutoplayTravel = 0.6f
private const val CollapseMillis = 280

/**
 * How a demo's row deletes: [exitMillis] is the length of the 0→1 [SwipeRowState.exit]
 * timeline, the gap closes from [collapseAt] of that timeline, and [settle] springs a row back
 * when it is released short of the threshold.
 */
internal class SwipeSpec(
    val exitMillis: Int,
    val collapseAt: Float = 1f,
    val settle: AnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 500f),
)

@Stable
internal class SwipeRowState(
    private val scope: CoroutineScope,
    private val spec: SwipeSpec,
    private val flingVelocity: Float,
    private val onRemoved: () -> Unit,
) {
    /** Leftward finger travel in px; only negative while a settle spring overshoots. */
    val drag = Animatable(0f)

    /** The demo's delete animation, run as a single linear 0→1 timeline. */
    val exit = Animatable(0f)

    /** 0→1 as the row's slot closes and the rows below slide up. */
    val collapse = Animatable(0f)

    var width by mutableFloatStateOf(0f)
    var deleting by mutableStateOf(false)
        private set

    /** [drag] at the moment the row was let go past the threshold. */
    var releasedDrag = 0f
        private set

    val fraction: Float get() = if (width > 0f) drag.value / width else 0f

    fun dragBy(delta: Float) {
        if (deleting) return
        scope.launch { drag.snapTo((drag.value - delta).coerceAtLeast(0f)) }
    }

    fun release(velocity: Float) {
        if (deleting) return
        if (fraction > DeleteThreshold || velocity < -flingVelocity) delete() else settle()
    }

    fun settle() {
        if (!deleting) scope.launch { drag.animateTo(0f, spec.settle) }
    }

    suspend fun autoSwipe() {
        drag.animateTo(width * AutoplayTravel, tween(700, easing = FastOutSlowInEasing))
        delay(AutoplayHoldMillis)
        release(0f)
    }

    private fun delete() {
        deleting = true
        releasedDrag = drag.value
        scope.launch {
            val timeline = launch { exit.animateTo(1f, tween(spec.exitMillis, easing = LinearEasing)) }
            delay((spec.exitMillis * spec.collapseAt).toLong())
            collapse.animateTo(1f, tween(CollapseMillis, easing = FastOutSlowInEasing))
            timeline.join()
            onRemoved()
        }
    }
}

/** Maps [t] onto 0..1 across the window [start]..[end] of a 0..1 timeline. */
internal fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

internal fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

/**
 * The shared inbox: a header, four swipeable message rows and a Replay pill. [row] draws one
 * message for its [SwipeRowState]; the scene owns the gesture, autoplay, and gap collapse.
 */
@Composable
internal fun SwipeDeleteScene(
    number: Int,
    title: String,
    caption: String,
    spec: SwipeSpec,
    row: @Composable (message: Message, state: SwipeRowState) -> Unit,
) {
    val messages = remember { inbox().toMutableStateList() }
    var round by remember { mutableIntStateOf(0) }
    var interacted by remember { mutableStateOf(false) }
    val autoplayTarget = remember(round) { messages.firstOrNull() }
    val flingVelocity = with(LocalDensity.current) { FlingVelocity.toPx() }

    MenuStage(number, title, caption) {
        Column(Modifier.fillMaxSize().padding(top = 22.dp, bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
                Text("Inbox", color = MenuColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Swipe left to delete", color = MenuColors.Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            interacted = true
                        }
                    },
            ) {
                messages.forEach { message ->
                    key(message) {
                        SwipeSlot(
                            message = message,
                            spec = spec,
                            flingVelocity = flingVelocity,
                            autoplay = message === autoplayTarget && !interacted,
                            onRemoved = { messages.remove(message) },
                            row = row,
                        )
                    }
                }
                AnimatedVisibility(messages.isEmpty(), enter = fadeIn(tween(400)), exit = fadeOut(tween(150))) {
                    Text(
                        "All caught up",
                        color = MenuColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            ReplayPill(
                visible = messages.size < InboxSize,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                messages.clear()
                messages.addAll(inbox())
                round++
                interacted = false
            }
        }
    }
}

@Composable
private fun SwipeSlot(
    message: Message,
    spec: SwipeSpec,
    flingVelocity: Float,
    autoplay: Boolean,
    onRemoved: () -> Unit,
    row: @Composable (Message, SwipeRowState) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = remember { SwipeRowState(scope, spec, flingVelocity, onRemoved) }

    if (autoplay) {
        LaunchedEffect(Unit) {
            delay(AutoplayDelayMillis)
            try {
                state.autoSwipe()
            } catch (e: CancellationException) {
                // A real touch cancelled the demo swipe part-way; don't leave the row hanging open.
                state.settle()
                throw e
            }
        }
    }

    Box(
        Modifier
            // Keeps the deleting row's effects drawn above its neighbours as they slide up.
            .zIndex(if (state.deleting) 1f else 0f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = (placeable.height * (1f - state.collapse.value)).roundToInt().coerceAtLeast(0)
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .padding(horizontal = 16.dp, vertical = RowGap / 2)
            .onSizeChanged { state.width = it.width.toFloat() }
            .draggable(
                state = rememberDraggableState { delta -> state.dragBy(delta) },
                orientation = Orientation.Horizontal,
                enabled = !state.deleting,
                onDragStopped = { velocity -> state.release(velocity) },
            ),
    ) {
        row(message, state)
    }
}

@Composable
internal fun MessageRow(message: Message, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RowShape)
            .background(MenuColors.Tile)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(message.color, CircleShape), contentAlignment = Alignment.Center) {
            Text(message.name.take(1), color = message.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(message.name, color = MenuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                message.preview,
                color = MenuColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(message.time, color = MenuColors.Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.Top).padding(top = 14.dp))
    }
}

@Composable
private fun ReplayPill(visible: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(260), label = "replay")
    Text(
        "Replay",
        color = MenuColors.Text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .graphicsLayer {
                alpha = shown
                scaleX = 0.9f + 0.1f * shown
                scaleY = 0.9f + 0.1f * shown
            }
            .clip(RoundedCornerShape(50))
            .background(MenuColors.Tile)
            .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(50))
            .clickable(enabled = visible, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
    )
}

/** Stroked trash can of [size] centred on [center]; [lid] 0→1 swings the lid up on its left hinge. */
internal fun DrawScope.drawTrash(center: Offset, size: Float, color: Color, lid: Float) {
    if (size <= 0f) return
    val left = center.x - size / 2
    val top = center.y - size / 2
    fun at(x: Float, y: Float) = Offset(left + size * x, top + size * y)
    val stroke = Stroke(size * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val body = Path().apply {
        moveTo(left + size * 0.24f, top + size * 0.38f)
        lineTo(left + size * 0.3f, top + size * 0.88f)
        lineTo(left + size * 0.7f, top + size * 0.88f)
        lineTo(left + size * 0.76f, top + size * 0.38f)
    }
    drawPath(body, color, style = stroke)
    drawLine(color, at(0.43f, 0.5f), at(0.43f, 0.76f), stroke.width, StrokeCap.Round)
    drawLine(color, at(0.57f, 0.5f), at(0.57f, 0.76f), stroke.width, StrokeCap.Round)
    rotate(-42f * lid, pivot = at(0.16f, 0.28f)) {
        drawLine(color, at(0.16f, 0.28f), at(0.84f, 0.28f), stroke.width, StrokeCap.Round)
        val handle = Path().apply {
            moveTo(left + size * 0.4f, top + size * 0.28f)
            lineTo(left + size * 0.4f, top + size * 0.17f)
            lineTo(left + size * 0.6f, top + size * 0.17f)
            lineTo(left + size * 0.6f, top + size * 0.28f)
        }
        drawPath(handle, color, style = stroke)
    }
}
