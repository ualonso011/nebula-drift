package com.nebuladrift.rendering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for [FadeTransition] phase progression.
 *
 * These tests exercise the update/phase lifecycle without any
 * GL-dependent code — the transition's timing logic is pure math.
 *
 * Covers:
 * - Phase progression: FADE_OUT → SWITCH → FADE_IN → DONE
 * - Alpha values at each phase boundary
 * - SWITCH signal timing (exactly one frame)
 * - [isComplete] behaviour at end of transition
 * - [reset] restarts the transition
 */
class ScreenTransitionTest {

    @Test
    fun `starts in FADE_OUT phase`() {
        val transition = FadeTransition(duration = 1f)
        assertEquals(FadeTransition.Phase.FADE_OUT, transition.phase)
        assertEquals(0f, transition.alpha)
        assertFalse(transition.isComplete)
    }

    @Test
    fun `fade out ramps alpha from 0 to 1`() {
        val transition = FadeTransition(duration = 1f)

        // Halfway through fade-out
        transition.update(0.5f)
        assertEquals(FadeTransition.Phase.FADE_OUT, transition.phase)
        assertEquals(0.5f, transition.alpha, 0.001f)

        // End of fade-out
        transition.update(0.5f)
        assertEquals(FadeTransition.Phase.FADE_IN, transition.phase) // SWITCH was consumed
        assertEquals(1f, transition.alpha, 0.001f) // FADE_IN alpha at timer=0
    }

    @Test
    fun `update returns true exactly on SWITCH phase`() {
        val transition = FadeTransition(duration = 0.5f)

        // Before end of fade-out
        var result = transition.update(0.3f)
        assertFalse(result, "Should not switch before fade-out is done")
        assertEquals(FadeTransition.Phase.FADE_OUT, transition.phase)

        // Complete the fade-out - this should trigger SWITCH
        result = transition.update(0.2f)
        assertTrue(result, "Should return true exactly when SWITCH phase triggers")
        // After update returns true, phase is now FADE_IN
        assertEquals(FadeTransition.Phase.FADE_IN, transition.phase)
    }

    @Test
    fun `fade in ramps alpha from 1 to 0`() {
        val transition = FadeTransition(duration = 1f)
        transition.update(1.0f) // completes fade-out and SWITCH
        assertEquals(FadeTransition.Phase.FADE_IN, transition.phase)
        assertEquals(1f, transition.alpha, 0.001f) // alpha at start of FADE_IN

        transition.update(0.3f)
        assertEquals(0.7f, transition.alpha, 0.001f)

        transition.update(0.7f)
        assertEquals(FadeTransition.Phase.DONE, transition.phase)
        assertEquals(0f, transition.alpha, 0.001f)
    }

    @Test
    fun `isComplete is true after transition finishes`() {
        val transition = FadeTransition(duration = 0.25f)
        assertFalse(transition.isComplete)

        // Run through full transition: FADE_OUT(0.25s) + SWITCH + FADE_IN(0.25s)
        transition.update(0.25f) // end of FADE_OUT → SWITCH → FADE_IN
        assertFalse(transition.isComplete, "Should not be complete mid-transition")

        transition.update(0.25f) // end of FADE_IN → DONE
        assertTrue(transition.isComplete, "Should be complete after FADE_IN ends")
    }

    @Test
    fun `update after completion does nothing`() {
        val transition = FadeTransition(duration = 0.5f)
        // Complete transition
        transition.update(0.5f)
        transition.update(0.5f)
        assertTrue(transition.isComplete)
        assertEquals(FadeTransition.Phase.DONE, transition.phase)

        // Further updates should not change state
        transition.update(1.0f)
        assertEquals(FadeTransition.Phase.DONE, transition.phase)
        assertEquals(0f, transition.alpha, 0.001f)
    }

    @Test
    fun `exactly-once switch signal`() {
        val transition = FadeTransition(duration = 0.5f)
        var switchCount = 0

        // Run through multiple updates — SWITCH should fire exactly once
        for (i in 0 until 10) {
            if (transition.update(0.1f)) {
                switchCount++
            }
        }

        assertEquals(1, switchCount, "SWITCH signal should fire exactly once")
    }

    @Test
    fun `alpha stays within zero-to-one range`() {
        val transition = FadeTransition(duration = 0.5f)

        // Run through the entire transition with small steps
        for (i in 0 until 30) {
            transition.update(0.05f)
            assertTrue(transition.alpha in 0f..1f, "Alpha should be in [0,1] at all times")
        }

        assertTrue(transition.isComplete)
        assertEquals(0f, transition.alpha, 0.001f)
    }

    @Test
    fun `timer resets on phase transitions`() {
        val transition = FadeTransition(duration = 1f)

        // Advance 0.8s into fade-out
        transition.update(0.8f)
        assertEquals(0.8f, transition.timer, 0.001f)

        // Complete fade-out
        transition.update(0.2f) // triggers SWITCH → FADE_IN, timer reset to 0
        assertEquals(FadeTransition.Phase.FADE_IN, transition.phase)
        assertEquals(0f, transition.timer, 0.001f, "Timer should reset at phase change")
    }

    @Test
    fun `reset restarts from FADE_OUT`() {
        val transition = FadeTransition(duration = 0.5f)
        transition.update(0.5f) // end of FADE_OUT → SWITCH → FADE_IN
        transition.update(0.5f) // end of FADE_IN → DONE
        assertTrue(transition.isComplete)

        transition.reset()
        assertEquals(FadeTransition.Phase.FADE_OUT, transition.phase)
        assertEquals(0f, transition.timer, 0.001f)
        assertEquals(0f, transition.alpha, 0.001f)
        assertFalse(transition.isComplete)
    }

    @Test
    fun `uses configured duration`() {
        val transition = FadeTransition(duration = 2.0f)
        assertFalse(transition.update(1.0f))
        assertEquals(FadeTransition.Phase.FADE_OUT, transition.phase)
        assertEquals(0.5f, transition.alpha, 0.001f)
    }

    @Test
    fun `alpha is zero at start of transition`() {
        val transition = FadeTransition(duration = 1f)
        assertEquals(0f, transition.alpha)
    }
}
