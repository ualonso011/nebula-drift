package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Texture

/**
 * A decorative planet drifting across the background.
 *
 * Purely visual — no collision, no gameplay interaction.
 */
data class Planet(
    /** Procedurally generated texture for this planet's type. */
    val texture: Texture,
    /** World X position (right edge spawn → drifts left). */
    var worldX: Float,
    /** World Y position (random, middle 60% of screen). */
    val worldY: Float,
    /** Visual radius in world units. */
    val radius: Float,
    /** Scroll speed multiplier (0.06–0.15 of base scroll). */
    val scrollSpeed: Float,
    /** Which type of planet this is. */
    val type: PlanetType
)

enum class PlanetType {
    GAS_GIANT,
    ROCKY,
    RINGED
}
