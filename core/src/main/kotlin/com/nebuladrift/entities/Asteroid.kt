package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Asteroid size tiers.
 *
 * Each tier defines the starting HP and collision radius.
 */
enum class AsteroidSize(val hp: Int, val radius: Float) {
    LARGE(Constants.ASTEROID_LARGE_HP, Constants.ASTEROID_LARGE_RADIUS),
    MEDIUM(Constants.ASTEROID_MEDIUM_HP, Constants.ASTEROID_MEDIUM_RADIUS),
    SMALL(Constants.ASTEROID_SMALL_HP, Constants.ASTEROID_SMALL_RADIUS)
}

/**
 * An asteroid entity.
 *
 * Moves at constant velocity, rotates, and degrades through size
 * tiers when hit by lasers. Large asteroids split into medium,
 * medium into small, small just get destroyed.
 */
class Asteroid(
    position: Vector2,
    velocity: Vector2,
    val size: AsteroidSize
) : Entity {

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = size.radius

    /** Current hit points. Starts at [AsteroidSize.hp] for the given size. */
    var health: Int = size.hp
        private set

    /** Visual rotation in degrees. */
    var rotation: Float = Random.nextFloat() * 360f

    /** Rotation speed in degrees per second. */
    val rotationSpeed: Float = (Random.nextFloat() - 0.5f) * 120f

    /** Reduce HP by one. @return true if HP reached zero (destroyed). */
    fun hit(): Boolean {
        health--
        return health <= 0
    }

    /** Whether the asteroid has zero HP. */
    val isDestroyed: Boolean get() = health <= 0

    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        rotation += rotationSpeed * delta
    }
}
