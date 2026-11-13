package com.azersun.uitemplates.demos.clinic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private data class ChatThread(
    val id: Int,
    val name: String,
    val color: Color,
    val time: String,
    val preview: String,
    val tag: String,
    val unread: Int,
)

private val Threads = listOf(
    ChatThread(1, "Amina Khan", Color(0xFF1F8A5B), "10:15", "Anything after 10 works for me", "Booking", 2),
    ChatThread(2, "Ana Souza", Color(0xFF7B5BD6), "10:27", "Hi! I just moved to the area…", "Booking", 1),
    ChatThread(3, "Noah Bennett", Color(0xFFD64545), "10:34", "Hello, my son Noah has a fever", "Urgent", 3),
    ChatThread(4, "Marcus Webb", Color(0xFF2D7FDB), "10:28", "Thursday 11:15 please", "Results", 0),
    ChatThread(5, "Elena Rossi", Color(0xFFE07B1F), "9:53", "Is there parking at the clinic?", "Question", 1),
    ChatThread(6, "Kwame Mensah", Color(0xFF1A9DB8), "Yesterday", "Booked a visit", "Template", 0),
)

private enum class Delivery { Sent, Delivered, Read }

private class Message(val id: Int, val text: String, val mine: Boolean, val time: String, val animate: Boolean) {
    var delivery by mutableStateOf(Delivery.Sent)
}

private val QuickReplies = listOf(
    "Parking" to "Free patient parking behind the building — the entrance is on Harbor St, next to the pharmacy.",
    "Opening hours" to "We're open Mon–Fri 8:00–18:00 and Saturday 9:00–13:00.",
    "What to bring" to "Please bring your ID, insurance card and any current medication.",
    "Running late" to "No problem — we'll hold your slot for 15 minutes.",
)

@Composable
fun InboxDemo() {
    var generation by remember { mutableIntStateOf(0) }
    key(generation) { InboxFlow(onReplay = { generation++ }) }
}

@Composable
private fun InboxFlow(onReplay: () -> Unit) {
    var openThread by remember { mutableStateOf<ChatThread?>(null) }
    var autoChip by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        delay(1300)
        openThread = Threads.first { it.name == "Elena Rossi" }
        delay(1100)
        autoChip = 0
    }

    Box(Modifier.fillMaxSize().background(Clinic.Background)) {
        AnimatedContent(
            targetState = openThread,
            transitionSpec = {
                val opening = targetState != null
                (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { if (opening) it else -it / 4 } + fadeIn(tween(200)))
                    .togetherWith(slideOutHorizontally(tween(260)) { if (opening) -it / 4 else it } + fadeOut(tween(200)))
            },
            label = "inbox",
        ) { thread ->
            if (thread == null) {
                ThreadList(onOpen = { openThread = it }, onReplay = onReplay)
            } else {
                Conversation(thread, autoChip = autoChip, onBack = { openThread = null; autoChip = null })
            }
        }
    }
}

@Composable
private fun ThreadList(onOpen: (ChatThread) -> Unit, onReplay: () -> Unit) {
    val started = rememberStarted()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Inbox", color = Clinic.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ReplayChip(onClick = onReplay)
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiTile("Unread", "${countUpOr(started, 7)}", Modifier.weight(1f))
            KpiTile("Waiting on us", "${countUpOr(started, 6)}", Modifier.weight(1f))
            KpiTile("Median reply", "${countUpOr(started, 8)} min", Modifier.weight(1f))
        }
        Text(
            "Search name, phone, message",
            color = Clinic.Muted,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Clinic.Card)
                .border(1.dp, Clinic.Line, RoundedCornerShape(12.dp))
                .padding(12.dp),
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Threads.forEachIndexed { i, thread ->
                Rise(i, stepMillis = 45) { ThreadRow(thread, onClick = { onOpen(thread) }) }
            }
        }
    }
}

@Composable
private fun countUpOr(started: Boolean, value: Int) = com.azersun.uitemplates.common.countUp(if (started) value else 0, 900)

@Composable
private fun ThreadRow(thread: ChatThread, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Clinic.Card)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(thread.name.split(" ").joinToString("") { it.take(1) }, thread.color, 36.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(thread.name, color = Clinic.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(thread.time, color = if (thread.unread > 0) Clinic.Green else Clinic.Muted, fontSize = 10.sp)
            }
            Text(thread.preview, color = Clinic.Muted, fontSize = 11.sp, maxLines = 1)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(thread.tag, if (thread.tag == "Urgent") Color(0xFFD64545) else Clinic.Green, if (thread.tag == "Urgent") Color(0xFFFDE4E4) else Clinic.GreenSoft)
                Spacer(Modifier.weight(1f))
                if (thread.unread > 0) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(Clinic.Green), contentAlignment = Alignment.Center) {
                        Text("${thread.unread}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Conversation(thread: ChatThread, autoChip: Int?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            Message(1, "Hi, I booked for Saturday at 11.", mine = false, time = "9:41", animate = false),
            Message(2, "Great, you're all set with Dr. Silva", mine = true, time = "9:44", animate = false).apply { delivery = Delivery.Read },
            Message(3, thread.preview + " First time visiting.", mine = false, time = "9:53", animate = false),
        )
    }
    var draft by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val sendPulse = remember { Animatable(1f) }
    val scroll = rememberScrollState()

    fun send() {
        if (draft.isBlank()) return
        val message = Message(messages.size + 1, draft, mine = true, time = "10:40", animate = true)
        draft = ""
        scope.launch {
            sendPulse.snapTo(0.8f)
            sendPulse.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
        messages += message
        scope.launch {
            delay(600)
            message.delivery = Delivery.Delivered
            delay(900)
            message.delivery = Delivery.Read
            delay(400)
            typing = true
            delay(1700)
            typing = false
            messages += Message(messages.size + 1, "Perfect, thank you! See you Saturday.", mine = false, time = "10:41", animate = true)
            busy = false
        }
    }

    fun useChip(index: Int) {
        if (busy) return
        busy = true
        val text = QuickReplies[index].second
        scope.launch {
            draft = ""
            for (end in 1..text.length step 3) {
                draft = text.take(end)
                delay(12)
            }
            draft = text
            delay(350)
            send()
        }
    }

    LaunchedEffect(autoChip) { autoChip?.let { useChip(it) } }
    LaunchedEffect(messages.size, typing) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().background(Color(0xFFEFEAE2))) {
        Row(
            Modifier.fillMaxWidth().background(Clinic.Card).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(14.dp)) {
                    val s = 2.dp.toPx()
                    drawLine(Clinic.Text, Offset(size.width * 0.65f, 0f), Offset(size.width * 0.2f, size.height / 2), s, StrokeCap.Round)
                    drawLine(Clinic.Text, Offset(size.width * 0.2f, size.height / 2), Offset(size.width * 0.65f, size.height), s, StrokeCap.Round)
                }
            }
            Avatar(thread.name.split(" ").joinToString("") { it.take(1) }, thread.color, 34.dp)
            Column(Modifier.padding(start = 10.dp)) {
                Text(thread.name, color = Clinic.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (typing) "typing…" else "WhatsApp · patient since 2021", color = if (typing) Clinic.Green else Clinic.Muted, fontSize = 10.sp)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "UNREAD MESSAGE",
                color = Clinic.Green,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp),
            )
            messages.forEach { message -> key(message.id) { Bubble(message) } }
            if (typing) TypingBubble()
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QuickReplies.forEachIndexed { i, (label, _) ->
                Text(
                    label,
                    color = Clinic.Text,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Clinic.Card)
                        .border(1.dp, Clinic.Line, RoundedCornerShape(50))
                        .clickable { useChip(i) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Clinic.Card)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hasDraft = draft.isNotEmpty()
            Text(
                if (hasDraft) draft else "Message ${thread.name.substringBefore(" ")}…",
                color = if (hasDraft) Clinic.Text else Clinic.Muted,
                fontSize = 12.sp,
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Clinic.Background)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            val sendColor by animateColorAsState(if (hasDraft) Clinic.Green else Clinic.Line, label = "send")
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = sendPulse.value
                        scaleY = sendPulse.value
                    }
                    .clip(CircleShape)
                    .background(sendColor)
                    .clickable { send() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val s = 2.dp.toPx()
                    val c = Color.White
                    drawLine(c, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), s, StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.55f, size.height * 0.1f), Offset(size.width, size.height * 0.5f), s, StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.55f, size.height * 0.9f), Offset(size.width, size.height * 0.5f), s, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun Bubble(message: Message) {
    val appear = remember { Animatable(if (message.animate) 0f else 1f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 420f)) }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (message.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .graphicsLayer {
                    val a = appear.value
                    alpha = a.coerceIn(0f, 1f)
                    scaleX = 0.8f + 0.2f * a
                    scaleY = 0.8f + 0.2f * a
                    translationY = (1f - a) * 24.dp.toPx()
                    transformOrigin = TransformOrigin(if (message.mine) 1f else 0f, 1f)
                }
                .clip(RoundedCornerShape(14.dp))
                .background(if (message.mine) Color(0xFFD9FDD3) else Clinic.Card)
                .padding(horizontal = 11.dp, vertical = 7.dp),
        ) {
            Text(message.text, color = Clinic.Text, fontSize = 13.sp, lineHeight = 17.sp)
            Row(Modifier.align(Alignment.End).padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message.time, color = Clinic.Muted, fontSize = 9.sp)
                if (message.mine) {
                    val tickColor by animateColorAsState(if (message.delivery == Delivery.Read) Clinic.Read else Clinic.Muted, tween(300), label = "tick")
                    Ticks(if (message.delivery == Delivery.Sent) 1 else 2, tickColor, Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "phase")
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Clinic.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { i ->
            val local = ((phase * 3f - i) % 3f + 3f) % 3f
            val lift = if (local < 1f) kotlin.math.sin(local * kotlin.math.PI.toFloat()) else 0f
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { translationY = -lift * 4.dp.toPx() }
                    .clip(CircleShape)
                    .background(Clinic.Muted.copy(alpha = 0.5f + 0.5f * lift)),
            )
        }
    }
}
