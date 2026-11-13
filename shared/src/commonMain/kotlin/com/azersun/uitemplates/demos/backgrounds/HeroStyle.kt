package com.azersun.uitemplates.demos.backgrounds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

private val BetaGreen = Color(0xFF34D399)
private val ButtonText = Color(0xFF0B0B14)

/** Where the autoplay taps land, as fractions of the hero's size. */
private val AutoTaps = listOf(Offset(0.28f, 0.3f), Offset(0.74f, 0.62f), Offset(0.4f, 0.84f))

/**
 * The shared landing-page hero. [onTap] receives the tap position as a fraction of the hero's
 * size (0..1 on each axis), so backgrounds can keep their state independent of layout size.
 */
@Composable
internal fun HeroStage(
    number: Int,
    title: String,
    caption: String,
    onTap: (Offset) -> Unit,
    background: @Composable () -> Unit,
) {
    val tap by rememberUpdatedState(onTap)
    var interacted by remember { mutableStateOf(false) }

    LaunchedEffect(interacted) {
        if (interacted) return@LaunchedEffect
        delay(700)
        AutoTaps.forEach {
            tap(it)
            delay(1400)
        }
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures { position ->
                        interacted = true
                        tap(Offset(position.x / size.width, position.y / size.height))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            background()
            HeroContent()
        }
    }
}

@Composable
private fun HeroContent() {
    Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(BetaGreen, CircleShape))
            Text(
                "Now in beta",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            "Build faster",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "Launch a polished site in minutes.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Box(
            Modifier
                .padding(top = 26.dp)
                .background(Color.White, CircleShape)
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Text("Get started", color = ButtonText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Seconds since first composition, updated every frame. Read it only in draw to avoid recomposing. */
@Composable
internal fun rememberFrameSeconds(): FloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { seconds.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return seconds
}
