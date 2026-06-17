package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Timer-based astronaut spawner.
 *
 * Spawns astronauts rarely (every [Constants.ASTRONAUT_SPAWN_INTERVAL]
 * seconds with jitter). Only one astronaut may be active at a time.
 * Astronauts spawn on the right edge at a random Y.
 */
class AstronautSpawnSystem : GameSystem {

    /** Base interval before random jitter is added. */
    private val baseInterval: Float = Constants.ASTRONAUT_SPAWN_INTERVAL

    /** Maximum extra seconds added for jitter. */
    private val jitterRange: Float = 3f

    private var timeSinceLastSpawn: Float = 0f

    /** Next spawn interval (randomised) to create variance. */
    private var nextInterval: Float = randomInterval()

    override fun update(delta: Float, context: GameContext) {
        // Only spawn if no astronaut is currently active
        if (context.astronauts.isNotEmpty()) return

        timeSinceLastSpawn += delta
        if (timeSinceLastSpawn < nextInterval) return
        timeSinceLastSpawn = 0f
        nextInterval = randomInterval()

        spawnAstronaut(context)
    }

    private fun spawnAstronaut(context: GameContext) {
        val x = Constants.WORLD_WIDTH + 1f
        val y = Random.nextFloat() * Constants.WORLD_HEIGHT

        val astronaut = Astronaut(position = Vector2(x, y))
        context.astronauts.add(astronaut)
    }

    /** Random interval in the range [baseInterval, baseInterval + jitterRange). */
    private fun randomInterval(): Float =
        baseInterval + Random.nextFloat() * jitterRange
}
