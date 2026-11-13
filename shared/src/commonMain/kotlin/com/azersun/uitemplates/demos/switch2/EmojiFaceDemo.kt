package com.azersun.uitemplates.demos.switch2

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.abs
import kotlin.math.roundToInt

private val FaceInk = Color(0xFF3B2A05)
private val FaceShade = Color(0xFFE9A21C)

@Composable
fun EmojiFaceDemo() {
    var on by remember { mutableStateOf(false) }
    val slide = animateFloatAsState(if (on) 1f else 0f, tween(650, easing = FastOutSlowInEasing), label = "slide")

    SwitchStage(
        5,
        "Emoji Face",
        "The knob is a face: it blinks as it slides and its frown turns into a smile.",
        autoplay = { on = true },
    ) {
        Box(
            Modifier
                .size(TrackWidth, TrackHeight)
                .drawWithCache {
                    val radius = CornerRadius(size.height / 2)
                    onDrawBehind {
                        drawRoundRect(TrackOff, cornerRadius = radius)
                        drawRoundRect(TrackOn, cornerRadius = radius, alpha = slide.value)
                    }
                }
                .toggleable(on, interactionSource = null, indication = null, role = Role.Switch) { on = it },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset { IntOffset((KnobInset + KnobTravel * slide.value).toPx().roundToInt(), 0) }
                    .size(KnobSize)
                    .drawWithCache {
                        val r = size.minDimension / 2
                        val eyeX = r * 0.32f
                        val eyeY = r * -0.18f
                        val eyeW = r * 0.17f
                        val eyeH = r * 0.24f
                        val cornerX = r * 0.4f
                        val mouth = Path()
                        val mouthStroke = Stroke(r * 0.1f, cap = StrokeCap.Round)
                        val shadowOffset = Offset(0f, 2.dp.toPx())
                        onDrawBehind {
                            val p = slide.value
                            // A quick blink centred on the middle of the slide.
                            val blink = (1f - abs(p - 0.5f) / 0.18f).coerceIn(0f, 1f)
                            val eyeHeight = eyeH * (1f - 0.88f * blink)

                            drawCircle(Color.Black, r, center + shadowOffset, alpha = 0.22f)
                            drawCircle(MenuColors.Yellow)
                            drawCircle(FaceShade, r, alpha = 0.35f, style = Stroke(r * 0.08f))
                            repeat(2) { i ->
                                val side = i * 2 - 1
                                val c = Offset(center.x + eyeX * side, center.y + eyeY)
                                drawOval(FaceInk, Offset(c.x - eyeW, c.y - eyeHeight), Size(eyeW * 2, eyeHeight * 2))
                                drawCircle(MenuColors.Pink, r * 0.12f, Offset(c.x + r * 0.1f * side, center.y + r * 0.2f), alpha = 0.45f * p)
                            }

                            // Frown corners sit low with the curve bowing up; the smile bows down.
                            val cornerY = center.y + mix(r * 0.46f, r * 0.24f, p)
                            val controlY = center.y + mix(r * 0.08f, r * 0.78f, p)
                            mouth.reset()
                            mouth.moveTo(center.x - cornerX, cornerY)
                            mouth.quadraticTo(center.x, controlY, center.x + cornerX, cornerY)
                            drawPath(mouth, FaceInk, style = mouthStroke)
                        }
                    },
            )
        }
    }
}
