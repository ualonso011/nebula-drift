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
 * All text is drawn using [FontManager.body()] for consistency and
 * readability. Use [GlyphLayout] for all centering operations.
 */
object UiComponents {

    /**
     * Draw a filled rectangle button with a white border and centered label.
     *
     * @param shapeRenderer  The shared shape renderer
     * @param spriteBatch    The shared sprite batch
     * @param font           The bitmap font for the label (use [FontManager.body] typically)
     * @param bounds         World-coordinate rectangle for the button
     * @param label          The text to draw (pre-translated)
     * @param color          Fill colour of the button (default dark blue)
     */
    fun drawButton(
        shapeRenderer: ShapeRenderer,
        spriteBatch: SpriteBatch,
        font: BitmapFont,
        bounds: Rectangle,
        label: String,
        color: Color = Color(0.15f, 0.35f, 0.6f, 1f)
    ) {
        // Draw button background
        shapeRenderer.color = color
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()

        // Draw button border
        shapeRenderer.color = Color(0.5f, 0.7f, 1f, 1f)
        shapeRenderer.begin(ShapeType.Line)
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()

        // Draw text centered using GlyphLayout
        val layout = GlyphLayout(font, label)
        val textX = bounds.x + (bounds.width - layout.width) / 2f
        val textY = bounds.y + bounds.height / 2f + layout.height / 2f
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
        val labelText = "$label: ${(value * 100).toInt()}%"
        val layout = GlyphLayout(font, labelText)
        val labelX = bounds.x + (bounds.width - layout.width) / 2f
        val labelY = bounds.y + bounds.height + layout.height + 0.15f

        spriteBatch.begin()
        font.color = Color.WHITE
        font.draw(spriteBatch, labelText, labelX, labelY)
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
        val thumbY = bounds.y + bounds.height / 2f
        shapeRenderer.color = Color.WHITE
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.circle(thumbX, thumbY, bounds.height * 1.5f, 16)
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
