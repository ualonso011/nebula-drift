package com.nebuladrift.systems

import com.badlogic.gdx.math.MathUtils
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.Laser
import com.nebuladrift.util.Constants

/**
 * Applies physics to all entities each frame.
 *
 * Behaviour:
 * - Ship: constant downward gravity, thrust when active, velocity damping, bounds clamp
 * - Lasers: update position, remove expired / off-screen
 * - Asteroids: update position, remove off-screen
 */
class PhysicsSystem : GameSystem {

    override fun update(delta: Float, context: GameContext) {
        updateShip(delta, context)
        updateLasers(delta, context)
        updateAsteroids(delta, context)
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
        ship.position.y = MathUtils.clamp(
            ship.position.y,
            ship.radius,
            Constants.WORLD_HEIGHT - ship.radius
        )

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
}
