package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2

/**
 * Base contract for all game entities.
 *
 * Every entity has a position, velocity, and collision radius.
 * The [update] method is called each frame with the frame delta.
 */
interface Entity {
    val position: Vector2
    val velocity: Vector2
    val radius: Float
    fun update(delta: Float)
}
