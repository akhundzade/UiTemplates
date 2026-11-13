package com.azersun.uitemplates.demos.switch2

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay

private val ButtonRadius = 62.dp
private val PlateGap = 14.dp
private val BaseDrop = 8.dp
private val DepthUp = 16.dp
private val DepthOn = 6.dp
private val DepthPressed = 3.dp
private val PlateWell = Color(0xFF0F0E22)
private val SideDark = Color(0xFF16142E)
private val SideLight = Color(0xFF2A2655)
private val FaceLight = Color(0xFF4A4590)
private val FaceDark = Color(0xFF2B2760)

@Composable
fun PushButton3DDemo() {
    var on by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = animateDpAsState(
        when {
            pressed -> DepthPressed
            on -> DepthOn
            else -> DepthUp
        },
        spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "depth",
    )
    val ring = animateFloatAsState(if (on) 1f else 0f, tween(380, delayMillis = if (on) 60 else 0), label = "ring")

    SwitchStage(
        3,
        "3D Push Button",
        "Round 3D button sinks when pressed and stays down. A cyan ring lights up.",
        autoplay = {
            val press = PressInteraction.Press(Offset.Zero)
            interaction.emit(press)
            try {
                delay(180)
            } finally {
                interaction.tryEmit(PressInteraction.Release(press))
            }
            on = true
        },
    ) {
        Box(
            Modifier
                .size(ButtonRadius * 2 + PlateGap * 2 + 48.dp)
                .drawWithCache {
                    val r = ButtonRadius.toPx()
                    val plate = r + PlateGap.toPx()
                    val base = size.center + Offset(0f, BaseDrop.toPx())
                    val sideBrush = Brush.horizontalGradient(
                        listOf(SideDark, SideLight, SideDark),
                        startX = base.x - r,
                        endX = base.x + r,
                    )
                    val faceBrush = Brush.verticalGradient(listOf(FaceLight, FaceDark), startY = base.y - r, endY = base.y + r)
                    val ringRadius = plate + 4.dp.toPx()
                    val rim = 2.5.dp.toPx()
                    val icon = r * 0.3f
                    val iconStroke = 5.dp.toPx()
                    val shadowOffset = Offset(0f, 3.dp.toPx())
                    val rimStroke = Stroke(rim)
                    val haloWide = Stroke(rim * 6f)
                    val haloNarrow = Stroke(rim * 2.5f)
                    val iconGlow = Stroke(iconStroke * 2.6f, cap = StrokeCap.Round)
                    val iconLine = Stroke(iconStroke, cap = StrokeCap.Round)
                    onDrawBehind {
                        val g = ring.value
                        val d = depth.value.toPx()
                        if (g > 0f) {
                            drawCircle(MenuColors.Cyan, ringRadius, base, alpha = 0.1f * g, style = haloWide)
                            drawCircle(MenuColors.Cyan, ringRadius, base, alpha = 0.25f * g, style = haloNarrow)
                        }
                        drawCircle(PlateWell, plate, base)
                        drawCircle(lerp(MenuColors.CardBorder, MenuColors.Cyan, g), plate, base, style = rimStroke)
                        drawCircle(Color.Black, r * 1.02f, base + shadowOffset, alpha = 0.45f)

                        // The extrusion: a capsule from the base up to the raised face.
                        drawCircle(sideBrush, r, base)
                        drawRect(sideBrush, Offset(base.x - r, base.y - d), Size(r * 2, d))

                        translate(0f, -d) {
                            drawCircle(faceBrush, r, base)
                            drawCircle(MenuColors.Cyan, r, base, alpha = 0.08f * g)
                            drawCircle(Color.White, r - rim / 2, base, alpha = 0.08f, style = rimStroke)
                            val iconColor = lerp(MenuColors.Muted, MenuColors.Cyan, g)
                            val arcTopLeft = Offset(base.x - icon, base.y - icon)
                            val arcSize = Size(icon * 2, icon * 2)
                            val top = Offset(base.x, base.y - icon * 1.25f)
                            val bottom = Offset(base.x, base.y - icon * 0.3f)
                            if (g > 0f) {
                                drawArc(MenuColors.Cyan, -60f, 300f, false, arcTopLeft, arcSize, alpha = 0.22f * g, style = iconGlow)
                                drawLine(MenuColors.Cyan, top, bottom, iconStroke * 2.6f, StrokeCap.Round, alpha = 0.22f * g)
                            }
                            drawArc(iconColor, -60f, 300f, false, arcTopLeft, arcSize, style = iconLine)
                            drawLine(iconColor, top, bottom, iconStroke, StrokeCap.Round)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .offset(y = BaseDrop)
                    .size((ButtonRadius + PlateGap) * 2)
                    .clip(CircleShape)
                    .toggleable(on, interaction, indication = null, role = Role.Switch) { on = it },
            )
        }
    }
}
