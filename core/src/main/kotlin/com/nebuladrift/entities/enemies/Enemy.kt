package com.nebuladrift.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Entity

/**
 * Enemy type identifiers for each class of hostile entity.
 */
enum class EnemyType {
    LIGHT_FIGHTER,
    MEDIUM_FRIGATE,
    HEAVY_DESTROYER,
    DARK_CLONE
}

/**
 * Visual damage state based on remaining health ratio.
 */
enum class EnemyDamageState {
    PRISTINE,
    DAMAGED,
    CRITICAL
}

/**
 * Abstract base for all enemy entities.
 *
 * Provides shared health/damage logic and a damage state derived
 * from the current-to-max health ratio. Each subclass defines its
 * type-specific constants (maxHealth, points, speed, radius) and
 * is responsible for passing position/velocity to the constructor.
 *
 * @param position Initial position (subclass should pass a copy)
 * @param velocity Constant velocity vector (subclass should pass a copy)
 * @param radius Collision circle radius
 * @param maxHealth Starting and maximum HP
 * @param points Score awarded when destroyed
 */
abstract class Enemy(
    position: Vector2,
    velocity: Vector2,
    override val radius: Float,
    val maxHealth: Int,
    val points: Int
) : Entity {

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()

    /** Current hit points. Starts at [maxHealth]. */
    var health: Int = maxHealth
        private set

    /** Whether the enemy has been reduced to zero HP. */
    val isDestroyed: Boolean get() = health <= 0

    /**
     * Reduce HP by one.
     * @return true if this enemy is now destroyed
     */
    fun takeDamage(): Boolean {
        health--
        return isDestroyed
    }

    /**
     * Visual damage state derived from remaining health ratio.
     * Used by renderer to tint or change appearance.
     */
    fun getDamageState(): EnemyDamageState {
        val ratio = health.toFloat() / maxHealth.toFloat()
        return when {
            ratio > 0.66f -> EnemyDamageState.PRISTINE
            ratio > 0.33f -> EnemyDamageState.DAMAGED
            else -> EnemyDamageState.CRITICAL
        }
    }

    /** Returns the [EnemyType] of this enemy instance. */
    abstract fun getType(): EnemyType

    /** Cooldown timer for enemy laser fire (seconds remaining).
     * Initialised negative so the enemy fires on its very first frame. */
    var fireCooldown: Float = -1f

    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
    }
}
