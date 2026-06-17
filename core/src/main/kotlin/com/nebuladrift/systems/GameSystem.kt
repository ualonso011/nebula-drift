package com.nebuladrift.systems

import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship

/**
 * Runtime context passed to every [GameSystem.update] call.
 *
 * Systems read and mutate the game state through this context,
 * including the entity lists, events queue, and score.
 *
 * @property ship The player's ship
 * @property asteroids Active asteroids (mutated by spawn + collision systems)
 * @property lasers Active lasers (mutated by input + collision systems)
 * @property events Frame-scoped events emitted during this update cycle
 * @property score Current player score
 */
data class GameContext(
    val ship: Ship,
    val asteroids: MutableList<Asteroid>,
    val lasers: MutableList<Laser>,
    val events: MutableList<GameEvent>,
    var score: Int
)

/**
 * Events emitted by systems during an update cycle.
 *
 * Consumed by the screen to trigger visual or audio feedback.
 */
sealed class GameEvent {
    /** An asteroid was destroyed: [asteroid] removed, [points] awarded. */
    data class AsteroidDestroyed(val asteroid: Asteroid, val points: Int) : GameEvent()

    /** Ship was hit: [remainingLives] left. */
    data class ShipHit(val remainingLives: Int) : GameEvent()

    /** A laser hit an asteroid (before damage resolution). */
    data class LaserAsteroidHit(val laser: Laser, val asteroid: Asteroid) : GameEvent()
}

/**
 * Stateless system contract.
 *
 * Each system receives the shared [GameContext] and the frame delta,
 * mutating entities or lists as needed.
 */
interface GameSystem {
    fun update(delta: Float, context: GameContext)
}
