package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for astronaut mechanics in [Astronaut].
 *
 * Covers state machine transitions (FLOATING → RESCUED / DEAD),
 * state timer management, animation completion, and removal logic.
 */
class AstronautTest {

    // ── Initial state ─────────────────────────────────────────

    @Test
    fun `astronaut starts in FLOATING state`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        assertEquals(Astronaut.State.FLOATING, astronaut.state, "Initial state should be FLOATING")
    }

    @Test
    fun `astronaut has correct radius`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        assertEquals(Constants.ASTRONAUT_RADIUS, astronaut.radius, 0.001f)
    }

    @Test
    fun `astronaut starts with zero state timer`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        assertEquals(0f, astronaut.stateTimer, 0.001f, "Initial state timer should be 0")
    }

    @Test
    fun `astronaut should not be removed when floating`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        assertFalse(astronaut.shouldRemove, "FLOATING astronaut should not be removed")
    }

    // ── Rescue transition ─────────────────────────────────────

    @Test
    fun `rescue changes state to RESCUED`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.rescue()
        assertEquals(Astronaut.State.RESCUED, astronaut.state, "After rescue, state should be RESCUED")
    }

    @Test
    fun `rescue resets state timer to zero`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.update(0.5f) // Advance timer while floating
        assertTrue(astronaut.stateTimer > 0f)

        astronaut.rescue()
        assertEquals(0f, astronaut.stateTimer, 0.001f, "Timer should reset on rescue")
    }

    @Test
    fun `rescue from non-floating state has no effect`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.rescue()
        assertEquals(Astronaut.State.RESCUED, astronaut.state)

        // Try to rescue again
        astronaut.rescue()
        assertEquals(Astronaut.State.RESCUED, astronaut.state, "Should remain RESCUED after second rescue")
    }

    // ── Kill transition ───────────────────────────────────────

    @Test
    fun `kill changes state to DEAD`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.kill()
        assertEquals(Astronaut.State.DEAD, astronaut.state, "After kill, state should be DEAD")
    }

    @Test
    fun `kill resets state timer to zero`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.update(0.3f)
        assertTrue(astronaut.stateTimer > 0f)

        astronaut.kill()
        assertEquals(0f, astronaut.stateTimer, 0.001f, "Timer should reset on kill")
    }

    @Test
    fun `kill from non-floating state has no effect`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.kill()
        assertEquals(Astronaut.State.DEAD, astronaut.state)

        // Try to kill again
        astronaut.kill()
        assertEquals(Astronaut.State.DEAD, astronaut.state, "Should remain DEAD after second kill")
    }

    // ── State timer progression ───────────────────────────────

    @Test
    fun `stateTimer increments when update is called while floating`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.update(0.1f)
        assertEquals(0.1f, astronaut.stateTimer, 0.001f, "Timer should increment by delta")
    }

    @Test
    fun `stateTimer increments through multiple updates`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.update(0.2f)
        astronaut.update(0.3f)
        astronaut.update(0.1f)
        assertEquals(0.6f, astronaut.stateTimer, 0.001f, "Timer should accumulate across updates")
    }

    @Test
    fun `stateTimer continues after rescue`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.rescue()
        astronaut.update(0.2f)
        assertEquals(0.2f, astronaut.stateTimer, 0.001f, "Timer should advance after rescue")
    }

    // ── Animation complete / shouldRemove ─────────────────────

    @Test
    fun `shouldRemove returns false when rescued but timer below threshold`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.rescue()
        astronaut.update(0.3f)
        assertFalse(astronaut.shouldRemove, "Should not remove before 0.5s animation time")
    }

    @Test
    fun `shouldRemove returns true when rescued and timer exceeds threshold`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.rescue()
        astronaut.update(0.6f)
        assertTrue(astronaut.shouldRemove, "Should remove after 0.5s animation time")
    }

    @Test
    fun `shouldRemove returns false when killed but timer below threshold`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.kill()
        astronaut.update(0.3f)
        assertFalse(astronaut.shouldRemove, "Should not remove before 0.5s animation time")
    }

    @Test
    fun `shouldRemove returns true when killed and timer exceeds threshold`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.kill()
        astronaut.update(0.5f + 0.01f)
        assertTrue(astronaut.shouldRemove, "Should remove after 0.5s animation time")
    }

    @Test
    fun `shouldRemove returns false forever while floating`() {
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        astronaut.update(10f) // long time floating
        assertFalse(astronaut.shouldRemove, "FLOATING astronaut should never be removed via shouldRemove")
    }

    // ── Astronaut movement ────────────────────────────────────

    @Test
    fun `astronaut moves left at constant speed`() {
        val astronaut = Astronaut(Vector2(10f, 5f))
        val initialX = astronaut.position.x
        astronaut.update(0.1f)
        assertTrue(astronaut.position.x < initialX, "Astronaut should drift left")
        assertEquals(10f - Constants.ASTRONAUT_SPEED * 0.1f, astronaut.position.x, 0.001f)
    }

    // ── Default velocity ──────────────────────────────────────

    @Test
    fun `astronaut default velocity is leftward`() {
        val astronaut = Astronaut(Vector2(10f, 5f))
        assertTrue(astronaut.velocity.x < 0f, "Default velocity should be leftward")
        assertEquals(-Constants.ASTRONAUT_SPEED, astronaut.velocity.x, 0.001f)
    }

    // ── State enum values ─────────────────────────────────────

    @Test
    fun `astronaut state enum has all three states`() {
        val states = Astronaut.State.values().toList()
        assertTrue(states.contains(Astronaut.State.FLOATING), "Should have FLOATING")
        assertTrue(states.contains(Astronaut.State.RESCUED), "Should have RESCUED")
        assertTrue(states.contains(Astronaut.State.DEAD), "Should have DEAD")
    }
}
