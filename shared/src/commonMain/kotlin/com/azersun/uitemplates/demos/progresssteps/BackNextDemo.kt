package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.abs

@Composable
fun BackNextDemo() {
    StepsScene(
        number = 3,
        title = "Back & Next",
        caption = "Next fills the line to the next dot and slides the title in from the right; Back reverses both.",
    ) { state ->
        val fill = remember { Animatable(StartStep.toFloat()) }
        LaunchedEffect(state.step) {
            fill.animateTo(state.step.toFloat(), spring(dampingRatio = 0.8f, stiffness = 170f))
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .drawWithCache {
                    val dot = 7.dp.toPx()
                    // Room for the current dot's ring at both ends.
                    val left = dot + 6.dp.toPx()
                    val right = size.width - left
                    val y = size.height / 2
                    val brush = Brush.horizontalGradient(listOf(StepColors.Blue, StepColors.Cyan), left, right)
                    val line = 3.dp.toPx()
                    val ring = Stroke(2.dp.toPx())
                    onDrawBehind {
                        val p = fill.value
                        val step = state.step
                        val span = right - left
                        drawLine(StepColors.Track, Offset(left, y), Offset(right, y), line, StrokeCap.Round)
                        drawLine(brush, Offset(left, y), Offset(left + span * p / LastStep, y), line, StrokeCap.Round)
                        for (i in 0..LastStep) {
                            val c = Offset(left + span * i / LastStep, y)
                            if (p >= i - 0.02f) {
                                drawCircle(StepColors.at(i.toFloat()), dot, c)
                            } else {
                                drawCircle(StepColors.Track, dot, c)
                                drawCircle(MenuColors.Tile, dot * 0.45f, c)
                            }
                            if (i == step) {
                                val arrival = (1f - abs(p - i) * 2f).coerceIn(0f, 1f)
                                drawCircle(StepColors.at(i.toFloat()), dot + 4.dp.toPx(), c, alpha = arrival * 0.6f, style = ring)
                            }
                        }
                    }
                },
        )
        Spacer(Modifier.height(28.dp))
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                val d = state.direction
                (slideInHorizontally(tween(380)) { it * d } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(380)) { -it * d } + fadeOut(tween(240)))
            },
            label = "title",
        ) { step ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(StepNames[step], color = MenuColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Step ${step + 1} of ${StepNames.size}", color = MenuColors.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
