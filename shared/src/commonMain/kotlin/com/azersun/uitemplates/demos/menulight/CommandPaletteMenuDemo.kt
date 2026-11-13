package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val FieldHeight = 46.dp
private val ResultHeight = 42.dp
private val ResultsPadding = 6.dp
private val ResultsHeight = ResultHeight * MenuOption.entries.size + ResultsPadding * 2
private val PaletteCorner = 12.dp
private const val MaxQueryLength = 18

private fun MenuOption.matches(query: String): Boolean = label.contains(query.trim(), ignoreCase = true)

@Composable
fun CommandPaletteMenuDemo() {
    val state = remember { MinimalMenuState() }
    val scope = rememberCoroutineScope()
    var field by remember { mutableStateOf(TextFieldValue()) }
    var focused by remember { mutableStateOf(false) }
    var caretOn by remember { mutableStateOf(true) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val expand = remember { Animatable(0f) }
    val results = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val query = field.text
    val ranked = remember(query) { MenuOption.entries.sortedBy { if (it.matches(query)) 0 else 1 } }
    val highlighted = ranked.firstOrNull { it.matches(query) }
    val choose = { option: MenuOption -> scope.launch { state.pick(option) }; Unit }
    val type = { text: String -> field = TextFieldValue(text, TextRange(text.length)) }

    LaunchedEffect(state.open) {
        if (state.open) {
            launch {
                delay(200)
                results.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 380f))
            }
            // Only a real tap brings up the keyboard; the autoplay types into the unfocused field.
            if (state.interacted) {
                launch {
                    delay(250)
                    focusRequester.requestFocus()
                }
            }
            expand.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 320f))
        } else {
            focusManager.clearFocus()
            results.animateTo(0f, tween(140))
            expand.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 420f))
            type("")
        }
    }
    // The stand-in caret holds still while text arrives and blinks once it pauses.
    LaunchedEffect(state.open, query) {
        caretOn = true
        if (!state.open) return@LaunchedEffect
        while (true) {
            delay(530)
            caretOn = !caretOn
        }
    }

    MinimalMenuScene(
        state,
        6,
        "Command Palette",
        "A search field opens wide out of the ⋮; typing filters the options and the match takes ↵.",
        autoplay = {
            state.open = true
            delay(750)
            for (i in 1.."lib".length) {
                type("lib".take(i))
                delay(230)
            }
            delay(700)
            state.pick(MenuOption.Library)
        },
    ) {
        MinimalPage(state) { Spacer(Modifier.size(KebabSize)) }
        if (state.open) {
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { state.open = false })
        }
        Layout(
            content = {
                Box {
                    Column {
                        Row(Modifier.fillMaxWidth().height(FieldHeight), verticalAlignment = Alignment.CenterVertically) {
                            Canvas(
                                Modifier
                                    .padding(start = 16.dp)
                                    .size(15.dp)
                                    .graphicsLayer { alpha = ((expand.value - 0.3f) / 0.5f).coerceIn(0f, 1f) },
                            ) {
                                val stroke = 1.4.dp.toPx()
                                val r = size.width * 0.34f
                                val c = Offset(r + stroke, r + stroke)
                                drawCircle(LightColors.Ink, r, c, style = Stroke(stroke))
                                drawLine(LightColors.Ink, c + Offset(r * 0.72f, r * 0.72f), Offset(size.width, size.height), stroke, StrokeCap.Round)
                            }
                            Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text("Search…", color = LightColors.Muted, fontSize = 13.sp, letterSpacing = 1.sp, maxLines = 1)
                                }
                                BasicTextField(
                                    value = field,
                                    onValueChange = { if (it.text.length <= MaxQueryLength) field = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focused = it.isFocused }
                                        .onPreviewKeyEvent {
                                            if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                                                state.open = false
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        .drawWithContent {
                                            val show = !focused && caretOn && state.open
                                            val result = textLayout
                                            drawContent()
                                            if (show && result != null) {
                                                val caret = result.getCursorRect(result.layoutInput.text.length)
                                                val x = caret.left + 1.dp.toPx()
                                                drawLine(LightColors.Ink, Offset(x, caret.top), Offset(x, caret.bottom), 1.5.dp.toPx())
                                            }
                                        },
                                    enabled = state.open,
                                    singleLine = true,
                                    textStyle = TextStyle(color = LightColors.Ink, fontSize = 13.sp, letterSpacing = 1.sp),
                                    cursorBrush = SolidColor(LightColors.Ink),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = { highlighted?.let(choose) }),
                                    onTextLayout = { textLayout = it },
                                )
                            }
                            Box(
                                Modifier
                                    .padding(end = 10.dp)
                                    .graphicsLayer { alpha = ((expand.value - 0.3f) / 0.5f).coerceIn(0f, 1f) }
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(1.dp, LightColors.Hairline, RoundedCornerShape(5.dp))
                                    .clickable(enabled = state.open, role = Role.Button, onClickLabel = "Close") { state.open = false }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Caps("ESC", color = LightColors.Muted, fontSize = 8.sp, letterSpacing = 1.sp)
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(ResultsHeight)
                                .padding(vertical = ResultsPadding),
                        ) {
                            MenuOption.entries.forEach { option ->
                                ResultRow(
                                    option,
                                    slot = ranked.indexOf(option),
                                    match = option.matches(query),
                                    highlighted = option == highlighted,
                                    current = option == state.current,
                                    enabled = state.open,
                                    onClick = { choose(option) },
                                )
                            }
                        }
                    }
                    Canvas(Modifier.align(Alignment.TopEnd).size(KebabSize)) {
                        drawKebabDots(center, LightColors.Ink, alpha = (1f - expand.value * 3f).coerceIn(0f, 1f))
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = PagePadding, start = PagePadding, end = PagePadding)
                .graphicsLayer {
                    val e = expand.value.coerceIn(0f, 1f)
                    shadowElevation = 16.dp.toPx() * e
                    ambientShadowColor = Color.Black.copy(alpha = 0.10f)
                    spotShadowColor = Color.Black.copy(alpha = 0.16f)
                    shape = RoundedCornerShape(paletteCorner(expand.value, size.width, size.height, density))
                    clip = true
                }
                .drawWithContent {
                    val radius = CornerRadius(paletteCorner(expand.value, size.width, size.height, density))
                    val divider = results.value.coerceIn(0f, 1f)
                    drawRoundRect(LightColors.Page, cornerRadius = radius)
                    drawContent()
                    if (divider > 0f) {
                        val y = FieldHeight.toPx()
                        drawLine(LightColors.Faint, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), alpha = divider)
                    }
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        LightColors.Hairline,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius((radius.x - stroke / 2).coerceAtLeast(0f)),
                        style = Stroke(stroke),
                    )
                }
                .clickable(enabled = !state.open, role = Role.Button, onClickLabel = "Search menu") { state.open = true },
        ) { measurables, constraints ->
            val kebab = KebabSize.toPx()
            val fullWidth = constraints.maxWidth
            // Width stops at the page edge; only the height shows the spring's overshoot.
            val width = (kebab + (fullWidth - kebab) * expand.value.coerceIn(0f, 1f)).roundToInt()
            val height = (kebab + (FieldHeight.toPx() - kebab) * expand.value + ResultsHeight.toPx() * results.value)
                .coerceAtLeast(0f)
                .roundToInt()
            val content = measurables.first().measure(
                Constraints.fixed(fullWidth, max(height, (FieldHeight + ResultsHeight).roundToPx())),
            )
            // Anchored top-right, so the field grows leftwards out of the kebab.
            layout(width, height) { content.place(width - content.width, 0) }
        }
    }
}

@Composable
private fun ResultRow(
    option: MenuOption,
    slot: Int,
    match: Boolean,
    highlighted: Boolean,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val y by animateDpAsState(ResultHeight * slot, spring(dampingRatio = 0.8f, stiffness = 500f), label = "slot")
    val strength by animateFloatAsState(if (match) 1f else 0.3f, tween(200), label = "dim")
    val focus by animateFloatAsState(if (highlighted) 1f else 0f, tween(180), label = "highlight")
    Row(
        Modifier
            .offset { IntOffset(0, y.roundToPx()) }
            .fillMaxWidth()
            .height(ResultHeight)
            .graphicsLayer { alpha = strength }
            .drawBehind {
                if (focus <= 0f) return@drawBehind
                drawRect(LightColors.Card, alpha = focus)
                drawRect(LightColors.Ink, size = Size(2.dp.toPx(), size.height), alpha = focus)
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(5.dp)) { drawCircle(LightColors.Ink, alpha = if (current) 1f else 0f) }
        Spacer(Modifier.width(10.dp))
        Caps(option.label, Modifier.weight(1f))
        ReturnHint(Modifier.graphicsLayer { alpha = focus })
    }
}

/** A tiny key cap with a ↵ arrow, drawn so it never depends on the font having the glyph. */
@Composable
private fun ReturnHint(modifier: Modifier) {
    Canvas(
        modifier
            .size(width = 22.dp, height = 18.dp)
            .border(1.dp, LightColors.Hairline, RoundedCornerShape(4.dp)),
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.2.dp.toPx()
        val corner = Offset(w * 0.32f, h * 0.62f)
        drawLine(LightColors.Ink, Offset(w * 0.68f, h * 0.28f), Offset(w * 0.68f, h * 0.62f), stroke, StrokeCap.Round)
        drawLine(LightColors.Ink, Offset(w * 0.68f, h * 0.62f), corner, stroke, StrokeCap.Round)
        drawLine(LightColors.Ink, corner, corner + Offset(w * 0.12f, -h * 0.15f), stroke, StrokeCap.Round)
        drawLine(LightColors.Ink, corner, corner + Offset(w * 0.12f, h * 0.15f), stroke, StrokeCap.Round)
    }
}

/** A circle while the field is still the kebab, settling to the palette's corner radius. */
private fun paletteCorner(progress: Float, width: Float, height: Float, density: Float): Float {
    val round = KebabSize.value / 2 * density
    val target = round + (PaletteCorner.value * density - round) * progress.coerceIn(0f, 1f)
    return min(target, min(width, height) / 2)
}
