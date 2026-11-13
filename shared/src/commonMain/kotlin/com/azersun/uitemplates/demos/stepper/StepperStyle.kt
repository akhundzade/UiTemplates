package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal enum class Side(val delta: Int) { Minus(-1), Plus(1) }

internal const val StartQuantity = 2
private const val MaxQuantity = 9

internal val PillWidth = 240.dp
private val PillHeight = 64.dp
private val ButtonSize = 46.dp
internal val ButtonInset = 9.dp

/** Horizontal room the number has between the two buttons. */
internal val NumberWindowInset = ButtonInset + ButtonSize

private val ButtonLight = Color(0xFF9CCBFF)
private val ButtonDeep = Color(0xFF3B6BFF)

/** Centre of a button in the pill's own coordinates, for effects that start at the tapped button. */
internal fun DrawScope.buttonCenter(side: Side): Offset {
    val inset = (ButtonInset + ButtonSize / 2).toPx()
    return Offset(if (side == Side.Minus) inset else size.width - inset, size.height / 2)
}

@Composable
internal fun QuantityText(value: Int, modifier: Modifier = Modifier) {
    Text(value.toString(), modifier, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
}

/**
 * The "Orbit Mug" quantity card all six stepper demos share. It owns the quantity and the
 * reel's autoplay (+, +, −); each demo hooks its motion in through the modifiers and slots.
 */
@Composable
internal fun StepperScene(
    number: Int,
    title: String,
    caption: String,
    onStep: (side: Side, next: Int) -> Unit,
    pillModifier: Modifier = Modifier,
    trackModifier: Modifier = Modifier,
    decoration: DrawScope.() -> Unit = {},
    buttonModifier: (Side) -> Modifier = { Modifier },
    glyphRotation: (Side) -> Float = { 0f },
    display: @Composable BoxScope.(quantity: Int) -> Unit,
) {
    var quantity by remember { mutableIntStateOf(StartQuantity) }
    val currentOnStep by rememberUpdatedState(onStep)

    fun tap(side: Side) {
        val next = (quantity + side.delta).coerceIn(0, MaxQuantity)
        if (next == quantity) return
        quantity = next
        currentOnStep(side, next)
    }

    LaunchedEffect(Unit) {
        delay(700)
        listOf(Side.Plus, Side.Plus, Side.Minus).forEachIndexed { i, side ->
            if (i > 0) delay(900)
            tap(side)
        }
    }

    MenuStage(number, title, caption) {
        // Not clipped: halos and stretches are allowed to spill past the card.
        Column(
            Modifier
                .width(PillWidth + 40.dp)
                .background(MenuColors.Tile, RoundedCornerShape(24.dp))
                .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Orbit Mug", color = MenuColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Quantity", color = MenuColors.Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Box(pillModifier.size(PillWidth, PillHeight)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .then(trackModifier)
                        .background(MenuColors.Screen, CircleShape)
                        .border(1.dp, MenuColors.CardBorder, CircleShape),
                )
                Canvas(Modifier.matchParentSize()) { decoration() }
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { display(quantity) }
                Side.entries.forEach { side ->
                    StepButton(
                        side = side,
                        enabled = if (side == Side.Plus) quantity < MaxQuantity else quantity > 0,
                        onClick = { tap(side) },
                        rotation = { glyphRotation(side) },
                        modifier = Modifier
                            .align(if (side == Side.Plus) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(horizontal = ButtonInset)
                            .then(buttonModifier(side)),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepButton(
    side: Side,
    enabled: Boolean,
    onClick: () -> Unit,
    rotation: () -> Float,
    modifier: Modifier,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, tween(200), label = "enabled")
    Canvas(
        modifier
            .size(ButtonSize)
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClickLabel = if (side == Side.Plus) "Increase quantity" else "Decrease quantity",
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        drawCircle(ButtonDeep.copy(alpha = 0.28f), radius = size.minDimension / 2 + 3.dp.toPx(), center = center + Offset(0f, 2.dp.toPx()))
        drawCircle(Brush.linearGradient(listOf(ButtonLight, ButtonDeep), Offset.Zero, Offset(size.width, size.height)))
        val arm = size.width * 0.19f
        val stroke = 2.8.dp.toPx()
        rotate(rotation(), center) {
            drawLine(Color.White, center - Offset(arm, 0f), center + Offset(arm, 0f), stroke, StrokeCap.Round)
            if (side == Side.Plus) {
                drawLine(Color.White, center - Offset(0f, arm), center + Offset(0f, arm), stroke, StrokeCap.Round)
            }
        }
    }
}
