package com.azersun.uitemplates.demos.error2

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val errorPart2Group: DemoGroup = DemoGroup(
    "Error · Part 2",
    listOf(
        Demo("error2-offline", "Offline", "Wi-Fi arcs scan, then a red slash cuts through", { OfflineErrorDemo() }),
        Demo("error2-card-declined", "Card declined", "The card flips to a red back, then flips home", { CardDeclinedDemo() }),
        Demo("error2-warning", "Warning pulse", "The triangle draws in, fills red and pulses", { WarningPulseDemo() }),
        Demo("error2-glitch", "Glitch", "200 OK glitches with an RGB split into a red 500", { GlitchDemo() }),
        Demo("error2-404", "404 Not found", "The magnifier's lens becomes the 0 in 404", { NotFoundDemo() }),
        Demo("error2-password", "Password rules", "One rule passes, two turn red and shake", { PasswordRulesDemo() }),
    ),
)
