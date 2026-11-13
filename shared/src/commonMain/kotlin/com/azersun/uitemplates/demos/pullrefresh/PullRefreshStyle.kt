package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import com.azersun.uitemplates.demos.menus.drawGlyph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

internal enum class RefreshPhase { Idle, Pulling, Refreshing, Done, Collapsing }

internal enum class FeedIcon { Heart, Live, Photo, Music, Chat, Follow }

internal class FeedPost(
    val name: String,
    val action: String,
    val icon: FeedIcon,
    val tint: Color,
    val avatar: List<Color>,
)

internal class FeedItem(val id: Int, val post: FeedPost)

private val InitialFeed = listOf(
    FeedPost("Kai Rowe", "liked your post", FeedIcon.Heart, Color(0xFFFB923C), listOf(Color(0xFFFB923C), Color(0xFFF43F5E))),
    FeedPost("Juno Vega", "started a live room", FeedIcon.Live, MenuColors.Cyan, listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))),
    FeedPost("Mira Hale", "posted 3 new photos", FeedIcon.Photo, Color(0xFF60A5FA), listOf(Color(0xFF60A5FA), MenuColors.Violet)),
    FeedPost("Leo Grant", "replied to your story", FeedIcon.Chat, MenuColors.Pink, listOf(Color(0xFFF472B6), Color(0xFFA855F7))),
)

/** Posts a refresh brings in, in order; repeated pulls cycle through them. */
private val NewPosts = listOf(
    FeedPost("Theo Park", "shared a new playlist", FeedIcon.Music, Color(0xFFA78BFA), listOf(Color(0xFFA78BFA), Color(0xFF6366F1))),
    FeedPost("Ada Quinn", "commented on your photo", FeedIcon.Chat, MenuColors.Yellow, listOf(Color(0xFFFFC53D), Color(0xFFF97316))),
    FeedPost("Rex Nolan", "started following you", FeedIcon.Follow, Color(0xFF34D399), listOf(Color(0xFF34D399), Color(0xFF0EA5E9))),
)

internal val RefreshThreshold = 80.dp
private val MaxPull = 150.dp
private const val RefreshMillis = 1200L
private const val CollapseMillis = 380
private const val FirstNewId = 100

internal val RowHeight = 60.dp
internal val RowPadding = 12.dp
internal val AvatarSize = 36.dp
private val RowGap = 10.dp

/**
 * Pull distance and refresh lifecycle for one feed. The pull is rubber-banded towards
 * [MaxPull]; releasing past [RefreshThreshold] holds the list there for [RefreshMillis],
 * then inserts the next post and collapses.
 */
@Stable
internal class PullRefreshState(
    private val scope: CoroutineScope,
    val thresholdPx: Float,
    private val maxPullPx: Float,
    private val releaseSpec: AnimationSpec<Float>,
    private val doneHoldMillis: Long,
) {
    var pull by mutableFloatStateOf(0f)
        private set
    var phase by mutableStateOf(RefreshPhase.Idle)
        private set
    val items = mutableStateListOf<FeedItem>()

    /** Id of the row the last refresh inserted, or -1 before any refresh. */
    var lastInsertedId by mutableIntStateOf(-1)
        private set

    private var insertedCount = 0
    private var job: Job? = null

    init {
        reset()
    }

    val progress: Float get() = pull / thresholdPx

    /** The post the next refresh will insert. */
    val upcoming: FeedPost get() = NewPosts[insertedCount % NewPosts.size]

    val canDrag: Boolean get() = phase == RefreshPhase.Idle || phase == RefreshPhase.Pulling

    /** From the release past the threshold until the list has collapsed again. */
    val isLoading: Boolean get() = !canDrag

    fun reset() {
        job?.cancel()
        pull = 0f
        phase = RefreshPhase.Idle
        items.clear()
        items.addAll(InitialFeed.mapIndexed { i, post -> FeedItem(i, post) })
        insertedCount = 0
        lastInsertedId = -1
    }

    /** Lets a finger catch the list while it springs back. */
    fun grab() {
        if (canDrag) job?.cancel()
    }

    fun dragBy(delta: Float) {
        if (!canDrag) return
        // Map back to the unbounded finger distance, add the delta, then rubber-band again.
        val ratio = (pull / maxPullPx).coerceIn(0f, 0.99f)
        val raw = (-maxPullPx * ln(1f - ratio) + delta).coerceAtLeast(0f)
        pull = maxPullPx * (1f - exp(-raw / maxPullPx))
        phase = if (pull > 0f) RefreshPhase.Pulling else RefreshPhase.Idle
    }

    fun release() {
        if (!canDrag) return
        if (pull <= 0f) {
            phase = RefreshPhase.Idle
            return
        }
        if (pull >= thresholdPx) refresh() else settleBack()
    }

    /** The autoplay's stand-in for a finger: pull past the threshold, pause, let go. */
    suspend fun autoPull() {
        phase = RefreshPhase.Pulling
        animate(pull, thresholdPx * 1.3f, animationSpec = tween(750, easing = FastOutSlowInEasing)) { v, _ -> pull = v }
        delay(120)
        release()
    }

    private fun settleBack() {
        job?.cancel()
        job = scope.launch {
            animate(pull, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)) { v, _ -> pull = v.coerceAtLeast(0f) }
            phase = RefreshPhase.Idle
        }
    }

    private fun refresh() {
        job?.cancel()
        job = scope.launch {
            phase = RefreshPhase.Refreshing
            val settle = launch { animate(pull, thresholdPx, animationSpec = releaseSpec) { v, _ -> pull = v } }
            delay(RefreshMillis)
            phase = RefreshPhase.Done
            delay(doneHoldMillis)
            settle.cancel()
            val id = FirstNewId + insertedCount
            items.add(0, FeedItem(id, upcoming))
            insertedCount++
            lastInsertedId = id
            phase = RefreshPhase.Collapsing
            animate(pull, 0f, animationSpec = tween(CollapseMillis, easing = FastOutSlowInEasing)) { v, _ -> pull = v }
            phase = RefreshPhase.Idle
        }
    }
}

/**
 * [releaseSpec] carries the list from where it was let go to the refresh height; [doneHoldMillis]
 * keeps it there after loading so an indicator can show its finished state before collapsing.
 */
@Composable
internal fun rememberPullRefreshState(
    releaseSpec: AnimationSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 380f),
    doneHoldMillis: Long = 0L,
): PullRefreshState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember {
        with(density) { PullRefreshState(scope, RefreshThreshold.toPx(), MaxPull.toPx(), releaseSpec, doneHoldMillis) }
    }
}

/**
 * The activity feed all six demos share. [indicator] fills the feed area and draws into the
 * strip the pull opens above the list (y from 0 to [PullRefreshState.pull]).
 */
@Composable
internal fun PullRefreshScene(
    number: Int,
    title: String,
    caption: String,
    state: PullRefreshState,
    rowAvatarHidden: (FeedItem) -> Boolean = { false },
    indicator: @Composable BoxScope.() -> Unit,
) {
    var run by remember { mutableIntStateOf(0) }
    var autoplay by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(run) {
        autoplay = coroutineContext[Job]
        delay(700)
        try {
            state.autoPull()
        } catch (e: CancellationException) {
            // A touch cut the autoplay short: let go where it is.
            state.release()
            throw e
        }
    }

    MenuStage(number, title, caption) {
        Column(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        autoplay?.cancel()
                    }
                }
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Activity", color = MenuColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Pull down to refresh", color = MenuColors.Muted, fontSize = 12.sp)
                }
                ReplayPill {
                    state.reset()
                    run++
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { state.dragBy(it) },
                        onDragStarted = { state.grab() },
                        onDragStopped = { state.release() },
                    ),
            ) {
                Column(Modifier.fillMaxWidth().offset { IntOffset(0, state.pull.roundToInt()) }) {
                    state.items.forEach { item ->
                        key(item.id) {
                            FeedRow(item, animateIn = item.id >= FirstNewId, avatarHidden = { rowAvatarHidden(item) })
                        }
                    }
                }
                Box(Modifier.matchParentSize(), content = indicator)
            }
        }
    }
}

@Composable
private fun FeedRow(item: FeedItem, animateIn: Boolean, avatarHidden: () -> Boolean) {
    val reveal = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (reveal.value < 1f) reveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, (placeable.height * reveal.value).roundToInt()) { placeable.place(0, 0) }
            }
            .padding(bottom = RowGap),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .graphicsLayer {
                    alpha = reveal.value
                    translationY = -(1f - reveal.value) * 16.dp.toPx()
                }
                .background(MenuColors.Tile, RoundedCornerShape(16.dp))
                .padding(horizontal = RowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(item.post, AvatarSize, Modifier.graphicsLayer { alpha = if (avatarHidden()) 0f else 1f })
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(item.post.name, color = MenuColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(item.post.action, color = MenuColors.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(32.dp).background(item.post.tint.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) { drawFeedIcon(item.post.icon, item.post.tint) }
            }
        }
    }
}

@Composable
internal fun Avatar(post: FeedPost, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(Brush.linearGradient(post.avatar), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(post.name.take(1), color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReplayPill(onClick: () -> Unit) {
    Text(
        "↺  Replay",
        color = MenuColors.Text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .clickable(onClickLabel = "Replay", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

private fun DrawScope.drawFeedIcon(icon: FeedIcon, color: Color) {
    val w = size.width
    val stroke = Stroke(width = w * 0.1f, cap = StrokeCap.Round)
    when (icon) {
        FeedIcon.Heart -> {
            val heart = Path().apply {
                moveTo(w * 0.5f, w * 0.86f)
                cubicTo(w * 0.1f, w * 0.6f, w * 0.02f, w * 0.3f, w * 0.26f, w * 0.18f)
                cubicTo(w * 0.4f, w * 0.12f, w * 0.5f, w * 0.24f, w * 0.5f, w * 0.32f)
                cubicTo(w * 0.5f, w * 0.24f, w * 0.6f, w * 0.12f, w * 0.74f, w * 0.18f)
                cubicTo(w * 0.98f, w * 0.3f, w * 0.9f, w * 0.6f, w * 0.5f, w * 0.86f)
                close()
            }
            drawPath(heart, color)
        }
        FeedIcon.Live -> {
            drawCircle(color, w * 0.11f)
            listOf(0.27f, 0.44f).forEach { r ->
                val topLeft = Offset(w * (0.5f - r), w * (0.5f - r))
                val arcSize = Size(w * r * 2, w * r * 2)
                drawArc(color, 140f, 80f, false, topLeft, arcSize, style = stroke)
                drawArc(color, -40f, 80f, false, topLeft, arcSize, style = stroke)
            }
        }
        FeedIcon.Photo -> drawGlyph(Glyph.Image, color)
        FeedIcon.Follow -> drawGlyph(Glyph.User, color)
        FeedIcon.Music -> {
            drawLine(color, Offset(w * 0.38f, w * 0.74f), Offset(w * 0.38f, w * 0.22f), stroke.width, StrokeCap.Round)
            drawLine(color, Offset(w * 0.8f, w * 0.64f), Offset(w * 0.8f, w * 0.12f), stroke.width, StrokeCap.Round)
            drawLine(color, Offset(w * 0.38f, w * 0.22f), Offset(w * 0.8f, w * 0.12f), w * 0.16f, StrokeCap.Round)
            drawCircle(color, w * 0.13f, Offset(w * 0.27f, w * 0.76f))
            drawCircle(color, w * 0.13f, Offset(w * 0.69f, w * 0.66f))
        }
        FeedIcon.Chat -> {
            drawRoundRect(color, Offset(w * 0.1f, w * 0.14f), Size(w * 0.8f, w * 0.58f), CornerRadius(w * 0.18f), style = stroke)
            val tail = Path().apply {
                moveTo(w * 0.3f, w * 0.72f)
                lineTo(w * 0.24f, w * 0.9f)
                lineTo(w * 0.48f, w * 0.72f)
            }
            drawPath(tail, color, style = stroke)
        }
    }
}
