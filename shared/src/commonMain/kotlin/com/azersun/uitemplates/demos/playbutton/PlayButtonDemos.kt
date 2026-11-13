package com.azersun.uitemplates.demos.playbutton

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val playButtonGroup: DemoGroup = DemoGroup(
    "Play button",
    listOf(
        Demo("playbutton-split", "Triangle split", "The triangle splits and springs into pause bars", { TriangleSplitDemo() }),
        Demo("playbutton-coin", "Coin flip", "Flips like a coin to the icon on its back", { CoinFlipDemo() }),
        Demo("playbutton-echo", "Echo rings", "Three rings echo out on every tap", { EchoRingsDemo() }),
        Demo("playbutton-wave", "Dancing wave", "Waveform bars dance to the beat while playing", { DancingWaveDemo() }),
        Demo("playbutton-comet", "Comet arc", "A comet circles the button, slowing to a stop on pause", { CometArcDemo() }),
        Demo("playbutton-squash", "Elastic squash", "Squashes and wobbles like jelly on tap", { ElasticSquashDemo() }),
    ),
)
