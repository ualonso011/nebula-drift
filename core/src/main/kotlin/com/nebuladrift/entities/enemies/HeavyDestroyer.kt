package com.nebuladrift.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Heavy, slow enemy with 3 HP.
 *
 * Large collision radius, slow speed, and high point value.
 * Rare in early game, more frequent at higher difficulty.
 */
class HeavyDestroyer(position: Vector2) : Enemy(
    position = position.cpy(),
    velocity = Vector2(-Constants.ENEMY_HEAVY_SPEED, 0f),
    radius = Constants.ENEMY_HEAVY_RADIUS,
    maxHealth = 3,
    points = Constants.SCORE_HEAVY_DESTROYER
) {
    override fun getType() = EnemyType.HEAVY_DESTROYER
}
