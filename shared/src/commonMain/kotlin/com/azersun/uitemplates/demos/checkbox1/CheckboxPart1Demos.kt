package com.azersun.uitemplates.demos.checkbox1

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val checkboxPart1Group: DemoGroup = DemoGroup(
    "Checkbox · Part 1",
    listOf(
        Demo("checkbox1-circle", "Circle to check", "The ring unwinds and its stroke flows into a ✓", { CircleToCheckDemo() }),
        Demo("checkbox1-strike", "Strike & sink", "The task is struck through and sinks to the bottom", { StrikeSinkDemo() }),
        Demo("checkbox1-select-all", "Select all", "Items check one by one, the parent goes – then ✓", { SelectAllDemo() }),
        Demo("checkbox1-radio", "Radio slide", "The dot stretches like goo to the next option", { RadioSlideDemo() }),
        Demo("checkbox1-cards", "Card select", "Photos shrink, a ✓ badge pops, a gradient frame glows", { CardSelectDemo() }),
        Demo("checkbox1-ink", "Ink fill", "Liquid rises with a wavy top, then a white ✓ draws in", { InkFillDemo() }),
    ),
)
