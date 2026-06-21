package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Parallax planet layer — planets scroll continuously as part of the background.
 *
 * Unlike timer-based spawn/despawn, this maintains N fixed planet "slots"
 * that are always visible. When a planet scrolls off the left edge, it
 * wraps around to the right with a new random type, size, and Y position.
 * The scroll speed is between the star and nebula parallax layers so
 * planets feel like mid-ground background elements.
 */
class PlanetSystem {

    companion object {
        /** Number of planet slots always visible. */
        private const val SLOT_COUNT = 3

        /** Planet scroll speed multiplier (between star 0.1 and nebula 0.3). */
        private const val SCROLL_SPEED = 0.2f
    }

    // ── Texture cache ─────────────────────────────────────────
    private var gasGiantTex: Texture? = null
    private var rockyTex: Texture? = null
    private var ringedTex: Texture? = null

    // ── Active planets ────────────────────────────────────────
    private val planets = mutableListOf<Planet>()

    private val rng = Random(System.nanoTime())

    /** World dimensions. */
    private var worldWidth = Constants.WORLD_WIDTH
    private var worldHeight = Constants.WORLD_HEIGHT

    /** Pre-generate all planet textures and seed initial planet slots. */
    fun init() {
        if (gasGiantTex != null) return // already initialised

        gasGiantTex = PlanetGenerator.createTexture(PlanetGenerator.generateGasGiant())
        rockyTex = PlanetGenerator.createTexture(PlanetGenerator.generateRocky())
        ringedTex = PlanetGenerator.createTexture(PlanetGenerator.generateRinged())

        // Seed initial planets spread across the world
        val halfWorld = worldWidth / 2f
        val spacing = worldWidth / SLOT_COUNT
        for (i in 0 until SLOT_COUNT) {
            val planet = createRandomPlanet()
            planet.worldX = -halfWorld + spacing / 2f + i * spacing
            planets.add(planet)
        }
    }

    /** Scroll all planets. When one exits left, wrap to the right. */
    fun update(delta: Float, baseScrollSpeed: Float) {
        val halfWorld = worldWidth / 2f
        val scrollAmount = baseScrollSpeed * SCROLL_SPEED * delta * 60f * 0.5f

        for (i in planets.indices) {
            val planet = planets[i]
            planet.worldX -= scrollAmount

            // Wrap to the right when fully off-screen left
            if (planet.worldX + planet.radius < -halfWorld) {
                planets[i] = createRandomPlanet().also {
                    it.worldX = halfWorld + it.radius  // off-screen right
                }
            }
        }
    }

    /** Render all active planets. */
    fun render(batch: SpriteBatch) {
        val halfWorld = worldWidth / 2f

        // Sort by type depth: larger planets behind smaller ones
        val sorted = planets.sortedBy { it.radius }
        for (planet in sorted) {
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

    /** Clear and re-seed planets for game restart. */
    fun reset() {
        planets.clear()
        // Re-seed
        if (gasGiantTex != null) {
            val halfWorld = worldWidth / 2f
            val spacing = worldWidth / SLOT_COUNT
            for (i in 0 until SLOT_COUNT) {
                val planet = createRandomPlanet()
                planet.worldX = -halfWorld + spacing / 2f + i * spacing
                planets.add(planet)
            }
        }
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

    /**
     * Create a new planet with random type, size, and Y position.
     * worldX is NOT set here — caller positions it.
     */
    private fun createRandomPlanet(): Planet {
        val type = PlanetType.entries.random(rng)

        val texture = when (type) {
            PlanetType.GAS_GIANT -> gasGiantTex
            PlanetType.ROCKY -> rockyTex
            PlanetType.RINGED -> ringedTex
        } ?: error("PlanetSystem.init() must be called before creating planets")

        val radius = when (type) {
            PlanetType.GAS_GIANT -> rng.nextFloat() * 0.5f + 2.5f  // 2.5–3.0
            PlanetType.ROCKY -> rng.nextFloat() * 0.5f + 1.5f     // 1.5–2.0
            PlanetType.RINGED -> rng.nextFloat() * 0.5f + 2.0f    // 2.0–2.5
        }

        val halfHeight = worldHeight / 2f
        // Y: middle 60% of screen height
        val yMin = halfHeight * 0.2f
        val yMax = halfHeight * 1.8f
        val worldY = yMin + rng.nextFloat() * (yMax - yMin)

        return Planet(
            texture = texture,
            worldX = 0f,  // caller positions
            worldY = worldY,
            radius = radius,
            scrollSpeed = SCROLL_SPEED,
            type = type
        )
    }
}
