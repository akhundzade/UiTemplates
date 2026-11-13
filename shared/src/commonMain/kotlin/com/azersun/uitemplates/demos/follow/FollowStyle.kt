package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlin.math.floor

internal const val BaseFollowers = 1_284

internal val ButtonHeight = 50.dp
internal val AvatarSize = 52.dp
internal val Pill = RoundedCornerShape(50)
internal val FollowGradient = Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Blue))
internal val FollowingSurface = Color(0xFF14122B)
internal val FollowingBorder = Color(0xFF3D3A72)

private const val AutoplayDelayMillis = 700L
private const val AutoplayUnfollowDelayMillis = 1500L

// Tabular figures keep "1,284" → "1,285" from shifting sideways as a digit rolls.
private val CountStyle = TextStyle(
    color = MenuColors.Text,
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

@Stable
internal class FollowState {
    /** Counts every tap, so demos that always turn the same way (coin, blinds) have a target. */
    var toggles by mutableIntStateOf(0)
        private set

    /** Set on the first real touch so the autoplay stops. */
    var interacted by mutableStateOf(false)

    /** In root coordinates, for demos that fly something between the button and the count. */
    var buttonBounds by mutableStateOf(Rect.Zero)
    var countCenter by mutableStateOf(Offset.Zero)

    val following: Boolean get() = toggles % 2 == 1
    val followers: Int get() = BaseFollowers + if (following) 1 else 0

    fun toggle() {
        toggles++
    }
}

/**
 * The profile card all six demos share, with the reel's autoplay (follow, then unfollow 1.5 s
 * later). [button] fills the full-width, clickable button slot; [overlay] is laid over the card.
 */
@Composable
internal fun FollowScene(
    state: FollowState,
    number: Int,
    title: String,
    caption: String,
    followers: Int = state.followers,
    countModifier: Modifier = Modifier,
    avatar: @Composable () -> Unit = { Avatar() },
    overlay: @Composable BoxScope.() -> Unit = {},
    button: @Composable BoxScope.() -> Unit,
) {
    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        state.toggle()
        delay(AutoplayUnfollowDelayMillis)
        state.toggle()
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.interacted = true
                    }
                },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MenuColors.Tile, RoundedCornerShape(24.dp))
                    .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    avatar()
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("Nova Lane", color = MenuColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("@nova.makes", color = MenuColors.Muted, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                    }
                    Column(
                        countModifier.onGloballyPositioned {
                            state.countCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        },
                        horizontalAlignment = Alignment.End,
                    ) {
                        RollingCount(followers)
                        Text("followers", color = MenuColors.Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ButtonHeight)
                        .onGloballyPositioned {
                            state.buttonBounds = Rect(it.positionInRoot(), it.size.toSize())
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = if (state.following) "Unfollow" else "Follow",
                        ) { state.toggle() },
                    content = button,
                )
            }
            overlay()
        }
    }
}

private fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())

@Composable
internal fun Avatar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MenuColors.Violet, MenuColors.Pink))),
        contentAlignment = Alignment.Center,
    ) {
        Text("NL", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** The follower count; only the digits that change roll, up for a gain and down for a loss. */
@Composable
private fun RollingCount(value: Int) {
    // A plain holder, not state: the direction only has to be right when a new value arrives,
    // which is exactly when AnimatedContent asks for its transition.
    val tracker = remember { CountTracker(value) }
    if (value != tracker.previous) {
        tracker.up = value > tracker.previous
        tracker.previous = value
    }
    val text = formatCount(value)
    Row {
        text.forEachIndexed { i, char ->
            key(i) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val sign = if (tracker.up) 1 else -1
                        (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { it * sign } + fadeIn(tween(160)))
                            .togetherWith(slideOutVertically(tween(200)) { -it * sign } + fadeOut(tween(160)))
                    },
                    label = "digit",
                ) { Text(it.toString(), style = CountStyle) }
            }
        }
    }
}

private class CountTracker(var previous: Int) {
    var up = true
}

private fun formatCount(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()

/** One side of the button, drawn as a full pill: the gradient "Follow" or the dark "Following". */
@Composable
internal fun FollowFace(following: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .followBackground { if (following) 1f else 0f },
        contentAlignment = Alignment.Center,
    ) {
        FollowLabel(following)
    }
}

/** The pill's surface at [following] 0 (gradient) to 1 (dark outlined); in between they cross-fade. */
internal fun Modifier.followBackground(following: () -> Float): Modifier = drawBehind {
    val f = following().coerceIn(0f, 1f)
    val radius = CornerRadius(size.height / 2)
    if (f < 1f) drawRoundRect(FollowGradient, cornerRadius = radius, alpha = 1f - f)
    if (f > 0f) {
        val stroke = 1.dp.toPx()
        drawRoundRect(FollowingSurface, cornerRadius = radius, alpha = f)
        drawRoundRect(
            FollowingBorder,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(size.height / 2 - stroke / 2),
            style = Stroke(stroke),
            alpha = f,
        )
    }
}

@Composable
internal fun FollowLabel(following: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FollowIcon(following)
        Spacer(Modifier.width(8.dp))
        Text(labelOf(following), color = labelColor(following), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

internal fun labelOf(following: Boolean): String = if (following) "Following" else "Follow"

internal fun labelColor(following: Boolean): Color = if (following) MenuColors.Text else Color.White

/** "+" for Follow, "✓" for Following, drawn so both platforms render the same stroke. */
@Composable
internal fun FollowIcon(following: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp)) { drawFollowIcon(following, labelColor(following)) }
}

private fun DrawScope.drawFollowIcon(following: Boolean, color: Color) {
    val w = size.width
    val stroke = w * 0.17f
    if (following) {
        drawLine(color, Offset(w * 0.08f, w * 0.52f), Offset(w * 0.38f, w * 0.82f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, w * 0.82f), Offset(w * 0.94f, w * 0.2f), stroke, StrokeCap.Round)
    } else {
        drawLine(color, Offset(w * 0.5f, w * 0.08f), Offset(w * 0.5f, w * 0.92f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.08f, w * 0.5f), Offset(w * 0.92f, w * 0.5f), stroke, StrokeCap.Round)
    }
}

/** A cross-fading button for demos whose effect happens elsewhere on the card. */
@Composable
internal fun FadingFollowButton(following: Boolean) {
    val fill by animateFloatAsState(if (following) 1f else 0f, tween(280), label = "fill")
    Box(Modifier.fillMaxSize().followBackground { fill }, contentAlignment = Alignment.Center) {
        Crossfade(following, animationSpec = tween(220), label = "label") { FollowLabel(it) }
    }
}

/** 0 or 1 for the face showing at [angle] degrees, given that every half turn swaps the face. */
internal fun faceAt(angle: Float): Int = (halfTurns(angle) % 2 + 2) % 2

/** [angle] folded into -90..90, so the face showing is drawn the right way up. */
internal fun tiltAt(angle: Float): Float = angle - halfTurns(angle) * 180f

private fun halfTurns(angle: Float): Int = floor((angle + 90f) / 180f).toInt()
