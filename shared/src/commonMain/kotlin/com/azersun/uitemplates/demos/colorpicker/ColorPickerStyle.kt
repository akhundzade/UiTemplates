package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal class Swatch(val name: String, val color: Color)

internal val Swatches = listOf(
    Swatch("Violet", Color(0xFF8B7CF6)),
    Swatch("Sky", Color(0xFF38BDF8)),
    Swatch("Rose", Color(0xFFF472B6)),
    Swatch("Gold", Color(0xFFFCD34D)),
)

internal val PanelColor = Color(0xFF13112A)
internal val DotSize = 24.dp
internal val RingRadius = 16.dp
internal val RingStroke = 2.dp

/** Where the paint lands on the tee, as a fraction of the tee box. */
internal val TeeImpact = Offset(0.5f, 0.5f)

private const val InitialSwatch = 3
private const val AutoplayDelayMillis = 700L
private const val AutoplayStepMillis = 1400L
private val AutoplaySwatches = listOf(1, 0)

private val SlotSize = 38.dp
private val CardShape = RoundedCornerShape(24.dp)
private val PanelShape = RoundedCornerShape(20.dp)
private val TeeInk = Color(0xFF14122B)

@Stable
internal class SwatchState {
    var selected by mutableIntStateOf(InitialSwatch)
        private set
    var previous by mutableIntStateOf(InitialSwatch)
        private set

    /** Set on the first real touch so the autoplay stops driving the picker. */
    var interacted by mutableStateOf(false)

    val color: Color get() = Swatches[selected].color

    /** False only before the first selection, so demos can skip animating the opening colour. */
    val changed: Boolean get() = selected != previous

    fun select(index: Int) {
        if (index == selected) return
        previous = selected
        selected = index
    }
}

@Composable
internal fun rememberSwatchState(): SwatchState = remember { SwatchState() }

/** Positions of the tee and swatches in the card's coordinates, for effects that travel between them. */
@Stable
internal class CardLayout {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var teeOrigin by mutableStateOf(Offset.Zero)
    internal var teeSize by mutableStateOf(Size.Zero)
    internal var rowOrigin by mutableStateOf(Offset.Zero)
    internal var rowSize by mutableStateOf(Size.Zero)

    val measured: Boolean get() = rowSize.width > 0f && teeSize.width > 0f

    val teeRect: Rect get() = Rect(teeOrigin - origin, teeSize)

    /** Centre of a (possibly fractional) swatch slot. */
    fun swatchCenter(index: Float): Offset =
        rowOrigin - origin + Offset((index + 0.5f) * rowSize.width / Swatches.size, rowSize.height / 2)
}

/**
 * The shared "Aurora Tee" product card. It owns the selection and the reel's autoplay
 * (Gold → Sky, then Violet). [tee] paints the tee into its square box (transformed by
 * [teeModifier]), [panel] paints the image panel behind it, [swatch] fills each swatch slot
 * and [overlay] draws over the whole card using [CardLayout] coordinates.
 */
@Composable
internal fun ColorPickerScene(
    state: SwatchState,
    number: Int,
    title: String,
    caption: String,
    tee: DrawScope.(TeeArt) -> Unit,
    teeModifier: Modifier = Modifier,
    panel: DrawScope.() -> Unit = { drawDefaultPanel() },
    overlay: DrawScope.(CardLayout) -> Unit = {},
    swatch: @Composable BoxScope.(index: Int, selected: Boolean) -> Unit = { i, selected -> DefaultSwatch(i, selected) },
) {
    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        AutoplaySwatches.forEachIndexed { i, index ->
            delay(if (i == 0) AutoplayDelayMillis else AutoplayStepMillis)
            state.select(index)
        }
    }
    val layout = remember { CardLayout() }

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
            Row(
                Modifier
                    .padding(horizontal = 12.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { layout.origin = it.positionInRoot() }
                    .drawWithContent {
                        drawContent()
                        overlay(layout)
                    }
                    .background(MenuColors.Tile, CardShape)
                    .border(1.dp, MenuColors.CardBorder, CardShape)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(PanelShape)
                        .drawBehind(panel),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .onGloballyPositioned {
                                layout.teeOrigin = it.positionInRoot()
                                layout.teeSize = it.size.toSize()
                            }
                            .then(teeModifier)
                            .drawWithCache {
                                val art = TeeArt(size)
                                onDrawBehind { tee(art) }
                            },
                    )
                }
                Spacer(Modifier.width(14.dp))
                ProductInfo(state, layout, swatch)
            }
        }
    }
}

@Composable
private fun ProductInfo(
    state: SwatchState,
    layout: CardLayout,
    swatch: @Composable BoxScope.(index: Int, selected: Boolean) -> Unit,
) {
    Column {
        Text(
            "AURORA TEE",
            color = MenuColors.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
        AnimatedContent(
            targetState = state.selected,
            transitionSpec = {
                (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220)))
                    .togetherWith(slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(160)))
                    .using(SizeTransform(clip = false))
            },
            label = "colour name",
            modifier = Modifier.padding(start = 6.dp, top = 2.dp),
        ) { index ->
            Text(Swatches[index].name, color = MenuColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Organic cotton",
            color = MenuColors.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, top = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.onGloballyPositioned {
                layout.rowOrigin = it.positionInRoot()
                layout.rowSize = it.size.toSize()
            },
        ) {
            Swatches.forEachIndexed { i, s ->
                val selected = i == state.selected
                Box(
                    Modifier
                        .size(SlotSize)
                        .selectable(
                            selected = selected,
                            interactionSource = null,
                            indication = null,
                            role = Role.RadioButton,
                        ) { state.select(i) }
                        .semantics { contentDescription = s.name },
                    contentAlignment = Alignment.Center,
                ) {
                    swatch(i, selected)
                }
            }
        }
    }
}

/** Swatch dot with the plain white selection ring, as on the reel's resting card. */
@Composable
internal fun BoxScope.DefaultSwatch(index: Int, selected: Boolean) {
    val ring = animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "ring")
    SwatchDot(index)
    SelectionRing({ ring.value })
}

@Composable
internal fun SwatchDot(index: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(DotSize).background(Swatches[index].color, CircleShape))
}

@Composable
internal fun BoxScope.SelectionRing(alpha: () -> Float) {
    Canvas(Modifier.matchParentSize()) {
        val a = alpha()
        if (a <= 0f) return@Canvas
        val radius = RingRadius.toPx() * (0.8f + 0.2f * a)
        drawCircle(Color.White, radius, alpha = a, style = Stroke(RingStroke.toPx()))
    }
}

internal fun DrawScope.drawDefaultPanel() {
    drawRect(PanelColor)
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.42f),
            radius = size.width * 0.62f,
        ),
    )
}

/** The tee's outline and details, built once per size and drawn in any colour by [drawTee]. */
internal class TeeArt(val size: Size) {
    private val w = size.width
    private val h = size.height

    val outline = Path().apply {
        moveTo(x(0.25f), y(0.17f))
        lineTo(x(0.39f), y(0.115f))
        quadraticTo(x(0.5f), y(0.08f), x(0.61f), y(0.115f))
        lineTo(x(0.75f), y(0.17f))
        lineTo(x(0.95f), y(0.37f))
        lineTo(x(0.85f), y(0.48f))
        lineTo(x(0.765f), y(0.415f))
        lineTo(x(0.775f), y(0.89f))
        quadraticTo(x(0.5f), y(0.915f), x(0.225f), y(0.89f))
        lineTo(x(0.235f), y(0.415f))
        lineTo(x(0.15f), y(0.48f))
        lineTo(x(0.05f), y(0.37f))
        close()
    }

    val impact = Offset(w * TeeImpact.x, h * TeeImpact.y)

    /** Vertical span of the tee, for effects that fill it from the bottom. */
    val top = y(0.07f)
    val bottom = y(0.93f)

    internal val neckInside = Path().apply {
        moveTo(x(0.4f), y(0.12f))
        quadraticTo(x(0.5f), y(0.095f), x(0.6f), y(0.12f))
        quadraticTo(x(0.5f), y(0.225f), x(0.4f), y(0.12f))
        close()
    }

    internal val neckRib = Path().apply {
        moveTo(x(0.4f), y(0.12f))
        quadraticTo(x(0.5f), y(0.225f), x(0.6f), y(0.12f))
    }

    internal val logo = Path().apply {
        listOf(0.4f, 0.44f).forEachIndexed { i, row ->
            val inset = i * 0.03f
            moveTo(x(0.41f + inset), y(row))
            val step = (0.18f - inset * 2) / 4
            for (k in 0 until 4) {
                val dir = if (k % 2 == 0) -1 else 1
                quadraticTo(x(0.41f + inset + step * (k + 0.5f)), y(row + dir * 0.022f), x(0.41f + inset + step * (k + 1)), y(row))
            }
        }
    }

    internal val edge = Stroke(w * 0.02f, join = StrokeJoin.Round)
    internal val ribStroke = Stroke(w * 0.022f, cap = StrokeCap.Round)
    internal val logoStroke = Stroke(w * 0.018f, cap = StrokeCap.Round)
    internal val seam = w * 0.008f

    internal val sideShade = Brush.horizontalGradient(
        0f to Color.Black.copy(alpha = 0.3f),
        0.3f to Color.Transparent,
        0.65f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.32f),
        startX = w * 0.05f,
        endX = w * 0.95f,
    )
    internal val hemShade = Brush.verticalGradient(
        0.55f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.2f),
        startY = 0f,
        endY = h * 0.92f,
    )

    private fun x(f: Float) = w * f
    private fun y(f: Float) = h * f
}

/** Paints the tee in [color]; [showLogo] is false while its back faces the viewer. */
internal fun DrawScope.drawTee(art: TeeArt, color: Color, showLogo: Boolean = true) {
    val w = art.size.width
    val h = art.size.height
    drawPath(art.outline, color)
    drawPath(art.outline, color, style = art.edge)
    drawPath(art.outline, art.sideShade)
    drawPath(art.outline, art.hemShade)
    drawPath(art.neckInside, lerp(color, Color.Black, 0.45f))
    drawPath(art.neckRib, lerp(color, Color.Black, 0.18f), style = art.ribStroke)
    val seamColor = Color.Black.copy(alpha = 0.14f)
    drawLine(seamColor, Offset(w * 0.27f, h * 0.18f), Offset(w * 0.245f, h * 0.405f), art.seam)
    drawLine(seamColor, Offset(w * 0.73f, h * 0.18f), Offset(w * 0.755f, h * 0.405f), art.seam)
    drawLine(seamColor, Offset(w * 0.105f, h * 0.405f), Offset(w * 0.18f, h * 0.465f), art.seam)
    drawLine(seamColor, Offset(w * 0.895f, h * 0.405f), Offset(w * 0.82f, h * 0.465f), art.seam)
    if (showLogo) drawPath(art.logo, TeeInk.copy(alpha = 0.5f), style = art.logoStroke)
}
