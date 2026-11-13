package com.azersun.uitemplates.demos.fab2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal class Note(val title: String, val subtitle: String, val time: String, val color: Color)

internal val Teal = Color(0xFF2DD4BF)
private val Green = Color(0xFF4ADE80)
private val Orange = Color(0xFFFB923C)

/** The first eight are the reel's list; the rest give the scroll-to-top demo something to scroll. */
internal val Notes: List<Note> = listOf(
    Note("Grocery list", "Oat milk, bread, eggs", "9:41", MenuColors.Blue),
    Note("Trip ideas", "Lisbon in spring", "9:02", MenuColors.Violet),
    Note("Book club", "Chapter 7 questions", "8:15", MenuColors.Pink),
    Note("Workout", "Legs + 20 min run", "Mon", Teal),
    Note("Gift ideas", "Something for Mia", "Sun", MenuColors.Yellow),
    Note("Recipes", "Lemon pasta, shakshuka", "Sat", Orange),
    Note("Plant care", "Water the fern on Sunday", "Fri", Green),
    Note("Movie night", "Three picks to vote on", "Thu", MenuColors.Cyan),
    Note("Reading list", "Two essays, one novel", "Wed", MenuColors.Violet),
    Note("Packing", "Charger, passport, socks", "Tue", MenuColors.Blue),
    Note("Budget", "Rent, phone, savings", "12 Nov", Green),
    Note("Errands", "Post office, pharmacy", "11 Nov", Orange),
    Note("Meeting notes", "Q4 roadmap review", "10 Nov", MenuColors.Pink),
    Note("Quotes", "Keep the ones that stick", "9 Nov", MenuColors.Yellow),
    Note("Playlist", "Songs for the road", "8 Nov", Teal),
    Note("Ideas", "An app that waters plants", "7 Nov", MenuColors.Cyan),
)

internal const val ReelNoteCount = 8

internal val FabSize = 60.dp
internal val FabInset = 20.dp
internal val FabGradient = Brush.linearGradient(listOf(MenuColors.Blue, MenuColors.Violet))

private const val AutoplayDelayMillis = 700L

/** The reel's autoplay: it runs once on open (and on each replay) until the first real touch. */
@Stable
internal class Autoplay {
    var run by mutableIntStateOf(0)
        private set
    var stopped by mutableStateOf(false)
        private set

    fun stop() {
        stopped = true
    }

    fun replay() {
        stopped = false
        run++
    }
}

@Composable
internal fun rememberAutoplay(): Autoplay = remember { Autoplay() }

/** Calls [reset], waits ~700 ms, then plays [script]; a real touch cancels it mid-way. */
@Composable
internal fun AutoplayEffect(
    autoplay: Autoplay,
    reset: suspend () -> Unit = {},
    script: suspend CoroutineScope.() -> Unit,
) {
    LaunchedEffect(autoplay.run, autoplay.stopped) {
        if (autoplay.stopped) return@LaunchedEffect
        reset()
        delay(AutoplayDelayMillis)
        script()
    }
}

/** The numbered stage holding the phone-like "Notes" screen every demo floats its button over. */
@Composable
internal fun NotesScene(
    number: Int,
    title: String,
    caption: String,
    autoplay: Autoplay,
    content: @Composable BoxScope.() -> Unit,
) {
    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MenuColors.Screen)
                .pointerInput(autoplay) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        autoplay.stop()
                    }
                },
            content = content,
        )
    }
}

@Composable
internal fun NotesHeader(
    modifier: Modifier = Modifier,
    count: Int = ReelNoteCount,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Notes", color = MenuColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "$count",
            color = MenuColors.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(CircleShape)
                .background(MenuColors.TileLight)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
internal fun NoteRow(note: Note) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(note.color),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(16.dp)) { drawNoteLines(Color.White.copy(alpha = 0.9f)) }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(note.title, color = MenuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(note.subtitle, color = MenuColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(note.time, color = MenuColors.Muted, fontSize = 11.sp)
    }
}

@Composable
internal fun NoteList(modifier: Modifier = Modifier, count: Int = ReelNoteCount) {
    Column(modifier) { Notes.take(count).forEach { NoteRow(it) } }
}

/** A round floating button; [paint] fills it so demos can blend colours without recomposing. */
@Composable
internal fun Fab(
    modifier: Modifier = Modifier,
    glow: Color = MenuColors.Blue,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    paint: DrawScope.() -> Unit = { drawRect(FabGradient) },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .size(FabSize)
            .shadow(14.dp, CircleShape, ambientColor = glow, spotColor = glow)
            .clip(CircleShape)
            .drawBehind(paint)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
internal fun ReplayPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .clickable(onClickLabel = "Replay", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(MenuColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = MenuColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Places the element with its centre on [point] (px, relative to the parent's top-left); read in
 * the placement phase, so animating the point only re-places it.
 */
internal fun Modifier.centeredAt(point: Density.() -> Offset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val density: Density = this
    layout(placeable.width, placeable.height) {
        val p = density.point()
        placeable.place((p.x - placeable.width / 2f).roundToInt(), (p.y - placeable.height / 2f).roundToInt())
    }
}

internal fun mix(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun DrawScope.drawPlus(color: Color) {
    val w = size.width
    val stroke = w * 0.12f
    drawLine(color, Offset(w * 0.5f, w * 0.12f), Offset(w * 0.5f, w * 0.88f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.12f, w * 0.5f), Offset(w * 0.88f, w * 0.5f), stroke, StrokeCap.Round)
}

private fun DrawScope.drawNoteLines(color: Color) {
    val w = size.width
    val stroke = w * 0.13f
    listOf(0.72f, 0.56f, 0.4f).forEachIndexed { i, length ->
        val y = w * (0.24f + i * 0.26f)
        drawLine(color, Offset(w * 0.12f, y), Offset(w * (0.12f + length), y), stroke, StrokeCap.Round)
    }
}

private fun DrawScope.drawReplayGlyph(color: Color) {
    val stroke = 1.6.dp.toPx()
    val radius = size.minDimension / 2 - stroke
    drawArc(
        color,
        startAngle = 0f,
        sweepAngle = 300f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    // Arrowhead at the arc's end (300°), pointing along the clockwise tangent.
    val end = center + Offset(radius * 0.5f, -radius * 0.866f)
    val along = Offset(0.866f, 0.5f)
    val across = Offset(0.5f, -0.866f)
    val head = 3.dp.toPx()
    val arrow = Path().apply {
        moveTo(end.x + along.x * head, end.y + along.y * head)
        lineTo(end.x + across.x * head * 0.9f, end.y + across.y * head * 0.9f)
        lineTo(end.x - across.x * head * 0.9f, end.y - across.y * head * 0.9f)
        close()
    }
    drawPath(arrow, color)
}
