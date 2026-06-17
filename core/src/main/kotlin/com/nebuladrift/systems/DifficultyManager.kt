package com.nebuladrift.systems

import com.nebuladrift.util.Constants

/**
 * Stateless difficulty provider that computes difficulty multipliers
 * from elapsed game time.
 *
 * Not a [GameSystem] — owned by GameScreen and updated each frame.
 * Systems read the current multipliers from the instance stored in
 * [GameContext.difficultyManager].
 *
 * ## Difficulty curve
 *
 * - Safe zone (0–15s): multipliers stay at minimum (1.0x for spawn rate,
 *   base scroll speed). No enemies spawn.
 * - Ramp (15–195s): values lerp linearly from start to end constants.
 * - Max difficulty (195s+): values hold at end constants.
 */
class DifficultyManager {

    /** Scroll speed multiplier applied to entity movement (1.0 = base). */
    var scrollSpeedMultiplier: Float = 1.0f

    /**
     * Asteroid spawn interval in seconds.
     * Initialises to the base constant; difficulty ramps from
     * [Constants.DIFFICULTY_START_ASTEROID_RATE] toward
     * [Constants.DIFFICULTY_END_ASTEROID_RATE] after the safe zone.
     */
    var asteroidSpawnRateMultiplier: Float = Constants.ASTEROID_SPAWN_INTERVAL

    /**
     * Enemy spawn interval in seconds.
     * Initialises to the base constant; difficulty ramps from
     * [Constants.DIFFICULTY_START_ENEMY_RATE] toward
     * [Constants.DIFFICULTY_END_ENEMY_RATE] after the safe zone.
     */
    var enemySpawnRateMultiplier: Float = Constants.ENEMY_SPAWN_INTERVAL

    /** Weighted distribution controlling which enemy types appear. */
    var enemyTypeWeights: EnemyTypeWeights = EnemyTypeWeights()

    /**
     * Update all multipliers based on [elapsedTime].
     * Call once per frame from GameScreen.
     */
    fun update(elapsedTime: Float) {
        // Safe zone: first N seconds at minimum difficulty
        if (elapsedTime < Constants.DIFFICULTY_SAFE_ZONE) return

        val t = (elapsedTime - Constants.DIFFICULTY_SAFE_ZONE) / Constants.DIFFICULTY_RAMP_DURATION
        val progress = t.coerceIn(0f, 1f)

        scrollSpeedMultiplier = lerp(
            Constants.DIFFICULTY_START_SCROLL_SPEED,
            Constants.DIFFICULTY_END_SCROLL_SPEED,
            progress
        )
        asteroidSpawnRateMultiplier = lerp(
            Constants.DIFFICULTY_START_ASTEROID_RATE,
            Constants.DIFFICULTY_END_ASTEROID_RATE,
            progress
        )
        enemySpawnRateMultiplier = lerp(
            Constants.DIFFICULTY_START_ENEMY_RATE,
            Constants.DIFFICULTY_END_ENEMY_RATE,
            progress
        )

        enemyTypeWeights = calculateEnemyWeights(progress)
    }

    /** Weighted distribution for enemy type selection. */
    data class EnemyTypeWeights(
        var fighter: Float = 1.0f,
        var frigate: Float = 0.0f,
        var destroyer: Float = 0.0f,
        var clone: Float = 0.0f
    ) {
        val total: Float get() = fighter + frigate + destroyer + clone
    }

    companion object {
        private fun lerp(start: Float, end: Float, t: Float): Float =
            start + (end - start) * t

        /**
         * Shift weights from early-game (mostly fighters) toward a
         * balanced late-game mix.
         */
        private fun calculateEnemyWeights(progress: Float): EnemyTypeWeights = EnemyTypeWeights(
            fighter = lerp(1.0f, 0.4f, progress),
            frigate = lerp(0.0f, 0.3f, progress),
            destroyer = lerp(0.0f, 0.2f, progress),
            clone = lerp(0.0f, 0.1f, progress)
        )
    }
}
