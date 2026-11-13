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
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val CoinOff = Color(0xFFC9C6E0)
private val CoinOffMark = Color(0xFF7A76A3)

@Composable
fun FlipKnobDemo() {
    var on by remember { mutableStateOf(false) }
    val flip = animateFloatAsState(if (on) 1f else 0f, tween(560, easing = FastOutSlowInEasing), label = "flip")

    SwitchStage(
        1,
        "Flip Knob",
        "Knob flips like a coin as it slides: a gray \"O\" face turns into a glowing \"I\".",
        autoplay = { on = true },
    ) {
        Box(
            Modifier
                .size(TrackWidth, TrackHeight)
                .drawWithCache {
                    val radius = CornerRadius(size.height / 2)
                    onDrawBehind {
                        drawRoundRect(TrackOff, cornerRadius = radius)
                        drawRoundRect(TrackOn, cornerRadius = radius, alpha = flip.value)
                    }
                }
                .toggleable(on, interactionSource = null, indication = null, role = Role.Switch) { on = it },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset { IntOffset((KnobInset + KnobTravel * flip.value).toPx().roundToInt(), 0) }
                    .size(KnobSize)
                    .graphicsLayer {
                        val p = flip.value
                        rotationY = p * 180f
                        // Lifts a little mid-flip, like a tossed coin.
                        val lift = 1f + 0.12f * sin(PI.toFloat() * p)
                        scaleX = lift
                        scaleY = lift
                        cameraDistance = 12f * density
                    }
                    .drawWithCache {
                        val r = size.minDimension / 2
                        val glow = Brush.radialGradient(
                            listOf(MenuColors.Blue.copy(alpha = 0.55f), Color.Transparent),
                            center = size.center,
                            radius = r * 1.6f,
                        )
                        val mark = r * 0.36f
                        val shadowOffset = Offset(0f, 2.dp.toPx())
                        onDrawBehind {
                            // Past 90° the back face is showing; the "I" is symmetric, so no un-mirroring is needed.
                            if (flip.value < 0.5f) {
                                drawCircle(Color.Black, r, center + shadowOffset, alpha = 0.25f)
                                drawCircle(CoinOff)
                                drawCircle(CoinOffMark, mark * 0.9f, style = Stroke(r * 0.11f))
                            } else {
                                drawCircle(glow, r * 1.6f)
                                drawCircle(Color.White)
                                val top = Offset(center.x, center.y - mark)
                                val bottom = Offset(center.x, center.y + mark)
                                drawLine(MenuColors.Blue, top, bottom, r * 0.34f, StrokeCap.Round, alpha = 0.25f)
                                drawLine(MenuColors.Blue, top, bottom, r * 0.12f, StrokeCap.Round)
                            }
                        }
                    },
            )
        }
    }
}
