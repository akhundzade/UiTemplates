package com.azersun.uitemplates.demos.receipt

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val receiptGroup: DemoGroup = DemoGroup(
    "Receipt checkout",
    listOf(
        Demo("receipt-print", "Printing receipt", "A thermal printer feeds the receipt; pay to tear it off", { ReceiptCheckoutDemo() }),
    ),
)
