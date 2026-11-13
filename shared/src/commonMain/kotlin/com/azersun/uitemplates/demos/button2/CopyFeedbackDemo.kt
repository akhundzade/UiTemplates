package com.azersun.uitemplates.demos.button2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.GlyphIcon
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val Link = "join.app/x7Kq2"
private const val TooltipMillis = 1500L
private val CopyButtonSize = 40.dp
private val CopyButtonInset = 8.dp
private val TailHeight = 6.dp
private val TooltipInk = Color(0xFF14122B)

@Composable
fun CopyFeedbackDemo() {
    val morph = remember { Animatable(0f) }
    val tooltip = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun copy() {
        job?.cancel()
        job = scope.launch {
            launch { morph.animateTo(1f, tween(460, easing = FastOutSlowInEasing)) }
            launch { tooltip.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f)) }
            delay(TooltipMillis)
            launch { tooltip.animateTo(0f, tween(160)) }
            morph.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
    }

    ButtonStage(
        4,
        "Copy Feedback",
        "The copy icon morphs into a ✓ and a \"Copied!\" tooltip pops up.",
        autoplay = { copy() },
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 320.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // A box as wide as the copy button's slot, so the tooltip centres over the button.
            Box(
                Modifier
                    .padding(end = CopyButtonInset)
                    .width(CopyButtonSize),
                contentAlignment = Alignment.Center,
            ) {
                Tooltip(
                    Modifier
                        .wrapContentWidth(unbounded = true)
                        .graphicsLayer {
                            val t = tooltip.value
                            alpha = t.coerceIn(0f, 1f)
                            scaleX = 0.4f + 0.6f * t
                            scaleY = 0.4f + 0.6f * t
                            translationY = (1f - t) * 6.dp.toPx()
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MenuColors.Tile)
                    .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(16.dp))
                    .padding(start = 16.dp, end = CopyButtonInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.Link, MenuColors.Muted, size = 18.dp)
                Text(
                    Link,
                    color = MenuColors.Text,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                )
                Box(
                    Modifier
                        .size(CopyButtonSize)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Copy link", role = Role.Button, onClick = { copy() })
                        .drawWithCache {
                            val w = size.width
                            val corner = CornerRadius(w * 0.3f)
                            val stroke = Stroke(w * 0.05f)
                            val small = w * 0.34f
                            val smallCorner = CornerRadius(w * 0.08f)
                            val frontRest = Offset(w * 0.4f, w * 0.4f)
                            val backRest = Offset(w * 0.27f, w * 0.27f)
                            onDrawBehind {
                                val m = morph.value
                                drawRoundRect(MenuColors.TileLight, cornerRadius = corner, alpha = 1f - smoothstep(0.1f, 0.5f, m))
                                // The back square slides onto the front one and fades into it.
                                val slide = smoothstep(0f, 0.4f, m)
                                val backAlpha = 1f - smoothstep(0.2f, 0.45f, m)
                                if (backAlpha > 0f) {
                                    val at = backRest + (frontRest - backRest) * slide
                                    drawRoundRect(MenuColors.Text, at, Size(small, small), smallCorner, stroke, backAlpha)
                                }
                                // The front square grows into the whole button and fills blue.
                                val grow = smoothstep(0.1f, 0.6f, m)
                                val side = mix(small, w, grow)
                                val topLeft = frontRest * (1f - grow)
                                val radius = CornerRadius(mix(smallCorner.x, corner.x, grow))
                                drawRoundRect(MenuColors.Blue, topLeft, Size(side, side), radius, alpha = grow)
                                drawRoundRect(
                                    lerp(MenuColors.Text, MenuColors.Blue, grow),
                                    topLeft,
                                    Size(side, side),
                                    radius,
                                    stroke,
                                )
                                val inset = w * 0.22f
                                drawCheck(
                                    smoothstep(0.5f, 1f, m),
                                    Color.White,
                                    Offset(inset, inset),
                                    w - inset * 2,
                                    w * 0.075f,
                                )
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun Tooltip(modifier: Modifier = Modifier) {
    Box(
        modifier
            .drawWithCache {
                val tail = TailHeight.toPx()
                val bubble = Size(size.width, size.height - tail)
                val arrow = Path().apply {
                    moveTo(size.width / 2 - tail, bubble.height - 1f)
                    lineTo(size.width / 2, size.height)
                    lineTo(size.width / 2 + tail, bubble.height - 1f)
                    close()
                }
                onDrawBehind {
                    drawRoundRect(Color.White, size = bubble, cornerRadius = CornerRadius(bubble.height / 2))
                    drawPath(arrow, Color.White)
                }
            }
            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 7.dp + TailHeight),
    ) {
        Text("Copied!", color = TooltipInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
