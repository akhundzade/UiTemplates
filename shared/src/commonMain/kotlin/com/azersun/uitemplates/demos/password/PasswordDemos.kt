package com.azersun.uitemplates.demos.password

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val passwordStrengthGroup: DemoGroup = DemoGroup(
    "Password strength",
    listOf(
        Demo("password-flow-fill", "Flow fill", "One liquid fill glides through the gaps and blends color", { FlowFillDemo() }),
        Demo("password-block-drop", "Block drop", "New segments drop out of the field and land with a squash", { BlockDropDemo() }),
        Demo("password-spark-feed", "Spark feed", "Each typed dot fires a spark that arcs into the bar", { SparkFeedDemo() }),
        Demo("password-chain-light", "Chain light", "Segments relight one after another with a flash", { ChainLightDemo() }),
        Demo("password-flap-label", "Flap label", "The word flips like a split-flap board, segments flap open", { FlapLabelDemo() }),
        Demo("password-liquid-link", "Liquid link", "The fill oozes across each gap, then the neck snaps", { LiquidLinkDemo() }),
    ),
)
