package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * A laser projectile fired by the player's ship.
 *
 * Moves right at [Constants.LASER_SPEED] and auto-removes after
 * [Constants.LASER_LIFETIME] seconds or when leaving the screen.
 */
class Laser(
    position: Vector2,
    velocity: Vector2 = Vector2(Constants.LASER_SPEED, 0f)
) : Entity {

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = Constants.LASER_RADIUS

    /** Age of this laser in seconds. */
    var age: Float = 0f
        private set

    /** Maximum lifetime before auto-expiry. */
    val lifetime: Float = Constants.LASER_LIFETIME

    /** Whether this laser has exceeded its maximum lifetime. */
    val isExpired: Boolean get() = age >= lifetime

    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        age += delta
    }
}
