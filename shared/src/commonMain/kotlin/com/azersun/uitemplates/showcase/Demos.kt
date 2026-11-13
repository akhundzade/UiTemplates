package com.azersun.uitemplates.showcase

import androidx.compose.runtime.Composable
import com.azersun.uitemplates.demos.clinic.BroadcastDemo
import com.azersun.uitemplates.demos.clinic.InboxDemo
import com.azersun.uitemplates.demos.clinic.PageSwitchDemo
import com.azersun.uitemplates.demos.clinic.PatientFlowDemo
import com.azersun.uitemplates.demos.inktheme.InkThemeDemo
import com.azersun.uitemplates.demos.kanban.KanbanDemo
import com.azersun.uitemplates.demos.menus.BottomSheetDemo
import com.azersun.uitemplates.demos.menus.CircleRevealDemo
import com.azersun.uitemplates.demos.menus.FabFanDemo
import com.azersun.uitemplates.demos.menus.HamburgerMorphDemo
import com.azersun.uitemplates.demos.menus.SideDrawerDemo
import com.azersun.uitemplates.demos.menus.StaggeredDropdownDemo

class Demo(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: @Composable () -> Unit,
)

class DemoGroup(val title: String, val demos: List<Demo>)

val demoGroups: List<DemoGroup> = listOf(
    DemoGroup(
        "Kanban CRM",
        listOf(
            Demo("kanban", "Drag & drop board", "Lift, tilt, column glow, confetti and counters", { KanbanDemo() }),
        ),
    ),
    DemoGroup(
        "Theme switch",
        listOf(
            Demo("ink-theme", "Ink theme toggle", "Dark ↔ light spreads from the toggle like ink in water", { InkThemeDemo() }),
        ),
    ),
    DemoGroup(
        "Menus",
        listOf(
            Demo("hamburger", "Hamburger morph", "Bars slide together, then spin into an ✕", { HamburgerMorphDemo() }),
            Demo("drawer", "Side drawer", "Springs in from the left, page dims, rows stagger", { SideDrawerDemo() }),
            Demo("fab-fan", "FAB fan", "+ turns into ✕ as five actions fan out on an arc", { FabFanDemo() }),
            Demo("bottom-sheet", "Bottom sheet", "Springs up over a dim, icons pop in one by one", { BottomSheetDemo() }),
            Demo("dropdown", "Staggered dropdown", "Chevron flips, card unfolds, items slide in", { StaggeredDropdownDemo() }),
            Demo("circle-reveal", "Circle reveal", "A circle grows from ☰ to fill the screen", { CircleRevealDemo() }),
        ),
    ),
    DemoGroup(
        "Clinic dashboard",
        listOf(
            Demo("patient-flow", "Patient flow", "Call in moves a card between lists, with undo", { PatientFlowDemo() }),
            Demo("inbox", "Inbox reply", "Open a chat, quick reply types in, ticks turn blue", { InboxDemo() }),
            Demo("broadcast", "Broadcast send", "Blurred confirm, then live delivery progress bars", { BroadcastDemo() }),
            Demo("page-switch", "Page switching", "Rail indicator slides, pages fade through, counters roll", { PageSwitchDemo() }),
        ),
    ),
)
