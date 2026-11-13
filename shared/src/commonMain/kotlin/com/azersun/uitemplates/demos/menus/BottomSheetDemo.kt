package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ShareTargets = listOf(
    Triple(Glyph.Image, "Photos", MenuColors.Violet),
    Triple(Glyph.Link, "Copy link", MenuColors.Cyan),
    Triple(Glyph.Pin, "Pin", MenuColors.Pink),
    Triple(Glyph.Pencil, "Note", MenuColors.Yellow),
)

@Composable
fun BottomSheetDemo() {
    var open by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    var sheetHeight by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(open) {
        if (open) {
            drag.snapTo(0f)
            reveal.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 340f))
        } else {
            reveal.animateTo(0f, tween(260))
            drag.snapTo(0f)
        }
    }

    MenuStage(4, "Bottom Sheet", "Sheet springs up from the bottom over a dim. Icons pop in one by one.") {
        Box(Modifier.fillMaxSize().padding(14.dp).clip(RoundedCornerShape(20.dp))) {
            MockScreen(
                topBar = {
                    MockTopBar(
                        leading = { Box(Modifier.size(28.dp).clip(CircleShape).background(MenuColors.TileLight)) },
                        trailing = {
                            Text(
                                "Share",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(MenuColors.Blue, MenuColors.Cyan)))
                                    .clickable { open = true }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        },
                    )
                },
            )
            val visible = reveal.value > 0.001f
            if (visible) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val dragFade = 1f - (drag.value / sheetHeight).coerceIn(0f, 1f)
                            alpha = reveal.value.coerceIn(0f, 1f) * dragFade
                        }
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = false },
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { sheetHeight = it.height.toFloat() }
                    .graphicsLayer {
                        translationY = (1f - reveal.value) * sheetHeight + drag.value
                        alpha = if (visible) 1f else 0f
                    }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF201D42))
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) }
                        },
                        onDragStopped = { velocity ->
                            if (drag.value > sheetHeight * 0.3f || velocity > 1200f) {
                                open = false
                            } else {
                                drag.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                            }
                        },
                    )
                    // Extra bottom padding hides the spring's overshoot below the sheet.
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(50)).background(Color(0xFF4A4680)))
                Text(
                    "Share to",
                    color = MenuColors.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ShareTargets.forEachIndexed { i, (glyph, label, color) ->
                        PopIcon(index = i, open = open, glyph = glyph, label = label, color = color)
                    }
                }
                Spacer(Modifier.height(20.dp))
                repeat(2) { i ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MenuColors.Tile)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Bar((120 + i * 30).dp, 8.dp, MenuColors.Muted.copy(alpha = 0.55f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PopIcon(index: Int, open: Boolean, glyph: Glyph, label: String, color: Color) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (open) {
            delay(140L + index * 70L)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
        } else {
            pop.snapTo(0f)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                GlyphIcon(glyph, Color.White, size = 20.dp)
            }
        }
        Text(
            label,
            color = MenuColors.Muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = pop.value.coerceIn(0f, 1f) },
        )
    }
}
