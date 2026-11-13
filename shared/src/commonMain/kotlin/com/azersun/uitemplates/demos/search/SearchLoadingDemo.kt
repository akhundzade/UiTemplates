package com.azersun.uitemplates.demos.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.StaggerIn
import kotlinx.coroutines.delay

private class Product(val name: String, val rating: String, val note: String?, val color: Color)

private val Catalog = listOf(
    Product("Arc Desk Lamp", "4.8", "Free shipping", MenuColors.Violet),
    Product("Mini Clip Lamp", "4.7", null, MenuColors.Cyan),
    Product("Glow Bulb Lamp", "4.9", null, MenuColors.Yellow),
    Product("Oak Side Table", "4.6", "Free shipping", MenuColors.Pink),
    Product("Wool Rug", "4.5", null, MenuColors.Blue),
    Product("Lounge Chair", "4.8", null, MenuColors.Red),
)

private const val LoadingMillis = 1200L
private val RowShape = RoundedCornerShape(14.dp)
private val IconShape = RoundedCornerShape(10.dp)

private fun search(query: String): List<Product> {
    val words = query.lowercase().split(' ').filter { it.length >= 3 }
    return Catalog.filter { product -> words.any { product.name.lowercase().contains(it) } }.take(3)
}

@Composable
fun SearchLoadingDemo() {
    val autoplay = rememberAutoplay()
    val query = remember { Query() }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Product>?>(null) }
    var searches by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    val press = remember { Animatable(1f) }

    fun submit() {
        if (query.text.isBlank() || loading) return
        focusManager.clearFocus()
        results = null
        loading = true
        searches++
    }

    LaunchedEffect(searches, loading) {
        if (!loading) return@LaunchedEffect
        press.animateTo(0.86f, tween(80))
        press.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 600f))
        delay(LoadingMillis)
        results = search(query.text)
        loading = false
    }

    AutoplayEffect(autoplay) {
        loading = false
        results = null
        autoplay.focus = false
        query.set("")
        delay(AutoplayDelayMillis)
        autoplay.focus = true
        query.type("desk lamp")
        delay(350)
        autoplay.focus = false
        submit()
    }

    SearchStage(6, "Search Loading", "Enter: the icon spins, skeletons shimmer, results fade in.", autoplay) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 20.dp, end = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
        ) {
            SearchField(
                query = query,
                placeholder = "Search products",
                glowing = autoplay.focus,
                caret = autoplay.focus,
                onSearch = ::submit,
                leading = { SearchOrSpin(loading) },
                trailing = {
                    Box(
                        Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = press.value
                                scaleY = press.value
                            }
                            .clip(CircleShape)
                            .background(SearchColors.Blue)
                            .clickable(onClick = ::submit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(18.dp)) { drawArrowRight(Color.White) }
                    }
                },
            )
            Spacer(Modifier.height(18.dp))
            val found = results
            when {
                loading -> SkeletonList()
                found == null -> Unit
                found.isEmpty() -> Text(
                    "No results for “${query.text.trim()}”",
                    Modifier.padding(start = 18.dp),
                    color = SearchColors.Muted,
                    fontSize = 13.sp,
                )
                else -> key(searches) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        found.forEachIndexed { i, product ->
                            StaggerIn(i, visible = true, offsetX = 0f, offsetY = 12f, baseDelayMillis = 40, stepMillis = 120) {
                                ResultRow(product)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The magnifier hands over to a spinning arc while a search runs. */
@Composable
private fun SearchOrSpin(loading: Boolean) {
    val spin by animateFloatAsState(if (loading) 1f else 0f, tween(180), label = "spin")
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        MagnifierIcon(
            SearchColors.Muted,
            Modifier.graphicsLayer {
                alpha = 1f - spin
                scaleX = 1f - spin * 0.4f
                scaleY = 1f - spin * 0.4f
            },
        )
        if (loading) {
            val turn by rememberInfiniteTransition(label = "spinner").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing)),
                label = "turn",
            )
            Canvas(
                Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        rotationZ = turn
                        alpha = spin
                    },
            ) {
                val stroke = 2.2.dp.toPx()
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, SearchColors.Violet, SearchColors.Cyan)),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun SkeletonList() {
    val sweep = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sweep",
    )
    Column(Modifier.shimmer(sweep), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { i ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RowShape)
                    .background(SearchColors.Row.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).background(SearchColors.RowLight, IconShape))
                Column(Modifier.padding(start = 12.dp)) {
                    Box(Modifier.fillMaxWidth(0.62f - i * 0.08f).height(11.dp).background(SearchColors.RowLight, Pill))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.4f).height(8.dp).background(SearchColors.RowLight.copy(alpha = 0.7f), Pill))
                }
            }
        }
    }
}

/** A soft highlight band that sweeps across, painted only where the skeleton already has pixels. */
private fun Modifier.shimmer(progress: State<Float>): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val band = size.width * 0.5f
        val highlight = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
            startX = 0f,
            endX = band,
        )
        onDrawWithContent {
            drawContent()
            translate(left = -band + (size.width + band) * progress.value) {
                drawRect(highlight, size = Size(band, size.height), blendMode = BlendMode.SrcAtop)
            }
        }
    }

@Composable
private fun ResultRow(product: Product) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(SearchColors.Row)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(Brush.linearGradient(listOf(product.color, product.color.copy(alpha = 0.6f))), IconShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width
                val shade = Path().apply {
                    moveTo(w * 0.3f, w * 0.12f)
                    lineTo(w * 0.7f, w * 0.12f)
                    lineTo(w * 0.86f, w * 0.46f)
                    lineTo(w * 0.14f, w * 0.46f)
                    close()
                }
                drawPath(shade, Color.White)
                drawLine(Color.White, Offset(w * 0.5f, w * 0.46f), Offset(w * 0.5f, w * 0.84f), w * 0.1f, StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.3f, w * 0.88f), Offset(w * 0.7f, w * 0.88f), w * 0.1f, StrokeCap.Round)
            }
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(product.name, color = SearchColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("★", color = MenuColors.Yellow, fontSize = 12.sp)
                Text(" ${product.rating}", color = SearchColors.Text, fontSize = 12.sp)
                if (product.note != null) Text(" · ${product.note}", color = SearchColors.Muted, fontSize = 12.sp)
            }
        }
    }
}
