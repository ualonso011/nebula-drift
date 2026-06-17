package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.nebuladrift.systems.GameEvent
import com.nebuladrift.util.Constants
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

/**
 * Explosion visual size category.
 */
enum class ExplosionSize { SMALL, MEDIUM, LARGE }

/**
 * Pool-backed particle system with five effect templates.
 *
 * Manages up to [Constants.PARTICLE_MAX_COUNT] simultaneous particles.
 * Particles are pooled for zero-allocation reuse. Each effect type
 * spawns a burst of coloured, velocity-driven particles that fade
 * over their lifetime.
 *
 * ## Effect templates
 * - **Explosion** – orange/red burst, configurable size
 * - **Engine trail** – small blue/white particles trailing downward
 * - **Rescue sparkle** – green/yellow upward sparkle burst
 * - **Debris sparkle** – golden sparkle burst
 * - **Damage sparks** – red/orange brief sparks
 *
 * Reacts to [GameEvent]s via [onGameEvent].
 */
class ParticleManager(atlas: SpriteAtlas? = null) {

    private val particles = mutableListOf<Particle>()
    private val pool = mutableListOf<Particle>()
    private val particleRegion: TextureRegion? = atlas?.findRegion("particle")

    /** Number of currently active particles. */
    val activeCount: Int get() = particles.size

    // ── Initialisation ──────────────────────────────────────────

    /**
     * Initialise the particle manager.
     * Must be called once before any spawn/render calls.
     * If no atlas was provided (testing), rendering is a no-op.
     */
    fun init() {
        // particleRegion is loaded lazily in render() if atlas was provided
    }

    // ── Pool helpers ────────────────────────────────────────────

    private fun obtain(): Particle {
        return if (pool.isNotEmpty()) pool.removeLast().also { it.reset() }
        else Particle()
    }

    private fun free(particle: Particle) {
        pool.add(particle)
    }

    // ── Effect spawners ─────────────────────────────────────────

    /**
     * Spawn an explosion effect at [position].
     * @param size Controls particle count, speed, and lifetime.
     */
    fun spawnExplosion(position: Vector2, size: ExplosionSize = ExplosionSize.MEDIUM) {
        val count = when (size) {
            ExplosionSize.SMALL -> 15
            ExplosionSize.MEDIUM -> 30
            ExplosionSize.LARGE -> 50
        }
        val speedRange = when (size) {
            ExplosionSize.SMALL -> 1f..3f
            ExplosionSize.MEDIUM -> 2f..5f
            ExplosionSize.LARGE -> 3f..7f
        }
        val lifeRange = when (size) {
            ExplosionSize.SMALL -> 0.3f..0.6f
            ExplosionSize.MEDIUM -> 0.4f..0.8f
            ExplosionSize.LARGE -> 0.5f..1.0f
        }
        val available = (Constants.PARTICLE_MAX_COUNT - particles.size).coerceAtLeast(0)
        val actualCount = count.coerceAtMost(available)

        for (i in 0 until actualCount) {
            val p = obtain()
            val angle = Random.nextFloat() * 360f
            val speed = speedRange.start + Random.nextFloat() * (speedRange.endInclusive - speedRange.start)
            p.x = position.x
            p.y = position.y
            p.vx = cos(angle * DEG_TO_RAD) * speed
            p.vy = sin(angle * DEG_TO_RAD) * speed
            p.life = lifeRange.start + Random.nextFloat() * (lifeRange.endInclusive - lifeRange.start)
            p.maxLife = p.life
            // Orange-to-red gradient
            val t = Random.nextFloat()
            p.color.set(1f, 0.4f + t * 0.5f, 0f, 1f)
            p.size = 0.08f + Random.nextFloat() * 0.12f
            p.alpha = 1f
            particles.add(p)
        }
    }

    /**
     * Spawn a small engine-trail particle behind the ship.
     * Call each frame while thrusting.
     */
    fun spawnEngineTrail(position: Vector2) {
        if (particles.size >= Constants.PARTICLE_MAX_COUNT) return

        val p = obtain()
        p.x = position.x + Random.nextFloat() * 0.3f - 0.15f
        p.y = position.y - 0.3f + Random.nextFloat() * 0.15f
        p.vx = Random.nextFloat() * 0.3f - 0.15f
        p.vy = -0.5f - Random.nextFloat() * 0.5f
        p.life = 0.15f + Random.nextFloat() * 0.15f
        p.maxLife = p.life
        p.color.set(0.4f, 0.7f, 1f, 1f) // light blue
        p.size = 0.04f + Random.nextFloat() * 0.04f
        p.alpha = 1f
        particles.add(p)
    }

    /**
     * Spawn a green/yellow sparkle burst (astronaut rescued).
     */
    fun spawnRescueSparkle(position: Vector2) {
        val count = 15
        val available = (Constants.PARTICLE_MAX_COUNT - particles.size).coerceAtLeast(0)
        val actualCount = count.coerceAtMost(available)

        for (i in 0 until actualCount) {
            val p = obtain()
            val angle = Random.nextFloat() * 360f
            val speed = 0.5f + Random.nextFloat() * 1.5f
            p.x = position.x
            p.y = position.y
            p.vx = cos(angle * DEG_TO_RAD) * speed
            p.vy = sin(angle * DEG_TO_RAD) * speed
            p.life = 0.4f + Random.nextFloat() * 0.4f
            p.maxLife = p.life
            p.color.set(
                0.3f + Random.nextFloat() * 0.3f,
                0.8f + Random.nextFloat() * 0.2f,
                0.1f,
                1f
            ) // green/yellow
            p.size = 0.05f + Random.nextFloat() * 0.06f
            p.alpha = 1f
            particles.add(p)
        }
    }

    /**
     * Spawn a golden sparkle burst (debris collected).
     */
    fun spawnDebrisSparkle(position: Vector2) {
        val count = 12
        val available = (Constants.PARTICLE_MAX_COUNT - particles.size).coerceAtLeast(0)
        val actualCount = count.coerceAtMost(available)

        for (i in 0 until actualCount) {
            val p = obtain()
            val angle = Random.nextFloat() * 360f
            val speed = 0.5f + Random.nextFloat() * 1.0f
            p.x = position.x
            p.y = position.y
            p.vx = cos(angle * DEG_TO_RAD) * speed
            p.vy = sin(angle * DEG_TO_RAD) * speed
            p.life = 0.5f + Random.nextFloat() * 0.3f
            p.maxLife = p.life
            p.color.set(1f, 0.85f, 0.0f, 1f) // golden
            p.size = 0.06f + Random.nextFloat() * 0.08f
            p.alpha = 1f
            particles.add(p)
        }
    }

    /**
     * Spawn red/orange damage sparks at [position] (ship hit).
     */
    fun spawnDamageSparks(position: Vector2) {
        val count = 10
        val available = (Constants.PARTICLE_MAX_COUNT - particles.size).coerceAtLeast(0)
        val actualCount = count.coerceAtMost(available)

        for (i in 0 until actualCount) {
            val p = obtain()
            val angle = Random.nextFloat() * 360f
            val speed = 1.0f + Random.nextFloat() * 2.0f
            p.x = position.x
            p.y = position.y
            p.vx = cos(angle * DEG_TO_RAD) * speed
            p.vy = sin(angle * DEG_TO_RAD) * speed
            p.life = 0.2f + Random.nextFloat() * 0.2f
            p.maxLife = p.life
            p.color.set(1f, 0.2f + Random.nextFloat() * 0.3f, 0f, 1f) // red/orange
            p.size = 0.05f + Random.nextFloat() * 0.05f
            p.alpha = 1f
            particles.add(p)
        }
    }

    // ── Event dispatch ──────────────────────────────────────────

    /**
     * Dispatch a list of frame-scoped [GameEvent]s to the matching
     * effect spawners.
     */
    fun onGameEvent(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                is GameEvent.AsteroidDestroyed -> spawnExplosion(event.asteroid.position)
                is GameEvent.EnemyDestroyed -> spawnExplosion(event.enemy.position)
                is GameEvent.ShipDestroyed -> spawnExplosion(event.ship.position, ExplosionSize.LARGE)
                is GameEvent.ShipHit -> spawnDamageSparks(event.ship.position)
                is GameEvent.AstronautRescued -> spawnRescueSparkle(event.astronaut.position)
                is GameEvent.DebrisCollected -> spawnDebrisSparkle(event.debris.position)
                else -> { /* LaserFired, AstronautKilled, LaserAsteroidHit — no particle feedback */ }
            }
        }
    }

    // ── Update / Render ─────────────────────────────────────────

    /**
     * Update all active particles and recycle expired ones.
     * @param delta Frame delta in seconds.
     */
    fun update(delta: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= delta
            if (p.life <= 0f) {
                iterator.remove()
                free(p)
                continue
            }
            p.x += p.vx * delta
            p.y += p.vy * delta
            // Friction
            p.vx *= 0.97f
            p.vy *= 0.97f
            // Fade alpha
            p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        }
    }

    /**
     * Draw all active particles.
     * The [SpriteBatch] must be active (between begin/end).
     * If no atlas was provided at construction, rendering is a no-op.
     */
    fun render(batch: SpriteBatch) {
        if (particles.isEmpty()) return
        val region = particleRegion ?: return // silently skip if no atlas (testing)

        val originalColor = batch.color.cpy()
        for (p in particles) {
            val a = (p.alpha * p.life / p.maxLife).coerceIn(0f, 1f)
            batch.color = Color(p.color.r, p.color.g, p.color.b, a)
            batch.draw(
                region,
                p.x - p.size / 2f,
                p.y - p.size / 2f,
                p.size,
                p.size
            )
        }
        batch.color = originalColor
    }

    /** Release all particles back to the pool. */
    fun clear() {
        pool.addAll(particles)
        particles.clear()
    }

    // ── Particle data class ─────────────────────────────────────

    /**
     * Single pooled particle. Reset via [reset] before reuse.
     */
    data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var life: Float = 0f,
        var maxLife: Float = 1f,
        val color: Color = Color(),
        var size: Float = 0.1f,
        var alpha: Float = 1f
    ) {
        /** Zero all fields for pool reuse. */
        fun reset() {
            x = 0f
            y = 0f
            vx = 0f
            vy = 0f
            life = 0f
            maxLife = 1f
            color.set(Color.WHITE)
            size = 0.1f
            alpha = 1f
        }
    }
}
