package com.nebuladrift.systems

import com.badlogic.gdx.math.MathUtils
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.DarkClone
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.entities.enemies.EnemyType
import com.nebuladrift.util.Constants

/**
 * Applies physics to all entities each frame.
 *
 * Behaviour:
 * - Ship: constant downward gravity, thrust when active, velocity damping, bounds clamp
 * - Lasers: update position, remove expired / off-screen
 * - Asteroids: update position, remove off-screen
 * - Enemies: move left at their base speed * difficulty scroll multiplier,
 *   DarkClone Y mirrored from [MirrorSystem], remove off-screen
 * - Astronauts: move left at ASTRONAUT_SPEED * difficulty scroll multiplier,
 *   remove after rescue/kill animation or off-screen
 * - Debris: move left at DEBRIS_SPEED * difficulty scroll multiplier,
 *   update glow phase oscillation, remove off-screen
 */
class PhysicsSystem : GameSystem {

    override fun update(delta: Float, context: GameContext) {
        updateShip(delta, context)
        updateLasers(delta, context)
        updateAsteroids(delta, context)
        updateEnemies(delta, context)
        updateAstronauts(delta, context)
        updateDebris(delta, context)
    }

    private fun updateShip(delta: Float, context: GameContext) {
        val ship = context.ship

        // Constant downward gravity
        ship.velocity.y += Constants.GRAVITY_ACCELERATION * delta

        // Upward thrust while active
        if (ship.isThrusting) {
            ship.velocity.y += Constants.SHIP_THRUST * delta
        }

        // Velocity damping (slows the ship naturally)
        ship.velocity.scl(Constants.VELOCITY_DAMPING)

        // Integrate position
        ship.position.mulAdd(ship.velocity, delta)

        // Clamp to screen bounds (respect collision radius)
        ship.position.x = MathUtils.clamp(
            ship.position.x,
            ship.radius,
            Constants.WORLD_WIDTH - ship.radius
        )
        
        // Check if ship fell off the bottom of the screen
        if (ship.position.y < -ship.radius) {
            // Ship fell off the bottom - take damage
            ship.takeDamage()
            // Reset position to middle-left if not destroyed
            if (!ship.isDestroyed) {
                ship.position.y = Constants.WORLD_HEIGHT / 2f
                ship.velocity.y = 0f
            }
        } else {
            // Clamp to top of screen only
            ship.position.y = MathUtils.clamp(
                ship.position.y,
                -ship.radius, // Allow going slightly below for detection
                Constants.WORLD_HEIGHT - ship.radius
            )
        }

        // Update ship timers (invulnerability)
        ship.update(delta)
    }

    private fun updateLasers(delta: Float, context: GameContext) {
        val toRemove = mutableListOf<Laser>()

        for (laser in context.lasers) {
            laser.update(delta)
            if (laser.isExpired ||
                laser.position.x < -laser.radius ||
                laser.position.x > Constants.WORLD_WIDTH + laser.radius ||
                laser.position.y < -laser.radius ||
                laser.position.y > Constants.WORLD_HEIGHT + laser.radius
            ) {
                toRemove.add(laser)
            }
        }

        context.lasers.removeAll(toRemove)
    }

    private fun updateAsteroids(delta: Float, context: GameContext) {
        val toRemove = mutableListOf<Asteroid>()

        for (asteroid in context.asteroids) {
            asteroid.update(delta)
            if (asteroid.position.x < -asteroid.radius * 2 ||
                asteroid.position.x > Constants.WORLD_WIDTH + asteroid.radius * 2 ||
                asteroid.position.y < -asteroid.radius * 2 ||
                asteroid.position.y > Constants.WORLD_HEIGHT + asteroid.radius * 2
            ) {
                toRemove.add(asteroid)
            }
        }

        context.asteroids.removeAll(toRemove)
    }

    // ── Enemies ────────────────────────────────────────────────

    private fun updateEnemies(delta: Float, context: GameContext) {
        val multiplier = context.difficultyManager.scrollSpeedMultiplier
        val toRemove = mutableListOf<Enemy>()

        for (enemy in context.enemies) {
            // DarkClone Y-position is driven by MirrorSystem
            if (enemy is DarkClone) {
                val mirrored = context.mirrorSystem.getMirroredAction(context.elapsedTime)
                if (mirrored != null) {
                    enemy.position.y = mirrored.position.y
                    enemy.isFiring = mirrored.isShooting
                }
            }

            // Apply difficulty scroll speed to X-velocity
            enemy.velocity.x = -enemyBaseSpeed(enemy) * multiplier
            enemy.update(delta)

            if (enemy.position.x < -enemy.radius * 2) {
                toRemove.add(enemy)
            }
        }

        context.enemies.removeAll(toRemove)
    }

    /** Base horizontal speed for each enemy type (units/s). */
    private fun enemyBaseSpeed(enemy: Enemy): Float = when (enemy.getType()) {
        EnemyType.LIGHT_FIGHTER -> Constants.ENEMY_LIGHT_SPEED
        EnemyType.MEDIUM_FRIGATE -> Constants.ENEMY_MEDIUM_SPEED
        EnemyType.HEAVY_DESTROYER -> Constants.ENEMY_HEAVY_SPEED
        EnemyType.DARK_CLONE -> Constants.ENEMY_CLONE_SPEED
    }

    // ── Astronauts ─────────────────────────────────────────────

    private fun updateAstronauts(delta: Float, context: GameContext) {
        val multiplier = context.difficultyManager.scrollSpeedMultiplier
        val toRemove = mutableListOf<Astronaut>()

        for (astronaut in context.astronauts) {
            astronaut.velocity.x = -Constants.ASTRONAUT_SPEED * multiplier
            astronaut.update(delta)

            if (astronaut.shouldRemove ||
                astronaut.position.x < -astronaut.radius * 2
            ) {
                toRemove.add(astronaut)
            }
        }

        context.astronauts.removeAll(toRemove)
    }

    // ── Debris ─────────────────────────────────────────────────

    private fun updateDebris(delta: Float, context: GameContext) {
        val multiplier = context.difficultyManager.scrollSpeedMultiplier
        val toRemove = mutableListOf<SpaceDebris>()

        for (debris in context.debris) {
            debris.velocity.x = -Constants.DEBRIS_SPEED * multiplier
            debris.update(delta)

            if (debris.position.x < -debris.radius * 2) {
                toRemove.add(debris)
            }
        }

        context.debris.removeAll(toRemove)
    }
}
