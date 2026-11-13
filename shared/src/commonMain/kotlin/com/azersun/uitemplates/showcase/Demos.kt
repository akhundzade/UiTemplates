package com.azersun.uitemplates.showcase

import androidx.compose.runtime.Composable
import com.azersun.uitemplates.demos.addtocart.addToCartGroup
import com.azersun.uitemplates.demos.backgrounds.backgroundsGroup
import com.azersun.uitemplates.demos.button2.buttonPart2Group
import com.azersun.uitemplates.demos.buttonstate.buttonStateGroup
import com.azersun.uitemplates.demos.checkbox1.checkboxPart1Group
import com.azersun.uitemplates.demos.checkbox2.checkboxPart2Group
import com.azersun.uitemplates.demos.colorpicker.colorPickerGroup
import com.azersun.uitemplates.demos.counter.counterGroup
import com.azersun.uitemplates.demos.darkmode.darkModeToggleGroup
import com.azersun.uitemplates.demos.datepicker.datePickerGroup
import com.azersun.uitemplates.demos.dialog.dialogGroup
import com.azersun.uitemplates.demos.error1.errorPart1Group
import com.azersun.uitemplates.demos.error2.errorPart2Group
import com.azersun.uitemplates.demos.fab2.floatingButtonGroup
import com.azersun.uitemplates.demos.follow.followButtonGroup
import com.azersun.uitemplates.demos.inktheme.InkThemeDemo
import com.azersun.uitemplates.demos.inputerror.inputErrorGroup
import com.azersun.uitemplates.demos.kanban.KanbanDemo
import com.azersun.uitemplates.demos.like.likeGroup
import com.azersun.uitemplates.demos.like2.likePart2Group
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
import com.azersun.uitemplates.demos.receipt.receiptGroup
import com.azersun.uitemplates.demos.screentransition.screenTransitionGroup
import com.azersun.uitemplates.demos.search.searchGroup
import com.azersun.uitemplates.demos.stepper.stepperGroup
import com.azersun.uitemplates.demos.swipedelete.swipeDeleteGroup
import com.azersun.uitemplates.demos.switch1.switchPart1Group
import com.azersun.uitemplates.demos.switch2.switchPart2Group
import com.azersun.uitemplates.demos.textfield.textFieldGroup
import com.azersun.uitemplates.demos.viewas.viewAsMenuGroup

class Demo(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: @Composable () -> Unit,
)

class DemoGroup(val title: String, val demos: List<Demo>)

class DemoCategory(val id: String, val title: String, val blurb: String, val groups: List<DemoGroup>) {
    val demoCount: Int get() = groups.sumOf { it.demos.size }
}

private val kanbanGroup = DemoGroup(
    "Kanban CRM",
    listOf(
        Demo("kanban", "Drag & drop board", "Lift, tilt, column glow, confetti and counters", { KanbanDemo() }),
    ),
)

private val inkThemeGroup = DemoGroup(
    "Theme switch",
    listOf(
        Demo("ink-theme", "Ink theme toggle", "Dark ↔ light spreads from the toggle like ink in water", { InkThemeDemo() }),
    ),
)

private val coreMenusGroup = DemoGroup(
    "Menus",
    listOf(
        Demo("hamburger", "Hamburger morph", "Bars slide together, then spin into an ✕", { HamburgerMorphDemo() }),
        Demo("drawer", "Side drawer", "Springs in from the left, page dims, rows stagger", { SideDrawerDemo() }),
        Demo("fab-fan", "FAB fan", "+ turns into ✕ as five actions fan out on an arc", { FabFanDemo() }),
        Demo("bottom-sheet", "Bottom sheet", "Springs up over a dim, icons pop in one by one", { BottomSheetDemo() }),
        Demo("dropdown", "Staggered dropdown", "Chevron flips, card unfolds, items slide in", { StaggeredDropdownDemo() }),
        Demo("circle-reveal", "Circle reveal", "A circle grows from ☰ to fill the screen", { CircleRevealDemo() }),
    ),
)

val demoCategories: List<DemoCategory> = listOf(
    DemoCategory(
        "buttons",
        "Buttons & actions",
        "Press feedback, state changes, play, follow and add to cart",
        listOf(buttonStateGroup, buttonPart2Group, playButtonGroup, followButtonGroup, addToCartGroup),
    ),
    DemoCategory(
        "toggles",
        "Toggles & selection",
        "Switches, checkboxes, steppers, swatches and date pickers",
        listOf(
            switchPart1Group,
            switchPart2Group,
            checkboxPart1Group,
            checkboxPart2Group,
            stepperGroup,
            colorPickerGroup,
            datePickerGroup,
        ),
    ),
    DemoCategory(
        "navigation",
        "Menus & navigation",
        "Menus, floating buttons, dialogs and screen transitions",
        listOf(
            coreMenusGroup,
            viewAsMenuGroup,
            minimalMenuGroup,
            floatingButtonGroup,
            dialogGroup,
            screenTransitionGroup,
        ),
    ),
    DemoCategory(
        "forms",
        "Text & forms",
        "Text fields, validation, passwords, codes and search",
        listOf(textFieldGroup, inputErrorGroup, passwordStrengthGroup, otpGroup, searchGroup),
    ),
    DemoCategory(
        "feedback",
        "Feedback & errors",
        "Error states, progress steps and pull to refresh",
        listOf(errorPart1Group, errorPart2Group, progressStepsGroup, pullRefreshGroup),
    ),
    DemoCategory(
        "lists",
        "Lists & data",
        "Drag and drop, swipe to delete and number counters",
        listOf(kanbanGroup, swipeDeleteGroup, counterGroup),
    ),
    DemoCategory(
        "social",
        "Likes & reactions",
        "Hearts, stars, bookmarks and reaction pickers",
        listOf(likeGroup, likePart2Group),
    ),
    DemoCategory(
        "theme",
        "Theme & backgrounds",
        "Dark/light theme switches and animated backgrounds",
        listOf(inkThemeGroup, darkModeToggleGroup, backgroundsGroup),
    ),
    DemoCategory(
        "showpieces",
        "Showpieces",
        "Larger one-off scenes",
        listOf(receiptGroup),
    ),
)

val demoGroups: List<DemoGroup> get() = demoCategories.flatMap { it.groups }
