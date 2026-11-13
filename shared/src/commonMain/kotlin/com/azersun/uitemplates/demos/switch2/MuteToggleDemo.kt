package com.azersun.uitemplates.demos.switch2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SpeakerButton = 120.dp
private val RippleReach = 72.dp
private const val RippleWaves = 2
private val SpeakerDark = Color(0xFF2A2650)

@Composable
fun MuteToggleDemo() {
    var soundOn by remember { mutableStateOf(false) }
    // 0 = slash fully drawn, 1 = wiped off.
    val wipe = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val nearArc = remember { Animatable(0f) }
    val farArc = remember { Animatable(0f) }
    // Hidden at 1; runs 0 → 1 once per unmute.
    val ripple = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        soundOn = !soundOn
        job?.cancel()
        job = scope.launch {
            if (soundOn) {
                launch { wipe.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                launch {
                    delay(80)
                    fill.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(220)
                    nearArc.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(330)
                    farArc.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                }
                delay(480)
                ripple.snapTo(0f)
                ripple.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing))
            } else {
                ripple.snapTo(1f)
                launch { farArc.animateTo(0f, tween(160)) }
                launch { nearArc.animateTo(0f, tween(200)) }
                launch { fill.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
                delay(120)
                wipe.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            }
        }
    }

    SwitchStage(
        6,
        "Mute Toggle",
        "Slash wipes off the speaker, then sound waves ripple out from it.",
        autoplay = { if (!soundOn) toggle() },
    ) {
        Box(
            Modifier
                .size(SpeakerButton + RippleReach * 2)
                .drawWithCache {
                    val r = SpeakerButton.toPx() / 2
                    val u = r / 60f
                    val reach = RippleReach.toPx()
                    val c = size.center
                    val speaker = Path().apply {
                        moveTo(c.x - 26 * u, c.y - 10 * u)
                        lineTo(c.x - 14 * u, c.y - 10 * u)
                        lineTo(c.x + 2 * u, c.y - 24 * u)
                        lineTo(c.x + 2 * u, c.y + 24 * u)
                        lineTo(c.x - 14 * u, c.y + 10 * u)
                        lineTo(c.x - 26 * u, c.y + 10 * u)
                        close()
                    }
                    val speakerEdge = Stroke(4 * u, join = StrokeJoin.Round)
                    val arcStroke = Stroke(5 * u, cap = StrokeCap.Round)
                    val waveStroke = Stroke(2.dp.toPx())
                    val border = Stroke(1.dp.toPx())
                    val arcCenter = Offset(c.x + 2 * u, c.y)
                    val slashStart = Offset(c.x - 30 * u, c.y - 30 * u)
                    val slashEnd = Offset(c.x + 30 * u, c.y + 30 * u)
                    onDrawBehind {
                        val f = fill.value
                        val w = wipe.value
                        val rp = ripple.value
                        if (rp < 1f) {
                            for (i in 0 until RippleWaves) {
                                val t = ((rp - i * 0.22f) / 0.78f).coerceIn(0f, 1f)
                                if (t <= 0f || t >= 1f) continue
                                drawCircle(MenuColors.Blue, r + reach * t, c, alpha = 0.6f * (1f - t), style = waveStroke)
                            }
                        }
                        if (f > 0f) drawCircle(MenuColors.Blue, r * 1.18f, c, alpha = 0.18f * f)
                        drawCircle(SpeakerDark, r, c)
                        drawCircle(MenuColors.CardBorder, r - border.width / 2, c, alpha = 1f - f, style = border)
                        // The blue grows out from the centre of the button.
                        if (f > 0f) drawCircle(MenuColors.Blue, r * f, c)

                        drawPath(speaker, Color.White)
                        drawPath(speaker, Color.White, style = speakerEdge)
                        drawWaveArc(nearArc.value, 14 * u, arcCenter, arcStroke)
                        drawWaveArc(farArc.value, 26 * u, arcCenter, arcStroke)

                        if (w < 1f) {
                            val from = slashStart + (slashEnd - slashStart) * w
                            // A halo in the button colour cuts the slash out of the speaker behind it.
                            drawLine(lerp(SpeakerDark, MenuColors.Blue, f), from, slashEnd, 13 * u, StrokeCap.Round)
                            drawLine(MenuColors.Pink, from, slashEnd, 6 * u, StrokeCap.Round)
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(SpeakerButton)
                    .clip(CircleShape)
                    .semantics { contentDescription = "Sound" }
                    .toggleable(soundOn, interactionSource = null, indication = null, role = Role.Switch) { toggle() },
            )
        }
    }
}

/** A sound-wave arc to the right of [center] that draws out from its middle as [progress] goes 0 → 1. */
private fun DrawScope.drawWaveArc(progress: Float, radius: Float, center: Offset, stroke: Stroke) {
    if (progress <= 0f) return
    drawArc(
        Color.White,
        startAngle = -45f * progress,
        sweepAngle = 90f * progress,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = stroke,
    )
}
