package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Collectible space debris with a pulsing glow effect.
 *
 * Drifts slowly left. When collected by the ship, awards an
 * extra life (capped at [Constants.SHIP_LIVES]).
 *
 * The [glowPhase] field drives a sine-based alpha oscillation
 * on an outer circle drawn by the renderer, creating a pulsing
 * glow effect without shaders.
 */
class SpaceDebris(
    position: Vector2,
    velocity: Vector2 = Vector2(-Constants.DEBRIS_SPEED, 0f)
) : Entity {

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = Constants.DEBRIS_RADIUS

    /** Phase angle for the pulsing glow effect (radians). */
    var glowPhase: Float = 0f
        private set

    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        glowPhase += delta * Constants.DEBRIS_GLOW_SPEED
    }
}
