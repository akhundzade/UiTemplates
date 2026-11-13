package com.azersun.uitemplates.demos.fab2

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val floatingButtonGroup: DemoGroup = DemoGroup(
    "Floating button",
    listOf(
        Demo("fab2-scrolltop", "Scroll to top", "A progress ring fills as you scroll; tap to fly back up", { ScrollToTopFabDemo() }),
        Demo("fab2-record", "Hold to record", "Hold the mic: it grows pink, waves pulse, a timer counts", { HoldToRecordFabDemo() }),
        Demo("fab2-grid", "Grid pop", "+ turns to × as six action tiles pop out one by one", { GridPopFabDemo() }),
        Demo("fab2-morph", "Context morph", "Each tab spins the button into a new icon and colour", { ContextMorphFabDemo() }),
        Demo("fab2-unread", "Unread count", "Message chips fly into the button and its badge rolls up", { UnreadCountFabDemo() }),
        Demo("fab2-split", "Split actions", "The button splits into Voice and Write with a gooey neck", { SplitActionsFabDemo() }),
    ),
)
