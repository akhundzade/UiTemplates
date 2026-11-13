package com.azersun.uitemplates.showcase

import androidx.compose.runtime.Composable
import com.azersun.uitemplates.demos.inktheme.InkThemeDemo
import com.azersun.uitemplates.demos.kanban.KanbanDemo

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
)
