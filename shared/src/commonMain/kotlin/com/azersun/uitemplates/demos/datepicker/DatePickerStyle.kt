package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal const val FirstDay = 15
internal val WeekDays = FirstDay..21

internal val CapsuleShape = RoundedCornerShape(50)
internal val CapsuleBrush = Brush.verticalGradient(listOf(Color(0xFF7FA6FF), MenuColors.Blue, Color(0xFF4537D6)))

/** Horizontal gap on each side of a day cell; the overlay uses it to find the capsule bounds. */
internal val CellInset = 3.dp

private const val InitialDay = 20
private const val Today = 17
private const val Letters = "SMTWTFS"
private val WeekdayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val SlotsByDay = mapOf(15 to 2, 16 to 4, 17 to 3, 18 to 5, 19 to 1, 20 to 6, 21 to 3)

private const val AutoplayDelayMillis = 700L
private const val AutoplayStepMillis = 1500L
private val AutoplayDays = listOf(16, 18)

private val CellHeight = 76.dp
private val CardShape = RoundedCornerShape(24.dp)

@Stable
internal class DatePickerState {
    var selected by mutableIntStateOf(InitialDay)
        private set
    var previous by mutableIntStateOf(InitialDay)
        private set

    /** Set on the first real touch so the autoplay stops driving the picker. */
    var interacted by mutableStateOf(false)

    fun select(day: Int) {
        if (day == selected) return
        previous = selected
        selected = day
    }
}

@Composable
internal fun rememberDatePickerState(): DatePickerState = remember { DatePickerState() }

/** Bounds of the capsule at a (possibly fractional) cell index, in the week strip's coordinates. */
internal fun DrawScope.cellRect(index: Float): Rect {
    val cell = size.width / WeekDays.count()
    val inset = CellInset.toPx()
    return Rect(index * cell + inset, 0f, (index + 1) * cell - inset, size.height)
}

/** The filled selection capsule, sized to its cell. */
@Composable
internal fun BoxScope.Capsule(modifier: Modifier = Modifier) {
    Box(modifier.matchParentSize().background(CapsuleBrush, CapsuleShape))
}

/**
 * The shared "November 2026" week card. It owns the selection and the reel's autoplay (16, then 18).
 * [capsule] draws each cell's selection behind its labels, [cellModifier] moves a whole cell and
 * [overlay] draws over the strip, with [cellRect] mapping a cell index to its bounds.
 */
@Composable
internal fun DatePickerScene(
    state: DatePickerState,
    number: Int,
    title: String,
    caption: String,
    cellModifier: (day: Int) -> Modifier = { Modifier },
    overlay: DrawScope.() -> Unit = {},
    capsule: @Composable BoxScope.(day: Int, selected: Boolean) -> Unit,
) {
    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        AutoplayDays.forEachIndexed { i, day ->
            delay(if (i == 0) AutoplayDelayMillis else AutoplayStepMillis)
            state.select(day)
        }
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
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .background(MenuColors.Tile, CardShape)
                    .border(1.dp, MenuColors.CardBorder, CardShape)
                    .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 16.dp),
            ) {
                MonthHeader()
                Spacer(Modifier.height(18.dp))
                WeekStrip(state, cellModifier, overlay, capsule)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MenuColors.CardBorder))
                Spacer(Modifier.height(14.dp))
                SelectionFooter(state.selected)
            }
        }
    }
}

@Composable
private fun MonthHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("November 2026", color = MenuColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        ArrowButton(pointsLeft = true)
        Spacer(Modifier.width(8.dp))
        ArrowButton(pointsLeft = false)
    }
}

@Composable
private fun ArrowButton(pointsLeft: Boolean) {
    Canvas(
        Modifier
            .size(30.dp)
            .background(MenuColors.TileLight, CircleShape),
    ) {
        val tip = if (pointsLeft) 0.42f else 0.58f
        val tail = if (pointsLeft) 0.56f else 0.44f
        val stroke = 1.8.dp.toPx()
        val w = size.width
        drawLine(MenuColors.Text, Offset(w * tail, w * 0.34f), Offset(w * tip, w * 0.5f), stroke, StrokeCap.Round)
        drawLine(MenuColors.Text, Offset(w * tip, w * 0.5f), Offset(w * tail, w * 0.66f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun WeekStrip(
    state: DatePickerState,
    cellModifier: (day: Int) -> Modifier,
    overlay: DrawScope.() -> Unit,
    capsule: @Composable BoxScope.(day: Int, selected: Boolean) -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(CellHeight)) {
        Row(Modifier.matchParentSize()) {
            WeekDays.forEach { day ->
                key(day) {
                    val selected = day == state.selected
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = CellInset)
                            .then(cellModifier(day))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClickLabel = "Select November $day",
                            ) { state.select(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day == Today) {
                            Box(Modifier.matchParentSize().border(1.dp, MenuColors.Text.copy(alpha = 0.18f), CapsuleShape))
                        }
                        capsule(day, selected)
                        DayLabel(day, selected)
                    }
                }
            }
        }
        Canvas(Modifier.matchParentSize()) { overlay() }
    }
}

@Composable
private fun DayLabel(day: Int, selected: Boolean) {
    val letterColor by animateColorAsState(
        if (selected) Color.White.copy(alpha = 0.85f) else MenuColors.Muted,
        tween(240),
        label = "letter",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Letters[day - FirstDay].toString(), color = letterColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(day.toString(), color = MenuColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectionFooter(day: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RollingText(day) { "${WeekdayNames[it - FirstDay]}, Nov $it" }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(6.dp).background(MenuColors.Cyan, CircleShape))
        Spacer(Modifier.width(6.dp))
        RollingText(day, color = MenuColors.Muted, fontWeight = FontWeight.Medium) {
            val slots = SlotsByDay.getValue(it)
            if (slots == 1) "1 slot" else "$slots slots"
        }
    }
}

@Composable
private fun RollingText(
    day: Int,
    color: Color = MenuColors.Text,
    fontWeight: FontWeight = FontWeight.SemiBold,
    text: (Int) -> String,
) {
    AnimatedContent(
        targetState = day,
        transitionSpec = {
            (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220)))
                .togetherWith(slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        label = "footer",
    ) {
        Text(text(it), color = color, fontSize = 14.sp, fontWeight = fontWeight)
    }
}
