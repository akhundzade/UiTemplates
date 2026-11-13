package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal object LightColors {
    val Background = Color(0xFFEFEEEB)
    val Card = Color(0xFFF6F5F2)
    val Backdrop = Color(0xFFE6E5E1)
    val Page = Color(0xFFFCFBF9)
    val Hairline = Color(0xFFBDBBB6)
    val Faint = Color(0xFFE2E0DB)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF8B8984)
}

internal enum class MenuOption(val label: String) {
    Home("HOME"),
    Explore("EXPLORE"),
    Library("LIBRARY"),
}

internal val PagePadding = 20.dp
internal val KebabSize = 36.dp
internal val OptionHeight = 40.dp

private const val AutoplayDelayMillis = 700L
private const val AutoplayPickDelayMillis = 1500L
private const val PickCloseDelayMillis = 240L

@Stable
internal class MinimalMenuState {
    var current by mutableStateOf(MenuOption.Home)
    var open by mutableStateOf(false)

    /** Set on the first real touch so the autoplay stops driving the menu. */
    var interacted by mutableStateOf(false)

    /** Lets the active dot visibly move to the picked option before the menu starts closing. */
    suspend fun pick(option: MenuOption) {
        current = option
        delay(PickCloseDelayMillis)
        open = false
    }
}

/**
 * The light stage plus the reel's autoplay (open, then pick LIBRARY). [autoplay] replaces the
 * default script; any touch inside the stage cancels it.
 */
@Composable
internal fun MinimalMenuScene(
    state: MinimalMenuState,
    number: Int,
    title: String,
    caption: String,
    autoplay: suspend () -> Unit = {
        state.open = true
        delay(AutoplayPickDelayMillis)
        state.pick(MenuOption.Library)
    },
    content: @Composable BoxScope.() -> Unit,
) {
    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        autoplay()
    }
    LightStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.interacted = true
                    }
                },
            content = content,
        )
    }
}

/** The light counterpart of the dark menus' stage: a hairline card with the caption box below. */
@Composable
private fun LightStage(
    number: Int,
    title: String,
    caption: String,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(LightColors.Background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .weight(1f)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(LightColors.Card)
                .border(1.dp, LightColors.Hairline, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
            content = content,
        )
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LightColors.Page)
                .border(1.dp, LightColors.Hairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text("$number  $title", color = LightColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(caption, color = LightColors.Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Small letter-spaced capitals, the reel's only type style. */
@Composable
internal fun Caps(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LightColors.Ink,
    fontSize: TextUnit = 10.sp,
    letterSpacing: TextUnit = 2.sp,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(text, modifier, color = color, fontSize = fontSize, letterSpacing = letterSpacing, fontWeight = fontWeight, maxLines = 1)
}

/**
 * The page every demo opens its menu over: the cross-fading caps title, [trailing] (normally the
 * kebab) at the top right, a hairline and three placeholder rows.
 */
@Composable
internal fun MinimalPage(state: MinimalMenuState, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .background(LightColors.Page)
            .padding(PagePadding),
    ) {
        Row(Modifier.fillMaxWidth().height(KebabSize), verticalAlignment = Alignment.CenterVertically) {
            Crossfade(state.current, Modifier.weight(1f), animationSpec = tween(280), label = "title") { option ->
                Caps(option.label, fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
            }
            trailing()
        }
        Spacer(Modifier.height(18.dp))
        Hairline()
        repeat(3) { i ->
            PlaceholderRow(i)
            Hairline()
        }
    }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LightColors.Faint))
}

@Composable
private fun PlaceholderRow(index: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (index == 0) LightColors.Ink else LightColors.Hairline))
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 14.dp)) {
            Box(Modifier.fillMaxWidth(0.62f - index * 0.08f).height(6.dp).clip(CircleShape).background(LightColors.Faint))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.38f).height(5.dp).clip(CircleShape).background(LightColors.Faint))
        }
        Box(Modifier.size(38.dp).border(1.dp, LightColors.Faint, RoundedCornerShape(6.dp)))
    }
}

/** Thin-outline circular ⋮ button that inverts to black while its menu is open. */
@Composable
internal fun KebabButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(if (open) 1f else 0f, tween(200), label = "kebabFill")
    Canvas(
        modifier
            .size(KebabSize)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Menu", onClick = onClick),
    ) {
        drawKebab(center, size.minDimension / 2, fill)
    }
}

/** The kebab circle at [fill] 0 (outlined) to 1 (solid black), shared with the demos that draw it themselves. */
internal fun DrawScope.drawKebab(center: Offset, radius: Float, fill: Float) {
    val stroke = 1.dp.toPx()
    drawCircle(lerp(LightColors.Page, LightColors.Ink, fill), radius, center)
    drawCircle(lerp(LightColors.Hairline, LightColors.Ink, fill), radius - stroke / 2, center, style = Stroke(stroke))
    drawKebabDots(center, lerp(LightColors.Ink, LightColors.Page, fill))
}

internal fun DrawScope.drawKebabDots(center: Offset, color: Color, alpha: Float = 1f) {
    val dot = 1.6.dp.toPx()
    val gap = 5.dp.toPx()
    for (i in -1..1) drawCircle(color, dot, center + Offset(0f, gap * i), alpha = alpha)
}

/** A caps option with the reel's dot before the active one. */
@Composable
internal fun OptionRow(
    option: MenuOption,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val dot by animateFloatAsState(if (active) 1f else 0f, tween(220), label = "dot")
    Row(
        modifier
            .fillMaxWidth()
            .height(OptionHeight)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(5.dp)) { drawCircle(LightColors.Ink, alpha = dot) }
        Spacer(Modifier.width(10.dp))
        Caps(option.label, Modifier.weight(1f), color = lerp(LightColors.Muted, LightColors.Ink, dot))
        trailing()
    }
}
