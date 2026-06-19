package com.nebuladrift.systems

import com.badlogic.gdx.math.Vector2
import com.nebuladrift.entities.Asteroid
import com.nebuladrift.entities.AsteroidSize
import com.nebuladrift.entities.Astronaut
import com.nebuladrift.entities.Laser
import com.nebuladrift.entities.Ship
import com.nebuladrift.entities.SpaceDebris
import com.nebuladrift.entities.enemies.HeavyDestroyer
import com.nebuladrift.entities.enemies.LightFighter
import com.nebuladrift.entities.enemies.MediumFrigate
import com.nebuladrift.util.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for circle-circle collision detection in [CollisionSystem].
 *
 * The [CollisionSystem.overlap] method is private, so all tests exercise
 * collision detection through the public [CollisionSystem.update] path:
 *
 * - Laser ↔ Asteroid: laser is consumed on overlap, asteroid loses HP
 * - Ship ↔ Asteroid: ship takes damage on overlap, asteroid is removed
 */
class CollisionTest {

    private lateinit var collisionSystem: CollisionSystem

    @BeforeEach
    fun setUp() {
        collisionSystem = CollisionSystem()
    }

    // ── Laser ↔ Asteroid — overlap variants ──────────────────

    @Test
    fun `laser and asteroid overlapping triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.25f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(laser), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser should be removed after collision")
        assertEquals(2, asteroid.health, "Asteroid should lose 1 HP (LARGE has 3 HP)")
    }

    @Test
    fun `laser and asteroid touching at distance equals sum of radii triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))

        // Use a slightly smaller distance than the exact sum of radii
        // to avoid floating-point precision issues when computing 8f + 0.85f
        val epsilon = 0.0001f
        val touchingDistance = Constants.LASER_RADIUS + Constants.ASTEROID_LARGE_RADIUS - epsilon
        val asteroid = Asteroid(
            position = Vector2(8f + touchingDistance, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(laser), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser should be removed when circles touch")
        assertEquals(2, asteroid.health, "Asteroid should lose 1 HP from touching collision")
    }

    @Test
    fun `laser and asteroid separate should not collide`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val justOutOfRange = Constants.LASER_RADIUS + Constants.ASTEROID_LARGE_RADIUS + 0.01f
        val asteroid = Asteroid(
            position = Vector2(8f + justOutOfRange, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(laser), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isNotEmpty(), "Laser should remain when no collision")
        assertEquals(3, asteroid.health, "Asteroid should not take damage")
    }

    @Test
    fun `laser inside asteroid triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(laser), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser inside asteroid should collide")
    }

    @Test
    fun `laser at same center as asteroid triggers collision`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.SMALL
        )
        val context = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(laser), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser at same center should collide")
    }

    @Test
    fun `asteroid destroyed on final hit awards points and fires event`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.2f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.SMALL // 1 HP
        )
        val context = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(laser), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertTrue(context.lasers.isEmpty(), "Laser consumed")
        assertTrue(context.asteroids.isEmpty(), "SMALL asteroid destroyed in one hit")
        assertTrue(context.events.any { it is GameEvent.AsteroidDestroyed }, "Destruction event fired")
        assertEquals(Constants.SCORE_SMALL, context.score, "Points awarded for destruction")
    }

    // ── Ship ↔ Asteroid — overlap variants ──────────────────

    @Test
    fun `ship and asteroid overlapping triggers damage`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.5f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship = localShip, asteroids = mutableListOf(asteroid), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertEquals(2, localShip.lives, "Ship should lose a life on collision")
        assertTrue(context.asteroids.isEmpty(), "Asteroid removed after ship collision")
        assertTrue(context.events.any { it is GameEvent.ShipHit }, "Ship hit event fired")
    }

    @Test
    fun `ship and asteroid separate should not collide`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(10f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val context = GameContext(ship = localShip, asteroids = mutableListOf(asteroid), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, context)

        assertEquals(3, localShip.lives, "Ship should not lose a life")
        assertTrue(context.asteroids.isNotEmpty(), "Asteroid should remain")
        assertFalse(context.events.any { it is GameEvent.ShipHit }, "No ship hit event")
    }

    @Test
    fun `invulnerable ship does not take damage from asteroid`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(asteroid), lasers = mutableListOf(), events = mutableListOf(), score = 0)

        // Frame 1: ship gets hit → becomes invulnerable
        collisionSystem.update(0.016f, ctx)
        assertEquals(2, localShip.lives, "Ship should lose a life in first collision")
        assertTrue(localShip.isInvulnerable, "Ship should be invulnerable after hit")

        // Frame 2: add a fresh asteroid while ship is still invulnerable
        val asteroid2 = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        ctx.asteroids.add(asteroid2)
        ctx.events.clear()

        collisionSystem.update(0.016f, ctx)

        assertEquals(2, localShip.lives, "Ship should NOT lose life while invulnerable")
        assertTrue(ctx.asteroids.isNotEmpty(), "Asteroid should NOT be removed when ship is invulnerable")
        assertFalse(ctx.events.any { it is GameEvent.ShipHit }, "No ship hit event while invulnerable")
    }

    // ── Ship ↔ Enemy ──────────────────────────────────────────

    @Test
    fun `ship colliding with enemy triggers damage`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val enemy = LightFighter(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(2, localShip.lives, "Ship should lose a life on enemy collision")
        assertTrue(ctx.enemies.isEmpty(), "Enemy should be removed after ship collision")
        assertTrue(ctx.events.any { it is GameEvent.ShipHit }, "Ship hit event should fire")
    }

    @Test
    fun `ship and enemy separate should not collide`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val enemy = LightFighter(Vector2(12f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(3, localShip.lives, "Ship should not lose life")
        assertTrue(ctx.enemies.isNotEmpty(), "Enemy should remain")
        assertFalse(ctx.events.any { it is GameEvent.ShipHit }, "No ship hit event")
    }

    @Test
    fun `invulnerable ship does not take damage from enemy`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        // First collision to trigger invulnerability
        val firstEnemy = LightFighter(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(firstEnemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)
        assertEquals(2, localShip.lives)
        assertTrue(localShip.isInvulnerable)

        // Second enemy during invulnerability
        ctx.enemies.add(LightFighter(Vector2(8.25f, 4.5f)))
        ctx.events.clear()

        collisionSystem.update(0.016f, ctx)

        assertEquals(2, localShip.lives, "Ship should not lose life while invulnerable")
        assertTrue(ctx.enemies.isNotEmpty(), "Enemy should not be removed when ship is invulnerable")
    }

    // ── Laser ↔ Enemy ─────────────────────────────────────────

    @Test
    fun `laser hitting enemy removes laser and damages enemy`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val enemy = LightFighter(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.lasers.isEmpty(), "Laser should be removed after collision")
        assertTrue(enemy.isDestroyed, "LightFighter should be destroyed by one laser hit")
    }

    @Test
    fun `laser hitting medium frigate requires two hits to destroy`() {
        val ship = Ship()
        // Place enemy far from ship center to avoid ship-enemy cross-collision
        val enemy = MediumFrigate(Vector2(8.25f, 8f))

        // First laser hit
        val laser1 = Laser(position = Vector2(8f, 8f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser1),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)
        collisionSystem.update(0.016f, ctx)

        assertFalse(enemy.isDestroyed, "MediumFrigate should survive first hit")
        assertEquals(1, enemy.health, "HP should be 1 after first hit")
        assertTrue(ctx.enemies.isNotEmpty(), "Enemy should still be in context")

        // Second laser hit
        val laser2 = Laser(position = Vector2(8f, 8f))
        ctx.lasers.add(laser2)
        collisionSystem.update(0.016f, ctx)

        assertTrue(enemy.isDestroyed, "MediumFrigate should be destroyed after two hits")
        assertTrue(ctx.enemies.isEmpty(), "Enemy should be removed after destruction")
    }

    @Test
    fun `laser hitting enemy awards points on destruction`() {
        val ship = Ship()
        // Place enemy far from ship center to avoid ship-enemy cross-collision
        val laser = Laser(position = Vector2(8f, 8f))
        val enemy = LightFighter(Vector2(8.25f, 8f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.events.any { it is GameEvent.EnemyDestroyed }, "Enemy destroyed event should fire")
        assertEquals(Constants.SCORE_LIGHT_FIGHTER, ctx.score, "Points should be awarded for enemy destruction")
    }

    @Test
    fun `laser and enemy separate do not collide`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val enemy = LightFighter(Vector2(12f, 4.5f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.lasers.isNotEmpty(), "Laser should remain when no collision")
        assertFalse(enemy.isDestroyed, "Enemy should not be destroyed")
    }

    // ── Ship ↔ Astronaut ──────────────────────────────────────

    @Test
    fun `ship colliding with floating astronaut triggers rescue`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val astronaut = Astronaut(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.RESCUED, astronaut.state, "Astronaut should be rescued")
        assertTrue(ctx.events.any { it is GameEvent.AstronautRescued }, "Rescue event should fire")
        assertEquals(Constants.SCORE_ASTRONAUT_RESCUE, ctx.score, "Rescue points should be awarded")
    }

    @Test
    fun `ship does not rescue already rescued astronaut`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val astronaut = Astronaut(Vector2(8.25f, 4.5f))
        astronaut.rescue() // Already rescued
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.RESCUED, astronaut.state, "Should remain RESCUED")
        assertEquals(0, ctx.score, "No rescue points should be awarded")
    }

    @Test
    fun `invulnerable ship does not rescue astronaut`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        // Make ship invulnerable
        localShip.takeDamage()
        assertTrue(localShip.isInvulnerable)

        val astronaut = Astronaut(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.FLOATING, astronaut.state, "Invulnerable ship should not rescue astronaut")
        assertFalse(ctx.events.any { it is GameEvent.AstronautRescued }, "No rescue event")
    }

    @Test
    fun `ship and astronaut separate should not rescue`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        val astronaut = Astronaut(Vector2(15f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.FLOATING, astronaut.state, "Astronaut should remain FLOATING")
        assertEquals(0, ctx.score, "No score change")
    }

    // ── Laser ↔ Astronaut ─────────────────────────────────────

    @Test
    fun `laser hitting floating astronaut triggers kill`() {
        val ship = Ship()
        // Place astronaut far from ship center to avoid ship-astronaut cross-collision
        val laser = Laser(position = Vector2(8f, 8f))
        val astronaut = Astronaut(Vector2(8.25f, 8f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.DEAD, astronaut.state, "Astronaut should be killed")
        assertTrue(ctx.lasers.isEmpty(), "Laser should be removed")
        assertTrue(ctx.events.any { it is GameEvent.AstronautKilled }, "Kill event should fire")
    }

    @Test
    fun `laser killing astronaut deducts penalty`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 8f))
        val astronaut = Astronaut(Vector2(8.25f, 8f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 100)

        collisionSystem.update(0.016f, ctx)

        assertEquals(
            100 - Constants.SCORE_ASTRONAUT_KILL_PENALTY,
            ctx.score,
            "Score should be reduced by kill penalty"
        )
    }

    @Test
    fun `laser does not kill already rescued astronaut`() {
        val ship = Ship()
        val laser = Laser(position = Vector2(8f, 4.5f))
        val astronaut = Astronaut(Vector2(8.25f, 4.5f))
        astronaut.rescue() // Already rescued
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser),
            enemies = mutableListOf(), astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 50)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.RESCUED, astronaut.state, "Should remain RESCUED")
        assertEquals(50, ctx.score, "Score should not change")
    }

    // ── Ship ↔ Debris ─────────────────────────────────────────

    @Test
    fun `ship colliding with debris adds a life`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        localShip.takeDamage() // 2 lives
        assertEquals(2, localShip.lives)

        val debris = SpaceDebris(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(3, localShip.lives, "Ship should gain a life from debris")
        assertTrue(ctx.debris.isEmpty(), "Debris should be removed after collection")
        assertTrue(ctx.events.any { it is GameEvent.DebrisCollected }, "Debris collected event should fire")
    }

    @Test
    fun `debris does not add life when ship is at max lives`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        assertEquals(3, localShip.lives) // Already at max

        val debris = SpaceDebris(Vector2(8.25f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(3, localShip.lives, "Should stay at max lives")
        assertTrue(ctx.debris.isNotEmpty(), "Debris should not be removed when at max lives")
        assertFalse(ctx.events.any { it is GameEvent.DebrisCollected }, "No collection event")
    }

    @Test
    fun `ship and debris separate should not collect`() {
        val localShip = Ship(position = Vector2(8f, 4.5f))
        localShip.takeDamage()
        val debris = SpaceDebris(Vector2(15f, 4.5f))
        val ctx = GameContext(ship = localShip, asteroids = mutableListOf(), lasers = mutableListOf(),
            enemies = mutableListOf(), astronauts = mutableListOf(), debris = mutableListOf(debris),
            events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(2, localShip.lives, "Lives should not change")
        assertTrue(ctx.debris.isNotEmpty(), "Debris should remain")
    }

    // ── Multiple collisions in one frame ──────────────────────

    @Test
    fun `multiple lasers hitting enemies in one frame`() {
        val ship = Ship()
        val laser1 = Laser(position = Vector2(8f, 4.5f))
        val laser2 = Laser(position = Vector2(9f, 3f))
        val enemy1 = LightFighter(Vector2(8.25f, 4.5f))
        val enemy2 = LightFighter(Vector2(9.25f, 3f))
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(), lasers = mutableListOf(laser1, laser2),
            enemies = mutableListOf(enemy1, enemy2), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.lasers.isEmpty(), "Both lasers should be consumed")
        assertTrue(ctx.enemies.isEmpty(), "Both enemies should be destroyed")
        assertEquals(
            2 * Constants.SCORE_LIGHT_FIGHTER,
            ctx.score,
            "Points for both enemies"
        )
    }

    // ── Enemy ↔ Asteroid ──────────────────────────────────────

    @Test
    fun `enemy overlapping asteroid is destroyed`() {
        val ship = Ship()
        val enemy = LightFighter(Vector2(8.2f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.enemies.isEmpty(), "Enemy should be destroyed by asteroid")
        assertEquals(0, ctx.score, "No score for enemy-asteroid collision")
    }

    @Test
    fun `asteroid survives enemy collision`() {
        val ship = Ship()
        val enemy = LightFighter(Vector2(8.2f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.asteroids.isNotEmpty(), "Asteroid should survive")
    }

    @Test
    fun `enemy and asteroid far apart do not collide`() {
        val ship = Ship()
        val enemy = LightFighter(Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(15f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            enemies = mutableListOf(enemy), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertTrue(ctx.enemies.isNotEmpty(), "Enemy should survive")
    }

    // ── Astronaut ↔ Asteroid ──────────────────────────────────

    @Test
    fun `astronaut overlapping asteroid is killed`() {
        val ship = Ship()
        val astronaut = Astronaut(Vector2(8.2f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.DEAD, astronaut.state, "Astronaut should be killed by asteroid")
        assertTrue(ctx.events.any { it is GameEvent.AstronautKilled }, "Kill event should fire")
    }

    @Test
    fun `astronaut kill by asteroid deducts penalty`() {
        val ship = Ship()
        val astronaut = Astronaut(Vector2(8.2f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 100)

        collisionSystem.update(0.016f, ctx)

        assertEquals(100 - Constants.SCORE_ASTRONAUT_KILL_PENALTY, ctx.score,
            "Score should be reduced by kill penalty")
    }

    @Test
    fun `rescued astronaut does not die from asteroid`() {
        val ship = Ship()
        val astronaut = Astronaut(Vector2(8.2f, 4.5f))
        astronaut.rescue()
        val asteroid = Asteroid(
            position = Vector2(8.3f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.RESCUED, astronaut.state, "Rescued astronaut should not die")
        assertEquals(0, ctx.score, "No penalty")
    }

    @Test
    fun `astronaut and asteroid far apart do not collide`() {
        val ship = Ship()
        val astronaut = Astronaut(Vector2(8f, 4.5f))
        val asteroid = Asteroid(
            position = Vector2(15f, 4.5f),
            velocity = Vector2.Zero.cpy(),
            size = AsteroidSize.LARGE
        )
        val ctx = GameContext(ship = ship, asteroids = mutableListOf(asteroid), lasers = mutableListOf(),
            astronauts = mutableListOf(astronaut), events = mutableListOf(), score = 0)

        collisionSystem.update(0.016f, ctx)

        assertEquals(Astronaut.State.FLOATING, astronaut.state, "Astronaut should survive")
    }
}
