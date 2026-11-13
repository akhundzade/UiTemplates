package com.azersun.uitemplates.demos.darkmode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class CardPalette(
    val dark: Boolean,
    val card: Color,
    val panel: Color,
    val divider: Color,
    val text: Color,
    val muted: Color,
    val value: Color,
    val iconBackground: Color,
    val icon: Color,
)

internal val DarkCard = CardPalette(
    dark = true,
    card = Color(0xFF1C1A38),
    panel = Color(0xFF262349),
    divider = Color(0xFF322E5C),
    text = Color(0xFFF2F0FF),
    muted = Color(0xFF8E8AB8),
    value = Color(0xFFA99CFF),
    iconBackground = Color(0xFF2E2A58),
    icon = Color(0xFFC9C2FF),
)

internal val LightCard = CardPalette(
    dark = false,
    card = Color(0xFFF3F1FB),
    panel = Color(0xFFE6E3F4),
    divider = Color(0xFFD8D4EC),
    text = Color(0xFF1A1736),
    muted = Color(0xFF6E6A8E),
    value = Color(0xFF6A4DF4),
    iconBackground = Color(0xFFE6E3F4),
    icon = Color(0xFFF5A524),
)

internal val CardPadding = 16.dp
internal val ToggleSize = 38.dp

/**
 * The profile card from the reel. [rays] scales the sun's rays (0 = tucked in), [iconRotation] and
 * [iconDropY] move only the sun/moon glyph, and [onRowTops] reports the two settings rows' tops in
 * root coordinates so a transition can wipe the card row by row.
 */
@Composable
internal fun ProfileCard(
    palette: CardPalette,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    rays: Float = 1f,
    iconRotation: Float = 0f,
    iconDropY: Float = 0f,
    onRowTops: ((Int, Float) -> Unit)? = null,
) {
    Column(
        modifier
            .width(320.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.card)
            .padding(CardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF8B6CFF), Color(0xFFFF6FB1)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("LS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Lumi Sato", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("@lumi.studio", color = palette.muted, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .size(ToggleSize)
                    .clip(CircleShape)
                    .background(palette.iconBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation
                            translationY = iconDropY
                        },
                ) {
                    if (palette.dark) drawMoon(palette.icon, palette.iconBackground) else drawSun(palette.icon, rays)
                }
            }
        }
        Column(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.panel),
        ) {
            SettingRow(
                palette = palette,
                iconColor = Color(0xFF5B8CFF),
                label = "Appearance",
                modifier = Modifier.reportTop(0, onRowTops),
                glyph = { drawHalfCircle(it) },
            ) {
                Text(if (palette.dark) "Dark" else "Light", color = palette.value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("  ›", color = palette.value, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
            SettingRow(
                palette = palette,
                iconColor = Color(0xFFFF6B8A),
                label = "Notifications",
                modifier = Modifier.reportTop(1, onRowTops),
                glyph = { drawBell(it) },
            ) {
                Switch(palette)
            }
        }
    }
}

private fun Modifier.reportTop(index: Int, onRowTops: ((Int, Float) -> Unit)?) =
    if (onRowTops == null) this else onGloballyPositioned { onRowTops(index, it.positionInRoot().y) }

@Composable
private fun SettingRow(
    palette: CardPalette,
    iconColor: Color,
    label: String,
    modifier: Modifier,
    glyph: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) { glyph(iconColor) }
        }
        Text(label, color = palette.text, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun Switch(palette: CardPalette) {
    Box(
        Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF2DD4A7)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (palette.dark) Color(0xFFF2F0FF) else Color.White),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSun(color: Color, rays: Float) {
    val c = center
    val w = size.width
    drawCircle(color, radius = w * 0.22f, center = c)
    if (rays <= 0.01f) return
    repeat(8) { i ->
        val a = i * PI.toFloat() / 4
        val dir = Offset(cos(a), sin(a))
        val inner = w * 0.32f
        val outer = inner + w * 0.15f * rays
        drawLine(color.copy(alpha = rays.coerceIn(0f, 1f)), c + dir * inner, c + dir * outer, w * 0.08f, StrokeCap.Round)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoon(color: Color, background: Color) {
    val w = size.width
    drawCircle(color, radius = w * 0.4f, center = center)
    drawCircle(background, radius = w * 0.34f, center = center + Offset(w * 0.2f, -w * 0.16f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHalfCircle(color: Color) {
    val w = size.width
    drawCircle(color, radius = w * 0.42f, center = center, style = Stroke(w * 0.12f))
    drawArc(color, 90f, 180f, true, Offset(w * 0.08f, w * 0.08f), Size(w * 0.84f, w * 0.84f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBell(color: Color) {
    val w = size.width
    drawArc(color, 180f, 180f, true, Offset(w * 0.18f, w * 0.12f), Size(w * 0.64f, w * 0.7f))
    drawRect(color, Offset(w * 0.18f, w * 0.46f), Size(w * 0.64f, w * 0.26f))
    drawRect(color, Offset(w * 0.08f, w * 0.68f), Size(w * 0.84f, w * 0.1f))
    drawCircle(color, w * 0.1f, Offset(w * 0.5f, w * 0.88f))
}
