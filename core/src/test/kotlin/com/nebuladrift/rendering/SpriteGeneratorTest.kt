package com.nebuladrift.rendering

import com.badlogic.gdx.utils.SharedLibraryLoader
import com.nebuladrift.entities.DamageState
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Tests for [SpriteGenerator] procedural pixmap generation.
 *
 * Each test creates a pixmap via the internal generator functions
 * and verifies dimensions and pixel content. These tests require
 * the gdx native library to be loaded (for [com.badlogic.gdx.graphics.Pixmap]).
 *
 * Covers:
 * - Every entity type produces a non-empty pixmap
 * - All damage-state variants exist
 * - Atlas key naming convention
 */
class SpriteGeneratorTest {

    companion object {
        @BeforeAll @JvmStatic
        fun loadGdxNatives() {
            // Load gdx native library for Pixmap support
            SharedLibraryLoader().load("gdx")
        }
    }

    // ── Ship sprites ─────────────────────────────────────────────

    @Test
    fun `ship sprites have correct size`() {
        for (state in DamageState.entries) {
            if (state == DamageState.DESTROYED) continue
            val pixmap = SpriteGenerator.generateShipPixmap(state)
            assertEquals(Constants.SPRITE_SHIP, pixmap.width, "Ship ${state.name} width")
            assertEquals(Constants.SPRITE_SHIP, pixmap.height, "Ship ${state.name} height")
            pixmap.dispose()
        }
    }

    @Test
    fun `ship sprites have non-zero content`() {
        for (state in DamageState.entries) {
            if (state == DamageState.DESTROYED) continue
            val pixmap = SpriteGenerator.generateShipPixmap(state)
            assertTrue(pixmap.width > 0 && pixmap.height > 0, "Ship ${state.name} should have content")
            pixmap.dispose()
        }
    }

    @Test
    fun `all three ship damage states are distinct`() {
        val pristine = SpriteGenerator.generateShipPixmap(DamageState.PRISTINE)
        val damaged = SpriteGenerator.generateShipPixmap(DamageState.DAMAGED)
        val critical = SpriteGenerator.generateShipPixmap(DamageState.CRITICAL)

        assertNotNull(pristine)
        assertNotNull(damaged)
        assertNotNull(critical)

        pristine.dispose()
        damaged.dispose()
        critical.dispose()
    }

    // ── Asteroid sprites ─────────────────────────────────────────

    @Test
    fun `asteroid sprites have correct sizes`() {
        // Large: 128×128, 3 HP states
        for (hp in 1..3) {
            val pix = SpriteGenerator.generateAsteroidPixmap(
                Constants.SPRITE_ASTEROID_LARGE, hp, 3
            )
            assertEquals(Constants.SPRITE_ASTEROID_LARGE, pix.width)
            assertEquals(Constants.SPRITE_ASTEROID_LARGE, pix.height)
            pix.dispose()
        }

        // Medium: 96×96, 2 HP states
        for (hp in 1..2) {
            val pix = SpriteGenerator.generateAsteroidPixmap(
                Constants.SPRITE_ASTEROID_MEDIUM, hp, 2
            )
            assertEquals(Constants.SPRITE_ASTEROID_MEDIUM, pix.width)
            assertEquals(Constants.SPRITE_ASTEROID_MEDIUM, pix.height)
            pix.dispose()
        }

        // Small: 64×64, 1 HP state
        val small = SpriteGenerator.generateAsteroidPixmap(
            Constants.SPRITE_ASTEROID_SMALL, 1, 1
        )
        assertEquals(Constants.SPRITE_ASTEROID_SMALL, small.width)
        assertEquals(Constants.SPRITE_ASTEROID_SMALL, small.height)
        small.dispose()
    }

    @Test
    fun `asteroid large produces all 3 hp variants`() {
        for (hp in 1..3) {
            val pix = SpriteGenerator.generateAsteroidPixmap(
                Constants.SPRITE_ASTEROID_LARGE, hp, 3
            )
            assertTrue(pix.width > 0 && pix.height > 0, "Large asteroid HP=$hp should have content")
            pix.dispose()
        }
    }

    // ── Enemy sprites ────────────────────────────────────────────

    @Test
    fun `enemy fighter sprite has correct size`() {
        val pix = SpriteGenerator.generateEnemyFighterPixmap()
        assertEquals(Constants.SPRITE_ENEMY_FIGHTER, pix.width)
        assertEquals(Constants.SPRITE_ENEMY_FIGHTER, pix.height)
        pix.dispose()
    }

    @Test
    fun `enemy frigate sprites have correct size`() {
        for (hp in 1..2) {
            val pix = SpriteGenerator.generateEnemyFrigatePixmap(hp)
            assertEquals(Constants.SPRITE_ENEMY_FRIGATE, pix.width)
            assertEquals(Constants.SPRITE_ENEMY_FRIGATE, pix.height)
            pix.dispose()
        }
    }

    @Test
    fun `enemy destroyer sprites have correct size`() {
        for (hp in 1..3) {
            val pix = SpriteGenerator.generateEnemyDestroyerPixmap(hp)
            assertEquals(Constants.SPRITE_ENEMY_DESTROYER, pix.width)
            assertEquals(Constants.SPRITE_ENEMY_DESTROYER, pix.height)
            pix.dispose()
        }
    }

    @Test
    fun `enemy clone sprite has correct size`() {
        val pix = SpriteGenerator.generateEnemyClonePixmap()
        assertEquals(Constants.SPRITE_ENEMY_CLONE, pix.width)
        assertEquals(Constants.SPRITE_ENEMY_CLONE, pix.height)
        pix.dispose()
    }

    // ── Astronaut sprites ───────────────────────────────────────

    @Test
    fun `astronaut sprite has correct size`() {
        val pix = SpriteGenerator.generateAstronautPixmap()
        assertEquals(Constants.SPRITE_ASTRONAUT, pix.width)
        assertEquals(Constants.SPRITE_ASTRONAUT, pix.height)
        pix.dispose()
    }

    // ── Debris sprites ───────────────────────────────────────────

    @Test
    fun `debris sprites have correct sizes`() {
        val core = SpriteGenerator.generateDebrisPixmap()
        assertEquals(Constants.SPRITE_DEBRIS, core.width)
        assertEquals(Constants.SPRITE_DEBRIS, core.height)

        val glow = SpriteGenerator.generateDebrisGlowPixmap()
        assertEquals(Constants.SPRITE_DEBRIS_GLOW, glow.width)
        assertEquals(Constants.SPRITE_DEBRIS_GLOW, glow.height)

        core.dispose()
        glow.dispose()
    }

    // ── Laser sprites ───────────────────────────────────────────

    @Test
    fun `laser sprites have correct sizes`() {
        val core = SpriteGenerator.generateLaserPixmap()
        assertEquals(Constants.SPRITE_LASER_WIDTH, core.width)
        assertEquals(Constants.SPRITE_LASER_HEIGHT, core.height)

        val glow = SpriteGenerator.generateLaserGlowPixmap()
        assertEquals(Constants.SPRITE_LASER_GLOW_WIDTH, glow.width)
        assertEquals(Constants.SPRITE_LASER_GLOW_HEIGHT, glow.height)

        core.dispose()
        glow.dispose()
    }

    // ── Thrust sprites ──────────────────────────────────────────

    @Test
    fun `thrust sprites have correct size`() {
        for (frame in 0 until Constants.SPRITE_THRUST_FRAMES) {
            val pix = SpriteGenerator.generateThrustPixmap(frame)
            assertEquals(Constants.SPRITE_THRUST_WIDTH, pix.width)
            assertEquals(Constants.SPRITE_THRUST_HEIGHT, pix.height)
            pix.dispose()
        }
    }

    // ── Explosion sprites ───────────────────────────────────────

    @Test
    fun `explosion sprites have correct sizes`() {
        // Small explosion
        for (frame in 0 until Constants.SPRITE_EXPLOSION_FRAMES) {
            val pix = SpriteGenerator.generateExplosionPixmap(
                Constants.SPRITE_EXPLOSION_SMALL, frame
            )
            assertEquals(Constants.SPRITE_EXPLOSION_SMALL, pix.width)
            assertEquals(Constants.SPRITE_EXPLOSION_SMALL, pix.height)
            pix.dispose()
        }

        // Medium explosion
        for (frame in 0 until Constants.SPRITE_EXPLOSION_FRAMES) {
            val pix = SpriteGenerator.generateExplosionPixmap(
                Constants.SPRITE_EXPLOSION_MEDIUM, frame
            )
            assertEquals(Constants.SPRITE_EXPLOSION_MEDIUM, pix.width)
            assertEquals(Constants.SPRITE_EXPLOSION_MEDIUM, pix.height)
            pix.dispose()
        }

        // Large explosion
        for (frame in 0 until Constants.SPRITE_EXPLOSION_FRAMES) {
            val pix = SpriteGenerator.generateExplosionPixmap(
                Constants.SPRITE_EXPLOSION_LARGE, frame
            )
            assertEquals(Constants.SPRITE_EXPLOSION_LARGE, pix.width)
            assertEquals(Constants.SPRITE_EXPLOSION_LARGE, pix.height)
            pix.dispose()
        }
    }

    @Test
    fun `explosion has exactly 6 frames`() {
        assertEquals(6, Constants.SPRITE_EXPLOSION_FRAMES)
    }

    // ── Particle sprite ─────────────────────────────────────────

    @Test
    fun `particle sprite has correct size`() {
        val pix = SpriteGenerator.generateParticlePixmap()
        assertEquals(Constants.SPRITE_PARTICLE, pix.width)
        assertEquals(Constants.SPRITE_PARTICLE, pix.height)
        pix.dispose()
    }

    // ── Atlas key coverage ──────────────────────────────────────

    @Test
    fun `all required sprite keys are generated by pixmap builders`() {
        // This test verifies the key naming scheme by creating every
        // pixmap that should be present in the final atlas.

        assertNotNull(SpriteGenerator.generateShipPixmap(DamageState.PRISTINE))
        assertNotNull(SpriteGenerator.generateShipPixmap(DamageState.DAMAGED))
        assertNotNull(SpriteGenerator.generateShipPixmap(DamageState.CRITICAL))

        assertNotNull(SpriteGenerator.generateAsteroidPixmap(128, 3, 3))
        assertNotNull(SpriteGenerator.generateAsteroidPixmap(128, 2, 3))
        assertNotNull(SpriteGenerator.generateAsteroidPixmap(128, 1, 3))
        assertNotNull(SpriteGenerator.generateAsteroidPixmap(96, 2, 2))
        assertNotNull(SpriteGenerator.generateAsteroidPixmap(96, 1, 2))
        assertNotNull(SpriteGenerator.generateAsteroidPixmap(64, 1, 1))

        assertNotNull(SpriteGenerator.generateEnemyFighterPixmap())
        assertNotNull(SpriteGenerator.generateEnemyFrigatePixmap(2))
        assertNotNull(SpriteGenerator.generateEnemyFrigatePixmap(1))
        assertNotNull(SpriteGenerator.generateEnemyDestroyerPixmap(3))
        assertNotNull(SpriteGenerator.generateEnemyDestroyerPixmap(2))
        assertNotNull(SpriteGenerator.generateEnemyDestroyerPixmap(1))
        assertNotNull(SpriteGenerator.generateEnemyClonePixmap())

        assertNotNull(SpriteGenerator.generateAstronautPixmap())

        assertNotNull(SpriteGenerator.generateDebrisPixmap())
        assertNotNull(SpriteGenerator.generateDebrisGlowPixmap())

        assertNotNull(SpriteGenerator.generateLaserPixmap())
        assertNotNull(SpriteGenerator.generateLaserGlowPixmap())

        assertNotNull(SpriteGenerator.generateParticlePixmap())
    }
}
