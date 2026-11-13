package com.azersun.uitemplates.demos.menulight

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val minimalMenuGroup: DemoGroup = DemoGroup(
    "Menu · Minimal",
    listOf(
        Demo("menulight-pushback", "Push back", "The page tilts back in 3D to reveal the nav behind", { PushBackMenuDemo() }),
        Demo("menulight-popover", "Kebab popover", "A popover scales out of the ⋮ from its corner", { KebabPopoverMenuDemo() }),
        Demo("menulight-gooey", "Gooey menu", "Bubbles drip out of the ⋮ with a gooey neck", { GooeyMenuDemo() }),
        Demo("menulight-fold", "Fold down", "The menu unfolds downward like folded paper", { FoldDownMenuDemo() }),
        Demo("menulight-morph", "Button to panel", "The ⋮ button grows into an outlined panel", { ButtonToPanelMenuDemo() }),
        Demo("menulight-palette", "Command palette", "A search field opens wide and filters as you type", { CommandPaletteMenuDemo() }),
    ),
)
