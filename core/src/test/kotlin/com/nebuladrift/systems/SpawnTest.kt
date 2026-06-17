package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        assertTrue(context.asteroids.isEmpty(), "No asteroids at game start")
    }

    @Test
    fun `asteroid spawns after spawn interval`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        assertEquals(1, context.asteroids.size, "One asteroid should spawn after interval")
    }

    @Test
    fun `no asteroid before spawn interval elapses`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL - 0.1f, context)

        assertTrue(context.asteroids.isEmpty(), "No spawn before interval elapses")
    }

    @Test
    fun `asteroids spawn repeatedly at intervals`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        // SpawnSystem uses `if` not `while`, so only one spawn per update call
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        assertEquals(3, context.asteroids.size, "Three asteroids should spawn after three intervals")
    }

    @Test
    fun `spawn timer resets after each spawn`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)
        assertEquals(1, context.asteroids.size)

        // A small additional delta should NOT trigger another spawn
        spawnSystem.update(0.1f, context)
        assertEquals(1, context.asteroids.size, "No extra spawn immediately after reset")
    }

    @Test
    fun `spawning across multiple frames accumulates`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        // Simulate 10 frames of 0.2 seconds each = 2.0 seconds total
        repeat(10) {
            spawnSystem.update(0.2f, context)
        }

        assertEquals(1, context.asteroids.size, "Should spawn one asteroid after 2 seconds across frames")
    }

    // ── Spawn position ───────────────────────────────────────

    @Test
    fun `asteroids spawn at right edge of screen`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        val asteroid = context.asteroids.first()
        val expectedMinX = Constants.WORLD_WIDTH
        assertTrue(asteroid.position.x >= expectedMinX, "Asteroid should spawn at or beyond right edge")
        assertTrue(asteroid.velocity.x < 0f, "Asteroid should move left (negative x velocity)")
    }

    @Test
    fun `asteroid spawns with random Y within screen bounds`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        spawnSystem.update(Constants.ASTEROID_SPAWN_INTERVAL, context)

        val asteroid = context.asteroids.first()
        assertTrue(asteroid.radius > 0f, "Asteroid should have positive radius")
        assertTrue(asteroid.health > 0, "Asteroid should have positive HP")
        assertTrue(asteroid.size in AsteroidSize.values().toList(), "Asteroid should have a valid size")
    }

    // ── Size distribution (approximate over many spawns) ─────

    @Test
    fun `size distribution produces multiple different sizes`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

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

    // ── EnemySpawnSystem ──────────────────────────────────────

    @Test
    fun `enemySpawnSystem does not spawn during safe zone`() {
        val enemySpawn = EnemySpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0,
            elapsedTime = Constants.DIFFICULTY_SAFE_ZONE - 1f)

        enemySpawn.update(Constants.ENEMY_SPAWN_INTERVAL, context)

        assertTrue(context.enemies.isEmpty(), "No enemies during safe zone")
    }

    @Test
    fun `enemySpawnSystem spawns enemy after safe zone`() {
        val enemySpawn = EnemySpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0,
            elapsedTime = Constants.DIFFICULTY_SAFE_ZONE + 1f)

        enemySpawn.update(Constants.ENEMY_SPAWN_INTERVAL, context)

        assertEquals(1, context.enemies.size, "One enemy should spawn after interval past safe zone")
    }

    @Test
    fun `enemySpawnSystem spawns enemy at right edge`() {
        val enemySpawn = EnemySpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0,
            elapsedTime = Constants.DIFFICULTY_SAFE_ZONE + 1f)

        enemySpawn.update(Constants.ENEMY_SPAWN_INTERVAL, context)

        val enemy = context.enemies.first()
        assertTrue(enemy.position.x >= Constants.WORLD_WIDTH, "Enemy should spawn at or beyond right edge, was ${enemy.position.x}")
        assertTrue(enemy.position.y >= 0f, "Enemy Y should be >= 0, was ${enemy.position.y}")
        assertTrue(enemy.position.y <= Constants.WORLD_HEIGHT, "Enemy Y should be <= WORLD_HEIGHT, was ${enemy.position.y}")
    }

    @Test
    fun `enemySpawnSystem does not spawn before interval elapses`() {
        val enemySpawn = EnemySpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0,
            elapsedTime = Constants.DIFFICULTY_SAFE_ZONE + 1f)

        enemySpawn.update(Constants.ENEMY_SPAWN_INTERVAL - 1f, context)

        assertTrue(context.enemies.isEmpty(), "No enemy should spawn before interval")
    }

    @Test
    fun `enemySpawnSystem resets timer after spawning`() {
        val enemySpawn = EnemySpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0,
            elapsedTime = Constants.DIFFICULTY_SAFE_ZONE + 1f)

        enemySpawn.update(Constants.ENEMY_SPAWN_INTERVAL, context)
        assertEquals(1, context.enemies.size)

        // Small additional delta should NOT spawn another
        enemySpawn.update(0.1f, context)
        assertEquals(1, context.enemies.size, "No extra spawn immediately after reset")
    }

    @Test
    fun `enemySpawnSystem uses difficulty weights for distribution`() {
        val enemySpawn = EnemySpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0,
            elapsedTime = Constants.DIFFICULTY_SAFE_ZONE + 1f)

        // Spawn several enemies and verify they all have valid types
        repeat(10) {
            enemySpawn.update(Constants.ENEMY_SPAWN_INTERVAL, context)
        }

        assertTrue(context.enemies.isNotEmpty(), "Should have spawned enemies")
        for (enemy in context.enemies) {
            assertNotNull(enemy.getType(), "Enemy should have a valid type")
        }
    }

    // ── AstronautSpawnSystem ──────────────────────────────────

    @Test
    fun `astronautSpawnSystem spawns astronaut after interval`() {
        val astroSpawn = AstronautSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(), score = 0)

        // Update past the full interval (base + max jitter)
        astroSpawn.update(Constants.ASTRONAUT_SPAWN_INTERVAL + 3f, context)

        assertEquals(1, context.astronauts.size, "One astronaut should spawn after interval")
    }

    @Test
    fun `astronautSpawnSystem spawns at right edge with random Y`() {
        val astroSpawn = AstronautSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(), score = 0)

        astroSpawn.update(Constants.ASTRONAUT_SPAWN_INTERVAL + 3f, context)

        val astronaut = context.astronauts.first()
        assertTrue(astronaut.position.x >= Constants.WORLD_WIDTH, "Astronaut should spawn at right edge")
        assertTrue(astronaut.position.y >= 0f, "Astronaut Y should be >= 0")
        assertTrue(astronaut.position.y <= Constants.WORLD_HEIGHT, "Astronaut Y should be within bounds")
    }

    @Test
    fun `astronautSpawnSystem enforces max 1 active`() {
        val astroSpawn = AstronautSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(), score = 0)

        // Spawn first astronaut
        astroSpawn.update(Constants.ASTRONAUT_SPAWN_INTERVAL + 3f, context)
        assertEquals(1, context.astronauts.size)

        // Try to spawn another while one is still active
        astroSpawn.update(Constants.ASTRONAUT_SPAWN_INTERVAL + 3f, context)
        assertEquals(1, context.astronauts.size, "Should not spawn second astronaut while one is active")
    }

    @Test
    fun `astronautSpawnSystem spawns again after previous is removed`() {
        val astroSpawn = AstronautSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(), score = 0)

        // Spawn first astronaut
        astroSpawn.update(Constants.ASTRONAUT_SPAWN_INTERVAL + 3f, context)
        assertEquals(1, context.astronauts.size)

        // Remove the astronaut (simulating collection/despawn)
        context.astronauts.clear()

        // Should spawn a new one after interval
        astroSpawn.update(Constants.ASTRONAUT_SPAWN_INTERVAL + 3f, context)
        assertEquals(1, context.astronauts.size, "Should spawn again after previous is removed")
    }

    // ── DebrisSpawnSystem ─────────────────────────────────────

    @Test
    fun `debrisSpawnSystem spawns debris after interval`() {
        val debrisSpawn = DebrisSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(),
            events = mutableListOf(), score = 0)

        // Update past the full interval (base + max jitter)
        debrisSpawn.update(Constants.DEBRIS_SPAWN_INTERVAL + 5f, context)

        assertEquals(1, context.debris.size, "One debris should spawn after interval")
    }

    @Test
    fun `debrisSpawnSystem spawns at right edge with random Y`() {
        val debrisSpawn = DebrisSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(),
            events = mutableListOf(), score = 0)

        debrisSpawn.update(Constants.DEBRIS_SPAWN_INTERVAL + 5f, context)

        val debris = context.debris.first()
        assertTrue(debris.position.x >= Constants.WORLD_WIDTH, "Debris should spawn at right edge")
        assertTrue(debris.position.y >= 0f, "Debris Y should be >= 0")
        assertTrue(debris.position.y <= Constants.WORLD_HEIGHT, "Debris Y should be within bounds")
    }

    @Test
    fun `debrisSpawnSystem enforces max 1 active`() {
        val debrisSpawn = DebrisSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(),
            events = mutableListOf(), score = 0)

        // Spawn first debris
        debrisSpawn.update(Constants.DEBRIS_SPAWN_INTERVAL + 5f, context)
        assertEquals(1, context.debris.size)

        // Try to spawn another while one is still active
        debrisSpawn.update(Constants.DEBRIS_SPAWN_INTERVAL + 5f, context)
        assertEquals(1, context.debris.size, "Should not spawn second debris while one is active")
    }

    @Test
    fun `debrisSpawnSystem spawns again after previous is removed`() {
        val debrisSpawn = DebrisSpawnSystem()
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(),
            events = mutableListOf(), score = 0)

        // Spawn first debris
        debrisSpawn.update(Constants.DEBRIS_SPAWN_INTERVAL + 5f, context)
        assertEquals(1, context.debris.size)

        // Remove the debris (simulating collection/despawn)
        context.debris.clear()

        // Should spawn a new one after interval
        debrisSpawn.update(Constants.DEBRIS_SPAWN_INTERVAL + 5f, context)
        assertEquals(1, context.debris.size, "Should spawn again after previous is removed")
    }
}
