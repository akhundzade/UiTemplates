package com.azersun.uitemplates.demos.viewas

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val viewAsMenuGroup: DemoGroup = DemoGroup(
    "Menu · View as",
    listOf(
        Demo("viewas-hinge", "Swing hinge", "Swings down from a pin and settles with a sway", { SwingHingeDemo() }),
        Demo("viewas-doors", "Double doors", "Two halves swing shut like doors", { DoubleDoorsDemo() }),
        Demo("viewas-liquid", "Liquid fill", "Fills up behind a moving wave, drains on pick", { LiquidFillDemo() }),
        Demo("viewas-zipper", "Zipper rows", "Rows slide in from alternating sides", { ZipperRowsDemo() }),
        Demo("viewas-scroll", "Scroll unroll", "Unrolls downward like a paper scroll", { ScrollUnrollDemo() }),
        Demo("viewas-line", "Line expand", "A line shoots out, then opens into the menu", { LineExpandDemo() }),
    ),
)
