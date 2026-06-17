package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Rectangle

/**
 * Reusable UI drawing helpers for all menu/settings/leaderboard screens.
 *
 * All methods use world-coordinate bounds (via [FitViewport]) so that
 * buttons and sliders render consistently across resolutions.
 *
 * Usage:
 * ```kotlin
 * UiComponents.drawButton(shapeRenderer, spriteBatch, font, bounds, "Play")
 * ```
 */
object UiComponents {

    /**
     * Draw a filled rectangle button with a white border and centered label.
     *
     * @param shapeRenderer  The shared shape renderer
     * @param spriteBatch    The shared sprite batch
     * @param font           The bitmap font for the label
     * @param bounds         World-coordinate rectangle for the button
     * @param label          The text to draw (pre-translated)
     * @param color          Fill colour of the button (default white)
     */
    fun drawButton(
        shapeRenderer: ShapeRenderer,
        spriteBatch: SpriteBatch,
        font: BitmapFont,
        bounds: Rectangle,
        label: String,
        color: Color = Color.WHITE
    ) {
        // Draw button background
        shapeRenderer.color = color
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()

        // Draw button border
        shapeRenderer.color = Color.WHITE
        shapeRenderer.begin(ShapeType.Line)
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()

        // Draw text centered
        val layout = GlyphLayout(font, label)
        val textWidth = layout.width
        val textX = bounds.x + (bounds.width - textWidth) / 2
        val textY = bounds.y + bounds.height / 2 + font.capHeight / 2
        spriteBatch.begin()
        font.color = Color.WHITE
        font.draw(spriteBatch, label, textX, textY)
        spriteBatch.end()
    }

    /**
     * Draw a horizontal slider with label, filled portion, and thumb circle.
     *
     * @param shapeRenderer  The shared shape renderer
     * @param spriteBatch    The shared sprite batch
     * @param font           The bitmap font for the label
     * @param bounds         World-coordinate rectangle for the slider bar
     * @param value          Current slider value (0.0 .. 1.0)
     * @param label          Localized label displayed above the bar
     */
    fun drawSlider(
        shapeRenderer: ShapeRenderer,
        spriteBatch: SpriteBatch,
        font: BitmapFont,
        bounds: Rectangle,
        value: Float,
        label: String
    ) {
        // Draw label above slider
        spriteBatch.begin()
        font.color = Color.WHITE
        font.draw(spriteBatch, "$label: ${(value * 100).toInt()}%", bounds.x, bounds.y + bounds.height + 25f)
        spriteBatch.end()

        // Draw bar background
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()

        // Draw filled portion
        shapeRenderer.color = Color.LIGHT_GRAY
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width * value, bounds.height)
        shapeRenderer.end()

        // Draw thumb
        val thumbX = bounds.x + value * bounds.width
        val thumbY = bounds.y + bounds.height / 2
        shapeRenderer.color = Color.WHITE
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.circle(thumbX, thumbY, 12f, 16)
        shapeRenderer.end()
    }

    /**
     * Check whether a world-coordinate point falls within the given bounds.
     * Useful for touch hit-testing.
     */
    fun isPointInBounds(x: Float, y: Float, bounds: Rectangle): Boolean {
        return x >= bounds.x && x <= bounds.x + bounds.width &&
                y >= bounds.y && y <= bounds.y + bounds.height
    }
}
