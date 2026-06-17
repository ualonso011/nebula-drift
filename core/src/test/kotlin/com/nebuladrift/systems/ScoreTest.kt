package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.enemies.EnemyType
import com.nebuladrift.entities.enemies.HeavyDestroyer
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.entities.enemies.MediumFrigate
import com.nebuladrift.entities.enemies.DarkClone
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for scoring logic in [ScoreSystem].
 *
 * Covers elapsed time tracking, time bonus accumulation, asteroid
 * destruction counting per size tier, and full state reset.
 */
class ScoreTest {

    private lateinit var scoreSystem: ScoreSystem

    @BeforeEach
    fun setUp() {
        scoreSystem = ScoreSystem()
    }

    // ── Time tracking ────────────────────────────────────────

    @Test
    fun `elapsed time accumulates with delta`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(1.5f, context)

        assertEquals(1.5f, scoreSystem.elapsedTime, 0.001f, "Elapsed time should match total delta")
    }

    @Test
    fun `multiple updates accumulate elapsed time`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(0.5f, context)
        scoreSystem.update(1.0f, context)
        scoreSystem.update(0.25f, context)

        assertEquals(1.75f, scoreSystem.elapsedTime, 0.001f, "Elapsed time should accumulate across updates")
    }

    // ── Time bonus (1 point per second) ──────────────────────

    @Test
    fun `time score awards 1 point per second`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(1.0f, context)

        assertEquals(Constants.SCORE_TIME_BONUS, context.score, "Score should increase by time bonus for 1 second")
    }

    @Test
    fun `time score for multiple seconds`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(3.0f, context)

        assertEquals(3 * Constants.SCORE_TIME_BONUS, context.score, "Score should increase for each full second")
    }

    @Test
    fun `partial seconds do not award time points until a full second`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(0.3f, context)
        assertEquals(0, context.score, "0.3 seconds should not award points")

        scoreSystem.update(0.3f, context)
        assertEquals(0, context.score, "0.6 seconds should not award points")

        scoreSystem.update(0.4f, context)
        assertEquals(Constants.SCORE_TIME_BONUS, context.score, "1.0 seconds should award points")
    }

    @Test
    fun `time score accumulates fractional seconds across frames`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(0.7f, context)
        scoreSystem.update(0.7f, context) // 1.4 total

        assertEquals(Constants.SCORE_TIME_BONUS, context.score, "Should award 1 point for the first full second")
    }

    // ── Asteroid destruction counting ────────────────────────

    @Test
    fun `asteroid destruction counts are tracked`() {
        val asteroid = Asteroid(
            position = Vector2.Zero.cpy(),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(),
            events = mutableListOf(GameEvent.AsteroidDestroyed(asteroid, Constants.SCORE_LARGE)),
            score = 0
        )

        scoreSystem.update(0.016f, context)

        assertEquals(1, scoreSystem.asteroidsDestroyed, "Should count one destroyed asteroid")
    }

    @Test
    fun `multiple asteroid destructions are counted`() {
        val ctx = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(),
            events = mutableListOf(),
            score = 0
        )
        val asteroid1 = Asteroid(Vector2.Zero.cpy(), Vector2.Zero.cpy(), AsteroidSize.LARGE)
        val asteroid2 = Asteroid(Vector2.Zero.cpy(), Vector2.Zero.cpy(), AsteroidSize.MEDIUM)
        val asteroid3 = Asteroid(Vector2.Zero.cpy(), Vector2.Zero.cpy(), AsteroidSize.SMALL)

        ctx.events.add(GameEvent.AsteroidDestroyed(asteroid1, Constants.SCORE_LARGE))
        ctx.events.add(GameEvent.AsteroidDestroyed(asteroid2, Constants.SCORE_MEDIUM))
        ctx.events.add(GameEvent.AsteroidDestroyed(asteroid3, Constants.SCORE_SMALL))

        scoreSystem.update(0.016f, context = ctx)

        assertEquals(3, scoreSystem.asteroidsDestroyed, "Should count all three destroyed asteroids")
    }

    @Test
    fun `time bonus accumulates alongside asteroid tracking`() {
        val asteroid = Asteroid(
            position = Vector2.Zero.cpy(),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.MEDIUM
        )
        val context = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(),
            events = mutableListOf(GameEvent.AsteroidDestroyed(asteroid, Constants.SCORE_MEDIUM)),
            score = 0
        )

        scoreSystem.update(2.5f, context)

        // ScoreSystem handles time bonus; destruction points are added by CollisionSystem
        assertEquals(
            2 * Constants.SCORE_TIME_BONUS,
            context.score,
            "Score should include time bonus only (destruction points handled by CollisionSystem)"
        )
        assertEquals(1, scoreSystem.asteroidsDestroyed, "Asteroid count should be tracked")
        assertEquals(2.5f, scoreSystem.elapsedTime, 0.001f, "Elapsed time should accumulate")
    }

    // ── Formatted time ───────────────────────────────────────

    @Test
    fun `formatted time shows zero for initial state`() {
        assertEquals("0:00", scoreSystem.formattedTime, "Initial formatted time should be 0:00")
    }

    @Test
    fun `formatted time shows minutes and seconds`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(127.0f, context) // 2 minutes 7 seconds

        assertEquals("2:07", scoreSystem.formattedTime, "Formatted time should show M:SS")
    }

    // ── Reset ────────────────────────────────────────────────

    @Test
    fun `reset clears elapsed time`() {
        val context = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        scoreSystem.update(5.0f, context)
        scoreSystem.reset()

        assertEquals(0f, scoreSystem.elapsedTime, 0.001f, "Elapsed time should reset to zero")
    }

    @Test
    fun `reset clears asteroid count and all tracking`() {
        val asteroid = Asteroid(
            position = Vector2.Zero.cpy(),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(
            ship = Ship(),
            asteroids = mutableListOf(),
            lasers = mutableListOf(),
            events = mutableListOf(GameEvent.AsteroidDestroyed(asteroid, Constants.SCORE_LARGE)),
            score = 10 // starting score
        )

        scoreSystem.update(3.0f, context)
        scoreSystem.reset()

        assertEquals(0, scoreSystem.asteroidsDestroyed, "Asteroid count should reset")
        assertEquals(0f, scoreSystem.elapsedTime, 0.001f, "Elapsed time should reset")
        assertEquals(0, scoreSystem.astronautsRescued, "Astronauts rescued should reset")
        assertEquals("0:00", scoreSystem.formattedTime, "Formatted time should reset")
    }

    // ── Enemy destruction points ──────────────────────────────

    @Test
    fun `lightFighter destruction awards 150 points`() {
        val enemy = LightFighter(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(GameEvent.EnemyDestroyed(enemy, Constants.SCORE_LIGHT_FIGHTER)),
            score = 0)

        scoreSystem.update(0.016f, ctx)

        assertEquals(1, scoreSystem.enemiesDestroyed, "Should count one enemy destroyed")
    }

    @Test
    fun `mediumFrigate destruction awards 250 points`() {
        val enemy = MediumFrigate(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(GameEvent.EnemyDestroyed(enemy, Constants.SCORE_MEDIUM_FRIGATE)),
            score = 0)

        scoreSystem.update(0.016f, ctx)

        assertEquals(1, scoreSystem.enemiesDestroyed)
    }

    @Test
    fun `heavyDestroyer destruction awards 400 points`() {
        val enemy = HeavyDestroyer(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(GameEvent.EnemyDestroyed(enemy, Constants.SCORE_HEAVY_DESTROYER)),
            score = 0)

        scoreSystem.update(0.016f, ctx)

        assertEquals(1, scoreSystem.enemiesDestroyed)
    }

    @Test
    fun `darkClone destruction awards 500 points`() {
        val enemy = DarkClone(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(GameEvent.EnemyDestroyed(enemy, Constants.SCORE_DARK_CLONE)),
            score = 0)

        scoreSystem.update(0.016f, ctx)

        assertEquals(1, scoreSystem.enemiesDestroyed)
    }

    @Test
    fun `multiple enemy destructions are counted`() {
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), events = mutableListOf(), score = 0)
        ctx.events.add(GameEvent.EnemyDestroyed(LightFighter(Vector2.Zero.cpy()), Constants.SCORE_LIGHT_FIGHTER))
        ctx.events.add(GameEvent.EnemyDestroyed(MediumFrigate(Vector2.Zero.cpy()), Constants.SCORE_MEDIUM_FRIGATE))
        ctx.events.add(GameEvent.EnemyDestroyed(HeavyDestroyer(Vector2.Zero.cpy()), Constants.SCORE_HEAVY_DESTROYER))

        scoreSystem.update(0.016f, ctx)

        assertEquals(3, scoreSystem.enemiesDestroyed, "Should count all three destroyed enemies")
    }

    // ── Astronaut scoring ─────────────────────────────────────

    @Test
    fun `astronaut rescue counts are tracked`() {
        val astronaut = Astronaut(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(GameEvent.AstronautRescued(astronaut)),
            score = 0)

        scoreSystem.update(0.016f, ctx)

        assertEquals(1, scoreSystem.astronautsRescued, "Should count one rescued astronaut")
    }

    @Test
    fun `astronaut kill counts are tracked`() {
        val astronaut = Astronaut(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(GameEvent.AstronautKilled(astronaut)),
            score = 0)

        scoreSystem.update(0.016f, ctx)

        assertEquals(1, scoreSystem.astronautsKilled, "Should count one killed astronaut")
    }

    @Test
    fun `multiple astronaut events are tracked`() {
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(), score = 0)
        ctx.events.add(GameEvent.AstronautRescued(Astronaut(Vector2.Zero.cpy())))
        ctx.events.add(GameEvent.AstronautRescued(Astronaut(Vector2.Zero.cpy())))
        ctx.events.add(GameEvent.AstronautKilled(Astronaut(Vector2.Zero.cpy())))

        scoreSystem.update(0.016f, ctx)

        assertEquals(2, scoreSystem.astronautsRescued, "Should count two rescues")
        assertEquals(1, scoreSystem.astronautsKilled, "Should count one kill")
    }

    // ── Stats: enemies destroyed by type ──────────────────────

    @Test
    fun `getEnemiesDestroyedByType returns empty map initially`() {
        val result = scoreSystem.getEnemiesDestroyedByType()
        assertTrue(result.isEmpty(), "Initial enemy-by-type map should be empty")
    }

    @Test
    fun `getEnemiesDestroyedByType tracks per-type counts`() {
        val fighter = LightFighter(Vector2.Zero.cpy())
        val frigate = MediumFrigate(Vector2.Zero.cpy())

        scoreSystem.addEnemyKill(fighter)
        scoreSystem.addEnemyKill(frigate)
        scoreSystem.addEnemyKill(fighter)

        val stats = scoreSystem.getEnemiesDestroyedByType()
        assertEquals(2, stats[EnemyType.LIGHT_FIGHTER], "Should have 2 LightFighter kills")
        assertEquals(1, stats[EnemyType.MEDIUM_FRIGATE], "Should have 1 MediumFrigate kill")
        assertEquals(3, scoreSystem.enemiesDestroyed, "Total enemies destroyed should be 3")
    }

    @Test
    fun `getEnemiesDestroyedByType returns immutable snapshot`() {
        scoreSystem.addEnemyKill(LightFighter(Vector2.Zero.cpy()))
        val stats = scoreSystem.getEnemiesDestroyedByType()

        // Map should be immutable (toMap())
        assertEquals(1, stats.size)
        assertEquals(1, stats[EnemyType.LIGHT_FIGHTER])
    }

    @Test
    fun `reset clears enemies destroyed by type`() {
        scoreSystem.addEnemyKill(LightFighter(Vector2.Zero.cpy()))
        scoreSystem.addEnemyKill(MediumFrigate(Vector2.Zero.cpy()))
        assertEquals(2, scoreSystem.enemiesDestroyed)

        scoreSystem.reset()

        assertEquals(0, scoreSystem.enemiesDestroyed, "Enemies destroyed should reset")
        assertTrue(scoreSystem.getEnemiesDestroyedByType().isEmpty(), "Per-type map should clear")
    }

    @Test
    fun `reset clears astronaut stats`() {
        val astronaut = Astronaut(Vector2.Zero.cpy())
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(),
            events = mutableListOf(GameEvent.AstronautRescued(astronaut), GameEvent.AstronautKilled(astronaut)),
            score = 0)

        scoreSystem.update(0.016f, ctx)
        assertEquals(1, scoreSystem.astronautsRescued)
        assertEquals(1, scoreSystem.astronautsKilled)

        scoreSystem.reset()

        assertEquals(0, scoreSystem.astronautsRescued, "Astronauts rescued should reset")
        assertEquals(0, scoreSystem.astronautsKilled, "Astronauts killed should reset")
    }

    // ── Combined tracking ─────────────────────────────────────

    @Test
    fun `all stats tracked simultaneously`() {
        val ctx = GameContext(ship = Ship(), asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), events = mutableListOf(), score = 0)

        ctx.events.add(GameEvent.AsteroidDestroyed(
            Asteroid(Vector2.Zero.cpy(), Vector2.Zero.cpy(), AsteroidSize.LARGE), 300))
        ctx.events.add(GameEvent.EnemyDestroyed(LightFighter(Vector2.Zero.cpy()), 150))
        ctx.events.add(GameEvent.AstronautRescued(Astronaut(Vector2.Zero.cpy())))
        ctx.events.add(GameEvent.AstronautKilled(Astronaut(Vector2.Zero.cpy())))

        scoreSystem.update(1.0f, ctx)

        assertEquals(1, scoreSystem.asteroidsDestroyed, "Should count asteroid")
        assertEquals(1, scoreSystem.enemiesDestroyed, "Should count enemy")
        assertEquals(1, scoreSystem.astronautsRescued, "Should count rescue")
        assertEquals(1, scoreSystem.astronautsKilled, "Should count kill")
    }
}
