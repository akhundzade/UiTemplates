package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class ViewMode(val label: String, val color: Color, val ink: Color) {
    Grid("Grid", MenuColors.Violet, Color.White),
    List("List", MenuColors.Blue, Color.White),
    Board("Board", MenuColors.Pink, Color.White),
    Calendar("Calendar", MenuColors.Yellow, Color(0xFF2A2140)),
    ;

    val next: ViewMode get() = entries[(ordinal + 1) % entries.size]
}

internal val MenuWidth = 168.dp
internal val MenuGap = 10.dp
internal val MenuCorner = 14.dp
internal val ButtonHeight = 40.dp
internal val WaveBlue = Color(0xFF5AB4FF)

private val ButtonWidth = 118.dp
private val LabelHeight = 16.dp
private val LabelGap = 6.dp
private val RowHeight = 40.dp
private val MenuShape = RoundedCornerShape(MenuCorner)
private val RowShape = RoundedCornerShape(10.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

private val SceneCard = Color(0xFF13112A)
private val PreviewSurface = Color(0xFF1E1B3D)
private val ButtonSurface = Color(0xFF1E1B3E)
private val MenuSurface = Color(0xFF221A4A)
private val MenuBorder = Color(0xFF3B2F7A)
private val Mauve = Color(0xFF9B7BD0)
private val RowHighlight = Brush.horizontalGradient(listOf(MenuColors.Blue, MenuColors.Violet))

private const val AutoplayDelayMillis = 700L
private const val AutoplayPickDelayMillis = 1900L
private const val PickCloseDelayMillis = 220L

@Stable
internal class ViewAsState {
    var view by mutableStateOf(ViewMode.Board)
    var open by mutableStateOf(false)

    /** Set on the first real tap so the autoplay stops driving the menu. */
    var interacted by mutableStateOf(false)

    /** Lets the highlight visibly move to the picked row before the menu starts closing. */
    suspend fun pick(mode: ViewMode) {
        view = mode
        delay(PickCloseDelayMillis)
        open = false
    }
}

@Composable
internal fun rememberViewAsState(): ViewAsState = remember { ViewAsState() }

/**
 * The shared "View as" card. [menu] is laid out with its top-left corner at the menu panel's
 * position: level with the button's top, [MenuGap] to the right of the button.
 */
@Composable
internal fun ViewAsScene(
    state: ViewAsState,
    number: Int,
    title: String,
    caption: String,
    menu: @Composable BoxScope.() -> Unit,
) {
    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        state.open = true
        delay(AutoplayPickDelayMillis)
        state.pick(state.view.next)
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .background(SceneCard, RoundedCornerShape(22.dp))
                .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(22.dp))
                .padding(14.dp),
        ) {
            Row {
                Column(Modifier.width(ButtonWidth)) {
                    Box(Modifier.height(LabelHeight), contentAlignment = Alignment.CenterStart) {
                        Text("VIEW AS", color = MenuColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    }
                    Spacer(Modifier.height(LabelGap))
                    ViewButton(state)
                    Text("12 projects", color = MenuColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
                Spacer(Modifier.width(12.dp))
                ProjectsPreview(state.view, Modifier.weight(1f).height(214.dp))
            }
            Box(Modifier.padding(start = ButtonWidth + MenuGap, top = LabelHeight + LabelGap), content = menu)
        }
    }
}

@Composable
private fun ViewButton(state: ViewAsState) {
    val glow by animateFloatAsState(if (state.open) 1f else 0f, tween(220), label = "glow")
    val chevron by animateFloatAsState(if (state.open) 180f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "chevron")
    Row(
        Modifier
            .width(ButtonWidth)
            .height(ButtonHeight)
            .drawBehind {
                if (glow <= 0f) return@drawBehind
                val step = 2.dp.toPx()
                for (i in 4 downTo 1) {
                    val inflate = step * i
                    drawRoundRect(
                        MenuColors.Violet,
                        topLeft = Offset(-inflate, -inflate),
                        size = Size(size.width + inflate * 2, size.height + inflate * 2),
                        cornerRadius = CornerRadius(12.dp.toPx() + inflate),
                        alpha = glow * 0.13f,
                    )
                }
            }
            .clip(ButtonShape)
            .background(ButtonSurface)
            .border(1.dp, lerp(MenuColors.CardBorder, MenuColors.Violet, glow), ButtonShape)
            .clickable {
                state.interacted = true
                state.open = !state.open
            }
            .padding(start = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(state.view, Modifier.weight(1f), animationSpec = tween(220), label = "label") { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ViewIcon(mode, 18.dp)
                Text(
                    mode.label,
                    color = MenuColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Canvas(Modifier.size(12.dp).graphicsLayer { rotationZ = chevron }) {
            val stroke = size.width * 0.15f
            drawLine(MenuColors.Muted, Offset(size.width * 0.15f, size.height * 0.35f), Offset(size.width * 0.5f, size.height * 0.7f), stroke, StrokeCap.Round)
            drawLine(MenuColors.Muted, Offset(size.width * 0.5f, size.height * 0.7f), Offset(size.width * 0.85f, size.height * 0.35f), stroke, StrokeCap.Round)
        }
    }
}

/**
 * The menu panel. [backgroundAlpha] fades only the surface and [row] decorates each row, so
 * demos can animate the surface and the rows independently.
 */
@Composable
internal fun ViewAsMenu(
    state: ViewAsState,
    modifier: Modifier = Modifier,
    backgroundAlpha: () -> Float = { 1f },
    row: (index: Int) -> Modifier = { Modifier },
) {
    val scope = rememberCoroutineScope()
    Box(modifier.width(MenuWidth)) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = backgroundAlpha() }
                .shadow(18.dp, MenuShape)
                .background(MenuSurface, MenuShape)
                .border(1.dp, MenuBorder, MenuShape),
        )
        Column(Modifier.clip(MenuShape).padding(6.dp)) {
            ViewMode.entries.forEach { mode ->
                MenuRow(mode, selected = mode == state.view, enabled = state.open, modifier = row(mode.ordinal)) {
                    state.interacted = true
                    scope.launch { state.pick(mode) }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(mode: ViewMode, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val highlight by animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "highlight")
    Row(
        modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawBehind {
                if (highlight > 0f) drawRoundRect(RowHighlight, cornerRadius = CornerRadius(10.dp.toPx()), alpha = highlight)
            }
            .clip(RowShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViewIcon(mode, 20.dp)
        Text(
            mode.label,
            color = MenuColors.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        Canvas(Modifier.size(12.dp).graphicsLayer { alpha = highlight }) {
            val check = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.52f)
                lineTo(size.width * 0.4f, size.height * 0.8f)
                lineTo(size.width * 0.9f, size.height * 0.22f)
            }
            drawPath(check, Color.White, style = Stroke(size.width * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** A filled rounded-square tile in the view's colour with a tiny glyph of that layout. */
@Composable
internal fun ViewIcon(mode: ViewMode, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val ink = mode.ink
        drawRoundRect(mode.color, cornerRadius = CornerRadius(w * 0.28f))
        fun block(x: Float, y: Float, bw: Float, bh: Float) =
            drawRoundRect(ink, Offset(w * x, w * y), Size(w * bw, w * bh), CornerRadius(w * 0.06f))
        when (mode) {
            ViewMode.Grid -> {
                block(0.25f, 0.25f, 0.21f, 0.21f); block(0.54f, 0.25f, 0.21f, 0.21f)
                block(0.25f, 0.54f, 0.21f, 0.21f); block(0.54f, 0.54f, 0.21f, 0.21f)
            }
            ViewMode.List -> listOf(0.32f, 0.5f, 0.68f).forEach { y ->
                drawLine(ink, Offset(w * 0.27f, w * y), Offset(w * 0.73f, w * y), w * 0.09f, StrokeCap.Round)
            }
            ViewMode.Board -> {
                block(0.23f, 0.25f, 0.15f, 0.5f); block(0.425f, 0.25f, 0.15f, 0.34f); block(0.62f, 0.25f, 0.15f, 0.42f)
            }
            ViewMode.Calendar -> {
                drawRoundRect(ink, Offset(w * 0.24f, w * 0.28f), Size(w * 0.52f, w * 0.48f), CornerRadius(w * 0.07f), style = Stroke(w * 0.08f))
                block(0.24f, 0.28f, 0.52f, 0.13f)
                drawLine(ink, Offset(w * 0.37f, w * 0.2f), Offset(w * 0.37f, w * 0.32f), w * 0.08f, StrokeCap.Round)
                drawLine(ink, Offset(w * 0.63f, w * 0.2f), Offset(w * 0.63f, w * 0.32f), w * 0.08f, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ProjectsPreview(view: ViewMode, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PreviewSurface)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Projects", color = MenuColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("12", color = MenuColors.Muted, fontSize = 11.sp)
        }
        Crossfade(view, Modifier.padding(top = 10.dp).fillMaxSize(), animationSpec = tween(360), label = "preview") { mode ->
            Canvas(Modifier.fillMaxSize()) { drawPreview(mode) }
        }
    }
}

private fun DrawScope.drawPreview(mode: ViewMode) {
    val w = size.width
    val h = size.height
    val gap = 6.dp.toPx()
    val radius = CornerRadius(5.dp.toPx())
    when (mode) {
        ViewMode.Board -> {
            val colWidth = (w - gap * 2) / 3
            val header = 6.dp.toPx()
            val block = 26.dp.toPx()
            listOf(2, 3, 1).forEachIndexed { col, blocks ->
                val x = col * (colWidth + gap)
                drawRoundRect(MenuColors.Pink, Offset(x, 0f), Size(colWidth, header), CornerRadius(header / 2))
                repeat(blocks) { i ->
                    val y = header + gap + i * (block + gap * 0.8f)
                    drawRoundRect(Mauve, Offset(x, y), Size(colWidth, block), radius, alpha = 0.55f)
                }
            }
        }
        ViewMode.Grid -> {
            val tileW = (w - gap) / 2
            val tileH = (h - gap * 2) / 3
            for (row in 0 until 3) for (col in 0 until 2) {
                val topLeft = Offset(col * (tileW + gap), row * (tileH + gap))
                drawRoundRect(MenuColors.Violet, topLeft, Size(tileW, tileH), radius, alpha = 0.32f)
                drawRoundRect(
                    MenuColors.Text,
                    topLeft + Offset(gap, gap),
                    Size(tileW * 0.5f, 4.dp.toPx()),
                    CornerRadius(2.dp.toPx()),
                    alpha = 0.35f,
                )
            }
        }
        ViewMode.List -> {
            val rowH = 24.dp.toPx()
            val dot = 4.dp.toPx()
            repeat(4) { i ->
                val y = i * (rowH + gap * 1.2f)
                drawRoundRect(MenuColors.Text, Offset(0f, y), Size(w, rowH), radius, alpha = 0.07f)
                drawCircle(MenuColors.Blue, dot, Offset(gap + dot, y + rowH / 2))
                drawRoundRect(
                    MenuColors.Text,
                    Offset(gap * 2 + dot * 2, y + rowH / 2 - 2.dp.toPx()),
                    Size(w * (0.55f - i * 0.06f), 4.dp.toPx()),
                    CornerRadius(2.dp.toPx()),
                    alpha = 0.4f,
                )
            }
        }
        ViewMode.Calendar -> {
            val cellGap = 3.dp.toPx()
            val cell = (w - cellGap * 6) / 7
            val accents = mapOf(3 to MenuColors.Yellow, 9 to MenuColors.Pink, 15 to MenuColors.Blue, 22 to MenuColors.Violet, 25 to MenuColors.Yellow)
            for (i in 0 until 28) {
                val topLeft = Offset((i % 7) * (cell + cellGap), (i / 7) * (cell + cellGap))
                val accent = accents[i]
                drawRoundRect(accent ?: MenuColors.Text, topLeft, Size(cell, cell), CornerRadius(3.dp.toPx()), alpha = if (accent != null) 0.85f else 0.07f)
            }
        }
    }
}
