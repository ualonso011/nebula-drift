package com.nebuladrift.rendering

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.systems.GameEvent
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [ParticleManager] pool-backed particle system.
 *
 * These tests exercise the pool/spawn/update/clear lifecycle without
 * needing an OpenGL context — the manager accepts an optional atlas
 * and uses a no-op render path when no atlas is provided.
 *
 * Covers:
 * - Spawning each effect type produces particles
 * - Update removes expired particles and recycles them to the pool
 * - [onGameEvent] dispatches to correct effect spawners
 * - Pool limits are respected (max [Constants.PARTICLE_MAX_COUNT])
 */
class ParticleManagerTest {

    /** Create a manager without GL-dependent atlas (pool logic only). */
    private fun createManager(): ParticleManager {
        return ParticleManager(atlas = null).also { it.init() }
    }

    private lateinit var manager: ParticleManager

    @BeforeEach
    fun setUp() {
        manager = createManager()
    }

    @Test
    fun `spawn explosion creates particles`() {
        manager.spawnExplosion(Vector2(5f, 5f))
        assertTrue(manager.activeCount > 0, "Explosion should create particles")
    }

    @Test
    fun `spawn explosion with different sizes creates varying counts`() {
        manager.spawnExplosion(Vector2.Zero, ExplosionSize.SMALL)
        val smallCount = manager.activeCount
        manager.clear()

        manager.spawnExplosion(Vector2.Zero, ExplosionSize.MEDIUM)
        val mediumCount = manager.activeCount
        manager.clear()

        manager.spawnExplosion(Vector2.Zero, ExplosionSize.LARGE)
        val largeCount = manager.activeCount

        assertTrue(smallCount < mediumCount, "Small explosion should have fewer particles than medium")
        assertTrue(mediumCount < largeCount, "Medium explosion should have fewer particles than large")
    }

    @Test
    fun `spawn engine trail creates single particle`() {
        manager.spawnEngineTrail(Vector2.Zero)
        assertTrue(manager.activeCount > 0, "Engine trail should create at least one particle")
    }

    @Test
    fun `spawn rescue sparkle creates particles`() {
        manager.spawnRescueSparkle(Vector2.Zero)
        assertTrue(manager.activeCount > 0, "Rescue sparkle should create particles")
    }

    @Test
    fun `spawn debris sparkle creates particles`() {
        manager.spawnDebrisSparkle(Vector2.Zero)
        assertTrue(manager.activeCount > 0, "Debris sparkle should create particles")
    }

    @Test
    fun `spawn damage sparks creates particles`() {
        manager.spawnDamageSparks(Vector2.Zero)
        assertTrue(manager.activeCount > 0, "Damage sparks should create particles")
    }

    @Test
    fun `update removes expired particles`() {
        manager.spawnExplosion(Vector2.Zero)
        assertTrue(manager.activeCount > 0, "Particles should exist before update")

        // Advance time past max particle lifetime (explosion max life ~1.0s)
        manager.update(2.0f)

        assertEquals(0, manager.activeCount, "All particles should be expired after 2 seconds")
    }

    @Test
    fun `update moves particles and applies friction`() {
        manager.spawnExplosion(Vector2(10f, 10f), ExplosionSize.LARGE)
        assertTrue(manager.activeCount > 0)

        // Save first particle position via reflection
        val particlesField = ParticleManager::class.java.getDeclaredField("particles")
        particlesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val beforeList = particlesField.get(manager) as List<ParticleManager.Particle>
        if (beforeList.isEmpty()) return
        val before = Vector2(beforeList.first().x, beforeList.first().y)

        manager.update(0.1f)

        @Suppress("UNCHECKED_CAST")
        val afterList = particlesField.get(manager) as List<ParticleManager.Particle>
        if (afterList.isEmpty()) return
        val after = Vector2(afterList.first().x, afterList.first().y)

        assertTrue(after.dst(before) > 0f, "Particle should have moved after update")
    }

    @Test
    fun `pool recycles expired particles`() {
        // Spawn several small explosions
        repeat(5) {
            manager.spawnExplosion(Vector2.Zero, ExplosionSize.SMALL)
        }
        assertTrue(manager.activeCount > 0, "Particles should exist after spawn")

        // Expire all
        manager.update(2.0f)
        assertEquals(0, manager.activeCount, "All should be expired")

        // Spawn again — recycled particles should be reused
        manager.spawnExplosion(Vector2.Zero, ExplosionSize.MEDIUM)
        assertTrue(manager.activeCount > 0, "Should spawn using recycled particles")
    }

    @Test
    fun `clear resets all particles`() {
        manager.spawnExplosion(Vector2.Zero, ExplosionSize.LARGE)
        assertTrue(manager.activeCount > 0)

        manager.clear()
        assertEquals(0, manager.activeCount, "Clear should remove all particles")
    }

    @Test
    fun `max pool size is respected`() {
        // Spawn more particles than max
        val overflow = Constants.PARTICLE_MAX_COUNT + 100
        for (i in 0 until overflow) {
            manager.spawnEngineTrail(Vector2.Zero)
        }
        assertTrue(
            manager.activeCount <= Constants.PARTICLE_MAX_COUNT,
            "Particle count should not exceed max (${Constants.PARTICLE_MAX_COUNT})"
        )
    }

    // ── Event dispatch tests ────────────────────────────────────

    @Test
    fun `onGameEvent AsteroidDestroyed spawns particles`() {
        val asteroid = Asteroid(
            position = Vector2(3f, 4f),
            velocity = Vector2.Zero,
            size = AsteroidSize.MEDIUM
        )
        manager.onGameEvent(listOf(GameEvent.AsteroidDestroyed(asteroid, 200)))
        assertTrue(manager.activeCount > 0, "AsteroidDestroyed should spawn explosion particles")
    }

    @Test
    fun `onGameEvent EnemyDestroyed spawns particles`() {
        val enemy = LightFighter(position = Vector2(5f, 5f))
        manager.onGameEvent(listOf(GameEvent.EnemyDestroyed(enemy, 150)))
        assertTrue(manager.activeCount > 0, "EnemyDestroyed should spawn explosion particles")
    }

    @Test
    fun `onGameEvent ShipHit spawns damage sparks`() {
        val ship = Ship(position = Vector2(2f, 2f))
        manager.onGameEvent(listOf(GameEvent.ShipHit(ship, 2)))
        assertTrue(manager.activeCount > 0, "ShipHit should spawn damage sparks")
    }

    @Test
    fun `onGameEvent ShipDestroyed spawns large explosion`() {
        val ship = Ship(position = Vector2(3f, 3f))
        manager.onGameEvent(listOf(GameEvent.ShipDestroyed(ship)))
        val count = manager.activeCount
        assertTrue(count > 30, "ShipDestroyed should spawn a large explosion (got $count)")
    }

    @Test
    fun `onGameEvent AstronautRescued spawns sparkle`() {
        val astro = com.nebuladrift.entities.Astronaut(position = Vector2(1f, 1f))
        manager.onGameEvent(listOf(GameEvent.AstronautRescued(astro)))
        assertTrue(manager.activeCount > 0, "AstronautRescued should spawn sparkle particles")
    }

    @Test
    fun `onGameEvent DebrisCollected spawns sparkle`() {
        val debris = SpaceDebris(position = Vector2(3f, 3f))
        manager.onGameEvent(listOf(GameEvent.DebrisCollected(debris)))
        assertTrue(manager.activeCount > 0, "DebrisCollected should spawn sparkle particles")
    }

    @Test
    fun `onGameEvent LaserFired does not spawn particles`() {
        val laser = Laser(position = Vector2.Zero)
        manager.onGameEvent(listOf(GameEvent.LaserFired(laser)))
        assertEquals(0, manager.activeCount, "LaserFired should not spawn particles")
    }

    @Test
    fun `onGameEvent multiple events dispatches all`() {
        val asteroid = Asteroid(
            position = Vector2(1f, 1f),
            velocity = Vector2.Zero,
            size = AsteroidSize.SMALL
        )
        val enemy = LightFighter(position = Vector2(2f, 2f))

        manager.onGameEvent(listOf(
            GameEvent.AsteroidDestroyed(asteroid, 100),
            GameEvent.EnemyDestroyed(enemy, 150)
        ))

        assertTrue(manager.activeCount > 5, "Multiple events should spawn many particles")
    }
}
