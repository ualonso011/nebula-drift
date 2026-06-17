package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.nebuladrift.util.Constants
import kotlin.random.Random

/**
 * Two-layer parallax scrolling background generated entirely at
 * runtime via [Pixmap] — no external image assets required.
 *
 * ## Layers
 * 1. **Far stars** — dark sky with scattered white dots (slow).
 * 2. **Near nebula** — colourful nebula-like blobs (medium).
 *
 * Each layer texture is 512×288 pixels (16:9, 32× world scale)
 * and scrolls seamlessly by drawing two copies side by side.
 *
 * Usage in [GameRenderer]:
 * ```
 * background.update(delta, scrollSpeed)
 * background.render(batch)
 * ```
 */
class ParallaxBackground {

    private val layers = mutableListOf<ParallaxLayer>()

    /** Initialise procedural textures. Call once after construction. */
    fun init() {
        val width = Constants.PARALLAX_BG_WIDTH
        val height = Constants.PARALLAX_BG_HEIGHT

        val farTex = createStarFieldTexture(width, height)
        layers.add(ParallaxLayer(TextureRegion(farTex), Constants.PARALLAX_FAR_SPEED))

        val nearTex = createNebulaTexture(width, height)
        layers.add(ParallaxLayer(TextureRegion(nearTex), Constants.PARALLAX_NEAR_SPEED))
    }

    /**
     * Update scroll offsets for all layers.
     * @param delta Frame delta in seconds.
     * @param scrollSpeed Base scroll speed in world-units/s.
     */
    fun update(delta: Float, scrollSpeed: Float) {
        for (layer in layers) layer.update(delta, scrollSpeed)
    }

    /**
     * Render all parallax layers.
     * The [SpriteBatch] must be active (between begin/end).
     */
    fun render(batch: SpriteBatch) {
        for (layer in layers) layer.render(batch)
    }

    /** Release GPU resources. */
    fun dispose() {
        for (layer in layers) layer.texture.texture.dispose()
        layers.clear()
    }

    // ── Procedural texture generators ─────────────────────────────

    private fun createStarFieldTexture(width: Int, height: Int): Texture {
        val pix = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pix.setColor(0.02f, 0.02f, 0.06f, 1f) // very dark blue-black
        pix.fill()

        val rng = Random(42) // fixed seed for reproducible star field
        repeat(Constants.PARALLAX_STAR_COUNT) {
            val sx = rng.nextInt(width)
            val sy = rng.nextInt(height)
            val brightness = 0.4f + rng.nextFloat() * 0.6f
            val starAlpha = 0.7f + rng.nextFloat() * 0.3f
            pix.setColor(brightness, brightness, brightness, starAlpha)
            if (rng.nextFloat() < 0.15f) {
                pix.fillCircle(sx, sy, 1) // brighter star
            } else {
                pix.drawPixel(sx, sy)
            }
        }

        return Texture(pix).also { pix.dispose() }
    }

    private fun createNebulaTexture(width: Int, height: Int): Texture {
        val pix = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pix.setColor(0f, 0f, 0f, 0f)
        pix.fill()

        val rng = Random(137)
        val blobCount = 8 + rng.nextInt(5)

        repeat(blobCount) {
            val cx = rng.nextInt(width)
            val cy = rng.nextInt(height)
            val radius = (20 + rng.nextInt(60)).toFloat()

            val hue = rng.nextFloat()
            val r = 0.1f + hue * 0.4f
            val g = 0.05f + (1f - hue) * 0.3f
            val b = 0.2f + hue * 0.5f
            val maxAlpha = 0.08f + rng.nextFloat() * 0.12f

            // Concentric circles for soft glow
            for (step in 6 downTo 1) {
                val dist = radius * step / 6
                val alpha = maxAlpha * (1f - step.toFloat() / 7f)
                pix.setColor(r, g, b, alpha)
                pix.fillCircle(cx.toInt(), cy.toInt(), dist.toInt())
            }
        }

        // A few brighter spots
        pix.setColor(0.3f, 0.2f, 0.5f, 0.06f)
        repeat(3) {
            pix.fillCircle(rng.nextInt(width), rng.nextInt(height), 40 + rng.nextInt(30))
        }

        return Texture(pix).also { pix.dispose() }
    }

    // ── Internal layer class ──────────────────────────────────────

    /**
     * A single parallax scrolling layer.
     *
     * @param texture The texture region for this layer.
     * @param speedMultiplier Fraction of the base scroll speed.
     */
    class ParallaxLayer(
        val texture: TextureRegion,
        private val speedMultiplier: Float
    ) {
        /** Current horizontal scroll offset in world units. */
        var offsetX: Float = 0f
            private set

        fun update(delta: Float, scrollSpeed: Float) {
            offsetX -= scrollSpeed * speedMultiplier * delta
            // Wrap seamlessly
            if (offsetX <= -Constants.WORLD_WIDTH) {
                offsetX += Constants.WORLD_WIDTH
            }
        }

        /**
         * Render this layer — draws the texture twice for seamless
         * horizontal tiling.
         */
        fun render(batch: SpriteBatch) {
            batch.draw(texture, offsetX, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
            batch.draw(
                texture,
                offsetX + Constants.WORLD_WIDTH, 0f,
                Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT
            )
        }
    }
}
