package com.azersun.uitemplates.demos.fab2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BadgeRed = Color(0xFFFF4D5E)
private val ChipStartX = 92.dp
private val ChipStartY = 86.dp
private val SenderColors = listOf(MenuColors.Violet, MenuColors.Cyan, MenuColors.Yellow)
private val FlightEasing = CubicBezierEasing(0.6f, 0f, 0.4f, 1f)
private const val FlightMillis = 820
private const val ChipHoldMillis = 260L
private const val AutoplayGapMillis = 1000L
private const val AutoplayMessages = 3

@Composable
fun UnreadCountFabDemo() {
    val autoplay = rememberAutoplay()
    val chips = remember { mutableStateListOf<Int>() }
    var nextId by remember { mutableIntStateOf(0) }
    var unread by remember { mutableIntStateOf(0) }
    val badge = remember { Animatable(0f) }
    val bump = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun arrive(id: Int) {
        chips.remove(id)
        unread++
        val first = unread == 1
        scope.launch {
            if (first) {
                badge.snapTo(0f)
                badge.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
            } else {
                badge.animateTo(1.3f, tween(100))
                badge.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 520f))
            }
        }
        scope.launch {
            bump.animateTo(1.12f, tween(90))
            bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }

    fun markRead() {
        if (unread == 0) return
        scope.launch {
            badge.animateTo(0f, tween(180))
            unread = 0
        }
    }

    AutoplayEffect(
        autoplay,
        reset = {
            chips.clear()
            unread = 0
            badge.snapTo(0f)
        },
    ) {
        repeat(AutoplayMessages) { i ->
            if (i > 0) delay(AutoplayGapMillis)
            chips += nextId++
        }
    }

    NotesScene(
        5,
        "Unread Count",
        "New messages fly along a curve into the button. With each one the red badge pops and its number rolls up. Tap the button to read them.",
        autoplay,
    ) {
        Column(Modifier.fillMaxSize()) {
            NotesHeader { ReplayPill(onClick = autoplay::replay) }
            NoteList()
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(FabInset),
        ) {
            Fab(
                Modifier.graphicsLayer {
                    scaleX = bump.value
                    scaleY = bump.value
                },
                onClick = { markRead() },
                onClickLabel = "Mark as read",
            ) {
                Canvas(Modifier.size(24.dp)) { drawChatGlyph(Color.White) }
            }
            if (unread > 0) {
                Badge(
                    count = unread,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .graphicsLayer {
                            scaleX = badge.value.coerceAtLeast(0f)
                            scaleY = badge.value.coerceAtLeast(0f)
                        },
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val fabCenterX = maxWidth - FabInset - FabSize / 2
            val fabCenterY = maxHeight - FabInset - FabSize / 2
            chips.forEach { id ->
                key(id) {
                    FlyingChip(
                        sender = SenderColors[id % SenderColors.size],
                        start = { Offset(ChipStartX.toPx(), ChipStartY.toPx()) },
                        end = { Offset(fabCenterX.toPx(), fabCenterY.toPx()) },
                        onArrive = { arrive(id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlyingChip(
    sender: Color,
    start: Density.() -> Offset,
    end: Density.() -> Offset,
    onArrive: () -> Unit,
) {
    val appear = remember { Animatable(0f) }
    val fly = remember { Animatable(0f) }
    val arrive by rememberUpdatedState(onArrive)
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 520f))
        delay(ChipHoldMillis)
        fly.animateTo(1f, tween(FlightMillis, easing = FlightEasing))
        arrive()
    }
    Row(
        Modifier
            .centeredAt {
                val from = start()
                val to = end()
                // Quadratic curve whose control point sits level with the start above the button:
                // the chip sets off sideways and dives down into it.
                quadratic(from, Offset(to.x, from.y), to, fly.value)
            }
            .graphicsLayer {
                val f = fly.value
                val s = (0.6f + 0.4f * appear.value) * mix(1f, 0.15f, f)
                scaleX = s.coerceAtLeast(0f)
                scaleY = s.coerceAtLeast(0f)
                alpha = appear.value.coerceIn(0f, 1f) * (1f - smoothstep(0.8f, 1f, f))
            }
            .clip(CircleShape)
            .background(Color(0xFF2B2858))
            .border(1.dp, MenuColors.CardBorder, CircleShape)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(sender))
        Spacer(Modifier.width(7.dp))
        Text("New message", color = MenuColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun Badge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MenuColors.Screen)
            .padding(2.dp)
            .clip(CircleShape)
            .background(BadgeRed)
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { it } + fadeIn(tween(140)))
                    .togetherWith(slideOutVertically(tween(180)) { -it } + fadeOut(tween(140)))
            },
            label = "badgeCount",
        ) { n ->
            Text(if (n > 9) "9+" else "$n", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun quadratic(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return p0 * (u * u) + p1 * (2f * u * t) + p2 * (t * t)
}

private fun DrawScope.drawChatGlyph(color: Color) {
    val w = size.width
    val stroke = Stroke(w * 0.1f, cap = StrokeCap.Round)
    drawRoundRect(color, Offset(w * 0.1f, w * 0.14f), Size(w * 0.8f, w * 0.6f), CornerRadius(w * 0.2f), style = stroke)
    drawLine(color, Offset(w * 0.3f, w * 0.74f), Offset(w * 0.24f, w * 0.9f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.24f, w * 0.9f), Offset(w * 0.46f, w * 0.74f), stroke.width, StrokeCap.Round)
    repeat(3) { i -> drawCircle(color, w * 0.055f, Offset(w * (0.32f + i * 0.18f), w * 0.44f)) }
}
