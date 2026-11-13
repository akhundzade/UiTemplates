package com.azersun.uitemplates.demos.checkbox2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckColors
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.checkMark
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import com.azersun.uitemplates.demos.checkbox1.checkStroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BoxSide = 40.dp
private val BoxCorner = 12.dp

@Composable
fun FlipTickDemo() {
    var on by remember { mutableStateOf(false) }
    val flip = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var liftJob by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        on = !on
        val target = if (on) 180f else 0f
        // A quick second tap restarts the lift; otherwise the first tap's settle would drop it mid-flip.
        liftJob?.cancel()
        liftJob = scope.launch {
            lift.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
            delay(330)
            lift.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 380f))
        }
        scope.launch {
            delay(90)
            flip.animateTo(target, tween(560, easing = FastOutSlowInEasing))
        }
    }

    CheckboxStage(
        3,
        "Flip Tick",
        "The box lifts and flips over in 3D. Its back is filled, with a ✓.",
        autoplay = { toggle() },
    ) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { toggle() }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(BoxSide)
                    .graphicsLayer {
                        rotationY = flip.value
                        val scale = 1f + 0.1f * lift.value
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = 14.dp.toPx() * lift.value
                        shape = RoundedCornerShape(BoxCorner)
                        cameraDistance = 12f * density
                    }
                    .drawWithCache {
                        val side = size.minDimension
                        val corner = CornerRadius(BoxCorner.toPx())
                        val stroke = 2.dp.toPx()
                        val face = Brush.linearGradient(listOf(Color(0xFF6E9BFF), Color(0xFF3D5BFF)), Offset.Zero, Offset(side, side))
                        val check = Path().apply { checkMark(Offset.Zero, side) }
                        val checkStyle = checkStroke(side)
                        onDrawBehind {
                            if (flip.value < 90f) {
                                drawRoundRect(CheckColors.Track, cornerRadius = corner)
                                drawRoundRect(
                                    CheckColors.Outline,
                                    Offset(stroke / 2, stroke / 2),
                                    Size(side - stroke, side - stroke),
                                    CornerRadius(corner.x - stroke / 2),
                                    style = Stroke(stroke),
                                )
                            } else {
                                // Past 90° we are looking at the back, which the layer shows mirrored; un-mirror it.
                                scale(-1f, 1f) {
                                    drawRoundRect(face, cornerRadius = corner)
                                    drawPath(check, Color.White, style = checkStyle)
                                }
                            }
                        }
                    },
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Notify me", color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                AnimatedContent(
                    on,
                    transitionSpec = {
                        (slideInVertically(tween(280, delayMillis = 250)) { it / 2 } + fadeIn(tween(280, delayMillis = 250))) togetherWith
                            (slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(160)))
                    },
                    label = "status",
                ) { isOn ->
                    Text(
                        if (isOn) "On · back in stock" else "Off",
                        color = if (isOn) CheckColors.Cyan else CheckColors.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
