package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Ship
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for asteroid spawning in [SpawnSystem].
 *
 * Covers spawn timing, position (right edge, random Y within bounds),
 * size distribution (approximate over many spawns), and the entity cap.
 */
class SpawnTest {

    private lateinit var spawnSystem: SpawnSystem

    @BeforeEach
    fun setUp() {
        spawnSystem = SpawnSystem()
    }

    // ── Spawn timing ─────────────────────────────────────────

    @Test
    fun `no asteroids at game start`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        assertTrue(context.asteroids.isEmpty(), "No asteroids at game start")
    }

    @Test
    fun `asteroid spawns after spawn interval`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        assertEquals(1, context.asteroids.size, "One asteroid should spawn after interval")
    }

    @Test
    fun `no asteroid before spawn interval elapses`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL - 0.1f, context)

        assertTrue(context.asteroids.isEmpty(), "No spawn before interval elapses")
    }

    @Test
    fun `asteroids spawn repeatedly at intervals`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // SpawnSystem uses `if` not `while`, so only one spawn per update call
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        assertEquals(3, context.asteroids.size, "Three asteroids should spawn after three intervals")
    }

    @Test
    fun `spawn timer resets after each spawn`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        assertEquals(1, context.asteroids.size)

        // A small additional delta should NOT trigger another spawn
        spawnSystem.update(0.1f, context)
        assertEquals(1, context.asteroids.size, "No extra spawn immediately after reset")
    }

    @Test
    fun `spawning across multiple frames accumulates`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // Simulate 10 frames of 0.2 seconds each = 2.0 seconds total
        repeat(10) {
            spawnSystem.update(0.2f, context)
        }

        assertEquals(1, context.asteroids.size, "Should spawn one asteroid after 2 seconds across frames")
    }

    // ── Spawn position ───────────────────────────────────────

    @Test
    fun `asteroids spawn at right edge of screen`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        val asteroid = context.asteroids.first()
        val expectedMinX = Constants.WORLD_WIDTH
        assertTrue(asteroid.position.x >= expectedMinX, "Asteroid should spawn at or beyond right edge")
        assertTrue(asteroid.velocity.x < 0f, "Asteroid should move left (negative x velocity)")
    }

    @Test
    fun `asteroid spawns with random Y within screen bounds`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // Spawn up to entity cap and verify all Y positions are within bounds
        repeat(Constants.ASTEROID_MAX_COUNT) {
            spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        }

        for (asteroid in context.asteroids) {
            assertTrue(asteroid.position.y >= 0f, "Asteroid Y should be >= 0, was ${asteroid.position.y}")
            assertTrue(
                asteroid.position.y <= Constants.WORLD_HEIGHT,
                "Asteroid Y should be <= WORLD_HEIGHT, was ${asteroid.position.y}"
            )
        }
    }

    @Test
    fun `spawned asteroid has correct entity structure`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        val asteroid = context.asteroids.first()
        assertTrue(asteroid.radius > 0f, "Asteroid should have positive radius")
        assertTrue(asteroid.health > 0, "Asteroid should have positive HP")
        assertTrue(asteroid.size in AsteroidSize.values().toList(), "Asteroid should have a valid size")
    }

    // ── Size distribution (approximate over many spawns) ─────

    @Test
    fun `size distribution produces multiple different sizes`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // Spawn up to the entity cap
        repeat(Constants.ASTEROID_MAX_COUNT) {
            spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        }

        assertEquals(Constants.ASTEROID_MAX_COUNT, context.asteroids.size,
            "Should spawn up to max count")

        val sizes = context.asteroids.map { it.size }.distinct()
        assertTrue(sizes.size >= 2, "Should produce at least 2 different asteroid sizes, got $sizes")
    }

    // ── Entity cap ───────────────────────────────────────────

    @Test
    fun `no spawning when entity cap reached`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // Fill the context with asteroids up to the cap
        repeat(Constants.ASTEROID_MAX_COUNT) { i ->
            context.asteroids.add(
                Asteroid(
                    position = Vector2(i.toFloat(), 0f),
                    velocity = Vector2.Zero.cpy(),
                    size = AsteroidSize.SMALL
                )
            )
        }

        assertEquals(Constants.ASTEROID_MAX_COUNT, context.asteroids.size)

        // Try to spawn more
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        assertEquals(
            Constants.ASTEROID_MAX_COUNT,
            context.asteroids.size,
            "No additional asteroid should spawn when at max count"
        )
    }

    @Test
    fun `spawning resumes when count drops below cap`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // Fill to cap
        repeat(Constants.ASTEROID_MAX_COUNT) {
            context.asteroids.add(
                Asteroid(Vector2.Zero.cpy(), Vector2.Zero.cpy(), AsteroidSize.SMALL)
            )
        }

        // Try spawning (should be blocked)
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        assertEquals(Constants.ASTEROID_MAX_COUNT, context.asteroids.size)

        // Remove some asteroids
        repeat(5) { context.asteroids.removeLast() }

        // Now spawning should work
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        assertEquals(
            Constants.ASTEROID_MAX_COUNT - 5 + 1,
            context.asteroids.size,
            "Spawning should resume when count drops below cap"
        )
    }

    @Test
    fun `spawning blocked exactly at cap threshold`() {
        val context = GameContext(Ship(), mutableListOf(), mutableListOf(), mutableListOf(), 0)

        // Fill to cap - 1, then one spawn should succeed
        repeat(Constants.ASTEROID_MAX_COUNT - 1) {
            context.asteroids.add(
                Asteroid(Vector2.Zero.cpy(), Vector2.Zero.cpy(), AsteroidSize.SMALL)
            )
        }

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        assertEquals(
            Constants.ASTEROID_MAX_COUNT,
            context.asteroids.size,
            "Should spawn one when at cap-1"
        )

        // One more spawn should be blocked
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        assertEquals(
            Constants.ASTEROID_MAX_COUNT,
            context.asteroids.size,
            "Should NOT spawn when at cap"
        )
    }
}
