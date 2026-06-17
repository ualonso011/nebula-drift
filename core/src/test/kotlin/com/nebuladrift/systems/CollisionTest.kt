package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for circle-circle collision detection in [CollisionSystem].
 *
 * The [CollisionSystem.overlap] method is private, so all tests exercise
 * collision detection through the public [CollisionSystem.update] path:
 *
 * - Laser ↔ Asteroid: laser is consumed on overlap, asteroid loses HP
 * - Ship ↔ Asteroid: ship takes damage on overlap, asteroid is removed
 */
class CollisionTest {

    private lateinit var collisionSystem: CollisionSystem

    @BeforeEach
    fun setUp() {
        collisionSystem = CollisionSystem()
    }

    // ── Laser ↔ Asteroid — overlap variants ──────────────────

    @Test
    fun `laser and asteroid overlapping triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.25f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship, mutableListOf(asteroid), mutableListOf(laser), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser should be removed after collision")
        assertEquals(2, asteroid.health, "Asteroid should lose 1 HP (LARGE has 3 HP)")
    }

    @Test
    fun `laser and asteroid touching at distance equals sum of radii triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))

        // Use a slightly smaller distance than the exact sum of radii
        // to avoid floating-point precision issues when computing 8f + 0.85f
        val epsilon = 0.0001f
        val touchingDistance = Constants.LASER_RADIUS + Constants.ASTEROID_LARGE_RADIUS - epsilon
        val asteroid = Asteroid(
            position = Vector2(8f + touchingDistance, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship, mutableListOf(asteroid), mutableListOf(laser), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser should be removed when circles touch")
        assertEquals(2, asteroid.health, "Asteroid should lose 1 HP from touching collision")
    }

    @Test
    fun `laser and asteroid separate should not collide`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val justOutOfRange = Constants.LASER_RADIUS + Constants.ASTEROID_LARGE_RADIUS + 0.01f
        val asteroid = Asteroid(
            position = Vector2(8f + justOutOfRange, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship, mutableListOf(asteroid), mutableListOf(laser), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isNotEmpty(), "Laser should remain when no collision")
        assertEquals(3, asteroid.health, "Asteroid should not take damage")
    }

    @Test
    fun `laser inside asteroid triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship, mutableListOf(asteroid), mutableListOf(laser), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser inside asteroid should collide")
    }

    @Test
    fun `laser at same center as asteroid triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.SMALL
        )
        val context = GameContext(ship, mutableListOf(asteroid), mutableListOf(laser), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser at same center should collide")
    }

    @Test
    fun `asteroid destroyed on final hit awards points and fires event`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.2f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.SMALL // 1 HP
        )
        val context = GameContext(ship, mutableListOf(asteroid), mutableListOf(laser), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser consumed")
        assertTrue(context.asteroids.isEmpty(), "SMALL asteroid destroyed in one hit")
        assertTrue(context.events.any { it is GameEvent.AsteroidDestroyed }, "Destruction event fired")
        assertEquals(Constants.SCORE_SMALL, context.score, "Points awarded for destruction")
    }

    // ── Ship ↔ Asteroid — overlap variants ──────────────────

    @Test
    fun `ship and asteroid overlapping triggers damage`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.5f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(localShip, mutableListOf(asteroid), mutableListOf(), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertEquals(2, localShip.lives, "Ship should lose a life on collision")
        assertTrue(context.asteroids.isEmpty(), "Asteroid removed after ship collision")
        assertTrue(context.events.any { it is GameEvent.ShipHit }, "Ship hit event fired")
    }

    @Test
    fun `ship and asteroid separate should not collide`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(10f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(localShip, mutableListOf(asteroid), mutableListOf(), mutableListOf(), 0)

        collisionSystem.update(0.016f, context)

        assertEquals(3, localShip.lives, "Ship should not lose a life")
        assertTrue(context.asteroids.isNotEmpty(), "Asteroid should remain")
        assertFalse(context.events.any { it is GameEvent.ShipHit }, "No ship hit event")
    }

    @Test
    fun `invulnerable ship does not take damage from asteroid`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(localShip, mutableListOf(asteroid), mutableListOf(), mutableListOf(), 0)

        // Frame 1: ship gets hit → becomes invulnerable
        collisionSystem.update(0.016f, ctx)
        assertEquals(2, localShip.lives, "Ship should lose a life in first collision")
        assertTrue(localShip.isInvulnerable, "Ship should be invulnerable after hit")

        // Frame 2: add a fresh asteroid while ship is still invulnerable
        val asteroid2 = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        ctx.asteroids.add(asteroid2)
        ctx.events.clear()

        collisionSystem.update(0.016f, ctx)

        assertEquals(2, localShip.lives, "Ship should NOT lose life while invulnerable")
        assertTrue(ctx.asteroids.isNotEmpty(), "Asteroid should NOT be removed when ship is invulnerable")
        assertFalse(ctx.events.any { it is GameEvent.ShipHit }, "No ship hit event while invulnerable")
    }
}
