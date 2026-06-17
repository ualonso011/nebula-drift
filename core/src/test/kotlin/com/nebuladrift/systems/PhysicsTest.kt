package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for physics simulation in [PhysicsSystem].
 *
 * Covers gravity, thrust, velocity damping, screen-bounds clamping,
 * and entity movement for all three entity types.
 */
class PhysicsTest {

    private lateinit var physicsSystem: PhysicsSystem

    @BeforeEach
    fun setUp() {
        physicsSystem = PhysicsSystem()
    }

    // ── Ship: gravity ────────────────────────────────────────

    @Test
    fun `ship falls when gravity applied and not thrusting`() {
        val ship = Ship()
        val initialY = ship.position.y
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        assertTrue(ship.position.y < initialY, "Ship should fall when not thrusting")
    }

    @Test
    fun `ship rises when thrusting counters gravity`() {
        val ship = Ship()
        ship.isThrusting = true
        val initialY = ship.position.y
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        assertTrue(ship.position.y > initialY, "Ship should rise when thrusting")
        assertTrue(ship.velocity.y > 0f, "Ship should have upward velocity when thrusting")
    }

    // ── Ship: screen-bounds clamping ─────────────────────────

    @Test
    fun `ship clamped to bottom screen bound`() {
        val ship = Ship()
        ship.position.y = -10f
        ship.velocity.y = -50f
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        assertEquals(
            Constants.SHIP_RADIUS,
            ship.position.y,
            0.001f,
            "Ship should be clamped to bottom of screen"
        )
    }

    @Test
    fun `ship clamped to top screen bound`() {
        val ship = Ship()
        ship.position.y = 20f
        ship.velocity.y = 50f
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        assertEquals(
            Constants.WORLD_HEIGHT - Constants.SHIP_RADIUS,
            ship.position.y,
            0.001f,
            "Ship should be clamped to top of screen"
        )
    }

    @Test
    fun `ship clamped to left screen bound`() {
        val ship = Ship()
        ship.position.x = -10f
        ship.velocity.x = -50f
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        assertEquals(
            Constants.SHIP_RADIUS,
            ship.position.x,
            0.001f,
            "Ship should be clamped to left of screen"
        )
    }

    @Test
    fun `ship clamped to right screen bound`() {
        val ship = Ship()
        ship.position.x = 30f
        ship.velocity.x = 50f
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        assertEquals(
            Constants.WORLD_WIDTH - Constants.SHIP_RADIUS,
            ship.position.x,
            0.001f,
            "Ship should be clamped to right of screen"
        )
    }

    // ── Ship: velocity damping ───────────────────────────────

    @Test
    fun `ship velocity is damped each frame`() {
        val ship = Ship()
        ship.velocity.set(10f, 0f)
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        // After damping: vx = 10 * 0.98 = 9.8 (only damping affects vx, no x forces)
        assertEquals(9.8f, ship.velocity.x, 0.001f, "Horizontal velocity should be damped by 0.98")
        assertTrue(ship.velocity.len() < 10f, "Velocity magnitude should decrease due to damping")
    }

    @Test
    fun `damping reduces both velocity components`() {
        val ship = Ship()
        ship.velocity.set(5f, 5f)
        val context = GameContext(ship, mutableListOf(), mutableListOf(), mutableListOf(), 0)

        physicsSystem.update(0.1f, context)

        // vx purely damped: 5 * 0.98 = 4.9
        assertEquals(4.9f, ship.velocity.x, 0.001f, "vx should be damped")
    }

    // ── Asteroids: movement ──────────────────────────────────

    @Test
    fun `asteroids move left at constant speed`() {
        val asteroid = Asteroid(
            position = Vector2(10f, 5f),
            velocity = Vector2(-5f, 0f),
            size = AsteroidSize.MEDIUM
        )
        val context = GameContext(
            Ship(),
            mutableListOf(asteroid),
            mutableListOf(),
            mutableListOf(),
            0
        )

        physicsSystem.update(0.1f, context)

        assertEquals(10f - 5f * 0.1f, asteroid.position.x, 0.001f, "Asteroid should move left")
    }

    @Test
    fun `asteroids with higher speed move faster`() {
        val slow = Asteroid(
            position = Vector2(10f, 5f),
            velocity = Vector2(-2f, 0f),
            size = AsteroidSize.LARGE
        )
        val fast = Asteroid(
            position = Vector2(10f, 6f),
            velocity = Vector2(-5f, 0f),
            size = AsteroidSize.SMALL
        )
        val context = GameContext(
            Ship(),
            mutableListOf(slow, fast),
            mutableListOf(),
            mutableListOf(),
            0
        )

        physicsSystem.update(0.1f, context)

        val slowDelta = 10f - slow.position.x
        val fastDelta = 10f - fast.position.x
        assertTrue(fastDelta > slowDelta, "Faster asteroid should cover more distance")
    }

    // ── Lasers: movement ─────────────────────────────────────

    @Test
    fun `lasers move right at high speed`() {
        val laser = Laser(position = Vector2(5f, 5f))
        val context = GameContext(
            Ship(),
            mutableListOf(),
            mutableListOf(laser),
            mutableListOf(),
            0
        )

        physicsSystem.update(0.1f, context)

        assertEquals(5f + Constants.LASER_SPEED * 0.1f, laser.position.x, 0.001f, "Laser should move right")
    }

    @Test
    fun `lasers age over time`() {
        val laser = Laser(position = Vector2(5f, 5f))
        val context = GameContext(
            Ship(),
            mutableListOf(),
            mutableListOf(laser),
            mutableListOf(),
            0
        )

        physicsSystem.update(0.5f, context)

        assertEquals(0.5f, laser.age, 0.001f, "Laser should age by delta")
    }

    @Test
    fun `expired lasers are removed from context`() {
        val laser = Laser(position = Vector2(5f, 5f))
        val context = GameContext(
            Ship(),
            mutableListOf(),
            mutableListOf(laser),
            mutableListOf(),
            0
        )

        // Run past laser lifetime
        physicsSystem.update(Constants.LASER_LIFETIME + 0.1f, context)

        assertTrue(context.lasers.isEmpty(), "Expired laser should be removed")
    }

    @Test
    fun `off-screen lasers are removed from context`() {
        val laser = Laser(position = Vector2(Constants.WORLD_WIDTH + 10f, 5f))
        val context = GameContext(
            Ship(),
            mutableListOf(),
            mutableListOf(laser),
            mutableListOf(),
            0
        )

        physicsSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Off-screen laser should be removed")
    }
}
