package com.nebuladrift.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * Kamikaze enemy — doesn't shoot, flies directly toward the player.
 *
 * Fast, low HP, high point value. Moves toward the ship's position
 * each frame instead of scrolling left. Explodes on contact.
 */
class Kamikaze(position: Vector2) : Enemy(
    position = position.cpy(),
    velocity = Vector2(0f, 0f), // velocity set by PhysicsSystem each frame
    radius = Constants.ENEMY_KAMIKAZE_RADIUS,
    maxHealth = 1,
    points = Constants.SCORE_KAMIKAZE
) {
    override fun getType() = EnemyType.KAMIKAZE
}
