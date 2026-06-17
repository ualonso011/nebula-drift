package com.nebuladrift.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Fast, fragile enemy with 1 HP.
 *
 * Small collision radius, high speed, and low point value.
 * The most common enemy type, especially in the early game.
 */
class LightFighter(position: Vector2) : Enemy(
    position = position.cpy(),
    velocity = Vector2(-Constants.ENEMY_LIGHT_SPEED, 0f),
    radius = Constants.ENEMY_LIGHT_RADIUS,
    maxHealth = 1,
    points = Constants.SCORE_LIGHT_FIGHTER
) {
    override fun getType() = EnemyType.LIGHT_FIGHTER
}
