package com.azersun.uitemplates.showcase

import androidx.compose.runtime.Composable
import com.azersun.uitemplates.demos.addtocart.addToCartGroup
import com.azersun.uitemplates.demos.backgrounds.backgroundsGroup
import com.azersun.uitemplates.demos.buttonstate.buttonStateGroup
import com.azersun.uitemplates.demos.colorpicker.colorPickerGroup
import com.azersun.uitemplates.demos.counter.counterGroup
import com.azersun.uitemplates.demos.darkmode.darkModeToggleGroup
import com.azersun.uitemplates.demos.datepicker.datePickerGroup
import com.azersun.uitemplates.demos.dialog.dialogGroup
import com.azersun.uitemplates.demos.follow.followButtonGroup
import com.azersun.uitemplates.demos.inktheme.InkThemeDemo
import com.azersun.uitemplates.demos.inputerror.inputErrorGroup
import com.azersun.uitemplates.demos.kanban.KanbanDemo
import com.azersun.uitemplates.demos.like.likeGroup
import com.azersun.uitemplates.demos.menulight.minimalMenuGroup
import com.azersun.uitemplates.demos.menus.BottomSheetDemo
import com.azersun.uitemplates.demos.menus.CircleRevealDemo
import com.azersun.uitemplates.demos.menus.FabFanDemo
import com.azersun.uitemplates.demos.menus.HamburgerMorphDemo
import com.azersun.uitemplates.demos.menus.SideDrawerDemo
import com.azersun.uitemplates.demos.menus.StaggeredDropdownDemo
import com.azersun.uitemplates.demos.otp.otpGroup
import com.azersun.uitemplates.demos.password.passwordStrengthGroup
import com.azersun.uitemplates.demos.playbutton.playButtonGroup
import com.azersun.uitemplates.demos.progresssteps.progressStepsGroup
import com.azersun.uitemplates.demos.pullrefresh.pullRefreshGroup
import com.azersun.uitemplates.demos.stepper.stepperGroup
import com.azersun.uitemplates.demos.swipedelete.swipeDeleteGroup
import com.azersun.uitemplates.demos.textfield.textFieldGroup
import com.azersun.uitemplates.demos.viewas.viewAsMenuGroup

class Demo(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: @Composable () -> Unit,
)

class DemoGroup(val title: String, val demos: List<Demo>)

val demoGroups: List<DemoGroup> = listOf(
    DemoGroup(
        "Kanban CRM",
        listOf(
            Demo("kanban", "Drag & drop board", "Lift, tilt, column glow, confetti and counters", { KanbanDemo() }),
        ),
    ),
    DemoGroup(
        "Theme switch",
        listOf(
            Demo("ink-theme", "Ink theme toggle", "Dark ↔ light spreads from the toggle like ink in water", { InkThemeDemo() }),
        ),
    ),
    DemoGroup(
        "Menus",
        listOf(
            Demo("hamburger", "Hamburger morph", "Bars slide together, then spin into an ✕", { HamburgerMorphDemo() }),
            Demo("drawer", "Side drawer", "Springs in from the left, page dims, rows stagger", { SideDrawerDemo() }),
            Demo("fab-fan", "FAB fan", "+ turns into ✕ as five actions fan out on an arc", { FabFanDemo() }),
            Demo("bottom-sheet", "Bottom sheet", "Springs up over a dim, icons pop in one by one", { BottomSheetDemo() }),
            Demo("dropdown", "Staggered dropdown", "Chevron flips, card unfolds, items slide in", { StaggeredDropdownDemo() }),
            Demo("circle-reveal", "Circle reveal", "A circle grows from ☰ to fill the screen", { CircleRevealDemo() }),
        ),
    ),
    viewAsMenuGroup,
    darkModeToggleGroup,
    backgroundsGroup,
    stepperGroup,
    inputErrorGroup,
    otpGroup,
    passwordStrengthGroup,
    swipeDeleteGroup,
    addToCartGroup,
    counterGroup,
    pullRefreshGroup,
    datePickerGroup,
    dialogGroup,
    minimalMenuGroup,
    followButtonGroup,
    colorPickerGroup,
    playButtonGroup,
    buttonStateGroup,
    progressStepsGroup,
    textFieldGroup,
    likeGroup,
)
