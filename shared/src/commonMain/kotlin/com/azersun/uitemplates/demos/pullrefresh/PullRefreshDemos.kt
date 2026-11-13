package com.azersun.uitemplates.demos.pullrefresh

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val pullRefreshGroup: DemoGroup = DemoGroup(
    "Pull to refresh",
    listOf(
        Demo("pullrefresh-line", "Top line", "A line grows with the pull, a light runs along it", { TopLineRefreshDemo() }),
        Demo("pullrefresh-coil", "Spring coil", "A coil stretches with the pull and bounces on release", { SpringCoilRefreshDemo() }),
        Demo("pullrefresh-sync", "Sync spin", "The refresh icon turns, spins, then morphs into a check", { SyncSpinRefreshDemo() }),
        Demo("pullrefresh-chevrons", "Chevron stack", "Chevrons light up one by one, then a light runs down", { ChevronStackRefreshDemo() }),
        Demo("pullrefresh-avatar", "Avatar drop", "The new post's avatar peeks out, then drops into its row", { AvatarDropRefreshDemo() }),
        Demo("pullrefresh-logo", "Logo trace", "The logo draws itself, then fills and spins while loading", { LogoTraceRefreshDemo() }),
    ),
)
