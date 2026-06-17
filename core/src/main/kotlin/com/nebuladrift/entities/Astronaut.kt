package com.nebuladrift.entities

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.util.Constants

/**
 * A floating astronaut that can be rescued or killed.
 *
 * Starts in [State.FLOATING] and drifts slowly left. When the
 * ship overlaps a FLOATING astronaut, it is rescued (state
 * becomes [State.RESCUED]). When a laser hits a FLOATING
 * astronaut, it is killed (state becomes [State.DEAD]).
 *
 * After a rescue or kill, [stateTimer] counts up. Once it exceeds
 * 0.5s the astronaut is removed by the physics system.
 */
class Astronaut(
    position: Vector2,
    velocity: Vector2 = Vector2(-Constants.ASTRONAUT_SPEED, 0f)
) : Entity {

    enum class State {
        FLOATING,
        RESCUED,
        DEAD
    }

    override val position: Vector2 = position.cpy()
    override val velocity: Vector2 = velocity.cpy()
    override val radius: Float = Constants.ASTRONAUT_RADIUS

    /** Current state in the astronaut lifecycle. */
    var state: State = State.FLOATING
        private set

    /** Time elapsed in the current state (used for animation). */
    var stateTimer: Float = 0f
        private set

    /** Whether the astronaut should be removed (animation complete). */
    val shouldRemove: Boolean get() = state != State.FLOATING && stateTimer > 0.5f

    /**
     * Rescue this astronaut. Only transitions from FLOATING state.
     */
    fun rescue() {
        if (state == State.FLOATING) {
            state = State.RESCUED
            stateTimer = 0f
        }
    }

    /**
     * Kill this astronaut. Only transitions from FLOATING state.
     */
    fun kill() {
        if (state == State.FLOATING) {
            state = State.DEAD
            stateTimer = 0f
        }
    }

    override fun update(delta: Float) {
        position.mulAdd(velocity, delta)
        stateTimer += delta
    }
}
