package com.azersun.uitemplates.demos.receipt

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class Phase { Idle, Printing, Printed, Tearing, Paid }

private const val TotalCents = 15590
private const val AutoplayDelayMillis = 700L
private const val AutopayDelayMillis = 1500L

private val FeedStep = 12.dp
private val PaperPeek = 6.dp
private val PullLimit = 24.dp
private val PullToTear = 64.dp
private val DetachLift = 14.dp
private val FlyDistance = 1400.dp
private val FlyDrift = 46.dp
private val CurlHeight = 18.dp
private val BayTopGap = 6.dp
private val Pill = RoundedCornerShape(50)

/** Accelerates away like a flick that keeps going. */
private val FlyEasing = CubicBezierEasing(0.45f, 0f, 0.85f, 0.45f)

@Stable
private class CheckoutState(private val scope: CoroutineScope, density: Density) {
    private val stepPx = with(density) { FeedStep.toPx() }
    private val peekPx = with(density) { PaperPeek.toPx() }
    private val tailPx = with(density) { PaperTail.toPx() }
    private val pullLimitPx = with(density) { PullLimit.toPx() }
    private val tearPullPx = with(density) { PullToTear.toPx() }
    private val detachPx = with(density) { DetachLift.toPx() }
    private val flyPx = with(density) { FlyDistance.toPx() }
    private val driftPx = with(density) { FlyDrift.toPx() }

    var phase by mutableStateOf(Phase.Idle)
        private set

    /** The receipt has been ripped off: its bottom edge is torn and a stub of the next one stays in the slot. */
    var torn by mutableStateOf(false)
        private set
    var autoplay by mutableStateOf(true)
    var pressTick by mutableIntStateOf(0)
        private set

    /** Unscaled height of the receipt, blank tail included. */
    var paperHeight by mutableFloatStateOf(0f)

    /** Unscaled paper pushed out above the slot. */
    val fed = Animatable(peekPx)

    /** 0 → 1 over each feed step; drives the motor vibration. */
    val buzz = Animatable(0f)
    val lift = Animatable(0f)
    val tilt = Animatable(0f)
    val drift = Animatable(0f)
    val fade = Animatable(0f)
    val cents = Animatable(TotalCents.toFloat())
    val paid = Animatable(0f)

    val canPay: Boolean get() = phase != Phase.Tearing && phase != Phase.Paid
    val printedHeight: Float get() = (paperHeight - tailPx).coerceAtLeast(0f)

    private var job: Job? = null
    private var pull = 0f

    fun start() {
        job?.cancel()
        job = scope.launch {
            reset()
            delay(AutoplayDelayMillis)
            print(stepPx, advanceMillis = 45, pauseMillis = 60)
            phase = Phase.Printed
            delay(AutopayDelayMillis)
            if (autoplay) {
                pressTick++
                tear()
            }
        }
    }

    fun pay(fromButton: Boolean) {
        if (!canPay) return
        if (fromButton) pressTick++
        job?.cancel()
        job = scope.launch {
            if (fed.value < printedHeight || paperHeight == 0f) {
                print(stepPx * 2, advanceMillis = 28, pauseMillis = 18)
            }
            tear()
        }
    }

    fun drag(delta: Float) {
        if (phase != Phase.Printed) return
        pull = (pull + delta).coerceAtMost(0f)
        val rubber = -pullLimitPx * (1f - exp(pull / pullLimitPx))
        scope.launch { lift.snapTo(rubber) }
    }

    fun release(velocity: Float) {
        if (phase != Phase.Printed) return
        val tearOff = pull < -tearPullPx || velocity < -900f
        pull = 0f
        if (tearOff) {
            pay(fromButton = false)
        } else {
            scope.launch { lift.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 600f)) }
        }
    }

    private suspend fun reset() {
        phase = Phase.Idle
        torn = false
        pull = 0f
        fed.snapTo(peekPx)
        buzz.snapTo(0f)
        lift.snapTo(0f)
        tilt.snapTo(0f)
        drift.snapTo(0f)
        fade.snapTo(0f)
        cents.snapTo(TotalCents.toFloat())
        paid.snapTo(0f)
    }

    /** Thermal printers feed in jerks: a short linear push, a pause, repeat. */
    private suspend fun print(step: Float, advanceMillis: Int, pauseMillis: Long) {
        phase = Phase.Printing
        snapshotFlow { paperHeight }.first { it > 0f }
        while (fed.value < printedHeight) {
            val target = min(fed.value + step, printedHeight)
            coroutineScope {
                launch {
                    buzz.snapTo(0f)
                    buzz.animateTo(1f, tween(advanceMillis, easing = LinearEasing))
                }
                fed.animateTo(target, tween(advanceMillis, easing = LinearEasing))
            }
            delay(pauseMillis)
        }
        buzz.snapTo(0f)
    }

    private suspend fun tear() = coroutineScope {
        phase = Phase.Tearing
        torn = true
        launch {
            buzz.snapTo(0f)
            buzz.animateTo(1f, tween(110, easing = LinearEasing))
            buzz.snapTo(0f)
        }
        launch { tilt.animateTo(-4f, tween(170, easing = FastOutSlowInEasing)) }
        lift.animateTo(lift.value - detachPx, tween(170, easing = FastOutSlowInEasing))
        delay(70)
        launch {
            delay(140)
            cents.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            phase = Phase.Paid
            paid.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 380f))
        }
        launch { tilt.animateTo(-12f, tween(700, easing = FastOutSlowInEasing)) }
        launch { drift.animateTo(driftPx, tween(700, easing = LinearOutSlowInEasing)) }
        launch { fade.animateTo(1f, tween(240, delayMillis = 460)) }
        lift.animateTo(-flyPx, tween(700, easing = FlyEasing))
    }
}

/** One sine wobble per feed step, zero at both ends so the motor never leaves the body offset. */
private fun vibration(t: Float): Float = if (t <= 0f || t >= 1f) 0f else sin(t * 6f * PI.toFloat())

private fun euros(cents: Int): String = "€${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

@Composable
fun ReceiptCheckoutDemo() {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val state = remember(density) { CheckoutState(scope, density) }
    LaunchedEffect(state) { state.start() }

    Box(
        Modifier
            .fillMaxSize()
            .background(ReceiptColors.Cream)
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    state.autoplay = false
                }
            },
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 16.dp),
        ) {
            TopBar()
            Spacer(Modifier.height(20.dp))
            Text(
                "Almost yours.",
                color = ReceiptColors.Ink,
                fontFamily = FontFamily.Serif,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your receipt prints as you check out — tear it off when you pay.",
                color = ReceiptColors.Muted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(18.dp))
            PayButton(state)
            Spacer(Modifier.height(10.dp))
            Text(
                buildAnnotatedString {
                    append("Paying with Lumen •••• 4242 · ")
                    withStyle(SpanStyle(color = ReceiptColors.Ink, textDecoration = TextDecoration.Underline)) {
                        append("Edit details")
                    }
                },
                Modifier.fillMaxWidth(),
                color = ReceiptColors.Muted,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            OrderPanel(state, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun TopBar() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Kōbō",
            color = ReceiptColors.Ink,
            fontFamily = FontFamily.Serif,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(11.dp)) {
            val stroke = 1.4.dp.toPx()
            val bodyTop = size.height * 0.45f
            drawArc(
                ReceiptColors.Muted,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(size.width * 0.22f, stroke / 2),
                size = Size(size.width * 0.56f, bodyTop * 1.4f),
                style = Stroke(stroke),
            )
            drawRoundRect(
                ReceiptColors.Muted,
                Offset(0f, bodyTop),
                Size(size.width, size.height - bodyTop),
                CornerRadius(1.5.dp.toPx()),
            )
        }
        Spacer(Modifier.width(5.dp))
        Text("Secure checkout", color = ReceiptColors.Muted, fontSize = 12.5.sp)
    }
}

@Composable
private fun PayButton(state: CheckoutState) {
    val press = remember { Animatable(1f) }
    LaunchedEffect(state.pressTick) {
        if (state.pressTick == 0) return@LaunchedEffect
        press.animateTo(0.96f, tween(80))
        press.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 600f))
    }
    val color by animateColorAsState(
        if (state.phase == Phase.Paid) ReceiptColors.OlivePaid else ReceiptColors.Olive,
        tween(420),
        label = "payColor",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .clip(Pill)
            .background(color)
            .clickable(enabled = state.canPay, role = Role.Button) { state.pay(fromButton = true) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Pay ${euros(state.cents.value.roundToInt())}",
            Modifier.graphicsLayer {
                val p = state.paid.value.coerceIn(0f, 1f)
                alpha = 1f - p
                translationY = -p * 14.dp.toPx()
            },
            style = TextStyle(
                color = ReceiptColors.OnOlive,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
        )
        Row(
            Modifier.graphicsLayer {
                val p = state.paid.value
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * 14.dp.toPx()
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Paid", color = ReceiptColors.OnOlive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Canvas(Modifier.size(14.dp)) {
                val p = state.paid.value.coerceIn(0f, 1f)
                val a = Offset(size.width * 0.1f, size.height * 0.55f)
                val b = Offset(size.width * 0.4f, size.height * 0.84f)
                val c = Offset(size.width * 0.92f, size.height * 0.18f)
                val stroke = 2.dp.toPx()
                // The short stroke takes the first third of the reveal, the long one the rest.
                val first = (p / 0.33f).coerceIn(0f, 1f)
                val second = ((p - 0.33f) / 0.67f).coerceIn(0f, 1f)
                if (first > 0f) drawLine(ReceiptColors.OnOlive, a, a + (b - a) * first, stroke, StrokeCap.Round)
                if (second > 0f) drawLine(ReceiptColors.OnOlive, b, b + (c - b) * second, stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun OrderPanel(state: CheckoutState, modifier: Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .background(ReceiptColors.Panel, shape)
            .border(1.dp, ReceiptColors.PanelBorder, shape)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        PanelHeader(state)
        PrintBay(state, Modifier.weight(1f).fillMaxWidth().padding(top = BayTopGap))
    }
}

@Composable
private fun PanelHeader(state: CheckoutState) {
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "livePulse",
    )
    val done = state.phase == Phase.Paid
    val replay by animateFloatAsState(if (done) 1f else 0f, tween(260), label = "replay")
    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .drawBehind { drawCircle(ReceiptColors.Live.copy(alpha = pulse)) },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "ORDER SUMMARY · LIVE",
            color = ReceiptColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                "4 items",
                Modifier.graphicsLayer { alpha = 1f - replay },
                color = ReceiptColors.Muted,
                fontSize = 12.sp,
            )
            if (replay > 0.01f) {
                ReplayPill(Modifier.graphicsLayer { alpha = replay }) {
                    state.autoplay = true
                    state.start()
                }
            }
        }
    }
}

/** The printer plus the paper above its slot; the paper is clipped at the slot line so it appears to come out of it. */
@Composable
private fun PrintBay(state: CheckoutState, modifier: Modifier) {
    BoxWithConstraints(
        modifier.graphicsLayer { translationX = vibration(state.buzz.value) * 0.7.dp.toPx() },
    ) {
        val density = LocalDensity.current
        val bayPx = constraints.maxHeight.toFloat()
        val windowPx = bayPx - with(density) { (PrinterHeight - SlotFromTop + 4.dp).toPx() }
        val printed = state.printedHeight
        // Shrinks the receipt on short screens so the whole printout stays visible above the slot.
        val scale = if (printed <= 0f) {
            1f
        } else {
            minOf(1f, windowPx / printed, (maxWidth - 48.dp) / ReceiptWidth).coerceAtLeast(0.4f)
        }
        val printing = state.phase == Phase.Printing
        val blink = if (printing) {
            rememberInfiniteTransition(label = "led").animateFloat(
                initialValue = 0.15f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing), RepeatMode.Reverse),
                label = "ledBlink",
            )
        } else {
            null
        }
        val ledColor by animateColorAsState(
            when (state.phase) {
                Phase.Idle -> ReceiptColors.AmberDim
                Phase.Paid -> ReceiptColors.Green
                else -> ReceiptColors.Amber
            },
            tween(380),
            label = "led",
        )
        val idle = state.phase == Phase.Idle

        ThermalPrinter(
            led = { ledColor },
            ledGlow = { blink?.value ?: if (idle) 0f else 1f },
            modifier = Modifier.align(Alignment.BottomCenter).width(ReceiptWidth * scale + 44.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = PrinterHeight - SlotFromTop)
                .slotMask(state, scale),
        ) {
            ReceiptPaper(
                torn = state.torn,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .wrapContentSize(Alignment.BottomCenter, unbounded = true)
                    .onSizeChanged { state.paperHeight = it.height.toFloat() }
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleX = scale
                        scaleY = scale
                        translationY = (size.height - state.fed.value) * scale + state.lift.value
                        translationX = state.drift.value
                        rotationZ = state.tilt.value
                        alpha = 1f - state.fade.value
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        enabled = state.phase == Phase.Printed,
                        state = rememberDraggableState { state.drag(it) },
                        onDragStopped = { velocity -> state.release(velocity) },
                    ),
            )
        }
    }
}

/**
 * Clips the paper at the slot (but nowhere else, so a torn receipt can fly over the whole page),
 * shades the paper where it curls into the slot, and draws the slot over it.
 */
private fun Modifier.slotMask(state: CheckoutState, scale: Float): Modifier = drawWithCache {
    val slotY = size.height
    val paperWidth = ReceiptWidth.toPx() * scale
    val paperLeft = (size.width - paperWidth) / 2
    val curl = CurlHeight.toPx() * scale
    val curlShade = Brush.verticalGradient(
        listOf(Color.Transparent, Color(0x14000000), Color(0x38000000)),
        startY = slotY - curl,
        endY = slotY,
    )
    val stubHeight = PaperPeek.toPx() * scale
    val stub = Path().apply {
        paperOutline(paperWidth, stubHeight, ToothWidth.toPx() * scale, ToothDepth.toPx() * scale, tornBottom = false)
        translate(Offset(paperLeft, slotY - stubHeight))
    }
    val slotWidth = paperWidth + 14.dp.toPx()
    val slotHeight = 5.dp.toPx()
    val hairline = 1.dp.toPx()
    onDrawWithContent {
        clipRect(left = -size.width, top = -size.height * 12f, right = size.width * 2f, bottom = slotY) {
            // The stub sits behind the departing receipt.
            val torn = state.torn
            if (torn) drawPath(stub, ReceiptColors.Paper)
            this@onDrawWithContent.drawContent()
            if (!torn) {
                val showing = (state.fed.value * scale - state.lift.value).coerceIn(0f, curl)
                if (showing > 0f) {
                    drawRect(curlShade, Offset(paperLeft, slotY - showing), Size(paperWidth, showing))
                }
            }
        }
        drawRoundRect(
            ReceiptColors.Slot,
            Offset((size.width - slotWidth) / 2, slotY - slotHeight / 2),
            Size(slotWidth, slotHeight),
            CornerRadius(slotHeight / 2),
        )
        drawLine(
            Color.White.copy(alpha = 0.1f),
            Offset((size.width - slotWidth) / 2 + slotHeight, slotY + slotHeight / 2 + hairline / 2),
            Offset((size.width + slotWidth) / 2 - slotHeight, slotY + slotHeight / 2 + hairline / 2),
            hairline,
        )
    }
}

@Composable
private fun ReplayPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(Pill)
            .background(ReceiptColors.Ink.copy(alpha = 0.07f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(11.dp)) {
            val stroke = 1.5.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            drawArc(
                ReceiptColors.Ink,
                startAngle = 40f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val tip = Offset(center.x + radius * 0.87f, center.y - radius * 0.5f)
            drawLine(ReceiptColors.Ink, tip, tip + Offset(0f, -stroke * 2.4f), stroke, StrokeCap.Round)
            drawLine(ReceiptColors.Ink, tip, tip + Offset(-stroke * 2.4f, 0f), stroke, StrokeCap.Round)
        }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = ReceiptColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
