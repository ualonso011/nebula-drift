package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.DarkClone
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.entities.enemies.MediumFrigate
import com.nebuladrift.entities.enemies.HeavyDestroyer
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, context)

        assertTrue(ship.position.y < initialY, "Ship should fall when not thrusting")
    }

    @Test
    fun `ship rises with upward velocity`() {
        val ship = Ship()
        ship.velocity.y = 8f // Flappy Bird style flap velocity
        val initialY = ship.position.y
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, context)

        assertTrue(ship.position.y > initialY, "Ship should rise with upward velocity")
    }

    // ── Ship: screen-bounds clamping ─────────────────────────

    @Test
    fun `ship takes damage when falling off bottom`() {
        val ship = Ship()
        ship.position.y = -10f
        ship.velocity.y = -50f
        val initialLives = ship.lives
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, context)

        // Ship should take damage and be repositioned to center
        assertEquals(initialLives - 1, ship.lives, "Ship should lose a life when falling off bottom")
        assertEquals(
            Constants.WORLD_HEIGHT / 2f,
            ship.position.y,
            0.001f,
            "Ship should be repositioned to center after falling"
        )
    }

    @Test
    fun `ship clamped to top screen bound`() {
        val ship = Ship()
        ship.position.y = 20f
        ship.velocity.y = 50f
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, context)

        // After damping: vx = 10 * 0.98 = 9.8 (only damping affects vx, no x forces)
        assertEquals(9.8f, ship.velocity.x, 0.001f, "Horizontal velocity should be damped by 0.98")
        assertTrue(ship.velocity.len() < 10f, "Velocity magnitude should decrease due to damping")
    }

    @Test
    fun `damping reduces both velocity components`() {
        val ship = Ship()
        ship.velocity.set(5f, 5f)
        val context = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
            ship = Ship(),
            asteroids = mutableListOf(asteroid),
            lasers = mutableListOf(),
            events = mutableListOf(),
            score = 0
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
            ship = Ship(),
            asteroids = mutableListOf(slow, fast),
            lasers = mutableListOf(),
            events = mutableListOf(),
            score = 0
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
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(laser),
            events = mutableListOf(),
            score = 0
        )

        physicsSystem.update(0.1f, context)

        assertEquals(5f + Constants.LASER_SPEED * 0.1f, laser.position.x, 0.001f, "Laser should move right")
    }

    @Test
    fun `lasers age over time`() {
        val laser = Laser(position = Vector2(5f, 5f))
        val context = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(laser),
            events = mutableListOf(),
            score = 0
        )

        physicsSystem.update(0.5f, context)

        assertEquals(0.5f, laser.age, 0.001f, "Laser should age by delta")
    }

    @Test
    fun `expired lasers are removed from context`() {
        val laser = Laser(position = Vector2(5f, 5f))
        val context = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(laser),
            events = mutableListOf(),
            score = 0
        )

        // Run past laser lifetime
        physicsSystem.update(Constants.LASER_LIFETIME + 0.1f, context)

        assertTrue(context.lasers.isEmpty(), "Expired laser should be removed")
    }

    @Test
    fun `off-screen lasers are removed from context`() {
        val laser = Laser(position = Vector2(Constants.WORLD_WIDTH + 10f, 5f))
        val context = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(laser),
            events = mutableListOf(),
            score = 0
        )

        physicsSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Off-screen laser should be removed")
    }

    // ── Enemies: movement ─────────────────────────────────────

    @Test
    fun `lightFighter moves left at its speed`() {
        val enemy = LightFighter(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.ENEMY_LIGHT_SPEED * 1.0f * 0.1f
        assertEquals(expectedX, enemy.position.x, 0.01f, "LightFighter should move left at its speed")
    }

    @Test
    fun `mediumFrigate moves left at its speed`() {
        val enemy = MediumFrigate(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.ENEMY_MEDIUM_SPEED * 1.0f * 0.1f
        assertEquals(expectedX, enemy.position.x, 0.01f)
    }

    @Test
    fun `heavyDestroyer moves left at its speed`() {
        val enemy = HeavyDestroyer(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.ENEMY_HEAVY_SPEED * 1.0f * 0.1f
        assertEquals(expectedX, enemy.position.x, 0.01f)
    }

    @Test
    fun `enemy speed is multiplied by scrollSpeedMultiplier`() {
        val enemy = LightFighter(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)
        ctx.difficultyManager.scrollSpeedMultiplier = 2.0f

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.ENEMY_LIGHT_SPEED * 2.0f * 0.1f
        assertEquals(expectedX, enemy.position.x, 0.01f,
            "Enemy should move faster with higher scroll speed multiplier")
    }

    @Test
    fun `off-screen enemies are removed from context`() {
        val enemy = LightFighter(Vector2(-10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        physicsSystem.update(0.016f, ctx)

        assertTrue(ctx.enemies.isEmpty(), "Off-screen enemy should be removed")
    }

    @Test
    fun `on-screen enemies are not removed`() {
        val enemy = LightFighter(Vector2(5f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        physicsSystem.update(0.016f, ctx)

        assertTrue(ctx.enemies.isNotEmpty(), "On-screen enemy should remain")
    }

    // ── Astronauts: movement ──────────────────────────────────

    @Test
    fun `astronaut moves left at ASTRONAUT_SPEED`() {
        val astronaut = Astronaut(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.ASTRONAUT_SPEED * 1.0f * 0.1f
        assertEquals(expectedX, astronaut.position.x, 0.01f,
            "Astronaut should move left at ASTRONAUT_SPEED")
    }

    @Test
    fun `astronaut speed is multiplied by scrollSpeedMultiplier`() {
        val astronaut = Astronaut(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)
        ctx.difficultyManager.scrollSpeedMultiplier = 1.5f

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.ASTRONAUT_SPEED * 1.5f * 0.1f
        assertEquals(expectedX, astronaut.position.x, 0.01f,
            "Astronaut should move faster with higher scroll multiplier")
    }

    @Test
    fun `astronaut removed after rescue animation completes`() {
        val astronaut = Astronaut(Vector2(10f, 5f))
        astronaut.rescue()
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        // Update past the 0.5s animation threshold
        physicsSystem.update(0.6f, ctx)

        assertTrue(ctx.astronauts.isEmpty(), "Astronaut should be removed after animation completes")
    }

    @Test
    fun `off-screen astronauts are removed`() {
        val astronaut = Astronaut(Vector2(-1f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        physicsSystem.update(0.016f, ctx)

        assertTrue(ctx.astronauts.isEmpty(), "Off-screen astronaut should be removed")
    }

    @Test
    fun `floating on-screen astronaut is not removed`() {
        val astronaut = Astronaut(Vector2(5f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        physicsSystem.update(0.016f, ctx)

        assertTrue(ctx.astronauts.isNotEmpty(), "On-screen floating astronaut should remain")
    }

    // ── Debris: movement ──────────────────────────────────────

    @Test
    fun `debris moves left at DEBRIS_SPEED`() {
        val debris = SpaceDebris(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.DEBRIS_SPEED * 1.0f * 0.1f
        assertEquals(expectedX, debris.position.x, 0.01f,
            "Debris should move left at DEBRIS_SPEED")
    }

    @Test
    fun `debris speed is multiplied by scrollSpeedMultiplier`() {
        val debris = SpaceDebris(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)
        ctx.difficultyManager.scrollSpeedMultiplier = 0.5f

        physicsSystem.update(0.1f, ctx)

        val expectedX = 10f - Constants.DEBRIS_SPEED * 0.5f * 0.1f
        assertEquals(expectedX, debris.position.x, 0.01f,
            "Debris should move slower with lower scroll multiplier")
    }

    @Test
    fun `debris glow phase updates over time`() {
        val debris = SpaceDebris(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)
        val initialGlow = debris.glowPhase

        physicsSystem.update(0.5f, ctx)

        assertTrue(debris.glowPhase > initialGlow, "Glow phase should increase over time")
        assertEquals(Constants.DEBRIS_GLOW_SPEED * 0.5f, debris.glowPhase, 0.001f,
            "Glow phase should increase at DEBRIS_GLOW_SPEED")
    }

    @Test
    fun `off-screen debris is removed from context`() {
        val debris = SpaceDebris(Vector2(-1f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)

        physicsSystem.update(0.016f, ctx)

        assertTrue(ctx.debris.isEmpty(), "Off-screen debris should be removed")
    }

    @Test
    fun `on-screen debris is not removed`() {
        val debris = SpaceDebris(Vector2(5f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)

        physicsSystem.update(0.016f, ctx)

        assertTrue(ctx.debris.isNotEmpty(), "On-screen debris should remain")
    }

    // ── DarkClone mirroring ───────────────────────────────────

    @Test
    fun `darkClone position is updated by mirror system`() {
        val clone = DarkClone(Vector2(10f, 5f))
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(clone), events = mutableListOf(), score = 0)

        // Record a player action for the mirror system
        ctx.mirrorSystem.recordPlayerAction(0.0f, ctx.ship, isShooting = false)
        // Set up the mirrored position
        ctx.ship.position.y = 3f
        ctx.mirrorSystem.recordPlayerAction(0.5f, ctx.ship, isShooting = false)

        // Advance elapsed time so mirror system returns the 0.0s action
        ctx.elapsedTime = 0.5f
        physicsSystem.update(0.016f, ctx)

        // The clone Y should be set by the mirror system
        // At elapsedTime=0.5f, target=0.0f, the mirrored action has ship.position.y=4.5f (initial)
        assertEquals(4.5f, clone.position.y, 0.01f, "DarkClone Y should mirror recorded ship Y")
    }
}
