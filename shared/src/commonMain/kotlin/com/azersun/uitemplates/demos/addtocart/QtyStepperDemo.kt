package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val StepperWidth = 0.78f

@Composable
fun QtyStepperDemo() {
    val scene = rememberCartScene()
    var qty by remember { mutableIntStateOf(0) }
    val width by animateFloatAsState(
        if (qty > 0) StepperWidth else 1f,
        spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "width",
    )

    fun add() {
        if (qty > 0) return
        qty = 1
        scene.count++
    }

    ProductScene(
        scene = scene,
        number = 2,
        title = "Qty Stepper",
        caption = "The button morphs into a stepper: trash, count and a filled +. Trash turns it back.",
        onAutoplay = ::add,
        button = {
            PillButton(
                onClick = if (qty == 0) ::add else null,
                modifier = Modifier.fillMaxWidth(width),
            ) {
                AnimatedContent(
                    targetState = qty > 0,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 80)) + scaleIn(tween(260, delayMillis = 60), initialScale = 0.88f)) togetherWith
                            fadeOut(tween(100))
                    },
                    contentAlignment = Alignment.Center,
                    label = "stepper",
                ) { stepper ->
                    if (stepper) {
                        Stepper(
                            qty = qty,
                            onTrash = {
                                scene.count -= qty
                                qty = 0
                            },
                            onPlus = {
                                qty++
                                scene.count++
                            },
                        )
                    } else {
                        AddLabel()
                    }
                }
            }
        },
    )
}

@Composable
private fun Stepper(qty: Int, onTrash: () -> Unit, onPlus: () -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) }

    Row(Modifier.fillMaxSize().padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onTrash),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(15.dp)) { drawTrashLine(CartColors.Ink, IconStroke.toPx()) }
        }
        Spacer(Modifier.weight(1f))
        AnimatedContent(
            targetState = qty,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInVertically(tween(200)) { it * direction } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(200)) { -it * direction } + fadeOut(tween(140)))
            },
            label = "qty",
        ) { value ->
            Text("$value", color = CartColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(30.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(CartColors.Ink)
                .clickable(onClick = onPlus),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(10.dp)) { drawPlus(Color.White, IconStroke.toPx() * 1.2f) }
        }
    }
}
