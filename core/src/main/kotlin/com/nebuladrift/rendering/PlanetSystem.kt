package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Manages decorative planet lifecycle: spawn, scroll, despawn, and render.
 *
 * Planets are purely visual — no collision, no gameplay interaction.
 * They drift from right to left at a slow parallax rate between the star
 * and nebula background layers.
 */
class PlanetSystem {

    // ── Texture cache ─────────────────────────────────────────
    private var gasGiantTex: Texture? = null
    private var rockyTex: Texture? = null
    private var ringedTex: Texture? = null

    // ── Active planets ────────────────────────────────────────
    private val planets = mutableListOf<Planet>()

    // ── Spawn timer ───────────────────────────────────────────
    /** Timer counting up; when >= interval, spawn a planet. */
    private var spawnTimer = 0f

    /** Random generator (declared BEFORE nextInterval to avoid NPE in jitter()). */
    private val rng = Random(System.nanoTime())

    /** Next spawn interval with jitter applied. */
    private var nextInterval = Constants.PLANET_SPAWN_INTERVAL + jitter()

    /** Track last type spawned to avoid repeats. */
    private var lastType: PlanetType? = null

    /** World width used for spawn/despawn calculations. */
    private var worldWidth = Constants.WORLD_WIDTH
    /** World height used for spawn Y calculation. */
    private var worldHeight = Constants.WORLD_HEIGHT

    /** Pre-generate all planet textures. Idempotent — safe to call multiple times. */
    fun init() {
        if (gasGiantTex != null) return // already initialised
        gasGiantTex = PlanetGenerator.createTexture(PlanetGenerator.generateGasGiant())
        rockyTex = PlanetGenerator.createTexture(PlanetGenerator.generateRocky())
        ringedTex = PlanetGenerator.createTexture(PlanetGenerator.generateRinged())
    }

    /** Update planet positions, timer, and spawn logic. */
    fun update(delta: Float, baseScrollSpeed: Float) {
        // ── Spawn timer ──
        spawnTimer += delta
        if (spawnTimer >= nextInterval && planets.size < Constants.PLANET_MAX_COUNT) {
            spawnTimer = 0f
            nextInterval = Constants.PLANET_SPAWN_INTERVAL + jitter()
            spawnPlanet()
        }

        // ── Scroll planets left ──
        val halfWorld = worldWidth / 2f
        val iterator = planets.iterator()
        while (iterator.hasNext()) {
            val planet = iterator.next()
            planet.worldX -= baseScrollSpeed * planet.scrollSpeed * delta * 60f * 0.5f

            // Despawn when fully off-screen left
            if (planet.worldX + planet.radius < -halfWorld) {
                iterator.remove()
            }
        }
    }

    /** Render all active planets. */
    fun render(batch: SpriteBatch) {
        for (planet in planets) {
            val r = planet.radius
            batch.draw(
                planet.texture,
                planet.worldX - r,
                planet.worldY - r,
                r * 2f,
                r * 2f
            )
        }
    }

    /** Clear all planets for game restart. */
    fun reset() {
        planets.clear()
        spawnTimer = 0f
        nextInterval = Constants.PLANET_SPAWN_INTERVAL + jitter()
        lastType = null
    }

    /** Dispose all cached textures. */
    fun dispose() {
        gasGiantTex?.dispose()
        rockyTex?.dispose()
        ringedTex?.dispose()
        gasGiantTex = null
        rockyTex = null
        ringedTex = null
        planets.clear()
    }

    // ── Internal ──────────────────────────────────────────────

    private fun spawnPlanet() {
        // Pick type (no repeat of last)
        val available = PlanetType.entries.filter { it != lastType }
        val type = available.random(rng)
        lastType = type

        val texture = when (type) {
            PlanetType.GAS_GIANT -> gasGiantTex
            PlanetType.ROCKY -> rockyTex
            PlanetType.RINGED -> ringedTex
        } ?: return

        val radius = when (type) {
            PlanetType.GAS_GIANT -> rng.nextFloat() * 0.5f + 2.5f  // 2.5–3.0
            PlanetType.ROCKY -> rng.nextFloat() * 0.5f + 1.5f     // 1.5–2.0
            PlanetType.RINGED -> rng.nextFloat() * 0.5f + 2.0f    // 2.0–2.5
        }

        val halfWorld = worldWidth / 2f
        val halfHeight = worldHeight / 2f

        // Y: middle 60% of screen height
        val yMin = halfHeight * 0.2f
        val yMax = halfHeight * 1.8f
        val worldY = yMin + rng.nextFloat() * (yMax - yMin)

        // Spawn off-screen right
        val worldX = halfWorld + radius

        val scrollSpeed = rng.nextFloat() * (Constants.PLANET_SCROLL_MAX - Constants.PLANET_SCROLL_MIN) + Constants.PLANET_SCROLL_MIN

        planets.add(Planet(
            texture = texture,
            worldX = worldX,
            worldY = worldY,
            radius = radius,
            scrollSpeed = scrollSpeed,
            type = type
        ))
    }

    private fun jitter(): Float = rng.nextFloat() * 10f - 5f  // ±5s
}
