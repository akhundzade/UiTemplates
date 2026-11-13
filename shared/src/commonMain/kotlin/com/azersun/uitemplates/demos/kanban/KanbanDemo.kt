package com.azersun.uitemplates.demos.kanban

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.common.Confetti
import com.azersun.uitemplates.common.countUp
import com.azersun.uitemplates.common.rememberConfettiState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val Backdrop = Brush.verticalGradient(listOf(Color(0xFF15171C), Color(0xFF0B0C0F)))
private val BoardSurface = Color(0xFFEDEFF3)
private val ColumnSurface = Color(0xFFF7F8FA)
private val Ink = Color(0xFF1B1E26)
private val Muted = Color(0xFF7A808C)

private enum class Stage(val title: String, val accent: Color, val progress: Float) {
    New("Novo pedido", Color(0xFF8A93A6), 0.18f),
    Measuring("Medição", Color(0xFF7C6CF2), 0.42f),
    Quote("Orçamento", Color(0xFFF59E42), 0.7f),
    Closed("Fechado", Color(0xFF22B07D), 1f),
}

private enum class Lead(val label: String, val text: Color, val background: Color) {
    Instagram("Instagram", Color(0xFFD6336C), Color(0xFFFDE2EC)),
    WhatsApp("WhatsApp", Color(0xFF1F9D5C), Color(0xFFDCF5E8)),
    Site("Site", Color(0xFF5B6170), Color(0xFFECEEF2)),
    Referral("Indicação", Color(0xFF6B5BD6), Color(0xFFECE8FF)),
}

private data class Deal(
    val id: Int,
    val title: String,
    val client: String,
    val avatar: Color,
    val value: Int,
    val lead: Lead,
    val stage: Stage,
    val previousStage: Stage = stage,
) {
    val initials: String get() = client.split(" ").take(2).joinToString("") { it.take(1) }
}

private fun initialDeals() = listOf(
    Deal(1, "Cozinha Planejada", "Luiza Matos", Color(0xFFFF8FA3), 18_500, Lead.Instagram, Stage.New),
    Deal(2, "Closet Casal", "Ricardo Teixeira", Color(0xFF8FB3FF), 12_800, Lead.Site, Stage.New),
    Deal(3, "Home Office", "Aline Barbosa", Color(0xFF7FD6A8), 7_900, Lead.WhatsApp, Stage.New),
    Deal(4, "Dormitório Casal", "Thiago Fernandes", Color(0xFFB59CFF), 24_600, Lead.WhatsApp, Stage.Measuring),
    Deal(5, "Sala e Painel TV", "Priscila Moura", Color(0xFFFFB86B), 15_300, Lead.Instagram, Stage.Measuring),
    Deal(6, "Apto Completo", "Eduardo Campos", Color(0xFF2A2F3A), 68_000, Lead.Referral, Stage.Quote),
    Deal(7, "Cozinha Gourmet", "Renata Albuquerque", Color(0xFFFF9E7A), 42_000, Lead.WhatsApp, Stage.Quote),
    Deal(8, "Banheiro e Lavand.", "Sofia Ribeiro", Color(0xFF2A2F3A), 21_400, Lead.Referral, Stage.Closed),
)

private val incomingDeals = listOf(
    Deal(0, "Quarto Infantil", "Camila Duarte", Color(0xFFFFC26B), 14_200, Lead.Instagram, Stage.New),
    Deal(0, "Lavanderia", "Bruno Lima", Color(0xFF8FD3FF), 9_600, Lead.WhatsApp, Stage.New),
    Deal(0, "Área Gourmet", "Marina Costa", Color(0xFFB59CFF), 31_000, Lead.Referral, Stage.New),
)

/** Sales closed earlier this month, outside the board. */
private const val EARLIER_SALES = 68_000

private fun formatBrl(value: Int): String {
    val digits = value.toString().reversed().chunked(3).joinToString(".").reversed()
    return "R$ $digits"
}

@Stable
private class BoardState {
    val columns = Stage.entries.associateWith { mutableStateListOf<Deal>() }
    var nextId by mutableIntStateOf(100)
    var incomingIndex by mutableIntStateOf(0)
    var closedCount by mutableIntStateOf(0)

    fun reset() {
        columns.values.forEach { it.clear() }
        initialDeals().forEach { columns.getValue(it.stage).add(it) }
        incomingIndex = 0
        closedCount = 0
    }

    fun total(stage: Stage) = columns.getValue(stage).sumOf { it.value }
    val openValue get() = Stage.entries.filter { it != Stage.Closed }.sumOf { total(it) }
    val salesValue get() = EARLIER_SALES + total(Stage.Closed)
    val openCount get() = Stage.entries.filter { it != Stage.Closed }.sumOf { columns.getValue(it).size }

    fun addIncoming() {
        val template = incomingDeals[incomingIndex % incomingDeals.size]
        incomingIndex++
        columns.getValue(Stage.New).add(0, template.copy(id = nextId++))
    }
}

@Stable
private class DragState {
    var deal by mutableStateOf<Deal?>(null)
    var from by mutableStateOf(Stage.New)
    var pointer by mutableStateOf(Offset.Zero)
    var grab by mutableStateOf(Offset.Zero)
    var cardWidthPx by mutableIntStateOf(0)
    var hover by mutableStateOf<Stage?>(null)
    var landingId by mutableStateOf<Int?>(null)
    var scripted by mutableStateOf(false)
    var tiltTarget by mutableStateOf(0f)
    val lift = Animatable(0f)
    val tilt = Animatable(0f)
    val position = Animatable(Offset.Zero, Offset.VectorConverter)

    val active get() = deal != null
}

@Composable
fun KanbanDemo() {
    val board = remember { BoardState().apply { reset() } }
    val drag = remember { DragState() }
    val confetti = rememberConfettiState()
    val scope = rememberCoroutineScope()
    val boardScroll = rememberScrollState()
    val density = LocalDensity.current

    val cardBounds = remember { mutableStateMapOf<Int, Rect>() }
    val columnBounds = remember { mutableStateMapOf<Stage, Rect>() }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    var generation by remember { mutableIntStateOf(0) }
    var introStarted by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<Deal?>(null) }

    fun hoveredStage(pointer: Offset): Stage? =
        columnBounds.entries.firstOrNull { (_, r) -> pointer.x in r.left..r.right }?.key

    fun targetIndex(stage: Stage, pointer: Offset): Int =
        board.columns.getValue(stage).count { deal ->
            deal.id != drag.deal?.id && (cardBounds[deal.id]?.center?.y ?: Float.MAX_VALUE) < pointer.y
        }

    fun startDrag(deal: Deal, pointer: Offset) {
        val bounds = cardBounds[deal.id] ?: return
        drag.deal = deal
        drag.from = deal.stage
        drag.pointer = pointer
        drag.grab = pointer - bounds.topLeft
        drag.cardWidthPx = bounds.width.roundToInt()
        drag.hover = deal.stage
        scope.launch { drag.lift.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f)) }
    }

    fun celebrate(deal: Deal, center: Offset) {
        board.closedCount++
        confetti.burst(center - origin)
        toast = deal
        scope.launch {
            delay(2400)
            if (toast == deal) toast = null
        }
    }

    suspend fun drop() {
        val deal = drag.deal ?: return
        val from = drag.from
        val to = drag.hover ?: from
        val index = targetIndex(to, drag.pointer)
        val start = drag.pointer - drag.grab
        drag.position.snapTo(start)
        cardBounds.remove(deal.id)
        board.columns.getValue(from).removeAll { it.id == deal.id }
        val target = board.columns.getValue(to)
        target.add(index.coerceAtMost(target.size), deal.copy(stage = to, previousStage = from))
        drag.landingId = deal.id
        drag.deal = deal.copy(stage = to, previousStage = from)

        val slot = snapshotFlow { cardBounds[deal.id] }.filterNotNull().first()
        coroutineScope {
            launch { drag.position.animateTo(slot.topLeft, spring(dampingRatio = 0.78f, stiffness = 420f)) }
            launch { drag.lift.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 380f)) }
            if (to == Stage.Closed && from != Stage.Closed) {
                // Celebrate on touchdown, not after the spring's long settle tail.
                launch {
                    delay(160)
                    celebrate(deal, slot.center)
                }
            }
        }
        drag.landingId = null
        drag.deal = null
        drag.hover = null
        drag.tiltTarget = 0f
        drag.tilt.snapTo(0f)
    }

    suspend fun scriptedDrag(dealId: Int, to: Stage) {
        val deal = board.columns.values.flatten().firstOrNull { it.id == dealId } ?: return
        val gutter = with(density) { 12.dp.toPx() }
        // On narrow screens the source card starts off-screen; bring it in so the lift is visible.
        val source = cardBounds[dealId] ?: return
        if (source.right > viewport.right - gutter) {
            val shift = (source.left - viewport.left - gutter).roundToInt()
            boardScroll.animateScrollTo(boardScroll.value + shift, tween(700, easing = FastOutSlowInEasing))
            delay(120)
        }
        val bounds = cardBounds[dealId] ?: return
        drag.scripted = true
        startDrag(deal, bounds.center)
        delay(380)
        val startPointer = drag.pointer
        val column = columnBounds[to] ?: return
        val overflow = column.right - (viewport.right - with(density) { 12.dp.toPx() })
        val scroll = if (overflow > 0) scope.launch {
            boardScroll.animateScrollTo(boardScroll.value + overflow.roundToInt(), tween(1100, easing = FastOutSlowInEasing))
        } else null
        val travel = Animatable(0f)
        val arc = with(density) { 36.dp.toPx() }
        val headerOffset = with(density) { 44.dp.toPx() }
        var last = startPointer
        travel.animateTo(1f, tween(1250, easing = FastOutSlowInEasing)) {
            val col = columnBounds[to] ?: column
            val dest = Offset(col.center.x - bounds.width / 2 + drag.grab.x, col.top + headerOffset + drag.grab.y)
            val p = lerp(startPointer, dest, value) - Offset(0f, sin(value * PI.toFloat()) * arc)
            drag.tiltTarget = ((p.x - last.x) * 0.35f).coerceIn(-7f, 7f)
            last = p
            drag.pointer = p
            drag.hover = hoveredStage(p)
        }
        scroll?.join()
        delay(260)
        drop()
        drag.scripted = false
    }

    // Tilt follows horizontal speed, then settles back.
    LaunchedEffect(drag.active) {
        while (drag.active) {
            withFrameNanos { }
            drag.tilt.snapTo(drag.tilt.value + (drag.tiltTarget - drag.tilt.value) * 0.18f)
            drag.tiltTarget *= 0.86f
        }
    }

    // Auto-scroll when a finger drag reaches the board edge.
    LaunchedEffect(drag.active, drag.scripted) {
        if (!drag.active || drag.scripted) return@LaunchedEffect
        val edge = with(density) { 56.dp.toPx() }
        val speed = with(density) { 14.dp.toPx() }
        while (drag.active) {
            withFrameNanos { }
            val x = drag.pointer.x
            val delta = when {
                x < viewport.left + edge -> -speed * (1f - (x - viewport.left) / edge).coerceIn(0f, 1f)
                x > viewport.right - edge -> speed * (1f - (viewport.right - x) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (delta != 0f) {
                boardScroll.scrollBy(delta)
                drag.hover = hoveredStage(drag.pointer)
            }
        }
    }

    LaunchedEffect(generation) {
        introStarted = false
        delay(120)
        introStarted = true
        delay(1700)
        scriptedDrag(dealId = 6, to = Stage.Closed)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Backdrop)
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        key(generation) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BoardSurface),
            ) {
                BoardHeader(onNew = { board.addIncoming() })
                KpiRow(board, introStarted)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val columnWidth = maxOf(236.dp, (maxWidth - 24.dp - 30.dp) / 4)
                    Row(
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { viewport = it.boundsInRoot() }
                            .horizontalScroll(boardScroll)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Stage.entries.forEachIndexed { columnIndex, stage ->
                            StageColumn(
                                stage = stage,
                                deals = board.columns.getValue(stage),
                                total = board.total(stage),
                                hovered = drag.active && drag.hover == stage && drag.landingId == null,
                                draggingId = drag.deal?.id,
                                landingId = drag.landingId,
                                columnIndex = columnIndex,
                                introStarted = introStarted,
                                modifier = Modifier
                                    .width(columnWidth)
                                    .fillMaxHeight()
                                    .onGloballyPositioned { columnBounds[stage] = it.unclippedBoundsInRoot() },
                                onCardPositioned = { id, rect -> cardBounds[id] = rect },
                                onDragStart = { deal, pointer -> if (!drag.active) startDrag(deal, pointer) },
                                onDrag = { delta ->
                                    drag.pointer += delta
                                    drag.tiltTarget = (delta.x * 0.6f).coerceIn(-8f, 8f)
                                    drag.hover = hoveredStage(drag.pointer)
                                },
                                onDragEnd = { scope.launch { drop() } },
                            )
                        }
                    }
                }
            }
        }

        drag.deal?.let { deal ->
            val landing = drag.landingId != null
            val topLeft = if (landing) drag.position.value else drag.pointer - drag.grab
            val lift = drag.lift.value
            DealCard(
                deal = deal,
                elevated = lift,
                modifier = Modifier
                    .offset { (topLeft - origin).round() }
                    .width(with(density) { drag.cardWidthPx.toDp() })
                    .graphicsLayer {
                        val scale = 1f + 0.06f * lift
                        scaleX = scale
                        scaleY = scale
                        rotationZ = -1.5f * lift + drag.tilt.value
                    },
            )
        }

        Confetti(confetti, Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = toast != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 200.dp),
            enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f), initialScale = 0.8f) +
                fadeIn(tween(160)) + slideInVertically(spring(stiffness = 500f)) { -it / 3 },
            exit = scaleOut(tween(220), targetScale = 0.92f) + fadeOut(tween(220)),
        ) {
            ClosedToast(toast)
        }

        ReplayPill(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 24.dp, bottom = 24.dp),
            onClick = {
                if (!drag.active) {
                    confetti.clear()
                    toast = null
                    introStarted = false
                    board.reset()
                    cardBounds.clear()
                    scope.launch { boardScroll.animateScrollTo(0) }
                    generation++
                }
            },
        )
    }
}

private fun Offset.round() = IntOffset(x.roundToInt(), y.roundToInt())

// boundsInRoot() clips to visible ancestors, which collapses off-screen columns and cards.
private fun LayoutCoordinates.unclippedBoundsInRoot() = Rect(positionInRoot(), size.toSize())

@Composable
private fun BoardHeader(onNew: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo()
            Text(
                "Orçamentos planejados",
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE1E4EA), RoundedCornerShape(50))
                    .clickable(onClick = onNew)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(" Novo orçamento", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todos", "Parados", "Este mês").forEachIndexed { i, label ->
                val selected = i == 0
                Text(
                    label,
                    color = if (selected) Color.White else Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Ink else Color.White)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun Logo() {
    Canvas(Modifier.size(24.dp)) {
        val stroke = 3.2.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.68f)
            cubicTo(size.width * 0.25f, size.height * 0.1f, size.width * 0.42f, size.height * 0.1f, size.width * 0.5f, size.height * 0.5f)
            cubicTo(size.width * 0.58f, size.height * 0.9f, size.width * 0.75f, size.height * 0.9f, size.width * 0.92f, size.height * 0.32f)
        }
        drawPath(
            path,
            Brush.linearGradient(listOf(Color(0xFF1C7CFF), Color(0xFF22C3EE))),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun KpiRow(board: BoardState, introStarted: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val open = countUp(if (introStarted) board.openValue else 0, 1200)
        val conversion = countUp(if (introStarted) 28 + board.closedCount * 2 else 0, 1200)
        val sales = countUp(if (introStarted) board.salesValue else 0, 1200, delayMillis = 100)
        KpiCard("Orçamentos abertos", formatBrl(open), "+${board.openCount} projetos", Color(0xFF2A2F3A), Modifier.weight(1f))
        KpiCard("Conversão", "$conversion%", "+2,1 pp", Color(0xFF7C6CF2), Modifier.weight(0.8f))
        KpiCard("Vendas no mês", formatBrl(sales), "+18%", Color(0xFF22B07D), Modifier.weight(1f))
    }
}

@Composable
private fun KpiCard(label: String, value: String, badge: String, tint: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(tint))
            Text(
                label,
                color = Muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Text(
            value,
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            badge,
            color = Color(0xFF1F9D5C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFDCF5E8))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun StageColumn(
    stage: Stage,
    deals: List<Deal>,
    total: Int,
    hovered: Boolean,
    draggingId: Int?,
    landingId: Int?,
    columnIndex: Int,
    introStarted: Boolean,
    modifier: Modifier,
    onCardPositioned: (Int, Rect) -> Unit,
    onDragStart: (Deal, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val highlight by animateFloatAsState(if (hovered) 1f else 0f, spring(stiffness = 400f), label = "hover")
    val shownTotal = countUp(if (introStarted) total else 0, 900)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ColumnSurface)
            .background(stage.accent.copy(alpha = 0.07f * highlight))
            .border(
                width = (1 + 1.5f * highlight).dp,
                color = lerpColor(Color(0xFFE3E6EC), stage.accent, highlight),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(stage.accent))
            Text(stage.title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            Text(
                "${deals.count { it.id != draggingId || landingId != null }}",
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE9EBF0))
                    .padding(horizontal = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(formatBrl(shownTotal), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            itemsIndexed(deals, key = { _, deal -> deal.id }) { index, deal ->
                val collapsed = deal.id == draggingId && landingId == null
                val collapse by animateFloatAsState(
                    if (collapsed) 0f else 1f,
                    spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "collapse",
                )
                val entry = remember { Animatable(if (introStarted) 1f else 0f) }
                LaunchedEffect(introStarted) {
                    if (introStarted && entry.value < 1f) {
                        delay(columnIndex * 90L + index * 70L)
                        entry.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 260f))
                    }
                }
                Box(
                    Modifier
                        .animateItem(
                            fadeInSpec = if (landingId == deal.id) null else tween(260),
                            placementSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        )
                        .collapseHeight(collapse)
                        .padding(bottom = 8.dp)
                        .graphicsLayer {
                            alpha = if (deal.id == draggingId) 0f else entry.value
                            translationY = (1f - entry.value) * 24.dp.toPx()
                        },
                ) {
                    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }
                    DealCard(
                        deal = deal,
                        elevated = 0f,
                        modifier = Modifier
                            .onGloballyPositioned {
                                val bounds = it.unclippedBoundsInRoot()
                                rootTopLeft = bounds.topLeft
                                onCardPositioned(deal.id, bounds)
                            }
                            .pointerInput(deal.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { local -> onDragStart(deal, rootTopLeft + local) },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd,
                                    onDrag = { change, delta ->
                                        change.consume()
                                        onDrag(delta)
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

private fun Modifier.collapseHeight(fraction: Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, (placeable.height * fraction.coerceAtLeast(0f)).roundToInt()) { placeable.place(0, 0) }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

@Composable
private fun DealCard(deal: Deal, elevated: Float, modifier: Modifier = Modifier) {
    val progress = remember(deal.id) { Animatable(deal.previousStage.progress) }
    LaunchedEffect(deal.stage) {
        progress.animateTo(deal.stage.progress, tween(900, delayMillis = 250, easing = FastOutSlowInEasing))
    }
    val barColor by animateColorAsState(deal.stage.accent, tween(700, delayMillis = 250), label = "bar")
    Column(
        modifier
            .shadow((2 + 22 * elevated).dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deal.title,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(formatBrl(deal.value), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(deal.avatar),
                contentAlignment = Alignment.Center,
            ) {
                Text(deal.initials, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                deal.client,
                color = Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Text(
                deal.lead.label,
                color = deal.lead.text,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(deal.lead.background)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        Box(
            Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFEDEFF3)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun ClosedToast(deal: Deal?) {
    Row(
        Modifier
            .shadow(18.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF22B07D))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1FB386), Color(0xFF34D3A0))))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(12.dp)) {
                val s = 2.2.dp.toPx()
                drawLine(Color.White, Offset(0f, size.height * 0.55f), Offset(size.width * 0.38f, size.height * 0.92f), s, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width * 0.38f, size.height * 0.92f), Offset(size.width, size.height * 0.1f), s, StrokeCap.Round)
            }
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text("Projeto fechado!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (deal != null) {
                Text("${deal.title} · ${formatBrl(deal.value)}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ReplayPill(modifier: Modifier, onClick: () -> Unit) {
    Text(
        "↺  Replay",
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Ink.copy(alpha = 0.88f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
