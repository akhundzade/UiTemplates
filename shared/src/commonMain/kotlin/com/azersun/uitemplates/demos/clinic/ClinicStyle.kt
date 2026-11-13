package com.azersun.uitemplates.demos.clinic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal object Clinic {
    val Sidebar = Color(0xFF0F3D2E)
    val Green = Color(0xFF1F8A5B)
    val GreenSoft = Color(0xFFDDF3E7)
    val Background = Color(0xFFF2F4F2)
    val Card = Color.White
    val Line = Color(0xFFE4E8E5)
    val Text = Color(0xFF15201B)
    val Muted = Color(0xFF6F7C75)
    val Orange = Color(0xFFF08A24)
    val OrangeSoft = Color(0xFFFFF0E0)
    val Blue = Color(0xFF3B82F6)
    val BlueSoft = Color(0xFFE3EDFF)
    val Read = Color(0xFF34B7F1)
}

@Composable
internal fun Avatar(initials: String, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = color, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun Pill(text: String, foreground: Color, background: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = foreground,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
internal fun ClinicButton(text: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        color = if (filled) Color.White else Clinic.Text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) Clinic.Green else Clinic.Card)
            .border(1.dp, if (filled) Clinic.Green else Clinic.Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
internal fun KpiTile(label: String, value: String, modifier: Modifier = Modifier, badge: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Clinic.Card)
            .border(1.dp, Clinic.Line, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Text(label, color = Clinic.Muted, fontSize = 10.sp, maxLines = 1)
        Text(value, color = Clinic.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        if (badge != null) Box(Modifier.padding(top = 3.dp)) { badge() }
    }
}

@Composable
internal fun ReplayChip(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        "↺ Replay",
        color = Clinic.Text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Clinic.Card)
            .border(1.dp, Clinic.Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Fades and lifts its content in once, [index] steps after first composition. */
@Composable
internal fun Rise(index: Int, modifier: Modifier = Modifier, delayMillis: Long = 60, stepMillis: Long = 55, content: @Composable () -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis + index * stepMillis)
        reveal.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
    }
    Box(
        modifier.graphicsLayer {
            alpha = reveal.value.coerceIn(0f, 1f)
            translationY = (1f - reveal.value) * 16.dp.toPx()
        },
    ) { content() }
}

/** False on the first frame, true after: lets count-ups start from zero when a screen appears. */
@Composable
internal fun rememberStarted(): Boolean {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return started
}

internal enum class ClinicIcon { Home, Chat, Calendar, Megaphone }

@Composable
internal fun ClinicIconView(icon: ClinicIcon, color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) { drawClinicIcon(icon, color) }
}

private fun DrawScope.drawClinicIcon(icon: ClinicIcon, color: Color) {
    val w = size.width
    val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(w * x1, w * y1), Offset(w * x2, w * y2), stroke.width, StrokeCap.Round)
    when (icon) {
        ClinicIcon.Home -> {
            line(0.12f, 0.48f, 0.5f, 0.14f); line(0.5f, 0.14f, 0.88f, 0.48f)
            drawRoundRect(color, Offset(w * 0.24f, w * 0.42f), Size(w * 0.52f, w * 0.44f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = stroke)
        }
        ClinicIcon.Chat -> {
            drawRoundRect(color, Offset(w * 0.12f, w * 0.16f), Size(w * 0.76f, w * 0.54f), androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = stroke)
            line(0.3f, 0.7f, 0.24f, 0.88f); line(0.24f, 0.88f, 0.46f, 0.7f)
        }
        ClinicIcon.Calendar -> {
            drawRoundRect(color, Offset(w * 0.12f, w * 0.2f), Size(w * 0.76f, w * 0.66f), androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = stroke)
            line(0.12f, 0.4f, 0.88f, 0.4f); line(0.34f, 0.1f, 0.34f, 0.26f); line(0.66f, 0.1f, 0.66f, 0.26f)
        }
        ClinicIcon.Megaphone -> {
            line(0.16f, 0.4f, 0.16f, 0.62f); line(0.16f, 0.4f, 0.74f, 0.18f); line(0.16f, 0.62f, 0.74f, 0.84f)
            line(0.74f, 0.18f, 0.74f, 0.84f); line(0.3f, 0.66f, 0.38f, 0.88f)
        }
    }
}

/** WhatsApp-style ticks: one for sent, two for delivered, two in blue for read. */
@Composable
internal fun Ticks(count: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 16.dp, height = 10.dp)) {
        val s = 1.4.dp.toPx()
        fun tick(dx: Float) {
            drawLine(color, Offset(dx, size.height * 0.55f), Offset(dx + size.height * 0.35f, size.height * 0.9f), s, StrokeCap.Round)
            drawLine(color, Offset(dx + size.height * 0.35f, size.height * 0.9f), Offset(dx + size.height * 0.95f, size.height * 0.1f), s, StrokeCap.Round)
        }
        tick(0f)
        if (count > 1) tick(size.height * 0.5f)
    }
}

@Composable
internal fun SectionHeader(title: String, count: Int, dot: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Text(title, color = Clinic.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 7.dp))
        Text(
            "$count",
            color = Clinic.Muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 7.dp).clip(RoundedCornerShape(50)).background(Clinic.Line).padding(horizontal = 6.dp),
        )
    }
}
