package com.azersun.uitemplates.demos.error1

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val errorPart1Group: DemoGroup = DemoGroup(
    "Error · Part 1",
    listOf(
        Demo("error1-shake-input", "Shake input", "The field shakes, turns red and an error slides in", { ShakeInputDemo() }),
        Demo("error1-cross", "Cross draw", "A ring draws in, turns red, a ✕ strokes in and shakes", { CrossDrawDemo() }),
        Demo("error1-button", "Button fail", "Spins, turns red with an ✕, shakes, then reads Try again", { ButtonFailDemo() }),
        Demo("error1-toast", "Error toast", "A Couldn't save toast drops in, then slides away", { ErrorToastDemo() }),
        Demo("error1-pin", "PIN shake", "Dots fill, turn red and shake, then clear one by one", { PinShakeDemo() }),
        Demo("error1-upload", "Upload failed", "The bar stalls at 68%, turns red, Retry finishes it", { UploadFailedDemo() }),
    ),
)
