package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Timer-based space debris spawner.
 *
 * Spawns debris very rarely (every [Constants.DEBRIS_SPAWN_INTERVAL]
 * seconds with jitter). Debris grants an extra life when collected.
 * Only one debris may be active at a time.
 */
class DebrisSpawnSystem : GameSystem {

    /** 3 visually distinct debris variants. */
    private val debrisVariants = listOf("debris_gear", "debris_antenna", "debris_panel")

    /** Base interval before random jitter is added. */
    private val baseInterval: Float = Constants.DEBRIS_SPAWN_INTERVAL

    /** Maximum extra seconds added for jitter. */
    private val jitterRange: Float = 5f

    private var timeSinceLastSpawn: Float = 0f

    /** Next spawn interval (randomised) to create variance. */
    private var nextInterval: Float = randomInterval()

    override fun update(delta: Float, context: GameContext) {
        // Only spawn if no debris is currently active
        if (context.debris.isNotEmpty()) return

        timeSinceLastSpawn += delta
        if (timeSinceLastSpawn < nextInterval) return
        timeSinceLastSpawn = 0f
        nextInterval = randomInterval()

        spawnDebris(context)
    }

    private fun spawnDebris(context: GameContext) {
        val x = Constants.WORLD_WIDTH + 1f
        val y = Random.nextFloat() * Constants.WORLD_HEIGHT
        val variant = debrisVariants[Random.nextInt(debrisVariants.size)]
        val debris = SpaceDebris(position = Vector2(x, y), spriteKey = variant)
        context.debris.add(debris)
    }

    /** Random interval in the range [baseInterval, baseInterval + jitterRange). */
    private fun randomInterval(): Float =
        baseInterval + Random.nextFloat() * jitterRange
}
