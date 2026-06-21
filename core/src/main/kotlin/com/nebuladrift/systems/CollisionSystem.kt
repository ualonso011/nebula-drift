package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Entity
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.LaserOwner
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.util.Constants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Circle-circle collision detection and response.
 *
 * Checks two collision pairs each frame:
 * 1. Laser ↔ Asteroid — asteroid loses 1 HP, laser is consumed
 * 2. Ship ↔ Asteroid — ship loses 1 life (if not invulnerable), asteroid removed
 *
 * When a large or medium asteroid is destroyed, it spawns 1–2 smaller
 * asteroids at its position (size degradation per REQ-INIT-005).
 */
class CollisionSystem : GameSystem {

    override fun update(delta: Float, context: GameContext) {
        checkLaserAsteroidCollisions(context)
        checkShipAsteroidCollisions(context)
        checkLaserEnemyCollisions(context)
        checkShipEnemyCollisions(context)
        checkShipAstronautCollisions(context)
        checkLaserAstronautCollisions(context)
        checkShipDebrisCollisions(context)

        // Enemy laser collisions (Phase 2 — new)
        checkEnemyLaserShipCollisions(context)
        checkEnemyLaserAsteroidCollisions(context)

        // Laser ↔ Laser
        checkLaserLaserCollisions(context)

        // Entity ↔ Asteroid (enemies and astronauts die on contact)
        checkEnemyAsteroidCollisions(context)
        checkAstronautAsteroidCollisions(context)
    }

    // ── Laser ↔ Asteroid ──────────────────────────────────────

    private fun checkLaserAsteroidCollisions(context: GameContext) {
        val lasersToRemove = mutableListOf<Laser>()
        val asteroidsToRemove = mutableListOf<Asteroid>()
        val newAsteroids = mutableListOf<Asteroid>()

        for (laser in context.lasers) {
            // Skip enemy lasers — handled by checkEnemyLaserAsteroidCollisions
            if (laser.owner != LaserOwner.PLAYER) continue
            if (lasersToRemove.contains(laser)) continue

            for (asteroid in context.asteroids) {
                if (asteroidsToRemove.contains(asteroid)) continue

                if (overlap(laser, asteroid)) {
                    // Laser hits asteroid
                    lasersToRemove.add(laser)
                    context.events.add(GameEvent.LaserAsteroidHit(laser, asteroid))

                    // Impact effect: slow asteroid and deflect trajectory
                    asteroid.velocity.scl(0.7f) // reduce speed by 30%
                    // Nudge trajectory perpendicular to incoming laser
                    val nudgeAngle = Random.nextFloat() * 0.6f - 0.3f // ±0.3 radians
                    val cosA = kotlin.math.cos(nudgeAngle.toDouble()).toFloat()
                    val sinA = kotlin.math.sin(nudgeAngle.toDouble()).toFloat()
                    val oldVx = asteroid.velocity.x
                    val oldVy = asteroid.velocity.y
                    asteroid.velocity.x = oldVx * cosA - oldVy * sinA
                    asteroid.velocity.y = oldVx * sinA + oldVy * cosA

                    if (asteroid.hit()) {
                        // Asteroid destroyed — award points and split
                        asteroidsToRemove.add(asteroid)
                        val points = destructionPoints(asteroid.size)
                        context.events.add(GameEvent.AsteroidDestroyed(asteroid, points))
                        context.score += points
                        spawnChildren(asteroid, newAsteroids)
                    }
                    break
                }
            }
        }

        context.lasers.removeAll(lasersToRemove)
        context.asteroids.removeAll(asteroidsToRemove)
        context.asteroids.addAll(newAsteroids)
    }

    // ── Ship ↔ Asteroid ───────────────────────────────────────

    private fun checkShipAsteroidCollisions(context: GameContext) {
        val ship = context.ship
        if (ship.isInvulnerable || ship.isDestroyed) return

        val toRemove = mutableListOf<Asteroid>()

        for (asteroid in context.asteroids) {
            if (overlap(ship, asteroid)) {
                if (ship.takeDamage()) {
                    context.events.add(GameEvent.ShipHit(ship, ship.lives))
                    if (ship.isDestroyed) {
                        context.events.add(GameEvent.ShipDestroyed(ship))
                    }
                    toRemove.add(asteroid)
                }
                break // Only one collision per frame
            }
        }

        context.asteroids.removeAll(toRemove)
    }

    // ── Laser ↔ Enemy ─────────────────────────────────────────

    private fun checkLaserEnemyCollisions(context: GameContext) {
        val lasersToRemove = mutableListOf<Laser>()
        val enemiesToRemove = mutableListOf<Enemy>()

        for (laser in context.lasers) {
            // Only player lasers damage enemies — enemy lasers are checked
            // against the player ship via checkEnemyLaserShipCollisions.
            if (laser.owner != LaserOwner.PLAYER) continue
            if (lasersToRemove.contains(laser)) continue

            for (enemy in context.enemies) {
                if (enemiesToRemove.contains(enemy)) continue

                if (overlap(laser, enemy)) {
                    lasersToRemove.add(laser)

                    if (enemy.takeDamage()) {
                        enemiesToRemove.add(enemy)
                        context.events.add(GameEvent.EnemyDestroyed(enemy, enemy.points))
                        context.score += enemy.points
                    }
                    break
                }
            }
        }

        context.lasers.removeAll(lasersToRemove)
        context.enemies.removeAll(enemiesToRemove)
    }

    // ── Ship ↔ Enemy ──────────────────────────────────────────

    private fun checkShipEnemyCollisions(context: GameContext) {
        val ship = context.ship
        if (ship.isInvulnerable || ship.isDestroyed) return

        val toRemove = mutableListOf<Enemy>()

        for (enemy in context.enemies) {
            if (overlap(ship, enemy)) {
                if (ship.takeDamage()) {
                    context.events.add(GameEvent.ShipHit(ship, ship.lives))
                    if (ship.isDestroyed) {
                        context.events.add(GameEvent.ShipDestroyed(ship))
                    }
                    toRemove.add(enemy)
                }
                break
            }
        }

        context.enemies.removeAll(toRemove)
    }

    // ── Ship ↔ Astronaut ──────────────────────────────────────

    private fun checkShipAstronautCollisions(context: GameContext) {
        val ship = context.ship
        if (ship.isInvulnerable || ship.isDestroyed) return

        for (astronaut in context.astronauts) {
            if (astronaut.state == Astronaut.State.FLOATING && overlap(ship, astronaut)) {
                astronaut.rescue()
                context.events.add(GameEvent.AstronautRescued(astronaut))
                context.score += Constants.SCORE_ASTRONAUT_RESCUE
                break
            }
        }
    }

    // ── Laser ↔ Astronaut ─────────────────────────────────────

    private fun checkLaserAstronautCollisions(context: GameContext) {
        val lasersToRemove = mutableListOf<Laser>()

        for (laser in context.lasers) {
            if (lasersToRemove.contains(laser)) continue

            for (astronaut in context.astronauts) {
                if (astronaut.state == Astronaut.State.FLOATING && overlap(laser, astronaut)) {
                    lasersToRemove.add(laser)
                    astronaut.kill()
                    context.events.add(GameEvent.AstronautKilled(astronaut))
                    context.score -= Constants.SCORE_ASTRONAUT_KILL_PENALTY
                    break
                }
            }
        }

        context.lasers.removeAll(lasersToRemove)
    }

    // ── Ship ↔ Debris ─────────────────────────────────────────

    private fun checkShipDebrisCollisions(context: GameContext) {
        val ship = context.ship
        if (ship.isDestroyed || ship.lives >= Constants.SHIP_LIVES) return

        val toRemove = mutableListOf<SpaceDebris>()

        for (debris in context.debris) {
            if (overlap(ship, debris)) {
                ship.addLife()
                context.events.add(GameEvent.DebrisCollected(debris))
                toRemove.add(debris)
                break
            }
        }

        context.debris.removeAll(toRemove)
    }

    // ── Enemy laser ↔ Ship ─────────────────────────────────────

    /**
     * Check collisions between enemy lasers and the player ship.
     * Enemy lasers that overlap the ship will damage it (skip
     * invulnerability check — already performed by [Ship.takeDamage]).
     */
    private fun checkEnemyLaserShipCollisions(context: GameContext) {
        val ship = context.ship
        if (ship.isDestroyed) return

        val lasersToRemove = mutableListOf<Laser>()

        for (laser in context.lasers) {
            if (laser.owner == LaserOwner.PLAYER) continue
            if (lasersToRemove.contains(laser)) continue

            if (overlap(laser, ship)) {
                lasersToRemove.add(laser) // always consume the laser on contact
                if (!ship.isInvulnerable && ship.takeDamage()) {
                    context.events.add(GameEvent.ShipHit(ship, ship.lives))
                    if (ship.isDestroyed) {
                        context.events.add(GameEvent.ShipDestroyed(ship))
                    }
                }
                break // only one hit per frame
            }
        }

        context.lasers.removeAll(lasersToRemove)
    }

    // ── Enemy laser ↔ Asteroid ─────────────────────────────────

    /**
     * Enemy lasers pass through asteroids — the laser is consumed
     * but the asteroid takes no damage.
     */
    private fun checkEnemyLaserAsteroidCollisions(context: GameContext) {
        val lasersToRemove = mutableListOf<Laser>()

        for (laser in context.lasers) {
            if (laser.owner == LaserOwner.PLAYER) continue
            if (lasersToRemove.contains(laser)) continue

            for (asteroid in context.asteroids) {
                if (overlap(laser, asteroid)) {
                    lasersToRemove.add(laser)
                    break
                }
            }
        }

        context.lasers.removeAll(lasersToRemove)
    }

    // ── Laser ↔ Laser ─────────────────────────────────────────

    /**
     * When a player laser overlaps an enemy laser, both are consumed
     * and a disintegration effect is emitted.
     */
    private fun checkLaserLaserCollisions(context: GameContext) {
        val lasersToRemove = mutableListOf<Laser>()

        for (playerLaser in context.lasers) {
            if (playerLaser.owner != LaserOwner.PLAYER) continue
            if (lasersToRemove.contains(playerLaser)) continue

            for (enemyLaser in context.lasers) {
                if (enemyLaser.owner == LaserOwner.PLAYER) continue
                if (lasersToRemove.contains(enemyLaser)) continue

                if (overlap(playerLaser, enemyLaser)) {
                    lasersToRemove.add(playerLaser)
                    lasersToRemove.add(enemyLaser)
                    context.events.add(GameEvent.LaserLaserHit(playerLaser, enemyLaser))
                    break
                }
            }
        }

        context.lasers.removeAll(lasersToRemove)
    }

    // ── Enemy ↔ Asteroid ─────────────────────────────────────

    /**
     * When an enemy overlaps an asteroid, the enemy is destroyed
     * (no points — the player didn't cause it). The asteroid is NOT
     * removed so it continues its path.
     */
    private fun checkEnemyAsteroidCollisions(context: GameContext) {
        val enemiesToRemove = mutableListOf<Enemy>()

        for (enemy in context.enemies) {
            for (asteroid in context.asteroids) {
                if (overlap(enemy, asteroid)) {
                    enemiesToRemove.add(enemy)
                    // Enemy destroyed by asteroid — no score event
                    break
                }
            }
        }

        context.enemies.removeAll(enemiesToRemove)
    }

    // ── Astronaut ↔ Asteroid ─────────────────────────────────

    /**
     * When a floating astronaut overlaps an asteroid, the astronaut
     * is killed — same as being hit by a laser.
     */
    private fun checkAstronautAsteroidCollisions(context: GameContext) {
        for (astronaut in context.astronauts) {
            if (astronaut.state != Astronaut.State.FLOATING) continue

            for (asteroid in context.asteroids) {
                if (overlap(astronaut, asteroid)) {
                    astronaut.kill()
                    context.events.add(GameEvent.AstronautKilled(astronaut))
                    context.score -= Constants.SCORE_ASTRONAUT_KILL_PENALTY
                    break
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Circle-circle overlap test using squared distances to avoid sqrt.
     */
    private fun overlap(a: Entity, b: Entity): Boolean {
        val dx = a.position.x - b.position.x
        val dy = a.position.y - b.position.y
        val distSq = dx * dx + dy * dy
        val radiusSum = a.radius + b.radius
        return distSq <= radiusSum * radiusSum
    }

    /** Points awarded for destroying an asteroid of the given size. */
    private fun destructionPoints(size: AsteroidSize): Int = when (size) {
        AsteroidSize.LARGE -> Constants.SCORE_LARGE
        AsteroidSize.MEDIUM -> Constants.SCORE_MEDIUM
        AsteroidSize.SMALL -> Constants.SCORE_SMALL
    }

    /**
     * Spawn smaller asteroids from a destroyed one.
     * Large → medium, medium → small, small → nothing.
     */
    private fun spawnChildren(destroyed: Asteroid, result: MutableList<Asteroid>) {
        val childSize = when (destroyed.size) {
            AsteroidSize.LARGE -> AsteroidSize.MEDIUM
            AsteroidSize.MEDIUM -> AsteroidSize.SMALL
            AsteroidSize.SMALL -> return
        }

        val count = if (Random.nextBoolean()) 1 else 2
        val basePos = destroyed.position.cpy()

        for (i in 0 until count) {
            val speed = Random.nextFloat() *
                (Constants.ASTEROID_MAX_SPEED - Constants.ASTEROID_MIN_SPEED) +
                Constants.ASTEROID_MIN_SPEED
            val angle = Random.nextFloat() * 2f * kotlin.math.PI.toFloat()
            val vx = cos(angle.toDouble()).toFloat() * speed
            val vy = sin(angle.toDouble()).toFloat() * speed

            result.add(
                Asteroid(
                    position = Vector2(basePos.x + cos(angle.toDouble()).toFloat() * 0.1f,
                                       basePos.y + sin(angle.toDouble()).toFloat() * 0.1f),
                    velocity = Vector2(vx, vy),
                    size = childSize,
                    type = destroyed.type
                )
            )
        }
    }
}
