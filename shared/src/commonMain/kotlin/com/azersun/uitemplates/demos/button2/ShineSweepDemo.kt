package com.azersun.uitemplates.demos.button2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val ProWidth = 236.dp
private val ProHeight = 60.dp
private val SparkleMargin = 34.dp
private val ShineBand = 46.dp
private const val ShineAngle = 20f
private const val SweepMillis = 850
private const val SweepPeriodMillis = 2500L
private const val TwinkleMillis = 2400
private val ProGradient = Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Pink, Orange))

/** Where each sparkle sits, as a fraction of the pill (outside 0..1 is beyond its edge). */
private class Sparkle(val x: Float, val y: Float, val radiusDp: Float, val phase: Float, val color: Color)

private val Sparkles = listOf(
    Sparkle(-0.05f, 0.12f, 7f, 0f, Color.White),
    Sparkle(0.2f, -0.28f, 5.5f, 0.32f, MenuColors.Yellow),
    Sparkle(0.6f, -0.3f, 8f, 0.66f, Color.White),
    Sparkle(1.05f, 0.24f, 6.5f, 0.14f, MenuColors.Yellow),
    Sparkle(0.86f, 1.28f, 7.5f, 0.5f, Color.White),
    Sparkle(0.36f, 1.3f, 5.5f, 0.84f, MenuColors.Yellow),
    Sparkle(-0.02f, 0.98f, 5f, 0.42f, Color.White),
)

/** A four-point star spanning -1..1 on both axes, scaled up where it is drawn. */
private val UnitStar = Path().apply {
    moveTo(0f, -1f)
    quadraticTo(0.14f, -0.14f, 1f, 0f)
    quadraticTo(0.14f, 0.14f, 0f, 1f)
    quadraticTo(-0.14f, 0.14f, -1f, 0f)
    quadraticTo(-0.14f, -0.14f, 0f, -1f)
    close()
}

@Composable
fun ShineSweepDemo() {
    val shine = remember { Animatable(0f) }
    var sweeps by remember { mutableIntStateOf(0) }
    val twinkle = rememberInfiniteTransition(label = "twinkle")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(TwinkleMillis, easing = LinearEasing)), label = "phase")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.5f, stiffness = 700f), label = "press")

    // The shine is the button's idle loop rather than a one-off autoplay, so it keeps going after a touch;
    // a tap restarts the loop with an immediate sweep.
    LaunchedEffect(sweeps) {
        if (sweeps == 0) delay(700)
        while (true) {
            shine.snapTo(0f)
            shine.animateTo(1f, tween(SweepMillis, easing = FastOutSlowInEasing))
            delay(SweepPeriodMillis - SweepMillis)
        }
    }

    ButtonStage(
        5,
        "Shine Sweep",
        "A diagonal shine sweeps across the button and sparkles twinkle.",
    ) {
        Box(
            Modifier
                .size(ProWidth + SparkleMargin * 2, ProHeight + SparkleMargin * 2)
                .drawWithCache {
                    val pill = Size(ProWidth.toPx(), ProHeight.toPx())
                    val origin = Offset((size.width - pill.width) / 2, (size.height - pill.height) / 2)
                    onDrawWithContent {
                        drawContent()
                        val t = twinkle.value
                        for (i in Sparkles.indices) {
                            val sparkle = Sparkles[i]
                            val local = (t + sparkle.phase) % 1f
                            val wave = sin(2f * PI.toFloat() * local)
                            if (wave <= 0f) continue
                            val r = sparkle.radiusDp.dp.toPx() * wave * wave
                            translate(origin.x + pill.width * sparkle.x, origin.y + pill.height * sparkle.y) {
                                rotate(local * 90f, Offset.Zero) {
                                    scale(r, r, Offset.Zero) { drawPath(UnitStar, sparkle.color) }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .size(ProWidth, ProHeight)
                    .graphicsLayer {
                        scaleX = press
                        scaleY = press
                    }
                    .shadow(18.dp, CircleShape, ambientColor = MenuColors.Pink, spotColor = MenuColors.Pink)
                    .clip(CircleShape)
                    .drawWithCache {
                        val band = ShineBand.toPx()
                        val bandBrush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                            startX = 0f,
                            endX = band,
                        )
                        val h = size.height
                        onDrawBehind {
                            drawRect(ProGradient)
                            val s = shine.value
                            if (s <= 0f || s >= 1f) return@onDrawBehind
                            val x = mix(-band - h, size.width + h, s)
                            rotate(ShineAngle, Offset(x + band / 2, h / 2)) {
                                translate(x, -h) { drawRect(bandBrush, size = Size(band, h * 3)) }
                            }
                        }
                    }
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClickLabel = "Upgrade to Pro",
                        role = Role.Button,
                        onClick = { sweeps++ },
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(15.dp)) {
                    val r = size.width / 2
                    translate(center.x, center.y) { scale(r, r, Offset.Zero) { drawPath(UnitStar, Color.White) } }
                }
                Spacer(Modifier.width(8.dp))
                Text("Upgrade to Pro", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
