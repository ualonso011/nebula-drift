package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Identifies which entity owns/fired a laser, used for collision
 * filtering and sprite selection at render time.
 */
enum class LaserOwner {
    PLAYER,
    LIGHT_FIGHTER,
    MEDIUM_FRIGATE,
    HEAVY_DESTROYER,
    DARK_CLONE
}

/**
 * A laser projectile fired by the player's ship or an enemy.
 *
 * Moves right at [Constants.LASER_SPEED] (player) or toward the
 * player (enemies) and auto-removes after [lifetime] seconds.
 *
 * @param owner  Identifies who fired this laser; used for render
 *               color selection and collision filtering.
 */
class Laser(
    position: Vector2,
    velocity: Vector2 = Vector2(Constants.LASER_SPEED, 0f),
    override val radius: Float = Constants.LASER_RADIUS,
    val lifetime: Float = Constants.LASER_LIFETIME,
    val owner: LaserOwner = LaserOwner.PLAYER
) : Entity {

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()

    /** Age of this laser in seconds. */
    var age: Float = 0f
        private set

    /** Whether this laser has exceeded its maximum lifetime. */
    val isExpired: Boolean get() = age >= lifetime

    /** Whether this laser was fired by the player. */
    val isPlayerLaser: Boolean get() = owner == LaserOwner.PLAYER

    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        age += delta
    }
}
