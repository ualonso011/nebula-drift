package com.nebuladrift.systems

import com.nebuladrift.util.Constants

/**
 * Tracks game scoring and statistics.
 *
 * Each frame, [update] accumulates the time-bonus (points per second
 * survived) and counts asteroids destroyed from the events queue.
 * The actual score integer lives in [GameContext.score] and is
 * modified both by this system (time bonus) and by [CollisionSystem]
 * (destruction points).
 */
class ScoreSystem : GameSystem {

    /** Total elapsed play time in seconds. */
    var elapsedTime: Float = 0f
        private set

    /** Number of asteroids destroyed this session. */
    var asteroidsDestroyed: Int = 0
        private set

    /** Astronauts rescued (placeholder — always 0 in v0.1). */
    var astronautsRescued: Int = 0
        private set

    /** Accumulator for the 1-point-per-second time bonus. */
    private var timeAccumulator: Float = 0f

    override fun update(delta: Float, context: GameContext) {
        elapsedTime += delta

        // Time bonus: +1 point per second survived
        timeAccumulator += delta
        while (timeAccumulator >= 1f) {
            timeAccumulator -= 1f
            context.score += Constants.SCORE_TIME_BONUS
        }

        // Count events
        for (event in context.events) {
            when (event) {
                is GameEvent.AsteroidDestroyed -> asteroidsDestroyed++
                else -> { /* no-op for now */ }
            }
        }
    }

    /** Format elapsed time as M:SS. */
    val formattedTime: String
        get() {
            val totalSeconds = elapsedTime.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    /** Reset all state for a new game. */
    fun reset() {
        elapsedTime = 0f
        asteroidsDestroyed = 0
        astronautsRescued = 0
        timeAccumulator = 0f
    }
}
