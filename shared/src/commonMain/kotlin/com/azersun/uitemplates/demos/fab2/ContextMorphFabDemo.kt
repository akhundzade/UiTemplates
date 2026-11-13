package com.azersun.uitemplates.demos.fab2

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.drawGlyph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private class Tab(val label: String, val color: Color, val icon: DrawScope.(Color) -> Unit)

private val Tabs = listOf(
    Tab("Notes", MenuColors.Blue) { drawGlyph(Glyph.Pencil, it) },
    Tab("Tasks", Teal) { drawPlus(it) },
    Tab("Photos", MenuColors.Pink) { drawGlyph(Glyph.Camera, it) },
)

private class Task(val title: String, val due: String, val done: Boolean)

private val Tasks = listOf(
    Task("Buy oat milk", "Today", true),
    Task("Book flights to Lisbon", "Today", false),
    Task("Call the vet", "Tomorrow", false),
    Task("Return library books", "Fri", true),
    Task("Plan movie night", "Sat", false),
    Task("Repot the fern", "Sun", false),
)

private val PhotoTints = listOf(
    Color(0xFF7C5CFF) to Color(0xFFFF8FB8),
    Color(0xFF22D3EE) to Color(0xFF4F7BFF),
    Color(0xFFFFC53D) to Color(0xFFFB923C),
    Color(0xFF2DD4BF) to Color(0xFF166534),
    Color(0xFFFF5CA8) to Color(0xFF7C5CFF),
    Color(0xFF4F7BFF) to Color(0xFF1E1B4B),
    Color(0xFFFB923C) to Color(0xFFFF5C6C),
    Color(0xFF4ADE80) to Color(0xFF22D3EE),
    Color(0xFF8E8AB8) to Color(0xFF2E2A5C),
)

private val Sheen = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent))
private const val SpinMillis = 650
private const val AutoplayStepMillis = 1500L

@Composable
fun ContextMorphFabDemo() {
    val autoplay = rememberAutoplay()
    var selected by remember { mutableIntStateOf(0) }
    val spin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val color by animateColorAsState(Tabs[selected].color, tween(500), label = "fabColor")
    val slide by animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.75f, stiffness = 420f), label = "underline")

    fun select(index: Int) {
        if (index == selected) return
        val direction = if (index > selected) 1f else -1f
        selected = index
        // Spinning on from the current target keeps quick taps adding whole turns, never stopping askew.
        scope.launch { spin.animateTo(spin.targetValue + 360f * direction, tween(SpinMillis, easing = FastOutSlowInEasing)) }
    }

    AutoplayEffect(autoplay) {
        select(1)
        delay(AutoplayStepMillis)
        select(2)
    }

    NotesScene(
        4,
        "Context Morph",
        "Switch tabs: the button spins into a new icon and colour for that tab while the list cross-fades.",
        autoplay,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp)
                    .drawBehind {
                        val tabWidth = size.width / Tabs.size
                        val lineWidth = 28.dp.toPx()
                        val lineHeight = 3.dp.toPx()
                        val x = (slide + 0.5f) * tabWidth - lineWidth / 2
                        drawLine(
                            Color.White.copy(alpha = 0.06f),
                            Offset(0f, size.height - 0.5.dp.toPx()),
                            Offset(size.width, size.height - 0.5.dp.toPx()),
                            1.dp.toPx(),
                        )
                        drawRoundRect(
                            color,
                            Offset(x, size.height - lineHeight),
                            Size(lineWidth, lineHeight),
                            CornerRadius(lineHeight / 2),
                        )
                    },
            ) {
                Tabs.forEachIndexed { i, tab ->
                    val tint by animateColorAsState(
                        if (i == selected) MenuColors.Text else MenuColors.Muted,
                        tween(250),
                        label = "tab",
                    )
                    Text(
                        tab.label,
                        color = tint,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) { select(i) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
            Crossfade(
                targetState = selected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                animationSpec = tween(320),
                label = "content",
            ) { tab ->
                when (tab) {
                    0 -> NoteList(Modifier.padding(top = 8.dp))
                    1 -> TaskList()
                    else -> PhotoGrid()
                }
            }
        }
        Fab(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(FabInset)
                .graphicsLayer {
                    val turn = spin.value
                    rotationZ = turn
                    val lap = ((turn % 360f) + 360f) % 360f / 360f
                    val s = 1f - 0.14f * sin(PI.toFloat() * lap)
                    scaleX = s
                    scaleY = s
                },
            glow = color,
            paint = {
                drawRect(color)
                drawRect(Sheen)
            },
        ) {
            Crossfade(selected, animationSpec = tween(240, delayMillis = 120), label = "icon") { tab ->
                Canvas(Modifier.size(22.dp)) { Tabs[tab].icon(this, Color.White) }
            }
        }
    }
}

@Composable
private fun TaskList() {
    Column(Modifier.padding(top = 8.dp)) {
        Tasks.forEach { task ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(22.dp)) { drawCheckCircle(task.done) }
                Text(
                    task.title,
                    color = if (task.done) MenuColors.Muted else MenuColors.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(task.due, color = MenuColors.Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PhotoGrid() {
    Column(
        Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhotoTints.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (from, to) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(from, to)))
                            .drawBehind {
                                // Two soft hills and a sun, so the tiles read as photos.
                                drawCircle(Color.White.copy(alpha = 0.35f), size.width * 0.1f, Offset(size.width * 0.72f, size.height * 0.3f))
                                drawOval(Color.Black.copy(alpha = 0.18f), Offset(-size.width * 0.2f, size.height * 0.62f), Size(size.width * 0.9f, size.height * 0.7f))
                                drawOval(Color.Black.copy(alpha = 0.24f), Offset(size.width * 0.35f, size.height * 0.7f), Size(size.width, size.height * 0.6f))
                            },
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCheckCircle(done: Boolean) {
    val w = size.width
    if (done) {
        drawCircle(Teal)
        val stroke = w * 0.11f
        drawLine(Color(0xFF0B2E2A), Offset(w * 0.28f, w * 0.52f), Offset(w * 0.44f, w * 0.68f), stroke, StrokeCap.Round)
        drawLine(Color(0xFF0B2E2A), Offset(w * 0.44f, w * 0.68f), Offset(w * 0.74f, w * 0.36f), stroke, StrokeCap.Round)
    } else {
        val stroke = 1.5.dp.toPx()
        drawCircle(MenuColors.Muted, radius = w / 2 - stroke / 2, style = Stroke(stroke))
    }
}
