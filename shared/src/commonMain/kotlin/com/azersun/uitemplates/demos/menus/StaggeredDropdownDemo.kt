package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DropdownItems = listOf(
    "Profile" to MenuColors.Violet,
    "Settings" to MenuColors.Blue,
    "Saved" to MenuColors.Yellow,
    "Log out" to MenuColors.Red,
)

@Composable
fun StaggeredDropdownDemo() {
    var open by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val unfold by animateFloatAsState(
        if (open) 1f else 0f,
        if (open) spring(dampingRatio = 0.72f, stiffness = 420f) else tween(180),
        label = "unfold",
    )
    val chevron by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "chevron")

    MenuStage(5, "Staggered Dropdown", "Chevron flips, the card unfolds down, items slide in one after another.") {
        Box(Modifier.fillMaxSize().padding(top = 60.dp), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.Start) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MenuColors.TileLight)
                        .border(1.dp, if (open) MenuColors.Violet.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { open = !open }
                        .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Menu", color = MenuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Canvas(Modifier.padding(start = 10.dp).size(14.dp).graphicsLayer { rotationZ = chevron }) {
                        val stroke = size.width * 0.14f
                        drawLine(MenuColors.Text, Offset(size.width * 0.15f, size.height * 0.35f), Offset(size.width * 0.5f, size.height * 0.7f), stroke, StrokeCap.Round)
                        drawLine(MenuColors.Text, Offset(size.width * 0.5f, size.height * 0.7f), Offset(size.width * 0.85f, size.height * 0.35f), stroke, StrokeCap.Round)
                    }
                }
                Column(
                    Modifier
                        .padding(top = 8.dp)
                        .width(200.dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleY = unfold.coerceAtLeast(0f)
                            alpha = unfold.coerceIn(0f, 1f)
                        }
                        .shadow(16.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF221F45))
                        .padding(6.dp),
                ) {
                    DropdownItems.forEachIndexed { i, (label, dot) ->
                        StaggerIn(index = i, visible = open, offsetX = 0f, offsetY = -10f, baseDelayMillis = 70, stepMillis = 55) {
                            val danger = i == DropdownItems.lastIndex
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected == i) MenuColors.Violet.copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable {
                                        selected = i
                                        scope.launch {
                                            delay(220)
                                            open = false
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                                Text(
                                    label,
                                    color = if (danger) MenuColors.Red else MenuColors.Text,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
