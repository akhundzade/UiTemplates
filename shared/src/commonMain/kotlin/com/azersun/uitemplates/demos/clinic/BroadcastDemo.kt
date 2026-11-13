package com.azersun.uitemplates.demos.clinic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.common.countUp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val RECIPIENTS = 150

private enum class SendState { Draft, Sending, Sent }

@Composable
fun BroadcastDemo() {
    var generation by remember { mutableIntStateOf(0) }
    key(generation) { BroadcastScreen(onReplay = { generation++ }) }
}

@Composable
private fun BroadcastScreen(onReplay: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(SendState.Draft) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val started = rememberStarted()
    val blur by animateDpAsState(if (confirming) 6.dp else 0.dp, tween(260), label = "blur")
    val dim by animateFloatAsState(if (confirming) 1f else 0f, tween(260), label = "dim")

    fun confirm() {
        confirming = false
        state = SendState.Sending
        scope.launch {
            delay(450)
            progress.animateTo(1f, tween(5200, easing = LinearEasing))
            state = SendState.Sent
        }
    }

    LaunchedEffect(Unit) {
        delay(1200)
        confirming = true
        delay(1300)
        if (confirming) confirm()
    }

    val p = progress.value
    val sent = (RECIPIENTS * p).roundToInt()
    val delivered = (RECIPIENTS * ((p - 0.06f) / 0.94f).coerceIn(0f, 1f) * 0.97f).roundToInt()
    val read = (RECIPIENTS * ((p - 0.25f) / 0.75f).coerceIn(0f, 1f) * 0.62f).roundToInt()
    val replied = (RECIPIENTS * ((p - 0.45f) / 0.55f).coerceIn(0f, 1f) * 0.14f).roundToInt()
    val booked = (RECIPIENTS * ((p - 0.6f) / 0.4f).coerceIn(0f, 1f) * 0.07f).roundToInt()

    Box(Modifier.fillMaxSize().background(Clinic.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .blur(blur)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WhatsApp broadcasts and templates", color = Clinic.Muted, fontSize = 10.sp)
                    Text("Campaigns", color = Clinic.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                ReplayChip(onClick = onReplay)
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiTile("Messages sent", formatThousands(countUp(if (started) 6_472 else 0, 1100) + sent), Modifier.weight(1.2f))
                KpiTile("Read rate", "${countUp(if (started) 91 else 0, 1100)}%", Modifier.weight(1f))
                KpiTile("Bookings", "${countUp(if (started) 106 else 0, 1100) + booked}", Modifier.weight(1f))
            }

            Rise(0, Modifier.padding(top = 16.dp)) {
                CampaignCard(
                    title = "Well-child check-ups",
                    template = "annual_checkup_child",
                    audience = "Pediatrics · $RECIPIENTS recipients",
                    state = state,
                    onSend = { if (state == SendState.Draft) confirming = true },
                ) {
                    AnimatedVisibility(
                        visible = state != SendState.Draft,
                        enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) + fadeIn(tween(300, 150)),
                    ) {
                        Column(Modifier.padding(top = 12.dp)) {
                            ProgressRow("Sent", sent, Clinic.Green)
                            ProgressRow("Delivered", delivered, Color(0xFF29A36A))
                            ProgressRow("Read", read, Clinic.Blue)
                            ProgressRow("Replied", replied, Color(0xFF7B5BD6))
                            ProgressRow("Booked", booked, Clinic.Orange)
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (state == SendState.Sending) PulseDot(Clinic.Green) else Ticks(2, Clinic.Green)
                                Text(
                                    if (state == SendState.Sent) "Sent to $RECIPIENTS patients" else "Sending $sent of $RECIPIENTS",
                                    color = Clinic.Green,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
            Rise(1, Modifier.padding(top = 10.dp)) {
                CampaignCard("Annual check-up recall · Q4", "annual_checkup_recall", "All patients · 214 recipients", null, onSend = {})
            }
            Rise(2, Modifier.padding(top = 10.dp)) {
                CampaignCard("Holiday hours", "holiday_hours", "All patients · 720 recipients", null, onSend = {})
            }
        }

        if (dim > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dim }
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { confirming = false },
            )
        }
        AnimatedVisibility(
            visible = confirming,
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = 450f), initialScale = 0.88f) + fadeIn(tween(160)),
            exit = scaleOut(tween(160), targetScale = 0.94f) + fadeOut(tween(160)),
        ) {
            Column(
                Modifier
                    .widthIn(max = 360.dp)
                    .shadow(24.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Clinic.Card)
                    .padding(18.dp),
            ) {
                Text("Send “Well-child check-ups” now?", color = Clinic.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$RECIPIENTS patients will get annual_checkup_child on WhatsApp. 23 without consent are skipped.",
                    color = Clinic.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(Modifier.align(Alignment.End).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClinicButton("Cancel", filled = false) { confirming = false }
                    ClinicButton("Send to $RECIPIENTS", filled = true) { confirm() }
                }
            }
        }
    }
}

@Composable
private fun CampaignCard(
    title: String,
    template: String,
    audience: String,
    state: SendState?,
    onSend: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    val border by animateColorAsState(if (state == SendState.Sending) Clinic.Green else Clinic.Line, tween(400), label = "border")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Clinic.Card)
            .border(if (state == SendState.Sending) 1.5.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = 300f))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Clinic.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(template, color = Clinic.Muted, fontSize = 10.sp)
            }
            when (state) {
                SendState.Draft -> Pill("Draft", Clinic.Muted, Clinic.Line)
                SendState.Sending -> Pill("● Sending", Clinic.Green, Clinic.GreenSoft)
                SendState.Sent -> Pill("Sent", Clinic.Green, Clinic.GreenSoft)
                null -> Pill("Scheduled", Clinic.Blue, Clinic.BlueSoft)
            }
        }
        Text(audience, color = Clinic.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Hi {name}, it's been a year since your last check-up with Dr. Rahman. Tap below to choose a time.",
            color = Clinic.Text,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F8F3))
                .padding(10.dp),
        )
        if (state == SendState.Draft) {
            ClinicButton("Send now", filled = true, modifier = Modifier.padding(top = 10.dp), onClick = onSend)
        }
        extra()
    }
}

@Composable
private fun ProgressRow(label: String, value: Int, color: Color) {
    val fraction by animateFloatAsState(value / RECIPIENTS.toFloat(), spring(stiffness = 120f), label = label)
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Clinic.Muted, fontSize = 11.sp, modifier = Modifier.width(66.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(Clinic.Line)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
        }
        Text("$value", color = Clinic.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(34.dp).padding(start = 8.dp))
        Text("${value * 100 / RECIPIENTS}%", color = Clinic.Muted, fontSize = 10.sp, modifier = Modifier.width(32.dp))
    }
}

@Composable
private fun PulseDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "t")
    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(14.dp)
                .graphicsLayer {
                    scaleX = 0.4f + t
                    scaleY = 0.4f + t
                    alpha = 1f - t
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.4f)),
        )
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
    }
}

private fun formatThousands(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(",").reversed()
