package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.LaserOwner
import com.nebuladrift.entities.enemies.DarkClone
import com.nebuladrift.entities.enemies.Enemy
import com.nebuladrift.entities.enemies.EnemyType
import com.nebuladrift.entities.enemies.HeavyDestroyer
import com.nebuladrift.entities.enemies.Kamikaze
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.entities.enemies.MediumFrigate
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Timer-based enemy spawner + fire controller.
 *
 * Spawns enemies on the right edge of the screen at an interval
 * driven by [DifficultyManager.enemySpawnRateMultiplier]. The
 * type of enemy spawned is determined by the current
 * [DifficultyManager.EnemyTypeWeights] distribution.
 *
 * Also handles enemy laser fire: each enemy decrements its
 * [Enemy.fireCooldown] each frame and fires toward the player
 * when the timer expires.
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
        if (timeSinceLastSpawn >= spawnInterval) {
            timeSinceLastSpawn = 0f
            spawnEnemy(context)
        }

        // ── Enemy fire loop ────────────────────────────────────
        updateEnemyFire(delta, context)
    }

    // ── Enemy fire loop ──────────────────────────────────────────

    /**
     * Tick all enemy fire cooldowns and spawn lasers when ready.
     */
    private fun updateEnemyFire(delta: Float, context: GameContext) {
        val ship = context.ship
        if (ship.isDestroyed) return

        for (enemy in context.enemies) {
            // Kamikaze enemies don't shoot
            if (enemy is Kamikaze) continue

            if (enemy is DarkClone) {
                // DarkClone fires via MirrorSystem — check flag
                if (enemy.isFiring) {
                    fireEnemyLaser(enemy, context, ship.position)
                    enemy.isFiring = false
                }
            } else {
                enemy.fireCooldown -= delta
                if (enemy.fireCooldown <= 0f) {
                    fireEnemyLaser(enemy, context, ship.position)
                    enemy.fireCooldown = fireCooldownForType(enemy.getType())
                }
            }
        }
    }

    /**
     * Create and emit a laser from [enemy] toward [targetPos].
     */
    private fun fireEnemyLaser(enemy: Enemy, context: GameContext, targetPos: Vector2) {
        val owner = when (enemy.getType()) {
            EnemyType.LIGHT_FIGHTER -> LaserOwner.LIGHT_FIGHTER
            EnemyType.MEDIUM_FRIGATE -> LaserOwner.MEDIUM_FRIGATE
            EnemyType.HEAVY_DESTROYER -> LaserOwner.HEAVY_DESTROYER
            EnemyType.DARK_CLONE -> LaserOwner.DARK_CLONE
            EnemyType.KAMIKAZE -> LaserOwner.LIGHT_FIGHTER // fallback (never fires)
        }

        // Shoot straight left (toward the player's side)
        val direction = Vector2(-1f, 0f)
        val (laserSpeed, laserRadius) = when (enemy.getType()) {
            EnemyType.LIGHT_FIGHTER -> Constants.ENEMY_LIGHT_LASER_SPEED to Constants.ENEMY_LIGHT_LASER_RADIUS
            EnemyType.MEDIUM_FRIGATE -> Constants.ENEMY_MEDIUM_LASER_SPEED to Constants.ENEMY_MEDIUM_LASER_RADIUS
            EnemyType.HEAVY_DESTROYER -> Constants.ENEMY_HEAVY_LASER_SPEED to Constants.ENEMY_HEAVY_LASER_RADIUS
            EnemyType.DARK_CLONE -> Constants.ENEMY_CLONE_LASER_SPEED to Constants.ENEMY_CLONE_LASER_RADIUS
            EnemyType.KAMIKAZE -> Constants.ENEMY_LIGHT_LASER_SPEED to Constants.ENEMY_LIGHT_LASER_RADIUS // fallback
        }

        val laser = Laser(
            position = enemy.position.cpy(),
            velocity = direction.scl(laserSpeed),
            radius = laserRadius,
            lifetime = Constants.ENEMY_LASER_LIFETIME,
            owner = owner
        )
        context.lasers.add(laser)
    }

    /**
     * Return the fire cooldown (seconds) for the given enemy type.
     */
    private fun fireCooldownForType(type: EnemyType): Float = when (type) {
        EnemyType.LIGHT_FIGHTER -> Constants.ENEMY_LIGHT_FIRE_COOLDOWN
        EnemyType.MEDIUM_FRIGATE -> Constants.ENEMY_MEDIUM_FIRE_COOLDOWN
        EnemyType.HEAVY_DESTROYER -> Constants.ENEMY_HEAVY_FIRE_COOLDOWN
        EnemyType.DARK_CLONE -> Constants.ENEMY_CLONE_FIRE_COOLDOWN
        EnemyType.KAMIKAZE -> Float.MAX_VALUE // never fires
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
            roll < weights.fighter + weights.frigate + weights.destroyer + weights.clone -> EnemyType.DARK_CLONE
            else -> EnemyType.KAMIKAZE
        }

        val x = Constants.WORLD_WIDTH + 1f
        val y = Random.nextFloat() * Constants.WORLD_HEIGHT
        val position = Vector2(x, y)

        return when (type) {
            EnemyType.LIGHT_FIGHTER -> LightFighter(position)
            EnemyType.MEDIUM_FRIGATE -> MediumFrigate(position)
            EnemyType.HEAVY_DESTROYER -> HeavyDestroyer(position)
            EnemyType.DARK_CLONE -> DarkClone(position)
            EnemyType.KAMIKAZE -> Kamikaze(position)
        }
    }
}
