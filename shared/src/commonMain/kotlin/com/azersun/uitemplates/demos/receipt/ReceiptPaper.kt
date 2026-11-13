package com.azersun.uitemplates.demos.receipt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

internal object ReceiptColors {
    val Cream = Color(0xFFEFEBE4)
    val Ink = Color(0xFF1F1D19)
    val Muted = Color(0xFF8A8478)
    val Olive = Color(0xFF3E4326)
    val OlivePaid = Color(0xFF4E6834)
    val OnOlive = Color(0xFFF6F2E9)
    val Panel = Color(0xFFE6E1D7)
    val PanelBorder = Color(0xFFD9D2C4)
    val Paper = Color(0xFFFCFAF5)
    val PaperShade = Color(0xFFF1EDE2)
    val PaperInk = Color(0xFF2D2A25)
    val PaperShadow = Color(0xFF3A2E1C)
    val Slot = Color(0xFF090908)
    val Amber = Color(0xFFFFB02E)
    val AmberDim = Color(0xFF6B4E1F)
    val Green = Color(0xFF5BE38A)
    val Live = Color(0xFFC4573A)
}

internal val ReceiptWidth = 248.dp

/** Blank paper below the last printed line, so a pulled receipt never shows its bottom edge above the slot. */
internal val PaperTail = 36.dp
internal val ToothWidth = 8.dp
internal val ToothDepth = 3.5.dp

private val PaperBrush = Brush.verticalGradient(listOf(ReceiptColors.Paper, ReceiptColors.PaperShade))

/** Darkens the long edges a touch, as if the paper still remembers being rolled. */
private val EdgeShade = Brush.horizontalGradient(
    0f to Color(0x12000000),
    0.07f to Color.Transparent,
    0.93f to Color.Transparent,
    1f to Color(0x12000000),
)

private val Mono = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    lineHeight = 13.sp,
    color = ReceiptColors.PaperInk.copy(alpha = 0.9f),
)

private class LineItem(val quantity: Int, val name: String, val price: String)

private val Items = listOf(
    LineItem(1, "Ethiopia Guji 250g", "€16.50"),
    LineItem(2, "Ceramic cup, ash", "€96.00"),
    LineItem(1, "V60 dripper, clay", "€32.00"),
    LineItem(1, "Filter papers x100", "€6.50"),
)

/** Alternating bar and gap widths in units; odd length so the code starts and ends with a bar. */
private val BarUnits: IntArray = Random(2048).let { random ->
    IntArray(61) { i -> if (i % 2 == 0) 1 + random.nextInt(3) else 1 + random.nextInt(2) }
}
private val BarUnitTotal = BarUnits.sum()

/** Paper with a zigzag torn top edge, and a torn bottom edge once it has been ripped off. */
private class PaperShape(private val tornBottom: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        with(density) {
            path.paperOutline(size.width, size.height, ToothWidth.toPx(), ToothDepth.toPx(), tornBottom)
        }
        return Outline.Generic(path)
    }
}

private val IntactPaper = PaperShape(tornBottom = false)
private val TornPaper = PaperShape(tornBottom = true)

/** Shared with the stub left in the printer, so its teeth line up with the next receipt's. */
internal fun Path.paperOutline(width: Float, height: Float, tooth: Float, depth: Float, tornBottom: Boolean) {
    val teeth = max(1, (width / tooth).roundToInt())
    val step = width / teeth
    moveTo(0f, depth)
    for (i in 0 until teeth) {
        lineTo(step * (i + 0.5f), 0f)
        lineTo(step * (i + 1), depth)
    }
    if (tornBottom) {
        lineTo(width, height - depth)
        for (i in teeth - 1 downTo 0) {
            lineTo(step * (i + 0.5f), height)
            lineTo(step * i, height - depth)
        }
    } else {
        lineTo(width, height)
        lineTo(0f, height)
    }
    close()
}

@Composable
internal fun ReceiptPaper(torn: Boolean, modifier: Modifier = Modifier) {
    val shape = if (torn) TornPaper else IntactPaper
    Column(
        modifier
            .width(ReceiptWidth)
            .shadow(6.dp, shape, ambientColor = ReceiptColors.PaperShadow, spotColor = ReceiptColors.PaperShadow)
            .background(PaperBrush, shape)
            .background(EdgeShade, shape)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = PaperTail),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BeanLogo(Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text("KŌBŌ", style = Mono.copy(fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp))
        Text("COFFEE ROASTERS · LISBON", style = Mono.copy(fontSize = 8.5.sp, letterSpacing = 1.sp))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ORDER #2048", style = Mono)
            Text("05 NOV 2026", style = Mono)
            Text("14:32", style = Mono)
        }
        DashedRule()
        Items.forEach { item ->
            Row(Modifier.fillMaxWidth()) {
                Text("${item.quantity}", Modifier.width(16.dp), style = Mono)
                Text(item.name, Modifier.weight(1f), style = Mono, maxLines = 1)
                Text(item.price, style = Mono)
            }
        }
        DashedRule()
        AmountRow("SUBTOTAL", "€151.00")
        AmountRow("SHIPPING", "€4.90")
        AmountRow("VAT 23% incl.", "€29.15")
        Spacer(Modifier.height(3.dp))
        AmountRow("TOTAL", "€155.90", Mono.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold))
        DashedRule()
        AmountRow("LUMEN •••• 4242", "ONLINE")
        AmountRow("APPROVED", "AUTH 40668")
        Spacer(Modifier.height(8.dp))
        Barcode(Modifier.fillMaxWidth(0.84f).height(26.dp))
        Text("2048 0511 2026 1432", style = Mono.copy(fontSize = 7.5.sp, lineHeight = 11.sp, letterSpacing = 1.5.sp))
        Spacer(Modifier.height(4.dp))
        Text("THANK YOU · SEE YOU SOON", style = Mono.copy(letterSpacing = 0.5.sp))
    }
}

@Composable
private fun AmountRow(label: String, amount: String, style: TextStyle = Mono) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = style)
        Text(amount, style = style)
    }
}

@Composable
private fun DashedRule() {
    Canvas(Modifier.fillMaxWidth().padding(vertical = 5.dp).height(1.dp)) {
        val dash = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val y = size.height / 2
        var x = 0f
        while (x < size.width) {
            drawLine(Mono.color, Offset(x, y), Offset(minOf(x + dash, size.width), y), size.height)
            x += dash + gap
        }
    }
}

@Composable
private fun Barcode(modifier: Modifier) {
    Canvas(modifier) {
        val unit = size.width / BarUnitTotal
        var x = 0f
        BarUnits.forEachIndexed { i, units ->
            val w = units * unit
            if (i % 2 == 0) drawRect(ReceiptColors.PaperInk, Offset(x, 0f), Size(w, size.height))
            x += w
        }
    }
}

@Composable
private fun BeanLogo(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width * 0.56f
        val h = size.height * 0.88f
        rotate(-32f) {
            drawOval(ReceiptColors.PaperInk, Offset(center.x - w / 2, center.y - h / 2), Size(w, h))
            val crease = Path().apply {
                moveTo(center.x + w * 0.04f, center.y - h * 0.44f)
                cubicTo(
                    center.x - w * 0.34f, center.y - h * 0.12f,
                    center.x + w * 0.34f, center.y + h * 0.12f,
                    center.x - w * 0.04f, center.y + h * 0.44f,
                )
            }
            drawPath(crease, ReceiptColors.Paper, style = Stroke(w * 0.13f, cap = StrokeCap.Round))
        }
    }
}
