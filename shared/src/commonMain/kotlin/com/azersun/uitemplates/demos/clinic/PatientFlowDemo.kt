package com.azersun.uitemplates.demos.clinic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.common.countUp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Room { Waiting, Consult }

private data class Patient(
    val id: Int,
    val name: String,
    val time: String,
    val clinician: String,
    val color: Color,
    val waitMinutes: Int,
    val room: Room,
    val consultRoom: Int = 0,
) {
    val initials: String get() = name.split(" ").joinToString("") { it.take(1) }
    val firstName: String get() = name.substringBefore(" ")
}

private fun initialPatients() = listOf(
    Patient(1, "Adaeze Wood", "10:00 AM", "Dr. Okafor", Color(0xFFE07B1F), 26, Room.Waiting),
    Patient(2, "Camila Hill", "10:15 AM", "Dr. Silva", Color(0xFF1F8A5B), 19, Room.Waiting),
    Patient(3, "Isla Torres", "10:30 AM", "Dr. Rahman", Color(0xFF7B5BD6), 7, Room.Waiting),
    Patient(4, "Salma Kowalski", "10:45 AM", "Nurse Berg", Color(0xFF2D7FDB), 3, Room.Waiting),
    Patient(5, "Helen Ramirez", "9:30 AM", "Dr. Silva", Color(0xFFD64545), 0, Room.Consult, 4),
    Patient(6, "Marco Lopez", "9:45 AM", "Nurse Berg", Color(0xFF1A9DB8), 0, Room.Consult, 1),
)

private class Move(val patient: Patient, val index: Int)

@Composable
fun PatientFlowDemo() {
    var generation by remember { mutableIntStateOf(0) }
    key(generation) {
        PatientFlowBoard(onReplay = { generation++ })
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PatientFlowBoard(onReplay: () -> Unit) {
    val patients = remember { mutableStateListOf<Patient>().apply { addAll(initialPatients()) } }
    var seen by remember { mutableIntStateOf(21) }
    var highlightId by remember { mutableStateOf<Int?>(null) }
    var leavingId by remember { mutableStateOf<Int?>(null) }
    var lastMove by remember { mutableStateOf<Move?>(null) }
    val scope = rememberCoroutineScope()
    val started = rememberStarted()

    fun callIn(patient: Patient) {
        val index = patients.indexOfFirst { it.id == patient.id }
        if (index < 0) return
        patients.removeAt(index)
        val room = (1..4).first { r -> patients.none { it.room == Room.Consult && it.consultRoom == r } }
        patients.add(0, patient.copy(room = Room.Consult, consultRoom = room))
        lastMove = Move(patient, index)
        highlightId = patient.id
        scope.launch {
            delay(1400)
            if (highlightId == patient.id) highlightId = null
        }
        scope.launch {
            delay(3200)
            if (lastMove?.patient?.id == patient.id) lastMove = null
        }
    }

    fun undo() {
        val move = lastMove ?: return
        patients.removeAll { it.id == move.patient.id }
        patients.add(move.index.coerceAtMost(patients.size), move.patient)
        lastMove = null
    }

    fun finish(patient: Patient) {
        if (leavingId != null) return
        leavingId = patient.id
        scope.launch {
            delay(320)
            patients.removeAll { it.id == patient.id }
            leavingId = null
            seen++
        }
    }

    LaunchedEffect(Unit) {
        delay(1100)
        patients.firstOrNull { it.room == Room.Waiting }?.let { callIn(it) }
        delay(3800)
        // Finish the patient just called in, so the exit happens on screen.
        patients.firstOrNull { it.room == Room.Consult }?.let { finish(it) }
    }

    val cards = remember { mutableMapOf<Int, @Composable (Patient, Modifier) -> Unit>() }
    fun cardFor(id: Int) = cards.getOrPut(id) {
        movableContentOf { patient: Patient, modifier: Modifier ->
            PatientCard(
                patient = patient,
                highlighted = highlightId == patient.id,
                leaving = leavingId == patient.id,
                modifier = modifier,
                onAction = { if (patient.room == Room.Waiting) callIn(patient) else finish(patient) },
            )
        }
    }

    val waiting = patients.filter { it.room == Room.Waiting }
    val consult = patients.filter { it.room == Room.Consult }
    val moveSpring = BoundsTransform { _, _ -> spring(dampingRatio = 0.82f, stiffness = 260f) }

    Box(Modifier.fillMaxSize().background(Clinic.Background)) {
        LookaheadScope {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Saturday, October 17 · Northside Family Clinic", color = Clinic.Muted, fontSize = 10.sp)
                        Text("Good morning, Nadia", color = Clinic.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    ReplayChip(onClick = onReplay)
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiTile("Appointments", "${countUp(if (started) 81 else 0)}", Modifier.weight(1f))
                    KpiTile("Waiting room", "${countUp(if (started) waiting.size else 0, 500)}", Modifier.weight(1f)) {
                        Pill("avg 8 min", Clinic.Green, Clinic.GreenSoft)
                    }
                    KpiTile("Seen today", "${countUp(if (started) seen else 0, 600)}", Modifier.weight(1f))
                }

                SectionHeader("Waiting room", waiting.size, Clinic.Orange, Modifier.padding(top = 18.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    waiting.forEach { p -> key(p.id) { cardFor(p.id)(p, Modifier.animateBounds(this@LookaheadScope, boundsTransform = moveSpring)) } }
                    if (waiting.isEmpty()) Text("No one waiting", color = Clinic.Muted, fontSize = 12.sp)
                }

                SectionHeader("In consult", consult.size, Clinic.Blue, Modifier.padding(top = 18.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    consult.forEach { p -> key(p.id) { cardFor(p.id)(p, Modifier.animateBounds(this@LookaheadScope, boundsTransform = moveSpring)) } }
                }
                Spacer(Modifier.height(80.dp))
            }
        }

        AnimatedVisibility(
            visible = lastMove != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp),
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 420f)) { it * 2 } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it * 2 } + fadeOut(tween(220)),
        ) {
            // Keep the last text while the toast slides out.
            var shown by remember { mutableStateOf<Move?>(null) }
            lastMove?.let { shown = it }
            val move = shown
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Clinic.Text)
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ticks(1, Clinic.GreenSoft)
                Text(
                    if (move != null) "${move.patient.firstName} is with ${move.patient.clinician}" else "",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                )
                Text(
                    "Undo",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { undo() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PatientCard(patient: Patient, highlighted: Boolean, leaving: Boolean, modifier: Modifier, onAction: () -> Unit) {
    val inConsult = patient.room == Room.Consult
    val background by animateColorAsState(if (highlighted) Clinic.GreenSoft else Clinic.Card, tween(500), label = "flash")
    val accent by animateColorAsState(
        when {
            inConsult -> Clinic.Blue
            patient.waitMinutes > 15 -> Clinic.Orange
            else -> Color.Transparent
        },
        tween(400),
        label = "accent",
    )
    val exit by animateFloatAsState(if (leaving) 0f else 1f, tween(300), label = "exit")
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = exit
                scaleX = 0.92f + 0.08f * exit
                scaleY = 0.92f + 0.08f * exit
            }
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, Clinic.Line, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(58.dp).background(accent))
        Row(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(patient.initials, patient.color)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(patient.name, color = Clinic.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${patient.time} · ${patient.clinician}", color = Clinic.Muted, fontSize = 10.sp, maxLines = 1)
                Box(Modifier.padding(top = 3.dp)) {
                    if (inConsult) {
                        Pill("Room ${patient.consultRoom}", Clinic.Blue, Clinic.BlueSoft)
                    } else {
                        val late = patient.waitMinutes > 15
                        Pill("${patient.waitMinutes} min", if (late) Clinic.Orange else Clinic.Muted, if (late) Clinic.OrangeSoft else Clinic.Line)
                    }
                }
            }
            ClinicButton(if (inConsult) "Finish" else "Call in", filled = true, onClick = onAction)
        }
    }
}
