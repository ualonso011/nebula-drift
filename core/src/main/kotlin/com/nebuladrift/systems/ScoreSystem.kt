package com.nebuladrift.systems

import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.entities.enemies.EnemyType
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

    /** Number of astronauts rescued this session. */
    var astronautsRescued: Int = 0
        private set

    /** Number of astronauts killed this session. */
    var astronautsKilled: Int = 0
        private set

    /** Total number of enemies destroyed this session. */
    var enemiesDestroyed: Int = 0
        private set

    /** Enemies destroyed tracked by type. */
    private val enemiesDestroyedByTypeMap = mutableMapOf<EnemyType, Int>()

    override fun update(delta: Float, context: GameContext) {
        elapsedTime += delta

        // Count events
        for (event in context.events) {
            when (event) {
                is GameEvent.AsteroidDestroyed -> asteroidsDestroyed++
                is GameEvent.EnemyDestroyed -> addEnemyKill(event.enemy)
                is GameEvent.AstronautRescued -> astronautsRescued++
                is GameEvent.AstronautKilled -> astronautsKilled++
                else -> { /* DebrisCollected — no stat tracking needed */ }
            }
        }
    }

    /**
     * Record an enemy kill for both total and per-type stats.
     */
    fun addEnemyKill(enemy: Enemy) {
        enemiesDestroyed++
        val type = enemy.getType()
        enemiesDestroyedByTypeMap[type] = (enemiesDestroyedByTypeMap[type] ?: 0) + 1
    }

    /**
     * Record an astronaut rescue.
     */
    fun addAstronautRescue() {
        astronautsRescued++
    }

    /**
     * Record an astronaut killed.
     */
    fun addAstronautKill() {
        astronautsKilled++
    }

    /** Returns an immutable snapshot of enemies destroyed by type. */
    fun getEnemiesDestroyedByType(): Map<EnemyType, Int> =
        enemiesDestroyedByTypeMap.toMap()

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
        astronautsKilled = 0
        enemiesDestroyed = 0
        enemiesDestroyedByTypeMap.clear()
    }
}
