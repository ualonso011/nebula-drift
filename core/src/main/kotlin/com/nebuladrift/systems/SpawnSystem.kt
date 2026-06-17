package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Timer-based asteroid spawner.
 *
 * Spawns asteroids on the right side of the screen at an interval
 * driven by [DifficultyManager.asteroidSpawnRateMultiplier]. Size
 * distribution skews toward large (common), with fewer medium and
 * rare small asteroids.
 *
 * Asteroid speed is multiplied by
 * [DifficultyManager.scrollSpeedMultiplier] so difficulty affects
 * both spawn rate and scroll speed.
 *
 * Pauses spawning when [Constants.ASTEROID_MAX_COUNT] is reached.
 */
class SpawnSystem : GameSystem {

    private var timeSinceLastSpawn: Float = 0f

    override fun update(delta: Float, context: GameContext) {
        if (context.asteroids.size >= Constants.ASTEROID_MAX_COUNT) return

        timeSinceLastSpawn += delta
        val spawnInterval = context.difficultyManager.asteroidSpawnRateMultiplier
        if (timeSinceLastSpawn >= spawnInterval) {
            timeSinceLastSpawn = 0f
            spawnAsteroid(context)
        }
    }

    private fun spawnAsteroid(context: GameContext) {
        // Size distribution: 50% large, 30% medium, 20% small
        val size = when (Random.nextInt(10)) {
            in 0..4 -> AsteroidSize.LARGE
            in 5..7 -> AsteroidSize.MEDIUM
            else -> AsteroidSize.SMALL
        }

        // Spawn just off the right edge with random Y
        val x = Constants.WORLD_WIDTH + size.radius * 2f
        val y = Random.nextFloat() * Constants.WORLD_HEIGHT

        // Leftward velocity with slight vertical variation, scaled by difficulty
        val scrollMultiplier = context.difficultyManager.scrollSpeedMultiplier
        val speed = Random.nextFloat() *
            (Constants.ASTEROID_MAX_SPEED - Constants.ASTEROID_MIN_SPEED) +
            Constants.ASTEROID_MIN_SPEED
        val vx = -speed * scrollMultiplier
        val vy = (Random.nextFloat() - 0.5f) * speed * 0.5f * scrollMultiplier

        val asteroid = Asteroid(
            position = Vector2(x, y),
            velocity = Vector2(vx, vy),
            size = size
        )

        context.asteroids.add(asteroid)
    }
}
