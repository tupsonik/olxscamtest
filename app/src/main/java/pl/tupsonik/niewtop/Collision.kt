package pl.tupsonik.flappysteroids

import kotlin.math.max
import kotlin.math.min

object Collision {
    fun circleIntersectsRect(
        cx: Float,
        cy: Float,
        radius: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Boolean {
        val closestX = cx.coerceIn(min(left, right), max(left, right))
        val closestY = cy.coerceIn(min(top, bottom), max(top, bottom))
        val dx = cx - closestX
        val dy = cy - closestY
        return dx * dx + dy * dy <= radius * radius
    }

    fun circleIntersectsCircle(
        ax: Float,
        ay: Float,
        ar: Float,
        bx: Float,
        by: Float,
        br: Float
    ): Boolean {
        val dx = ax - bx
        val dy = ay - by
        val distanceSquared = dx * dx + dy * dy
        val radiusSum = ar + br
        return distanceSquared <= radiusSum * radiusSum
    }
}
