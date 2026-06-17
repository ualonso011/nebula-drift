package com.nebuladrift.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Medium-armored enemy with 2 HP.
 *
 * Moderate speed, medium radius, and mid-range point value.
 * Becomes more frequent as difficulty ramps up.
 */
class MediumFrigate(position: Vector2) : Enemy(
    position = position.cpy(),
    velocity = Vector2(-Constants.ENEMY_MEDIUM_SPEED, 0f),
    radius = Constants.ENEMY_MEDIUM_RADIUS,
    maxHealth = 2,
    points = Constants.SCORE_MEDIUM_FRIGATE
) {
    override fun getType() = EnemyType.MEDIUM_FRIGATE
}
