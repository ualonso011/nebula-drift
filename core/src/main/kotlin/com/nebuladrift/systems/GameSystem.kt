package com.nebuladrift.systems

import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.Enemy

/**
 * Runtime context passed to every [GameSystem.update] call.
 *
 * Systems read and mutate the game state through this context,
 * including entity lists, the events queue, score, and elapsed
 * time since the session started.
 *
 * @property ship The player's ship
 * @property asteroids Active asteroids (mutated by spawn + collision systems)
 * @property lasers Active lasers (mutated by input + collision systems)
 * @property enemies Active enemies (mutated by spawn + collision systems)
 * @property astronauts Active astronauts (mutated by spawn + collision systems)
 * @property debris Active space debris (mutated by spawn + collision systems)
 * @property events Frame-scoped events emitted during this update cycle
 * @property score Current player score
 * @property elapsedTime Total time elapsed since session start (seconds)
 */
data class GameContext(
    val ship: Ship,
    val asteroids: MutableList<Asteroid>,
    val lasers: MutableList<Laser>,
    val enemies: MutableList<Enemy> = mutableListOf(),
    val astronauts: MutableList<Astronaut> = mutableListOf(),
    val debris: MutableList<SpaceDebris> = mutableListOf(),
    val events: MutableList<GameEvent>,
    var score: Int,
    var elapsedTime: Float = 0f,
    val difficultyManager: DifficultyManager = DifficultyManager(),
    val mirrorSystem: MirrorSystem = MirrorSystem()
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

    // ── New event types (v0.2.0) ──────────────────────────────

    /** An enemy was destroyed: [enemy] removed, [points] awarded. */
    data class EnemyDestroyed(val enemy: Enemy, val points: Int) : GameEvent()

    /** An astronaut was rescued by the ship. */
    data class AstronautRescued(val astronaut: Astronaut) : GameEvent()

    /** An astronaut was killed by a laser. */
    data class AstronautKilled(val astronaut: Astronaut) : GameEvent()

    /** Space debris was collected by the ship (extra life). */
    data class DebrisCollected(val debris: SpaceDebris) : GameEvent()
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
