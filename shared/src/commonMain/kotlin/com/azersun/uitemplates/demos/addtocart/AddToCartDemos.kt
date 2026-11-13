package com.azersun.uitemplates.demos.addtocart

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val addToCartGroup: DemoGroup = DemoGroup(
    "Add to cart",
    listOf(
        Demo("addtocart-fly", "Fly to cart", "A mini thumbnail arcs into the cart, which dips", { FlyToCartDemo() }),
        Demo("addtocart-stepper", "Qty stepper", "The button morphs into a trash · count · + stepper", { QtyStepperDemo() }),
        Demo("addtocart-hop", "Badge hop", "A dot flies to the cart, the badge hops and +1 floats", { BadgeHopDemo() }),
        Demo("addtocart-run", "Cart run", "A tiny cart runs across the button and catches the item", { CartRunDemo() }),
        Demo("addtocart-bag", "Into the bag", "A mug drops into the cart with sparkles and a tick", { IntoTheBagDemo() }),
        Demo("addtocart-check", "Plus to check", "+ grows into a disc, turns into ✓ and the button fills", { PlusToCheckDemo() }),
    ),
)
