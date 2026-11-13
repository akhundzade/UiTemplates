package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val ScrambleTickMillis = 40L
private const val FirstLockMillis = 360L
private const val LockStepMillis = 130L
private const val LockPopScale = 1.3f
private val Scrambling = Color(0xFF8B8FFF)

@Composable
fun DecodeLockDemo() {
    CounterScene(
        number = 5,
        title = "Decode Lock",
        caption = "Digits scramble through random numbers, then lock in on the new value from left to right.",
    ) { value ->
        DecodeNumber(value)
    }
}

@Composable
private fun DecodeNumber(value: Int) {
    val shown = remember { mutableStateListOf(*digitsOf(value).toTypedArray()) }
    var locked by remember { mutableIntStateOf(DigitCount) }
    val pops = remember { List(DigitCount) { Animatable(1f) } }

    LaunchedEffect(value) {
        val target = digitsOf(value)
        if (locked == DigitCount && shown.toList() == target) return@LaunchedEffect
        locked = 0
        pops.forEach { it.snapTo(1f) }
        var elapsed = 0L
        while (locked < DigitCount) {
            for (i in locked until DigitCount) {
                // Always a different digit, so every tick visibly changes.
                shown[i] = (shown[i] + 1 + Random.nextInt(9)) % 10
            }
            delay(ScrambleTickMillis)
            elapsed += ScrambleTickMillis
            while (locked < DigitCount && elapsed >= FirstLockMillis + locked * LockStepMillis) {
                val i = locked
                shown[i] = target[i]
                locked++
                launch {
                    pops[i].snapTo(LockPopScale)
                    pops[i].animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
                }
            }
        }
    }

    DigitRow { index ->
        val isLocked = index < locked
        // The lock pop's extra scale doubles as a brief cyan flash.
        val flash = ((pops[index].value - 1f) / (LockPopScale - 1f)).coerceIn(0f, 1f)
        DigitCell {
            Text(
                shown[index].toString(),
                style = NumberStyle,
                color = if (isLocked) lerp(Color.White, MenuColors.Cyan, flash) else Scrambling,
                modifier = Modifier.graphicsLayer {
                    val scale = pops[index].value
                    scaleX = scale
                    scaleY = scale
                    alpha = if (isLocked) 1f else 0.75f
                },
            )
        }
    }
}
