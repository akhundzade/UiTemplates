package com.azersun.uitemplates.demos.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class DialogAction { Cancel, Delete }

internal val DialogPink = Color(0xFFFF5C9A)

private val DeleteBrush = Brush.horizontalGradient(listOf(Color(0xFFFF7AB6), Color(0xFFFF4D7A)))
private val DialogSurface = Color(0xFF221F45)
private val DialogBorder = Color(0xFF363170)
private val PdfBlue = Color(0xFF3D7BFF)
private val Scrim = Color(0xA6000000)

private val DialogShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(16.dp)

private const val AutoplayDelayMillis = 700L
private const val AutoplayHoldMillis = 1500L
private const val AutoplayGapMillis = 1100L

@Stable
internal class DialogState {
    var open by mutableStateOf(false)
        private set

    /** How the dialog was last closed, so a demo can pick its exit motion. */
    var lastAction by mutableStateOf(DialogAction.Cancel)
        private set

    var deleted by mutableStateOf(false)
        private set

    /** Set on the first real touch so the autoplay stops driving the dialog. */
    var interacted by mutableStateOf(false)

    fun show() {
        if (!deleted) open = true
    }

    fun close(action: DialogAction) {
        if (!open) return
        lastAction = action
        open = false
        if (action == DialogAction.Delete) deleted = true
    }

    fun restore() {
        deleted = false
    }
}

@Composable
internal fun rememberDialogState(): DialogState = remember { DialogState() }

/**
 * The shared "Files" screen with its delete dialog, plus the reel's autoplay (open, Cancel,
 * open, Delete) and a Replay pill once the file is gone. A demo keeps [shown] true for as
 * long as its enter or exit motion runs and animates the dialog through [dialogModifier];
 * [appModifier] reaches the screen behind and [decoration] draws over the dialog's bounds.
 */
@Composable
internal fun DialogScene(
    state: DialogState,
    number: Int,
    title: String,
    caption: String,
    shown: Boolean,
    dim: () -> Float,
    dialogModifier: Modifier,
    appModifier: Modifier = Modifier,
    decoration: DrawScope.() -> Unit = {},
) {
    var replays by remember { mutableIntStateOf(0) }
    LaunchedEffect(replays, state.interacted) {
        if (state.interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        state.show()
        delay(AutoplayHoldMillis)
        state.close(DialogAction.Cancel)
        delay(AutoplayGapMillis)
        state.show()
        delay(AutoplayHoldMillis)
        state.close(DialogAction.Delete)
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.interacted = true
                    }
                },
        ) {
            FilesScreen(state, appModifier.align(Alignment.Center))
            if (shown) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = dim().coerceIn(0f, 1f) }
                        .background(Scrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Cancel",
                        ) { state.close(DialogAction.Cancel) },
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .widthIn(max = 320.dp)
                        .drawWithContent {
                            drawContent()
                            decoration()
                        },
                ) {
                    DeleteDialog(state, dialogModifier)
                }
            }
            AnimatedVisibility(
                visible = state.deleted,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                enter = fadeIn(tween(300, delayMillis = 600)),
                exit = fadeOut(tween(150)),
            ) {
                ReplayPill {
                    state.restore()
                    state.interacted = false
                    replays++
                }
            }
        }
    }
}

@Composable
private fun FilesScreen(state: DialogState, modifier: Modifier) {
    Column(
        modifier
            .padding(horizontal = 20.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("Files", color = MenuColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Recent", color = MenuColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = !state.deleted,
                enter = expandVertically(tween(320)) + fadeIn(tween(260, delayMillis = 120)),
                // Waits for the dialog to start leaving, so the collapse is seen behind it.
                exit = fadeOut(tween(220, delayMillis = 120)) + shrinkVertically(tween(320, delayMillis = 220)),
            ) {
                FileRow()
            }
            AnimatedVisibility(
                visible = state.deleted,
                enter = fadeIn(tween(300, delayMillis = 480)),
                exit = fadeOut(tween(120)),
            ) {
                Text(
                    "No recent files",
                    color = MenuColors.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        DeleteFileButton(available = !state.deleted, enabled = !state.deleted && !state.open, onClick = state::show)
    }
}

@Composable
private fun FileRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(MenuColors.Tile)
            .border(1.dp, MenuColors.CardBorder, RowShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(PdfBlue, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("PDF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Trip notes.pdf", color = MenuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("2.4 MB · Edited today", color = MenuColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Canvas(Modifier.size(20.dp)) {
            listOf(0.25f, 0.5f, 0.75f).forEach { x ->
                drawCircle(MenuColors.Muted, size.width * 0.07f, Offset(size.width * x, size.height / 2))
            }
        }
    }
}

@Composable
private fun DeleteFileButton(available: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (available) 1f else 0.4f, tween(200), label = "available")
    Row(
        Modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .border(1.5.dp, DialogPink, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrashIcon(DialogPink, 16.dp)
        Spacer(Modifier.width(8.dp))
        Text("Delete file", color = DialogPink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeleteDialog(state: DialogState, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(28.dp, DialogShape)
            .background(DialogSurface, DialogShape)
            .border(1.dp, DialogBorder, DialogShape)
            .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).background(DialogPink.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            TrashIcon(DialogPink, 22.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text("Delete file?", color = MenuColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "“Trip notes.pdf” will be removed.",
            color = MenuColors.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogButton("Cancel", SolidColor(MenuColors.TileLight), MenuColors.Text, Modifier.weight(1f)) {
                state.close(DialogAction.Cancel)
            }
            DialogButton("Delete", DeleteBrush, Color.White, Modifier.weight(1f)) {
                state.close(DialogAction.Delete)
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, background: Brush, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrashIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = Stroke(w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(w * 0.16f, w * 0.26f), Offset(w * 0.84f, w * 0.26f), stroke.width, StrokeCap.Round)
        val handle = Path().apply {
            moveTo(w * 0.38f, w * 0.26f)
            lineTo(w * 0.41f, w * 0.13f)
            lineTo(w * 0.59f, w * 0.13f)
            lineTo(w * 0.62f, w * 0.26f)
        }
        drawPath(handle, color, style = stroke)
        val body = Path().apply {
            moveTo(w * 0.25f, w * 0.26f)
            lineTo(w * 0.3f, w * 0.88f)
            lineTo(w * 0.7f, w * 0.88f)
            lineTo(w * 0.75f, w * 0.26f)
        }
        drawPath(body, color, style = stroke)
        drawLine(color, Offset(w * 0.43f, w * 0.42f), Offset(w * 0.44f, w * 0.72f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(w * 0.57f, w * 0.42f), Offset(w * 0.56f, w * 0.72f), stroke.width, StrokeCap.Round)
    }
}

@Composable
private fun ReplayPill(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClickLabel = "Replay", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(MenuColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = MenuColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun DrawScope.drawReplayGlyph(color: Color) {
    val stroke = size.width * 0.14f
    val radius = size.minDimension / 2 - stroke
    drawArc(
        color,
        startAngle = 40f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    val angle = 330f * PI.toFloat() / 180f
    val normal = Offset(cos(angle), sin(angle))
    val tangent = Offset(-normal.y, normal.x)
    val end = center + normal * radius
    val head = Path().apply {
        moveTo(end.x + tangent.x * stroke * 1.6f, end.y + tangent.y * stroke * 1.6f)
        lineTo(end.x + normal.x * stroke * 1.6f, end.y + normal.y * stroke * 1.6f)
        lineTo(end.x - normal.x * stroke * 1.6f, end.y - normal.y * stroke * 1.6f)
        close()
    }
    drawPath(head, color)
}
