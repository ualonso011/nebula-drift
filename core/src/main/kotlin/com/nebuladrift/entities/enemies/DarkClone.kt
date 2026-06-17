package com.nebuladrift.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * A copy of the player's ship that mirrors their movements
 * with a ~0.5s delay via [MirrorSystem].
 *
 * Has 2-3 HP (randomized), same collision radius as the player's
 * ship, and the highest point value among enemy types.
 *
 * The [isFiring] flag is set by MirrorSystem when the player
 * fired 30 frames ago, triggering DarkClone to fire a laser.
 */
class DarkClone(position: Vector2) : Enemy(
    position = position.cpy(),
    velocity = Vector2(-Constants.ENEMY_CLONE_SPEED, 0f),
    radius = Constants.SHIP_RADIUS,  // Same size as player
    maxHealth = if (Random.nextBoolean()) 2 else 3,
    points = Constants.SCORE_DARK_CLONE
) {
    /** Whether the clone is currently firing (set by MirrorSystem). */
    var isFiring: Boolean = false

    override fun getType() = EnemyType.DARK_CLONE
}
