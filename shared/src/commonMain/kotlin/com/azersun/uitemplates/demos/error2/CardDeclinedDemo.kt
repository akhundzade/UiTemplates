package com.azersun.uitemplates.demos.error2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private const val FlipMillis = 560
private const val DeclinedHoldMillis = 1200L
private val CardShape = RoundedCornerShape(18.dp)
private val FrontBrush = Brush.linearGradient(listOf(Color(0xFF4F7BFF), Color(0xFF6A4BE8), Color(0xFF3A2C9E)))
private val BackBrush = Brush.linearGradient(listOf(Color(0xFFFF6B7A), ErrorColors.DeepRed))
private val Gold = Brush.linearGradient(listOf(Color(0xFFFFE29A), Color(0xFFD9A441)))

@Composable
fun CardDeclinedDemo() {
    val flip = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }
    val showBack by remember { derivedStateOf { flip.value > 90f } }
    val scope = rememberCoroutineScope()

    fun pay() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                flip.animateTo(180f, tween(FlipMillis, easing = FastOutSlowInEasing))
                delay(DeclinedHoldMillis)
                flip.animateTo(0f, tween(FlipMillis, easing = FastOutSlowInEasing))
            } finally {
                busy = false
            }
        }
    }

    ErrorStage(
        2,
        "Card Declined",
        "Card flips over to a red back: \"Card declined\". Flips back a second later.",
        autoplay = { pay() },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.586f)
                    .graphicsLayer {
                        rotationY = flip.value
                        cameraDistance = 12f * density
                        val lift = 1f + 0.06f * sin(flip.value / 180f * PI.toFloat())
                        scaleX = lift
                        scaleY = lift
                    }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = "Pay with this card",
                        role = Role.Button,
                        onClick = ::pay,
                    ),
            ) {
                if (showBack) {
                    // Counter-rotated so the back's content isn't mirrored.
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { CardBack() }
                } else {
                    CardFront()
                }
            }
            Text(
                "Tap the card to pay \$24.00",
                color = ErrorColors.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
    }
}

@Composable
private fun CardFront() {
    Column(
        Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(FrontBrush)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.08f), size.width * 0.42f, Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.05f), size.width * 0.3f, Offset(size.width * 0.78f, size.height * 1.05f))
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Chip()
            Spacer(Modifier.width(10.dp))
            Contactless()
            Spacer(Modifier.weight(1f))
            Text("DEBIT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Column {
            Text(
                "••••  4821",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("ALEX MORGAN", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("09/28", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun CardBack() {
    Column(
        Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(BackBrush),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(30.dp).background(Color(0xCC2A0A14)))
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(Modifier.size(34.dp)) {
                drawCircle(Color.White)
                val stroke = size.width * 0.12f
                drawLine(
                    ErrorColors.DeepRed,
                    Offset(center.x, size.height * 0.24f),
                    Offset(center.x, size.height * 0.56f),
                    stroke,
                    StrokeCap.Round,
                )
                drawCircle(ErrorColors.DeepRed, stroke * 0.62f, Offset(center.x, size.height * 0.76f))
            }
            Text(
                "Card declined",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text("Try another card", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun Chip() {
    Canvas(Modifier.size(40.dp, 30.dp)) {
        val r = CornerRadius(size.height * 0.2f)
        drawRoundRect(Gold, cornerRadius = r)
        val line = Color(0x66704A10)
        val stroke = size.height * 0.05f
        drawLine(line, Offset(0f, size.height * 0.36f), Offset(size.width, size.height * 0.36f), stroke)
        drawLine(line, Offset(0f, size.height * 0.64f), Offset(size.width, size.height * 0.64f), stroke)
        drawLine(line, Offset(size.width * 0.38f, 0f), Offset(size.width * 0.38f, size.height), stroke)
        drawLine(line, Offset(size.width * 0.62f, 0f), Offset(size.width * 0.62f, size.height), stroke)
    }
}

@Composable
private fun Contactless() {
    Canvas(Modifier.size(18.dp)) {
        val stroke = Stroke(size.width * 0.1f, cap = StrokeCap.Round)
        repeat(3) { i ->
            val r = size.width * (0.2f + i * 0.2f)
            drawArc(
                Color.White.copy(alpha = 0.85f),
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(size.width * 0.05f - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = stroke,
            )
        }
    }
}
