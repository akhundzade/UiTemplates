package com.azersun.uitemplates.demos.swipedelete

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val swipeDeleteGroup: DemoGroup = DemoGroup(
    "Swipe to delete",
    listOf(
        Demo("swipedelete-bin", "Bin lid flip", "A red bin's lid flips open, then the bar closes up", { BinLidFlipDemo() }),
        Demo("swipedelete-pop", "Pop to dot", "Turns red, shrinks to a dot and pops into sparks", { PopToDotDemo() }),
        Demo("swipedelete-blade", "Blade slice", "A blade follows the finger and splits the row in two", { BladeSliceDemo() }),
        Demo("swipedelete-dust", "Dust crumble", "Crumbles into sand from right to left and blows away", { DustCrumbleDemo() }),
        Demo("swipedelete-elastic", "Elastic snap", "Stretches like rubber, then snaps off to the left", { ElasticSnapDemo() }),
        Demo("swipedelete-genie", "Genie bin", "Sucked into a little red bin like a genie", { GenieBinDemo() }),
    ),
)
