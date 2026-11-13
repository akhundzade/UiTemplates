package com.azersun.uitemplates.demos.receipt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val PrinterHeight = 78.dp

/** Distance from the printer's top edge down to the paper slot. */
internal val SlotFromTop = 18.dp

private val FloorGap = 8.dp
private val LabelStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 7.5.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 2.5.sp,
    color = Color(0xFF5F5B54),
)

/**
 * The black thermal printer: a rounded body with a recessed slot band, vents and a status LED.
 * The slot itself is drawn by the paper bay, on top of the paper.
 */
@Composable
internal fun ThermalPrinter(led: () -> Color, ledGlow: () -> Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(PrinterHeight)
            .drawWithCache {
                val bodyHeight = size.height - FloorGap.toPx()
                val corner = CornerRadius(16.dp.toPx())
                val body = Brush.verticalGradient(
                    listOf(Color(0xFF3B3935), Color(0xFF242320), Color(0xFF161513)),
                    startY = 0f,
                    endY = bodyHeight,
                )
                val floorCenter = Offset(size.width / 2, bodyHeight)
                val floorRadius = size.width * 0.56f
                val floor = Brush.radialGradient(
                    listOf(Color(0x47000000), Color.Transparent),
                    center = floorCenter,
                    radius = floorRadius,
                )
                val slot = SlotFromTop.toPx()
                val band = 13.dp.toPx()
                val inset = 12.dp.toPx()
                val hairline = 1.dp.toPx()
                val ledCenter = Offset(size.width - 26.dp.toPx(), bodyHeight * 0.68f)
                val ledRadius = 3.5.dp.toPx()
                val vent = 2.dp.toPx()
                onDrawBehind {
                    scale(1f, 0.16f, pivot = floorCenter) { drawCircle(floor, floorRadius, floorCenter) }
                    drawRoundRect(body, size = Size(size.width, bodyHeight), cornerRadius = corner)
                    drawLine(
                        Color.White.copy(alpha = 0.14f),
                        Offset(corner.x, hairline),
                        Offset(size.width - corner.x, hairline),
                        hairline,
                    )
                    drawRoundRect(
                        Color(0xFF121110),
                        Offset(inset, slot - band / 2),
                        Size(size.width - inset * 2, band),
                        CornerRadius(band / 2),
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.07f),
                        Offset(inset + band / 2, slot + band / 2 + hairline),
                        Offset(size.width - inset - band / 2, slot + band / 2 + hairline),
                        hairline,
                    )
                    val seam = bodyHeight * 0.48f
                    drawLine(Color.Black.copy(alpha = 0.4f), Offset(inset, seam), Offset(size.width - inset, seam), hairline)
                    drawLine(Color.White.copy(alpha = 0.04f), Offset(inset, seam + hairline), Offset(size.width - inset, seam + hairline), hairline)
                    repeat(3) { i ->
                        val y = ledCenter.y + (i - 1) * 5.dp.toPx()
                        drawLine(Color(0xFF0E0D0C), Offset(20.dp.toPx(), y), Offset(42.dp.toPx(), y), vent, StrokeCap.Round)
                    }

                    val color = led()
                    val glow = ledGlow()
                    drawCircle(color.copy(alpha = 0.14f * glow), ledRadius * 4.2f, ledCenter)
                    drawCircle(color.copy(alpha = 0.3f * glow), ledRadius * 2.2f, ledCenter)
                    drawCircle(Color.Black.copy(alpha = 0.6f), ledRadius + hairline, ledCenter)
                    drawCircle(color.copy(alpha = 0.35f + 0.65f * glow), ledRadius, ledCenter)
                }
            },
    ) {
        Text("KŌBŌ · TP-80", Modifier.align(Alignment.BottomCenter).padding(bottom = FloorGap + 9.dp), style = LabelStyle)
    }
}
