package com.azersun.uitemplates.demos.backgrounds

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val backgroundsGroup: DemoGroup = DemoGroup(
    "Backgrounds",
    listOf(
        Demo("backgrounds-mesh", "Mesh gradient", "Soft blobs drift to new spots on every tap", { MeshGradientDemo() }),
        Demo("backgrounds-aurora", "Aurora", "Glowing bands sway, then surge on tap", { AuroraBackgroundDemo() }),
        Demo("backgrounds-noise", "Noise texture", "Film grain over a glow that pulses on tap", { NoiseTextureDemo() }),
        Demo("backgrounds-dots", "Dot grid", "Each tap sends a ripple through a fading dot grid", { DotGridDemo() }),
        Demo("backgrounds-spotlight", "Spotlight", "A soft light glides to the tap and lights the grid", { SpotlightDemo() }),
        Demo("backgrounds-gradient", "Animated gradient", "A flowing gradient turns angle and hue on tap", { AnimatedGradientDemo() }),
    ),
)
