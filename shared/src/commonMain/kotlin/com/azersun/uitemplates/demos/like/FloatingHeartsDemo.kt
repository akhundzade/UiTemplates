package com.azersun.uitemplates.demos.like

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Bar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.TimeSource

private const val MaxHearts = 48
private const val HoldSpawnMillis = 140L
private val ButtonSize = 56.dp
private val ButtonMargin = 20.dp
private val HeartColors = listOf(
    LikeColors.Pink,
    LikeColors.Violet,
    LikeColors.Cyan,
    LikeColors.Yellow,
    Color(0xFFFF7A59),
    Color(0xFFFF4D6D),
    Color.White,
)
private val AutoplayGaps = longArrayOf(0, 240, 190, 300, 170, 260, 210, 280)

/** A fixed pool of heart slots, reused round-robin so a long hold allocates nothing per frame. */
@Stable
private class HeartField {
    private val clock = TimeSource.Monotonic.markNow()
    val start = LongArray(MaxHearts) { -1L }
    val life = IntArray(MaxHearts)
    val side = FloatArray(MaxHearts)
    val sway = FloatArray(MaxHearts)
    val phase = FloatArray(MaxHearts)
    val lean = FloatArray(MaxHearts)
    val color = IntArray(MaxHearts)
    private var next = 0

    /** The time drawn at; bumped every frame while any heart is in the air. */
    var now by mutableLongStateOf(0L)
    var running by mutableStateOf(false)

    fun elapsed(): Long = clock.elapsedNow().inWholeMilliseconds

    fun spawn() {
        val i = next
        next = (next + 1) % MaxHearts
        start[i] = elapsed()
        life[i] = Random.nextInt(2200, 3200)
        side[i] = Random.nextFloat() * 16f + 20f
        sway[i] = Random.nextFloat() * 14f + 8f
        phase[i] = Random.nextFloat() * 2f * PI.toFloat()
        lean[i] = Random.nextFloat() * 50f + 10f
        color[i] = Random.nextInt(HeartColors.size)
        running = true
    }

    /** Frees finished slots and reports whether any heart is still flying at [time]. */
    fun anyAlive(time: Long): Boolean {
        var alive = false
        for (i in 0 until MaxHearts) {
            if (start[i] < 0) continue
            if (time - start[i] >= life[i]) start[i] = -1L else alive = true
        }
        return alive
    }
}

@Composable
fun FloatingHeartsDemo() {
    val field = remember { HeartField() }
    val press = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(field.running) {
        if (!field.running) return@LaunchedEffect
        do {
            withFrameMillis { field.now = field.elapsed() }
        } while (field.anyAlive(field.now))
        field.running = false
    }

    fun tap() {
        field.spawn()
        scope.launch {
            press.animateTo(0.86f, tween(70))
            press.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 600f))
        }
    }

    LikeStage(
        4,
        "Floating Hearts",
        "Tap or hold the heart: colourful hearts float up the stream, swaying as they fade.",
        autoplay = {
            AutoplayGaps.forEach { gap ->
                delay(gap)
                tap()
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val backdrop = Brush.verticalGradient(listOf(Color(0xFF2B1D5E), Color(0xFF1A1438), Color(0xFF0E0B22)))
                    val glow = Brush.radialGradient(
                        listOf(LikeColors.Pink.copy(alpha = 0.28f), Color.Transparent),
                        Offset(size.width * 0.8f, size.height * 0.2f),
                        size.width * 0.7f,
                    )
                    onDrawBehind {
                        drawRect(backdrop)
                        drawRect(glow)
                    }
                },
        ) {
            LiveChip(Modifier.align(Alignment.TopStart).padding(16.dp))
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = ButtonSize + ButtonMargin * 2, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CommentRow(Brush.linearGradient(listOf(LikeColors.Cyan, LikeColors.Violet)), 72.dp, 132.dp)
                CommentRow(Brush.linearGradient(listOf(LikeColors.Yellow, LikeColors.Pink)), 56.dp, 104.dp)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ButtonMargin)
                    .size(ButtonSize)
                    .graphicsLayer {
                        scaleX = press.value
                        scaleY = press.value
                    }
                    .background(LikeColors.Pink, CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Send a heart"
                        onClick {
                            tap()
                            true
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            tap()
                            while (true) {
                                val released = withTimeoutOrNull(HoldSpawnMillis) {
                                    waitForUpOrCancellation()
                                    true
                                }
                                if (released != null) break
                                tap()
                            }
                        }
                    }
                    .drawWithCache {
                        val side = size.width * 0.46f
                        val path = Path().apply { heart(size.center - Offset(side / 2, side / 2), side) }
                        onDrawBehind { drawPath(path, Color.White) }
                    },
            )
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val path = Path()
                        val origin = Offset(
                            size.width - (ButtonMargin + ButtonSize / 2).toPx(),
                            size.height - (ButtonMargin + ButtonSize / 2).toPx() - 16.dp.toPx(),
                        )
                        val rise = size.height * 0.68f
                        onDrawBehind {
                            val now = field.now
                            for (i in 0 until MaxHearts) {
                                val start = field.start[i]
                                if (start < 0) continue
                                val t = (now - start) / field.life[i].toFloat()
                                if (t < 0f || t >= 1f) continue
                                val grow = (t / 0.1f).coerceAtMost(1f)
                                val pop = grow * (1f + 0.3f * sin(grow * PI.toFloat()))
                                val side = field.side[i].dp.toPx() * pop
                                val x = origin.x - field.lean[i].dp.toPx() * t +
                                    sin(t * 2.4f * PI.toFloat() + field.phase[i]) * field.sway[i].dp.toPx() * grow
                                val y = origin.y - rise * t
                                val alpha = if (t < 0.65f) 1f else (1f - t) / 0.35f
                                path.heart(Offset(x - side / 2, y - side / 2), side)
                                drawPath(path, HeartColors[field.color[i]], alpha = alpha)
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun LiveChip(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(LikeColors.Pink, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun CommentRow(avatar: Brush, nameWidth: Dp, lineWidth: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(avatar, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Bar(nameWidth, 8.dp, LikeColors.Text.copy(alpha = 0.8f))
            Spacer(Modifier.height(6.dp))
            Bar(lineWidth, 7.dp, LikeColors.Outline.copy(alpha = 0.5f))
        }
    }
}
