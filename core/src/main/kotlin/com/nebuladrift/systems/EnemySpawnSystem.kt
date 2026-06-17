package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.enemies.DarkClone
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.entities.enemies.EnemyType
import com.nebuladrift.entities.enemies.HeavyDestroyer
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.entities.enemies.MediumFrigate
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Timer-based enemy spawner.
 *
 * Spawns enemies on the right edge of the screen at an interval
 * driven by [DifficultyManager.enemySpawnRateMultiplier]. The
 * type of enemy spawned is determined by the current
 * [DifficultyManager.EnemyTypeWeights] distribution.
 *
 * Respects the safe zone — no enemies spawn during the first
 * [Constants.DIFFICULTY_SAFE_ZONE] seconds.
 */
class EnemySpawnSystem : GameSystem {

    private var timeSinceLastSpawn: Float = 0f

    override fun update(delta: Float, context: GameContext) {
        // No enemies during safe zone
        if (context.elapsedTime < Constants.DIFFICULTY_SAFE_ZONE) return

        timeSinceLastSpawn += delta

        val spawnInterval = context.difficultyManager.enemySpawnRateMultiplier
        if (timeSinceLastSpawn < spawnInterval) return
        timeSinceLastSpawn = 0f

        spawnEnemy(context)
    }

    private fun spawnEnemy(context: GameContext) {
        val enemy = createEnemy(context) ?: return
        context.enemies.add(enemy)
    }

    /**
     * Create an enemy of a random type determined by the current
     * difficulty weight distribution. Returns `null` if total
     * weight is zero (fallback during transition).
     */
    private fun createEnemy(context: GameContext): Enemy? {
        val weights = context.difficultyManager.enemyTypeWeights
        if (weights.total <= 0f) return null

        val roll = Random.nextFloat() * weights.total
        val type = when {
            roll < weights.fighter -> EnemyType.LIGHT_FIGHTER
            roll < weights.fighter + weights.frigate -> EnemyType.MEDIUM_FRIGATE
            roll < weights.fighter + weights.frigate + weights.destroyer -> EnemyType.HEAVY_DESTROYER
            else -> EnemyType.DARK_CLONE
        }

        val x = Constants.WORLD_WIDTH + 1f
        val y = Random.nextFloat() * Constants.WORLD_HEIGHT
        val position = Vector2(x, y)

        return when (type) {
            EnemyType.LIGHT_FIGHTER -> LightFighter(position)
            EnemyType.MEDIUM_FRIGATE -> MediumFrigate(position)
            EnemyType.HEAVY_DESTROYER -> HeavyDestroyer(position)
            EnemyType.DARK_CLONE -> DarkClone(position)
        }
    }
}
