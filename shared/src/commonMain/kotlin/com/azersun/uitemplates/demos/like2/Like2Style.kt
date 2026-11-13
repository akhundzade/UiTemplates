package com.azersun.uitemplates.demos.like2

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val StarPoints = 10

/**
 * Replaces this path with a five-point star around [center], tips at [outer] and dips at [inner],
 * with every corner rounded by [corner] so the fill matches a round-joined outline.
 */
internal fun Path.star(center: Offset, outer: Float, inner: Float, corner: Float) {
    reset()
    for (step in 0..StarPoints) {
        val i = step % StarPoints
        val v = starPoint(center, outer, inner, i)
        val toPrev = starPoint(center, outer, inner, (i + StarPoints - 1) % StarPoints) - v
        val toNext = starPoint(center, outer, inner, (i + 1) % StarPoints) - v
        val entry = v + toPrev / toPrev.getDistance() * corner
        val exit = v + toNext / toNext.getDistance() * corner
        if (step == 0) {
            moveTo(exit.x, exit.y)
        } else {
            lineTo(entry.x, entry.y)
            quadraticTo(v.x, v.y, exit.x, exit.y)
        }
    }
    close()
}

private fun starPoint(center: Offset, outer: Float, inner: Float, index: Int): Offset {
    val radius = if (index % 2 == 0) outer else inner
    val angle = (-90f + index * 36f) * PI.toFloat() / 180f
    return center + Offset(cos(angle) * radius, sin(angle) * radius)
}
