package com.azersun.uitemplates.demos.button2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.common.Confetti
import com.azersun.uitemplates.common.rememberConfettiState
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay

private val CelebrateGradient = Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Pink))

@Composable
fun ConfettiBurstDemo() {
    val confetti = rememberConfettiState()
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 1 = fully squished; the release spring undershoots below 0, which stretches it like jelly.
    val squish = remember { Animatable(0f) }

    LaunchedEffect(pressed) {
        if (pressed) {
            squish.animateTo(1f, tween(110))
        } else {
            squish.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 450f))
        }
    }

    fun celebrate() {
        // The button sits at the centre of the confetti canvas.
        confetti.burst(Offset(stageSize.width / 2f, stageSize.height / 2f))
    }

    ButtonStage(
        3,
        "Confetti Burst",
        "A tap bursts confetti out of the button; the pieces flutter and fall.",
        autoplay = {
            val press = PressInteraction.Press(Offset.Zero)
            interaction.emit(press)
            try {
                delay(160)
            } finally {
                interaction.tryEmit(PressInteraction.Release(press))
            }
            celebrate()
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { stageSize = it },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(196.dp, 60.dp)
                    .graphicsLayer {
                        val q = squish.value
                        scaleX = 1f + 0.08f * q
                        scaleY = 1f - 0.16f * q
                    }
                    .shadow(18.dp, CircleShape, ambientColor = MenuColors.Pink, spotColor = MenuColors.Pink)
                    .clip(CircleShape)
                    .drawWithCache { onDrawBehind { drawRect(CelebrateGradient) } }
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClickLabel = "Celebrate",
                        role = Role.Button,
                        onClick = { celebrate() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Celebrate", color = MenuColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Confetti(confetti, Modifier.fillMaxSize())
        }
    }
}
