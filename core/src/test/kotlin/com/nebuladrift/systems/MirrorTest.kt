package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Ship
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for the Dark Clone mirroring queue in [MirrorSystem].
 *
 * Covers recording player actions, retrieving mirrored actions
 * with ~0.5s delay, queue size management, and reset behaviour.
 */
class MirrorTest {

    private lateinit var mirrorSystem: MirrorSystem
    private lateinit var ship: Ship

    @BeforeEach
    fun setUp() {
        mirrorSystem = MirrorSystem()
        ship = Ship()
    }

    // ── Recording ─────────────────────────────────────────────

    @Test
    fun `recordPlayerAction stores action with timestamp`() {
        ship.position.set(8f, 4.5f)
        mirrorSystem.recordPlayerAction(1.0f, ship, isShooting = false)

        val action = mirrorSystem.getMirroredAction(1.0f)
        assertNotNull(action, "Should retrieve action at same timestamp")
        assertEquals(1.0f, action!!.time, 0.001f, "Timestamp should match")
    }

    @Test
    fun `recordPlayerAction stores correct position`() {
        ship.position.set(5f, 3f)
        mirrorSystem.recordPlayerAction(1.0f, ship, isShooting = true)

        val action = mirrorSystem.getMirroredAction(1.0f)
        assertNotNull(action)
        assertEquals(5f, action!!.position.x, 0.001f, "Stored position X should match")
        assertEquals(3f, action.position.y, 0.001f, "Stored position Y should match")
    }

    @Test
    fun `recordPlayerAction stores shooting state`() {
        mirrorSystem.recordPlayerAction(1.0f, ship, isShooting = true)
        val action = mirrorSystem.getMirroredAction(1.0f)
        assertNotNull(action)
        assertTrue(action!!.isShooting, "Shooting flag should be stored")
    }

    // ── Mirror delay ──────────────────────────────────────────

    @Test
    fun `getMirroredAction returns action from approximately half a second ago`() {
        mirrorSystem.recordPlayerAction(0.0f, ship, isShooting = false)
        mirrorSystem.recordPlayerAction(0.5f, ship, isShooting = false)

        // At current time = 0.5s, target = 0.0s -> should get the 0.0s action
        val action = mirrorSystem.getMirroredAction(0.5f)
        assertNotNull(action, "Should return action from ~0.5s ago")
        assertEquals(0.0f, action!!.time, 0.001f, "Should return the 0.0s action")
    }

    @Test
    fun `getMirroredAction returns null when action is too recent`() {
        mirrorSystem.recordPlayerAction(0.0f, ship, isShooting = false)

        // At current time = 0.6f, delay 0.5f -> target = 0.1f
        // Action at time=0.0 has it.time >= 0.1? No (0.0 < 0.1). Returns null.
        val action = mirrorSystem.getMirroredAction(0.6f)
        assertNull(action, "Should return null when action is too recent for the mirror delay")
    }

    @Test
    fun `getMirroredAction returns the first action at or after target time`() {
        mirrorSystem.recordPlayerAction(1.0f, ship, isShooting = false)
        mirrorSystem.recordPlayerAction(1.5f, ship, isShooting = true)

        // current = 2.0f, target = 1.5f -> should find the 1.5s action (it.time >= 1.5)
        val action = mirrorSystem.getMirroredAction(2.0f)
        assertNotNull(action)
        assertEquals(1.5f, action!!.time, 0.001f, "Should match the 1.5s action")
    }

    // ── Queue size management ─────────────────────────────────

    @Test
    fun `queue maintains CLONE_MIRROR_QUEUE_SIZE entries`() {
        // Record 31 actions (0..30) — queue holds max 30, so time=0 is evicted
        for (i in 0..<Constants.CLONE_MIRROR_QUEUE_SIZE + 1) {
            mirrorSystem.recordPlayerAction(i.toFloat(), ship, isShooting = false)
        }

        // Queue now has entries 1..30 (30 items). At currentTime=1.0f,
        // target = 0.5f. The first action with time >= 0.5 is time=1 (not 0).
        val action = mirrorSystem.getMirroredAction(1.0f)
        assertNotNull(action, "Queue should still have actions")
        assertEquals(1.0f, action!!.time, 0.001f,
            "Oldest action should be time=1 (time=0 was evicted)")
    }

    @Test
    fun `most recent actions are still available after queue fills`() {
        val fillCount = Constants.CLONE_MIRROR_QUEUE_SIZE + 10
        for (i in 0..<fillCount) {
            mirrorSystem.recordPlayerAction(i.toFloat(), ship, isShooting = false)
        }

        // Recent action at fillCount - 1 (time=39) should be available
        // At currentTime = 39.5f, target = 39.0f, action at time=39 >= 39 ✓
        val recent = mirrorSystem.getMirroredAction(39.5f)
        assertNotNull(recent, "Recent actions should still be in the queue")
        assertEquals(39.0f, recent!!.time, 0.001f, "Should retrieve the last recorded action")
    }

    @Test
    fun `queue size never exceeds CLONE_MIRROR_QUEUE_SIZE`() {
        for (i in 0..<50) {
            mirrorSystem.recordPlayerAction(i.toFloat(), ship, isShooting = false)
        }

        // Queue has at most 30 entries (times 20..49). At currentTime=50f,
        // target = 49.5f. Action at time=49 has 49 < 49.5, so no match → null.
        assertNull(mirrorSystem.getMirroredAction(50f),
            "No action old enough with delay of 0.5s")

        // At currentTime=49.5f, target=49.0f, action at time=49 >= 49.0 ✓
        val action = mirrorSystem.getMirroredAction(49.5f)
        assertNotNull(action, "Last action should be retrievable")
        assertEquals(49.0f, action!!.time, 0.001f, "Should get action at time=49")
    }

    // ── Empty queue ───────────────────────────────────────────

    @Test
    fun `empty queue returns null`() {
        assertNull(mirrorSystem.getMirroredAction(0f), "Empty queue should return null")
        assertNull(mirrorSystem.getMirroredAction(10f), "Empty queue should return null at any time")
    }

    // ── Boundary: action at exact delay ───────────────────────

    @Test
    fun `mirrored position matches recorded position after delay`() {
        // Simulate recording positions over time
        ship.position.set(8f, 3f)
        mirrorSystem.recordPlayerAction(0.0f, ship, isShooting = false)

        ship.position.set(8f, 4f)
        mirrorSystem.recordPlayerAction(0.5f, ship, isShooting = false)

        ship.position.set(8f, 5f)
        mirrorSystem.recordPlayerAction(1.0f, ship, isShooting = true)

        // At 1.5s: target = 1.0s -> should get position.y = 5f
        val action = mirrorSystem.getMirroredAction(1.5f)
        assertNotNull(action)
        assertEquals(5f, action!!.position.y, 0.001f, "Should mirror Y position from 0.5s ago")
    }

    // ── Reset ─────────────────────────────────────────────────

    @Test
    fun `reset clears the queue`() {
        mirrorSystem.recordPlayerAction(0.0f, ship, isShooting = false)
        mirrorSystem.recordPlayerAction(0.5f, ship, isShooting = false)

        mirrorSystem.reset()

        assertNull(mirrorSystem.getMirroredAction(0.5f), "After reset, queue should be empty")
        assertNull(mirrorSystem.getMirroredAction(1.0f), "After reset, no actions available")
    }

    @Test
    fun `reset enables fresh recording`() {
        mirrorSystem.recordPlayerAction(0.0f, ship, isShooting = false)
        mirrorSystem.reset()

        mirrorSystem.recordPlayerAction(10.0f, ship, isShooting = true)
        val action = mirrorSystem.getMirroredAction(10.0f)
        assertNotNull(action, "After reset and re-record, action should be available")
        assertEquals(10.0f, action!!.time, 0.001f, "New recording should have correct timestamp")
    }

    // ── Position copy independence ────────────────────────────

    @Test
    fun `recorded position is independent of ship position changes`() {
        ship.position.set(8f, 4f)
        mirrorSystem.recordPlayerAction(0.0f, ship, isShooting = false)

        // Move ship
        ship.position.set(2f, 1f)

        val action = mirrorSystem.getMirroredAction(0.0f)
        assertNotNull(action)
        assertEquals(8f, action!!.position.x, 0.001f, "Recorded position should be independent of ship")
        assertEquals(4f, action.position.y, 0.001f)
    }
}
