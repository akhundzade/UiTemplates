package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal const val DigitCount = 4
private const val Goal = 10_000
private val Places = intArrayOf(1000, 100, 10, 1)

private val Days = listOf("Mon" to 8_612, "Tue" to 9_105, "Wed" to 7_348, "Thu" to 9_876)

private val TabGradient = Brush.horizontalGradient(listOf(Color(0xFF6E9BFF), Color(0xFF3B5BFF)))
private val BarGradient = listOf(MenuColors.Violet, MenuColors.Blue)

// Tabular figures keep every digit the same width, so per-digit cells line up and never jitter.
internal val NumberStyle = TextStyle(
    color = Color.White,
    fontSize = 44.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

/** The [DigitCount] digits of [value], most significant first. */
internal fun digitsOf(value: Int): List<Int> = List(DigitCount) { value / Places[it] % 10 }

internal fun formatSteps(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()

/** The number as [DigitCount] digit cells with the thousands comma in place. */
@Composable
internal fun DigitRow(modifier: Modifier = Modifier, digit: @Composable (index: Int) -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(DigitCount) { i ->
            if (i == DigitCount - 3) Text(",", style = NumberStyle)
            digit(i)
        }
    }
}

/** A cell exactly one digit wide and one line tall, whatever the content does inside it. */
@Composable
internal fun DigitCell(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("0", style = NumberStyle, modifier = Modifier.alpha(0f))
        content()
    }
}

/**
 * The step-count card all six demos share: day tabs, the number and the goal bar. It owns the
 * selected day and the reel's autoplay (Tue, Wed, Thu); [display] draws the number its own way.
 */
@Composable
internal fun CounterScene(
    number: Int,
    title: String,
    caption: String,
    display: @Composable (value: Int) -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    var interacted by remember { mutableStateOf(false) }
    val value = Days[selected].second

    LaunchedEffect(Unit) {
        for (day in 1..Days.lastIndex) {
            delay(if (day == 1) 700L else 1200L)
            if (interacted) return@LaunchedEffect
            selected = day
        }
    }

    MenuStage(number, title, caption) {
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        interacted = true
                    }
                }
                .background(MenuColors.Tile, RoundedCornerShape(24.dp))
                .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            DayTabs(selected, onSelect = { selected = it })
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                display(value)
                Text(
                    "steps",
                    color = MenuColors.Muted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            GoalBar(value / Goal.toFloat())
        }
    }
}

@Composable
private fun DayTabs(selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(MenuColors.Screen, CircleShape)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / Days.size
        val indicatorX by animateDpAsState(tabWidth * selected, spring(dampingRatio = 0.8f, stiffness = 500f), label = "tab")
        Box(
            Modifier
                .offset { IntOffset(indicatorX.roundToPx(), 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .background(TabGradient, CircleShape),
        )
        Row(Modifier.fillMaxSize()) {
            Days.forEachIndexed { i, (label, _) ->
                val color by animateColorAsState(if (i == selected) Color.White else MenuColors.Muted, tween(220), label = "tabText")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(role = Role.Tab, onClickLabel = label) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GoalBar(fraction: Float) {
    val fill by animateFloatAsState(fraction, tween(900, easing = FastOutSlowInEasing), label = "goal")
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(MenuColors.Screen, cornerRadius = corner)
        val width = size.width * fill.coerceIn(0f, 1f)
        drawRoundRect(
            Brush.horizontalGradient(BarGradient, startX = 0f, endX = width.coerceAtLeast(1f)),
            size = Size(width, size.height),
            cornerRadius = corner,
        )
    }
}
