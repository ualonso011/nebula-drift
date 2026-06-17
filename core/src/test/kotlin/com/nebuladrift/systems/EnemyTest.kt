package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.enemies.DarkClone
import com.nebuladrift.entities.enemies.EnemyDamageState
import com.nebuladrift.entities.enemies.EnemyType
import com.nebuladrift.entities.enemies.HeavyDestroyer
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.entities.enemies.MediumFrigate
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for enemy mechanics in [com.nebuladrift.entities.enemies.Enemy]
 * and its four subclasses.
 *
 * Covers initial stats (HP, points), damage application, destruction
 * state transitions, and damage-state reporting for all enemy types.
 */
class EnemyTest {

    // ── LightFighter (1 HP, 150 points) ───────────────────────

    @Test
    fun `lightFighter starts with 1 HP`() {
        val enemy = LightFighter(Vector2(8f, 4.5f))
        assertEquals(1, enemy.maxHealth, "LightFighter should have 1 max HP")
        assertEquals(1, enemy.health, "LightFighter should start at full HP")
        assertEquals(Constants.SCORE_LIGHT_FIGHTER, enemy.points, "LightFighter should award 150 points")
        assertEquals(EnemyType.LIGHT_FIGHTER, enemy.getType(), "Type should be LIGHT_FIGHTER")
    }

    @Test
    fun `lightFighter destroyed on first hit`() {
        val enemy = LightFighter(Vector2(8f, 4.5f))
        assertFalse(enemy.isDestroyed, "Should not be destroyed initially")
        val destroyed = enemy.takeDamage()
        assertTrue(destroyed, "takeDamage should return true when destroyed")
        assertTrue(enemy.isDestroyed, "LightFighter should be destroyed after 1 hit")
        assertEquals(0, enemy.health, "HP should be 0 after one hit")
    }

    @Test
    fun `lightFighter damage state is pristine only when full health`() {
        val enemy = LightFighter(Vector2(8f, 4.5f))
        assertEquals(EnemyDamageState.PRISTINE, enemy.getDamageState(), "Full HP should be PRISTINE")
        enemy.takeDamage()
        assertEquals(EnemyDamageState.CRITICAL, enemy.getDamageState(), "0 HP should be CRITICAL")
    }

    // ── MediumFrigate (2 HP, 250 points) ──────────────────────

    @Test
    fun `mediumFrigate starts with 2 HP`() {
        val enemy = MediumFrigate(Vector2(8f, 4.5f))
        assertEquals(2, enemy.maxHealth, "MediumFrigate should have 2 max HP")
        assertEquals(2, enemy.health, "MediumFrigate should start at full HP")
        assertEquals(Constants.SCORE_MEDIUM_FRIGATE, enemy.points, "MediumFrigate should award 250 points")
        assertEquals(EnemyType.MEDIUM_FRIGATE, enemy.getType(), "Type should be MEDIUM_FRIGATE")
    }

    @Test
    fun `mediumFrigate takes two hits to destroy`() {
        val enemy = MediumFrigate(Vector2(8f, 4.5f))
        assertFalse(enemy.takeDamage(), "First hit should not destroy")
        assertEquals(1, enemy.health, "HP should be 1 after first hit")
        assertFalse(enemy.isDestroyed, "Should not be destroyed after first hit")
        assertTrue(enemy.takeDamage(), "Second hit should destroy")
        assertTrue(enemy.isDestroyed, "Should be destroyed after second hit")
    }

    @Test
    fun `mediumFrigate damage state transitions correctly`() {
        val enemy = MediumFrigate(Vector2(8f, 4.5f))
        assertEquals(EnemyDamageState.PRISTINE, enemy.getDamageState(), "2/2 HP should be PRISTINE")
        enemy.takeDamage()
        assertEquals(EnemyDamageState.DAMAGED, enemy.getDamageState(), "1/2 HP should be DAMAGED (ratio 0.5)")
        enemy.takeDamage()
        assertEquals(EnemyDamageState.CRITICAL, enemy.getDamageState(), "0/2 HP should be CRITICAL")
    }

    // ── HeavyDestroyer (3 HP, 400 points) ─────────────────────

    @Test
    fun `heavyDestroyer starts with 3 HP`() {
        val enemy = HeavyDestroyer(Vector2(8f, 4.5f))
        assertEquals(3, enemy.maxHealth, "HeavyDestroyer should have 3 max HP")
        assertEquals(3, enemy.health, "HeavyDestroyer should start at full HP")
        assertEquals(Constants.SCORE_HEAVY_DESTROYER, enemy.points, "HeavyDestroyer should award 400 points")
        assertEquals(EnemyType.HEAVY_DESTROYER, enemy.getType(), "Type should be HEAVY_DESTROYER")
    }

    @Test
    fun `heavyDestroyer takes three hits to destroy`() {
        val enemy = HeavyDestroyer(Vector2(8f, 4.5f))
        assertFalse(enemy.takeDamage(), "First hit should not destroy")
        assertFalse(enemy.takeDamage(), "Second hit should not destroy")
        assertEquals(1, enemy.health, "HP should be 1 after two hits")
        assertTrue(enemy.takeDamage(), "Third hit should destroy")
        assertTrue(enemy.isDestroyed, "Should be destroyed after third hit")
    }

    @Test
    fun `heavyDestroyer damage state transitions correctly`() {
        val enemy = HeavyDestroyer(Vector2(8f, 4.5f))
        assertEquals(EnemyDamageState.PRISTINE, enemy.getDamageState(), "3/3 HP should be PRISTINE")
        enemy.takeDamage()
        assertEquals(EnemyDamageState.PRISTINE, enemy.getDamageState(), "2/3 HP (0.66) should still be PRISTINE")
        enemy.takeDamage()
        assertEquals(EnemyDamageState.DAMAGED, enemy.getDamageState(), "1/3 HP (0.33) should be DAMAGED")
        enemy.takeDamage()
        assertEquals(EnemyDamageState.CRITICAL, enemy.getDamageState(), "0/3 HP should be CRITICAL")
    }

    // ── DarkClone (2-3 HP, 500 points) ───────────────────────

    @Test
    fun `darkClone has randomized HP between 2 and 3`() {
        val enemy = DarkClone(Vector2(8f, 4.5f))
        assertTrue(enemy.maxHealth in 2..3, "DarkClone should have 2 or 3 HP, got ${enemy.maxHealth}")
        assertEquals(enemy.maxHealth, enemy.health, "DarkClone should start at full HP")
        assertEquals(Constants.SCORE_DARK_CLONE, enemy.points, "DarkClone should award 500 points")
        assertEquals(EnemyType.DARK_CLONE, enemy.getType(), "Type should be DARK_CLONE")
    }

    @Test
    fun `darkClone isFiring defaults to false`() {
        val enemy = DarkClone(Vector2(8f, 4.5f))
        assertFalse(enemy.isFiring, "DarkClone should not be firing by default")
    }

    @Test
    fun `darkClone can produce both HP values over multiple instances`() {
        // Create several clones and verify we see both 2 and 3 HP values
        val hps = (0..<20).map { DarkClone(Vector2(8f, 4.5f)).maxHealth }.distinct()
        assertTrue(hps.containsAll(listOf(2, 3)) || hps.size >= 2,
            "Over 20 instances, should see both 2 and 3 HP, got $hps")
    }

    // ── Shared enemy mechanics ────────────────────────────────

    @Test
    fun `takeDamage reduces HP by one each call`() {
        val enemy = HeavyDestroyer(Vector2(8f, 4.5f))
        assertEquals(3, enemy.health)
        enemy.takeDamage()
        assertEquals(2, enemy.health)
        enemy.takeDamage()
        assertEquals(1, enemy.health)
    }

    @Test
    fun `isDestroyed becomes true when HP reaches zero`() {
        val enemy = LightFighter(Vector2(8f, 4.5f))
        assertFalse(enemy.isDestroyed)
        enemy.takeDamage()
        assertTrue(enemy.isDestroyed)
    }

    @Test
    fun `takeDamage on already destroyed enemy returns true and HP stays at 0`() {
        val enemy = LightFighter(Vector2(8f, 4.5f))
        enemy.takeDamage()
        assertTrue(enemy.isDestroyed)
        // Additional damage should still report destroyed (HP already 0 or less)
        val result = enemy.takeDamage()
        assertTrue(result, "takeDamage on dead enemy should still return true")
        assertTrue(enemy.health <= 0, "HP should be <= 0 after destruction")
    }

    @Test
    fun `getDamageState returns PRISTINE for full health`() {
        val enemy = HeavyDestroyer(Vector2(8f, 4.5f))
        assertEquals(EnemyDamageState.PRISTINE, enemy.getDamageState())
    }

    @Test
    fun `getDamageState returns DAMAGED for health between 33 and 66 percent`() {
        val enemy = MediumFrigate(Vector2(8f, 4.5f))
        enemy.takeDamage() // 1/2 = 0.5, between 0.33 and 0.66
        assertEquals(EnemyDamageState.DAMAGED, enemy.getDamageState())
    }

    @Test
    fun `getDamageState returns CRITICAL for health at or below 33 percent`() {
        val enemy = HeavyDestroyer(Vector2(8f, 4.5f))
        enemy.takeDamage() // 2/3 ≈ 0.66 -> PRISTINE
        enemy.takeDamage() // 1/3 ≈ 0.33 -> DAMAGED (ratio > 0.33)
        // Actually 1/3 = 0.33..., which is > 0.33f, so it's DAMAGED
        assertEquals(EnemyDamageState.DAMAGED, enemy.getDamageState(), "1/3 HP is 0.333, still > 0.33")
        enemy.takeDamage() // 0/3 = 0.0 -> CRITICAL
        assertEquals(EnemyDamageState.CRITICAL, enemy.getDamageState(), "0/3 HP should be CRITICAL")
    }

    @Test
    fun `enemies move left via update`() {
        val enemy = LightFighter(Vector2(10f, 5f))
        val initialX = enemy.position.x
        enemy.update(0.1f)
        assertTrue(enemy.position.x < initialX, "Enemy should move left after update")
    }

    @Test
    fun `all enemy types have positive radius`() {
        assertTrue(LightFighter(Vector2.Zero.cpy()).radius > 0f)
        assertTrue(MediumFrigate(Vector2.Zero.cpy()).radius > 0f)
        assertTrue(HeavyDestroyer(Vector2.Zero.cpy()).radius > 0f)
        assertTrue(DarkClone(Vector2.Zero.cpy()).radius > 0f)
    }

    @Test
    fun `all enemy types have positive speed`() {
        assertTrue(Constants.ENEMY_LIGHT_SPEED > 0f)
        assertTrue(Constants.ENEMY_MEDIUM_SPEED > 0f)
        assertTrue(Constants.ENEMY_HEAVY_SPEED > 0f)
        assertTrue(Constants.ENEMY_CLONE_SPEED > 0f)
    }
}
