package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Visual damage states based on remaining lives.
 */
enum class DamageState(val livesRemaining: Int) {
    PRISTINE(3),
    DAMAGED(2),
    CRITICAL(1),
    DESTROYED(0)
}

/**
 * The player's ship entity.
 *
 * Starts with [Constants.SHIP_LIVES] lives and becomes invulnerable
 * for [Constants.INVULNERABILITY_TIME] seconds after each hit.
 * The [damageState] is derived from remaining lives.
 */
class Ship(
    position: Vector2 = Vector2(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f),
    velocity: Vector2 = Vector2.Zero.cpy()
) : Entity {

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = Constants.SHIP_RADIUS

    /** Remaining lives. 0 means destroyed. */
    var lives: Int = Constants.SHIP_LIVES
        private set

    /** Remaining invulnerability time in seconds. 0 = vulnerable. */
    var invulnerabilityTimer: Float = 0f
        private set

    /** Whether the ship is currently thrusting (input-driven). */
    var isThrusting: Boolean = false

    /** Whether the ship is within the invulnerability window. */
    val isInvulnerable: Boolean get() = invulnerabilityTimer > 0f

    /** Whether the ship has zero lives. */
    val isDestroyed: Boolean get() = lives <= 0

    /** Current visual damage state derived from lives. */
    val damageState: DamageState
        get() = when {
            lives >= 3 -> DamageState.PRISTINE
            lives == 2 -> DamageState.DAMAGED
            lives == 1 -> DamageState.CRITICAL
            else -> DamageState.DESTROYED
        }

    /**
     * Add one life, capped at [Constants.SHIP_LIVES].
     * @return true if a life was added, false if already at max
     */
    fun addLife(): Boolean {
        if (lives >= Constants.SHIP_LIVES) return false
        lives++
        return true
    }

    /**
     * Reduce lives by one if not invulnerable and not already destroyed.
     * Starts the invulnerability timer on successful hit.
     * @return true if damage was applied, false if blocked (invulnerable/already dead)
     */
    fun takeDamage(): Boolean {
        if (isInvulnerable || isDestroyed) return false
        lives--
        if (!isDestroyed) {
            invulnerabilityTimer = Constants.INVULNERABILITY_TIME
        }
        return true
    }

    /** Decrement invulnerability timer. Call each frame. */
    private fun updateInvulnerability(delta: Float) {
        if (invulnerabilityTimer > 0f) {
            invulnerabilityTimer = (invulnerabilityTimer - delta).coerceAtLeast(0f)
        }
    }

    override fun update(delta: Float) {
        updateInvulnerability(delta)
    }
}
