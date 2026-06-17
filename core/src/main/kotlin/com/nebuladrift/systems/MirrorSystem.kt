package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Ship
import com.nebuladrift.util.Constants

/**
 * Queue-based player action recorder for Dark Clone mirroring.
 *
 * Records a snapshot of the player's state each frame. The
 * [getMirroredAction] method returns the state from ~0.5s ago
 * (CLONE_MIRROR_DELAY), which drives the DarkClone's Y-position
 * and firing.
 *
 * Not a [GameSystem] — owned by GameScreen which calls
 * [recordPlayerAction] each frame before the mirror system runs.
 */
class MirrorSystem {

    /** Queue of recent player actions, ordered by timestamp. */
    private val actionQueue = ArrayDeque<PlayerAction>()

    /**
     * Snapshot of player state at a given time.
     */
    data class PlayerAction(
        val time: Float,
        val position: Vector2,
        val isShooting: Boolean
    )

    /**
     * Record the current player state. Call this each frame
     * from GameScreen (after input is processed).
     *
     * The queue is bounded to [Constants.CLONE_MIRROR_QUEUE_SIZE]
     * entries (~0.5s at 60 FPS).
     */
    fun recordPlayerAction(
        elapsedTime: Float,
        ship: Ship,
        isShooting: Boolean
    ) {
        actionQueue.addLast(
            PlayerAction(elapsedTime, ship.position.cpy(), isShooting)
        )
        while (actionQueue.size > Constants.CLONE_MIRROR_QUEUE_SIZE) {
            actionQueue.removeFirst()
        }
    }

    /**
     * Return the action from ~[Constants.CLONE_MIRROR_DELAY] seconds
     * ago, or `null` if no action is available yet.
     *
     * The returned action drives DarkClone mirroring. The clone's
     * Y-position is set to the mirrored position, and [PlayerAction.isShooting]
     * triggers laser fire.
     */
    fun getMirroredAction(currentTime: Float): PlayerAction? {
        val targetTime = currentTime - Constants.CLONE_MIRROR_DELAY
        return actionQueue.firstOrNull { it.time >= targetTime }
    }

    /** Clear the queue (call on game restart). */
    fun reset() {
        actionQueue.clear()
    }
}
