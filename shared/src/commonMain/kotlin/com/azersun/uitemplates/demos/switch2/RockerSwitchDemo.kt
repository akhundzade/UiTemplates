package com.azersun.uitemplates.demos.switch2

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors

private const val TiltDegrees = 12f
private val PlateWidth = 150.dp
private val PlateHeight = 236.dp
private val PlateCorner = 26.dp
private val RockerWidth = 84.dp
private val RockerHeight = 148.dp
private val RockerCorner = 14.dp
private val RockerDrop = 10.dp
private val WellPad = 5.dp
private val LedTop = 22.dp
private val LedRadius = 4.dp
private val PlateGradient = Brush.verticalGradient(listOf(Color(0xFF2C2955), Color(0xFF1E1B3E)))
private val RockerGradient = Brush.verticalGradient(listOf(Color(0xFF423D80), Color(0xFF2E2A5C)))
private val LedOff = Color(0xFF3A3670)

@Composable
fun RockerSwitchDemo() {
    var on by remember { mutableStateOf(false) }
    // Positive rotationX tips the top edge away from the viewer, so "on" presses the I half in.
    val tilt = animateFloatAsState(
        if (on) TiltDegrees else -TiltDegrees,
        spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "tilt",
    )
    val led = animateFloatAsState(if (on) 1f else 0f, tween(220, delayMillis = if (on) 80 else 0), label = "led")

    SwitchStage(
        4,
        "Rocker Switch",
        "Wall rocker snaps to the other side: pressed half darkens, the LED glows.",
        autoplay = { on = true },
    ) {
        Box(
            Modifier
                .size(PlateWidth, PlateHeight)
                .clip(RoundedCornerShape(PlateCorner))
                .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(PlateCorner))
                .drawWithCache {
                    val ledCenter = Offset(size.width / 2, LedTop.toPx())
                    val ledRadius = LedRadius.toPx()
                    val ledGlow = Brush.radialGradient(
                        listOf(MenuColors.Cyan.copy(alpha = 0.6f), Color.Transparent),
                        center = ledCenter,
                        radius = ledRadius * 5f,
                    )
                    val pad = WellPad.toPx()
                    val well = Size(RockerWidth.toPx() + pad * 2, RockerHeight.toPx() + pad * 2)
                    val wellTopLeft = Offset((size.width - well.width) / 2, (size.height - well.height) / 2 + RockerDrop.toPx())
                    val wellCorner = CornerRadius((RockerCorner + WellPad).toPx())
                    onDrawBehind {
                        drawRect(PlateGradient)
                        drawRoundRect(Color(0xFF0F0E22), wellTopLeft, well, wellCorner)
                        val g = led.value
                        if (g > 0f) drawCircle(ledGlow, ledRadius * 5f, ledCenter, alpha = g)
                        drawCircle(LedOff, ledRadius, ledCenter)
                        drawCircle(MenuColors.Cyan, ledRadius, ledCenter, alpha = g)
                        drawCircle(Color.White, ledRadius * 0.4f, ledCenter, alpha = 0.9f * g)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .offset(y = RockerDrop)
                    .size(RockerWidth, RockerHeight)
                    .graphicsLayer {
                        rotationX = tilt.value
                        cameraDistance = 12f * density
                    }
                    .clip(RoundedCornerShape(RockerCorner))
                    .toggleable(on, interactionSource = null, indication = null, role = Role.Switch) { on = it }
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val markStroke = 3.5.dp.toPx()
                        val ringStroke = Stroke(markStroke)
                        val mark = MenuColors.Text.copy(alpha = 0.85f)
                        onDrawBehind {
                            drawRect(RockerGradient)
                            // -1 = bottom (O) pressed in, 1 = top (I) pressed in.
                            val t = (tilt.value / TiltDegrees).coerceIn(-1f, 1f)
                            val topPressed = t.coerceAtLeast(0f)
                            val bottomPressed = (-t).coerceAtLeast(0f)
                            clipRect(bottom = h / 2) {
                                drawRect(Color.Black, alpha = 0.38f * topPressed)
                                drawRect(Color.White, alpha = 0.06f * bottomPressed)
                            }
                            clipRect(top = h / 2) {
                                drawRect(Color.Black, alpha = 0.38f * bottomPressed)
                                drawRect(Color.White, alpha = 0.06f * topPressed)
                            }
                            drawLine(Color.Black, Offset(0f, h / 2), Offset(w, h / 2), 1.dp.toPx(), alpha = 0.35f)
                            drawLine(
                                mark,
                                Offset(w / 2, h * 0.25f - 11.dp.toPx()),
                                Offset(w / 2, h * 0.25f + 11.dp.toPx()),
                                markStroke,
                                StrokeCap.Round,
                            )
                            drawCircle(mark, 10.dp.toPx(), Offset(w / 2, h * 0.75f), style = ringStroke)
                        }
                    },
            )
        }
    }
}
