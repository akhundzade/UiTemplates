package com.azersun.uitemplates.demos.clinic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.common.countUp
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class Page(val icon: ClinicIcon, val title: String) {
    Today(ClinicIcon.Home, "Good morning, Nadia"),
    Inbox(ClinicIcon.Chat, "Inbox"),
    Schedule(ClinicIcon.Calendar, "Schedule"),
    Campaigns(ClinicIcon.Megaphone, "Campaigns"),
}

private val RailItem = 48.dp
private val RailGap = 10.dp

@Composable
fun PageSwitchDemo() {
    var page by remember { mutableStateOf(Page.Today) }
    var autoplay by remember { mutableStateOf(true) }

    LaunchedEffect(autoplay) {
        while (autoplay) {
            delay(2600)
            if (autoplay) page = Page.entries[(page.ordinal + 1) % Page.entries.size]
        }
    }

    Row(Modifier.fillMaxSize().background(Clinic.Background)) {
        NavRail(page, onSelect = {
            autoplay = false
            page = it
        })
        AnimatedContent(
            targetState = page,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            // Fade through the background, as in the reel: out quickly, then the new page in.
            transitionSpec = { fadeIn(tween(260, delayMillis = 140)) togetherWith fadeOut(tween(140)) },
            label = "page",
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Text("Northside Family Clinic", color = Clinic.Muted, fontSize = 10.sp)
                Text(current.title, color = Clinic.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                when (current) {
                    Page.Today -> TodayPage()
                    Page.Inbox -> InboxPage()
                    Page.Schedule -> SchedulePage()
                    Page.Campaigns -> CampaignsPage()
                }
            }
        }
    }
}

@Composable
private fun NavRail(page: Page, onSelect: (Page) -> Unit) {
    val indicator by animateDpAsState(
        (RailItem + RailGap) * page.ordinal,
        spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "indicator",
    )
    Column(
        Modifier
            .width(68.dp)
            .fillMaxHeight()
            .background(Clinic.Sidebar)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Clinic.Green), contentAlignment = Alignment.Center) {
            Text("N", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.padding(top = 24.dp)) {
            Box(
                Modifier
                    .offset(y = indicator)
                    .size(RailItem)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(RailGap)) {
                Page.entries.forEach { item ->
                    Box(
                        Modifier.size(RailItem).clip(RoundedCornerShape(14.dp)).clickable { onSelect(item) },
                        contentAlignment = Alignment.Center,
                    ) {
                        ClinicIconView(item.icon, if (item == page) Color.White else Color.White.copy(alpha = 0.5f))
                        if (item == Page.Inbox) {
                            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiRow(vararg items: Pair<String, Int>, suffix: List<String> = emptyList()) {
    val started = rememberStarted()
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, (label, value) ->
            Rise(i, Modifier.weight(1f), stepMillis = 70) {
                KpiTile(label, "${countUp(if (started) value else 0, 900, delayMillis = 120)}${suffix.getOrElse(i) { "" }}", Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TodayPage() {
    KpiRow("Appointments" to 81, "Waiting" to 5, "No-shows" to 4, suffix = listOf("", "", "%"))
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Hiroshi Patel", "Marcus Uddin", "Valentina Uddin", "Nadia Gupta", "Valentina Wang").forEachIndexed { i, name ->
            Rise(i + 2, stepMillis = 60) { SkeletonRow(name, if (i < 2) "Check in" else "Confirmed") }
        }
    }
}

@Composable
private fun InboxPage() {
    KpiRow("Unread" to 4, "Waiting on us" to 6, "Median reply" to 5, suffix = listOf("", "", "m"))
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Amina Khan", "Ana Souza", "Noah Bennett", "Marcus Webb", "Elena Rossi").forEachIndexed { i, name ->
            Rise(i + 2, stepMillis = 60) { SkeletonRow(name, if (i == 2) "Urgent" else "Booking") }
        }
    }
}

@Composable
private fun CampaignsPage() {
    KpiRow("Sent" to 6622, "Read rate" to 91, "Bookings" to 113, suffix = listOf("", "%", ""))
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Well-child check-ups", "Annual recall · Q4", "Holiday hours").forEachIndexed { i, title ->
            Rise(i + 2, stepMillis = 80) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Clinic.Card).border(1.dp, Clinic.Line, RoundedCornerShape(14.dp)).padding(12.dp),
                ) {
                    Text(title, color = Clinic.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F8F3)))
                }
            }
        }
    }
}

@Composable
private fun SchedulePage() {
    KpiRow("This week" to 421, "Booked" to 57, "Open slots" to 25, suffix = listOf("", "%", ""))
    val blocks = remember {
        val random = Random(7)
        List(5) { day -> List(9) { slot -> random.nextFloat() < 0.62f && !(slot == 4) } }
    }
    val colors = listOf(Clinic.Green, Clinic.Blue, Color(0xFF7B5BD6), Clinic.Orange)
    Row(
        Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Clinic.Card).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("MON", "TUE", "WED", "THU", "FRI").forEachIndexed { day, label ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Clinic.Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Text("${12 + day}", color = Clinic.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                blocks[day].forEachIndexed { slot, booked ->
                    if (slot == 4) {
                        Text("LUNCH", color = Clinic.Muted, fontSize = 7.sp, modifier = Modifier.height(22.dp).padding(top = 6.dp))
                    } else {
                        PopBlock(day * 2 + slot, if (booked) colors[(day + slot) % colors.size] else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun PopBlock(order: Int, color: Color?) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(160L + order * 28L)
        pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(5.dp))
            .background(color?.copy(alpha = 0.22f) ?: Clinic.Background)
            .border(1.dp, color?.copy(alpha = 0.5f) ?: Color.Transparent, RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun SkeletonRow(name: String, action: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Clinic.Card).border(1.dp, Clinic.Line, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name.split(" ").joinToString("") { it.take(1) }, Clinic.Green)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(name, color = Clinic.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.padding(top = 4.dp).width(90.dp).height(6.dp).clip(RoundedCornerShape(50)).background(Clinic.Line))
        }
        Pill(action, if (action == "Urgent") Color(0xFFD64545) else Clinic.Green, if (action == "Urgent") Color(0xFFFDE4E4) else Clinic.GreenSoft)
    }
}
