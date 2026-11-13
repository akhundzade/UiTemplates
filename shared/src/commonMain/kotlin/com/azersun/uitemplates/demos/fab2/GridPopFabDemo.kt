package com.azersun.uitemplates.demos.fab2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.drawGlyph
import kotlinx.coroutines.delay

private class GridAction(val label: String, val color: Color, val icon: DrawScope.(Color) -> Unit)

/** In reading order: two columns, three rows, the last row sitting just above the button. */
private val GridActions = listOf(
    GridAction("Note", MenuColors.Blue) { drawGlyph(Glyph.Pencil, it) },
    GridAction("Photo", MenuColors.Pink) { drawGlyph(Glyph.Image, it) },
    GridAction("Voice", MenuColors.Violet) { drawGlyph(Glyph.Mic, it) },
    GridAction("List", Teal) { drawListGlyph(it) },
    GridAction("Pin", MenuColors.Yellow) { drawGlyph(Glyph.Pin, it) },
    GridAction("Remind", MenuColors.Cyan) { drawGlyph(Glyph.Bell, it) },
)

private const val Columns = 2
private val TileWidth = 84.dp
private val TileHeight = 74.dp
private val TileGap = 10.dp
private val GridLift = 14.dp
private const val PopStepMillis = 55L
private const val FoldStepMillis = 35L
private const val AutoplayOpenMillis = 1800L

@Composable
fun GridPopFabDemo() {
    val autoplay = rememberAutoplay()
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 45f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "turn")
    val scrimState = animateFloatAsState(if (open) 1f else 0f, tween(260), label = "scrim")
    val scrimShown by remember { derivedStateOf { scrimState.value > 0f } }

    AutoplayEffect(autoplay) {
        open = true
        delay(AutoplayOpenMillis)
        open = false
    }

    NotesScene(
        3,
        "Grid Pop",
        "Tap +: it turns into × and six action tiles pop out of it one by one, then fold back in reverse.",
        autoplay,
    ) {
        Column(Modifier.fillMaxSize()) {
            NotesHeader()
            NoteList()
        }
        if (scrimShown) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimState.value }
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = false },
            )
        }
        val rows = GridActions.size / Columns
        GridActions.forEachIndexed { i, action ->
            val col = i % Columns
            val rowFromBottom = rows - 1 - i / Columns
            GridTile(
                action = action,
                // Nearest the button pops first, so the grid seems to spill out of it.
                order = GridActions.lastIndex - i,
                open = open,
                col = col,
                rowFromBottom = rowFromBottom,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = -(FabInset + (TileWidth + TileGap) * (Columns - 1 - col)),
                        y = -(FabInset + FabSize + GridLift + (TileHeight + TileGap) * rowFromBottom),
                    ),
            ) { open = false }
        }
        Fab(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(FabInset),
            onClick = { open = !open },
            onClickLabel = if (open) "Close actions" else "Open actions",
        ) {
            Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = turn }) { drawPlus(Color.White) }
        }
    }
}

@Composable
private fun GridTile(
    action: GridAction,
    order: Int,
    open: Boolean,
    col: Int,
    rowFromBottom: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (open) {
            delay(order * PopStepMillis)
            pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 420f))
        } else {
            delay((GridActions.lastIndex - order) * FoldStepMillis)
            pop.animateTo(0f, tween(170, easing = FastOutLinearInEasing))
        }
    }
    // From the tile's centre to the button's centre: where the tile starts and folds back to.
    val toFabX = (TileWidth + TileGap) * (Columns - 1 - col) + TileWidth / 2 - FabSize / 2
    val toFabY = FabSize + GridLift + (TileHeight + TileGap) * rowFromBottom + TileHeight / 2 - FabSize / 2
    Column(
        modifier
            .size(TileWidth, TileHeight)
            .graphicsLayer {
                val p = pop.value
                translationX = (1f - p) * toFabX.toPx()
                translationY = (1f - p) * toFabY.toPx()
                scaleX = p.coerceAtLeast(0f)
                scaleY = p.coerceAtLeast(0f)
                alpha = p.coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MenuColors.TileLight)
            .clickable(enabled = open, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(action.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(17.dp)) { action.icon(this, action.color) }
        }
        Text(
            action.label,
            color = MenuColors.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun DrawScope.drawListGlyph(color: Color) {
    val w = size.width
    val stroke = w * 0.1f
    repeat(3) { i ->
        val y = w * (0.24f + i * 0.26f)
        drawCircle(color, w * 0.06f, Offset(w * 0.16f, y))
        drawLine(color, Offset(w * 0.36f, y), Offset(w * 0.88f, y), stroke, StrokeCap.Round)
    }
}
