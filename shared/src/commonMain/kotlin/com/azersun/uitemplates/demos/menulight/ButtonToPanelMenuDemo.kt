package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val PanelWidth = 208.dp
private val PanelHeader = 46.dp
private val PanelHeight = PanelHeader + OptionHeight * MenuOption.entries.size + 10.dp
private val PanelCorner = 14.dp

@Composable
fun ButtonToPanelMenuDemo() {
    val state = remember { MinimalMenuState() }
    val scope = rememberCoroutineScope()
    val morph = remember { Animatable(0f) }
    LaunchedEffect(state.open) {
        morph.animateTo(
            if (state.open) 1f else 0f,
            if (state.open) spring(dampingRatio = 0.74f, stiffness = 300f) else spring(dampingRatio = 0.9f, stiffness = 420f),
        )
    }

    MinimalMenuScene(state, 5, "Button to Panel", "The ⋮ button itself grows into a black-outlined panel with a close × in its corner.") {
        // The header keeps the kebab's slot; the morphing button is laid over it.
        MinimalPage(state) { Spacer(Modifier.size(KebabSize)) }
        if (state.open) {
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { state.open = false })
        }
        Layout(
            content = {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(start = 6.dp, end = 6.dp, top = PanelHeader)) {
                        MenuOption.entries.forEachIndexed { i, option ->
                            OptionRow(
                                option,
                                active = option == state.current,
                                enabled = state.open,
                                onClick = { scope.launch { state.pick(option) } },
                                modifier = Modifier.graphicsLayer {
                                    val reveal = ((morph.value - 0.5f - i * 0.1f) / 0.3f).coerceIn(0f, 1f)
                                    alpha = reveal
                                    translationY = (1f - reveal) * 8.dp.toPx()
                                },
                            )
                        }
                    }
                    Caps(
                        "MENU",
                        Modifier
                            .padding(start = 20.dp, top = 12.dp)
                            .graphicsLayer { alpha = ((morph.value - 0.45f) / 0.3f).coerceIn(0f, 1f) },
                        color = LightColors.Muted,
                        fontSize = 9.sp,
                    )
                    Canvas(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(KebabSize)
                            .clip(CircleShape)
                            .clickable(enabled = state.open, role = Role.Button, onClickLabel = "Close") { state.open = false },
                    ) {
                        val p = morph.value
                        drawKebabDots(center, LightColors.Ink, alpha = (1f - p * 2.5f).coerceIn(0f, 1f))
                        val cross = ((p - 0.35f) / 0.4f).coerceIn(0f, 1f)
                        if (cross > 0f) {
                            val arm = 5.dp.toPx() * cross
                            rotate(90f * (1f - cross)) {
                                drawLine(LightColors.Ink, center - Offset(arm, arm), center + Offset(arm, arm), 1.5.dp.toPx(), StrokeCap.Round)
                                drawLine(LightColors.Ink, center + Offset(-arm, arm), center + Offset(arm, -arm), 1.5.dp.toPx(), StrokeCap.Round)
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = PagePadding, end = PagePadding)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(cornerFor(morph.value, size.width, size.height, density))
                }
                .drawWithContent {
                    val p = morph.value
                    val radius = cornerFor(p, size.width, size.height, density)
                    drawRoundRect(LightColors.Page, cornerRadius = CornerRadius(radius))
                    drawContent()
                    val stroke = mix(1f, 1.5f, p.coerceIn(0f, 1f)) * density
                    drawRoundRect(
                        lerp(LightColors.Hairline, LightColors.Ink, p.coerceIn(0f, 1f)),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius((radius - stroke / 2).coerceAtLeast(0f)),
                        style = Stroke(stroke),
                    )
                }
                .clickable(enabled = !state.open, role = Role.Button, onClickLabel = "Menu") { state.open = true },
        ) { measurables, _ ->
            val p = morph.value
            val kebab = KebabSize.toPx()
            val width = mix(kebab, PanelWidth.toPx(), p).coerceAtLeast(0f).roundToInt()
            val height = mix(kebab, PanelHeight.toPx(), p).coerceAtLeast(0f).roundToInt()
            // The spring's overshoot stretches the content too instead of opening a gap beside it.
            val panel = measurables.first().measure(
                Constraints.fixed(max(width, PanelWidth.roundToPx()), max(height, PanelHeight.roundToPx())),
            )
            // Anchored top-right, so the panel grows out of the button towards the bottom-left.
            layout(width, height) { panel.place(width - panel.width, 0) }
        }
    }
}

/** A circle while the button is small, settling to the panel's corner radius as it grows. */
private fun cornerFor(progress: Float, width: Float, height: Float, density: Float): Float {
    val round = KebabSize.value / 2 * density
    val target = mix(round, PanelCorner.value * density, progress.coerceIn(0f, 1f))
    return min(target, min(width, height) / 2)
}

private fun mix(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction
