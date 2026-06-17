package com.nebuladrift.systems

import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for difficulty progression in [DifficultyManager].
 *
 * Covers the safe zone, linear interpolation during the ramp phase,
 * clamping at maximum difficulty, and enemy-type weight distribution
 * shifts over time.
 */
class DifficultyTest {

    private lateinit var difficultyManager: DifficultyManager

    @BeforeEach
    fun setUp() {
        difficultyManager = DifficultyManager()
    }

    // ── Initial state ─────────────────────────────────────────

    @Test
    fun `initial scroll speed multiplier is 1`() {
        assertEquals(1.0f, difficultyManager.scrollSpeedMultiplier, 0.001f)
    }

    @Test
    fun `initial asteroid spawn rate is base interval`() {
        assertEquals(Constants.ASTEROID_SPAWN_INTERVAL, difficultyManager.asteroidSpawnRateMultiplier, 0.001f)
    }

    @Test
    fun `initial enemy spawn rate is base interval`() {
        assertEquals(Constants.ENEMY_SPAWN_INTERVAL, difficultyManager.enemySpawnRateMultiplier, 0.001f)
    }

    @Test
    fun `initial enemy weights have only fighters`() {
        val weights = difficultyManager.enemyTypeWeights
        assertEquals(1.0f, weights.fighter, 0.001f, "Only fighters at start")
        assertEquals(0.0f, weights.frigate, 0.001f, "No frigates at start")
        assertEquals(0.0f, weights.destroyer, 0.001f, "No destroyers at start")
        assertEquals(0.0f, weights.clone, 0.001f, "No clones at start")
    }

    // ── Safe zone (0-15 seconds) ──────────────────────────────

    @Test
    fun `safe zone does not change multipliers`() {
        difficultyManager.update(Constants.DIFFICULTY_SAFE_ZONE - 0.1f)
        assertEquals(1.0f, difficultyManager.scrollSpeedMultiplier, 0.001f)
        assertEquals(Constants.ASTEROID_SPAWN_INTERVAL, difficultyManager.asteroidSpawnRateMultiplier, 0.001f)
        assertEquals(Constants.ENEMY_SPAWN_INTERVAL, difficultyManager.enemySpawnRateMultiplier, 0.001f)
    }

    @Test
    fun `zero elapsed time keeps initial values`() {
        difficultyManager.update(0f)
        assertEquals(1.0f, difficultyManager.scrollSpeedMultiplier, 0.001f)
    }

    @Test
    fun `safe zone keeps fighter-only weights`() {
        difficultyManager.update(10f)
        assertEquals(1.0f, difficultyManager.enemyTypeWeights.fighter, 0.001f)
        assertEquals(0.0f, difficultyManager.enemyTypeWeights.frigate, 0.001f)
    }

    // ── After safe zone: interpolation begins ─────────────────

    @Test
    fun `scroll speed increases after safe zone`() {
        difficultyManager.update(Constants.DIFFICULTY_SAFE_ZONE + 0.01f)
        assertTrue(
            difficultyManager.scrollSpeedMultiplier > 1.0f,
            "Scroll speed multiplier should increase after safe zone"
        )
    }

    @Test
    fun `scroll speed at halfway should be midpoint`() {
        val halfDuration = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION / 2f
        difficultyManager.update(halfDuration)
        val expected = (Constants.DIFFICULTY_START_SCROLL_SPEED + Constants.DIFFICULTY_END_SCROLL_SPEED) / 2f
        assertEquals(expected, difficultyManager.scrollSpeedMultiplier, 0.1f,
            "At halfway, scroll speed should be near midpoint")
    }

    @Test
    fun `asteroid spawn rate decreases after safe zone`() {
        difficultyManager.update(Constants.DIFFICULTY_SAFE_ZONE + 1f)
        assertTrue(
            difficultyManager.asteroidSpawnRateMultiplier < Constants.ASTEROID_SPAWN_INTERVAL,
            "Asteroid spawn rate should decrease after safe zone"
        )
    }

    @Test
    fun `enemy spawn rate lerps from start toward end value after safe zone`() {
        // At ramp start, enemy rate lerps from DIFFICULTY_START_ENEMY_RATE (4f)
        // towards DIFFICULTY_END_ENEMY_RATE (2f). Right after safe zone, the
        // interval briefly increases (makes game slightly easier) before decreasing.
        difficultyManager.update(Constants.DIFFICULTY_SAFE_ZONE + 1f)
        val rate = difficultyManager.enemySpawnRateMultiplier
        assertTrue(
            rate <= Constants.DIFFICULTY_START_ENEMY_RATE,
            "Enemy spawn rate should be at most start value ${Constants.DIFFICULTY_START_ENEMY_RATE}, was $rate"
        )
        assertTrue(
            rate >= Constants.DIFFICULTY_END_ENEMY_RATE,
            "Enemy spawn rate should be at least end value ${Constants.DIFFICULTY_END_ENEMY_RATE}, was $rate"
        )
    }

    // ── Interpolation across the ramp ─────────────────────────

    @Test
    fun `scroll speed at ramp start matches start constant`() {
        // At exactly DIFFICULTY_SAFE_ZONE (15f), the elapsed time is NOT < 15f,
        // so it enters the ramp with progress=0, setting scrollSpeedMultiplier to
        // DIFFICULTY_START_SCROLL_SPEED (2f).
        difficultyManager.update(Constants.DIFFICULTY_SAFE_ZONE)
        assertEquals(
            Constants.DIFFICULTY_START_SCROLL_SPEED,
            difficultyManager.scrollSpeedMultiplier,
            0.001f,
            "At ramp start, scroll speed should be DIFFICULTY_START_SCROLL_SPEED"
        )
    }

    @Test
    fun `values approach end constants as ramp progresses`() {
        // Run nearly to end of ramp
        val nearEnd = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION * 0.95f
        difficultyManager.update(nearEnd)
        assertTrue(
            difficultyManager.scrollSpeedMultiplier < Constants.DIFFICULTY_END_SCROLL_SPEED + 0.1f,
            "Near ramp end, scroll speed should approach end value"
        )
        assertTrue(
            difficultyManager.scrollSpeedMultiplier > Constants.DIFFICULTY_START_SCROLL_SPEED,
            "Near ramp end, scroll speed should be above start value"
        )
    }

    // ── Clamping at max ───────────────────────────────────────

    @Test
    fun `multipliers clamp at end values after ramp`() {
        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION + 10f
        difficultyManager.update(beyondRamp)
        assertEquals(Constants.DIFFICULTY_END_SCROLL_SPEED, difficultyManager.scrollSpeedMultiplier, 0.001f)
        assertEquals(Constants.DIFFICULTY_END_ASTEROID_RATE, difficultyManager.asteroidSpawnRateMultiplier, 0.001f)
        assertEquals(Constants.DIFFICULTY_END_ENEMY_RATE, difficultyManager.enemySpawnRateMultiplier, 0.001f)
    }

    @Test
    fun `scroll speed does not exceed end value`() {
        val farBeyond = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION * 5f
        difficultyManager.update(farBeyond)
        assertTrue(
            difficultyManager.scrollSpeedMultiplier <= Constants.DIFFICULTY_END_SCROLL_SPEED + 0.001f,
            "Scroll speed should not exceed end value"
        )
    }

    @Test
    fun `asteroid spawn rate stays at end value after ramp`() {
        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION + 30f
        difficultyManager.update(beyondRamp)
        assertEquals(
            Constants.DIFFICULTY_END_ASTEROID_RATE,
            difficultyManager.asteroidSpawnRateMultiplier, 0.001f
        )
    }

    // ── Enemy type weights ────────────────────────────────────

    @Test
    fun `fighter weight decreases over time`() {
        difficultyManager.update(Constants.DIFFICULTY_SAFE_ZONE)
        val earlyFighter = difficultyManager.enemyTypeWeights.fighter

        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION + 1f
        difficultyManager.update(beyondRamp)
        val lateFighter = difficultyManager.enemyTypeWeights.fighter

        assertTrue(lateFighter < earlyFighter, "Fighter weight should decrease over time")
    }

    @Test
    fun `frigate weight increases from zero`() {
        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION / 2f
        difficultyManager.update(beyondRamp)
        assertTrue(difficultyManager.enemyTypeWeights.frigate > 0f, "Frigate weight should increase from zero")
    }

    @Test
    fun `destroyer weight increases from zero`() {
        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION / 2f
        difficultyManager.update(beyondRamp)
        assertTrue(difficultyManager.enemyTypeWeights.destroyer > 0f, "Destroyer weight should increase from zero")
    }

    @Test
    fun `clone weight increases from zero`() {
        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION / 2f
        difficultyManager.update(beyondRamp)
        assertTrue(difficultyManager.enemyTypeWeights.clone > 0f, "Clone weight should increase from zero")
    }

    @Test
    fun `enemy type weights sum to reasonable total`() {
        val midRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION / 2f
        difficultyManager.update(midRamp)
        val total = difficultyManager.enemyTypeWeights.total
        assertTrue(total > 0f, "Weight total should be positive")
        assertTrue(total <= 1.0f + 0.001f, "Weight total should be ≤ 1.0")
    }

    @Test
    fun `enemy type weights at max difficulty`() {
        val beyondRamp = Constants.DIFFICULTY_SAFE_ZONE + Constants.DIFFICULTY_RAMP_DURATION + 1f
        difficultyManager.update(beyondRamp)
        val w = difficultyManager.enemyTypeWeights
        assertEquals(0.4f, w.fighter, 0.001f, "Fighter should be 0.4 at max difficulty")
        assertEquals(0.3f, w.frigate, 0.001f, "Frigate should be 0.3 at max difficulty")
        assertEquals(0.2f, w.destroyer, 0.001f, "Destroyer should be 0.2 at max difficulty")
        assertEquals(0.1f, w.clone, 0.001f, "Clone should be 0.1 at max difficulty")
    }

    // ── EnemyTypeWeights ──────────────────────────────────────

    @Test
    fun `enemyTypeWeights total defaults to only fighters`() {
        val weights = DifficultyManager.EnemyTypeWeights()
        assertEquals(1.0f, weights.total, 0.001f, "Default total should be 1.0 (only fighters)")
    }

    @Test
    fun `enemyTypeWeights total updates with custom values`() {
        val weights = DifficultyManager.EnemyTypeWeights(fighter = 0.5f, frigate = 0.3f, destroyer = 0.2f)
        assertEquals(1.0f, weights.total, 0.001f)
    }

    // ── Sequential updates ────────────────────────────────────

    @Test
    fun `multiple update calls produce monotonic progression`() {
        var previous = 1.0f
        val timestamps = listOf(0f, 15f, 30f, 60f, 120f, 195f, 300f)
        for (t in timestamps) {
            difficultyManager.update(t)
            assertTrue(
                difficultyManager.scrollSpeedMultiplier >= previous,
                "Scroll speed should be monotonic non-decreasing (at t=$t)"
            )
            previous = difficultyManager.scrollSpeedMultiplier
        }
    }
}
