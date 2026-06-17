package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Lightweight sprite atlas wrapping a single packed [Texture].
 *
 * All sprites share one texture for efficient batch rendering
 * (single GPU bind per entity-render pass). The [findRegion] API
 * mirrors [com.badlogic.gdx.graphics.g2d.TextureAtlas.findRegion]
 * so the rest of the game treats it identically to a file-based atlas.
 */
class SpriteAtlas(
    val texture: Texture,
    private val regions: Map<String, TextureRegion>
) {
    /**
     * Look up a sprite region by key.
     * @throws IllegalArgumentException if the key is missing
     */
    fun findRegion(name: String): TextureRegion =
        regions[name] ?: throw IllegalArgumentException("Sprite key not found: '$name'")

    /** Release the underlying texture. */
    fun dispose() {
        texture.dispose()
    }
}
