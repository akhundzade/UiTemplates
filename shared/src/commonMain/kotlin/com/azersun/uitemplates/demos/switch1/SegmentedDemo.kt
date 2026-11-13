package com.azersun.uitemplates.demos.switch1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val Options = listOf("Monthly", "Yearly")
private val PillBrush = Brush.verticalGradient(listOf(Color(0xFF6F8DFF), Color(0xFF4565F2)))
private val ControlShape = RoundedCornerShape(50)
private val ControlWidth = 280.dp
private val ControlHeight = 56.dp
private val ControlPadding = 5.dp

@Composable
fun SegmentedDemo() {
    var yearly by remember { mutableStateOf(false) }
    // Pill edges in segment widths: Monthly spans 0..1, Yearly 1..2.
    val start = remember { Animatable(0f) }
    val end = remember { Animatable(1f) }
    val tag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun select(toYearly: Boolean) {
        if (toYearly == yearly) return
        yearly = toYearly
        val shift = if (toYearly) 1f else 0f
        val lead = if (toYearly) end else start
        val trail = if (toYearly) start else end
        job?.cancel()
        job = scope.launch {
            // A stiff leading edge and a soft, bouncier trailing one stretch the pill mid-move.
            launch { lead.animateTo(if (toYearly) shift + 1f else shift, spring(dampingRatio = 0.8f, stiffness = 700f)) }
            launch { trail.animateTo(if (toYearly) shift else shift + 1f, spring(dampingRatio = 0.6f, stiffness = 230f)) }
            if (toYearly) {
                delay(160)
                tag.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
            } else {
                tag.animateTo(0f, tween(140))
            }
        }
    }

    CheckboxStage(
        6,
        "Segmented",
        "Pill slides on a spring, stretching mid-move. A \"−20%\" tag pops in.",
        autoplay = { select(true) },
    ) {
        Box {
            Box(
                Modifier
                    .width(ControlWidth)
                    .height(ControlHeight)
                    .drawWithCache {
                        val pad = ControlPadding.toPx()
                        val segment = (size.width - pad * 2) / 2
                        val pillHeight = size.height - pad * 2
                        val corner = CornerRadius(size.height / 2)
                        val pillCorner = CornerRadius(pillHeight / 2)
                        val border = Stroke(1.dp.toPx())
                        val shadowDrop = 2.dp.toPx()
                        onDrawBehind {
                            drawRoundRect(MenuColors.Tile, cornerRadius = corner)
                            drawRoundRect(MenuColors.CardBorder, cornerRadius = corner, style = border)
                            var x0 = (pad + start.value * segment).coerceAtLeast(pad)
                            val x1 = (pad + end.value * segment).coerceAtMost(size.width - pad)
                            var width = x1 - x0
                            val minWidth = segment * 0.75f
                            if (width < minWidth) {
                                x0 = (x0 + x1) / 2 - minWidth / 2
                                width = minWidth
                            }
                            val pill = Size(width, pillHeight)
                            drawRoundRect(Color.Black, Offset(x0, pad + shadowDrop), pill, pillCorner, alpha = 0.22f)
                            drawRoundRect(PillBrush, Offset(x0, pad), pill, pillCorner)
                        }
                    },
            ) {
                Row(Modifier.fillMaxSize().padding(ControlPadding)) {
                    Options.forEachIndexed { index, label ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(interactionSource = null, indication = null, role = Role.Tab) { select(index == 1) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = SwitchColors.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.graphicsLayer {
                                    val pillCenter = (start.value + end.value) / 2
                                    alpha = (1f - abs(pillCenter - (index + 0.5f))).coerceIn(0f, 1f)
                                },
                            )
                        }
                    }
                }
            }
            Text(
                "−20%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-14).dp)
                    .graphicsLayer {
                        val pop = tag.value
                        scaleX = pop
                        scaleY = pop
                        alpha = pop.coerceIn(0f, 1f)
                        rotationZ = 8f
                        transformOrigin = TransformOrigin(0f, 1f)
                    }
                    .background(SwitchColors.Pink, ControlShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
