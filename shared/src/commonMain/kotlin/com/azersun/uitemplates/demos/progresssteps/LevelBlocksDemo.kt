package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

private val BaseHeights = listOf(34.dp, 56.dp, 78.dp, 100.dp)
private val Raise = 14.dp
private val BlockGap = 12.dp
private val BallRadius = 11.dp
private val HopPeak = 34.dp
private const val HopMillis = 560

private val Ball = MenuColors.Yellow
private val Eye = Color(0xFF1A1833)

@Composable
fun LevelBlocksDemo() {
    StepsScene(
        number = 5,
        title = "Level Blocks",
        caption = "A little ball hops up to the next block's top as that block springs taller; the blocks behind it stay lit.",
    ) { state ->
        val raises = remember { List(StepNames.size) { Animatable(if (it == StartStep) 1f else 0f) } }
        val lit = remember { List(StepNames.size) { Animatable(if (it <= StartStep) 1f else 0f) } }
        val hop = remember { Animatable(1f) }
        val squash = remember { Animatable(0f) }
        // Columns the ball jumps between; a float start lets a hop begin from mid-air.
        var fromColumn by remember { mutableFloatStateOf(StartStep.toFloat()) }
        var toColumn by remember { mutableIntStateOf(StartStep) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(state.step) {
            if (state.step == toColumn && hop.value >= 1f) return@LaunchedEffect
            fromColumn += (toColumn - fromColumn) * hop.value
            toColumn = state.step
            raises.forEachIndexed { i, raise ->
                scope.launch { raise.animateTo(if (i == state.step) 1f else 0f, spring(dampingRatio = 0.42f, stiffness = 240f)) }
            }
            lit.forEachIndexed { i, light ->
                scope.launch { light.animateTo(if (i <= state.step) 1f else 0f, tween(320)) }
            }
            squash.snapTo(0f)
            hop.snapTo(0f)
            hop.animateTo(1f, tween(HopMillis, easing = LinearEasing))
            fromColumn = toColumn.toFloat()
            squash.snapTo(1f)
            squash.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 520f))
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .drawWithCache {
                    val slot = size.width / StepNames.size
                    val gap = BlockGap.toPx()
                    val floor = size.height - 2.dp.toPx()
                    val corner = CornerRadius(8.dp.toPx())
                    val glow = 4.dp.toPx()
                    val ballRadius = BallRadius.toPx()
                    fun top(i: Int): Float = floor - BaseHeights[i].toPx() - Raise.toPx() * raises[i].value
                    onDrawBehind {
                        drawLine(StepColors.Track, Offset(0f, floor + 1.dp.toPx()), Offset(size.width, floor + 1.dp.toPx()), 2.dp.toPx())
                        for (i in StepNames.indices) {
                            val raise = raises[i].value
                            val y = top(i)
                            val left = slot * i + gap / 2
                            val width = slot - gap
                            val color = lerp(lerp(StepColors.Track, StepColors.Blue, lit[i].value), StepColors.Cyan, 0.4f * raise.coerceIn(0f, 1f))
                            if (raise > 0f) {
                                drawRoundRect(
                                    StepColors.Cyan,
                                    Offset(left - glow, y - glow),
                                    Size(width + 2 * glow, floor - y + glow),
                                    CornerRadius(corner.x + glow),
                                    alpha = 0.16f * raise.coerceIn(0f, 1f),
                                )
                            }
                            drawRoundRect(color, Offset(left, y), Size(width, floor - y), corner)
                            drawRoundRect(Color.White, Offset(left, y), Size(width, 5.dp.toPx()), corner, alpha = 0.12f + 0.12f * lit[i].value)
                        }

                        val t = hop.value
                        val column = fromColumn + (toColumn - fromColumn) * t
                        // After an interrupted hop the ball starts between two columns.
                        val low = floor(fromColumn).toInt()
                        val high = ceil(fromColumn).toInt()
                        val fromTop = top(low) + (top(high) - top(low)) * (fromColumn - low)
                        val toTop = top(toColumn)
                        val x = slot * (column + 0.5f)
                        val bottom = fromTop + (toTop - fromTop) * t - HopPeak.toPx() * 4f * t * (1f - t)
                        val y = bottom - ballRadius
                        val s = squash.value
                        val look = if (toColumn >= fromColumn) 1f else -1f
                        withTransform({ scale(1f + 0.22f * s, 1f - 0.22f * s, Offset(x, bottom)) }) {
                            drawCircle(Ball, ballRadius, Offset(x, y))
                            drawCircle(Color.White, ballRadius * 0.28f, Offset(x - ballRadius * 0.4f, y - ballRadius * 0.45f), alpha = 0.45f)
                            for (side in -1..1 step 2) {
                                val eye = Offset(x + side * ballRadius * 0.36f + look * ballRadius * 0.1f, y - ballRadius * 0.05f)
                                drawCircle(Color.White, ballRadius * 0.27f, eye)
                                drawCircle(Eye, ballRadius * 0.14f, eye + Offset(look * ballRadius * 0.09f, 0f))
                            }
                        }
                    }
                },
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            StepNames.indices.forEach { i ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { StepLabel(i, state.step) }
            }
        }
    }
}
