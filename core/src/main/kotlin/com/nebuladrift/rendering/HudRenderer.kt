package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.nebuladrift.entities.Ship
import com.nebuladrift.managers.I18nManager

/**
 * Renders the gameplay HUD overlay — a compact top-left card with
 * lives (hearts), score, timer, and astronauts rescued.
 *
 * ## Crash-safety design
 *
 * Uses **only** [ShapeRenderer.rect] for the card background.
 * Hearts rendered as Unicode text (♥) via [BitmapFont]. Wrapped in try-catch.
 *
 * ## Layout (top to bottom inside card)
 *
 * Line 1: ♥♥♥ (hearts)
 * Line 2: SCORE 1,234
 * Line 3: TIME 02:34
 * Line 4: ★ 5 (astronauts rescued)
 */
class HudRenderer {

    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val hudCamera = OrthographicCamera()
    private val layout = GlyphLayout()

    private val margin = 12f
    private val cardPadding = 14f
    private val lineHeight = 30f
    private val heartSpacing = 32f

    /** Space-themed font for score number and HUD values. */
    private val spaceFont: BitmapFont get() = FontManager.space()

    /** Roboto font for labels. */
    private val hudFont: BitmapFont get() = FontManager.hud()

    /** Call on screen resize to update HUD camera dimensions. */
    fun resize(width: Int, height: Int) {
        hudCamera.setToOrtho(false, width.toFloat(), height.toFloat())
    }

    /**
     * Draw the HUD for the current frame.
     * Wrapped in try-catch: HUD failure never kills the game.
     */
    fun render(ship: Ship, score: Int, timeString: String, astronautsRescued: Int = 0, i18n: I18nManager) {
        try {
            doRender(ship, score, timeString, astronautsRescued, i18n)
        } catch (e: Exception) {
            Gdx.app.error("HudRenderer", "HUD render failed — game continues without HUD", e)
        }
    }

    private fun doRender(ship: Ship, score: Int, timeString: String, astronautsRescued: Int, i18n: I18nManager) {
        hudCamera.update()
        shapeRenderer.projectionMatrix = hudCamera.combined
        batch.projectionMatrix = hudCamera.combined

        val viewportWidth = hudCamera.viewportWidth
        val viewportHeight = hudCamera.viewportHeight

        // Card dimensions — top-left, tall enough for 4 lines
        val cardW = 240f
        val cardH = 140f
        val cardX = margin
        val cardY = viewportHeight - margin - cardH

        // ════════════════════════════════════════════════════════
        // PHASE 1: ShapeRenderer — no SpriteBatch active
        // ════════════════════════════════════════════════════════
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Dark translucent background
        shapeRenderer.setColor(0.05f, 0.08f, 0.18f, 0.75f)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)

        // Subtle highlight bar at top
        shapeRenderer.setColor(0.2f, 0.5f, 1f, 0.15f)
        shapeRenderer.rect(cardX + 2f, cardY + cardH - 4f, cardW - 4f, 3f)

        shapeRenderer.end()

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(0.3f, 0.6f, 1f, 0.5f)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)
        shapeRenderer.end()

        // ════════════════════════════════════════════════════════
        // PHASE 2: SpriteBatch — no ShapeRenderer active
        // ════════════════════════════════════════════════════════
        batch.begin()

        // Line 1: Hearts (top of card)
        val heartsY = cardY + cardH - cardPadding - 2f
        drawHearts(cardX + cardPadding, heartsY, ship.lives)

        // Line 2: Score — "SCORE" label + number on same line
        val scoreLabelY = heartsY - lineHeight
        hudFont.color = Color(0.5f, 0.7f, 1f, 0.9f)
        hudFont.draw(batch, "SCORE ", cardX + cardPadding, scoreLabelY)

        spaceFont.color = Color.WHITE
        spaceFont.data.setScale(1.4f)
        val scoreText = "%,d".format(score)
        layout.setText(spaceFont, scoreText)
        hudFont.data.setScale(1f) // reset before measuring label
        layout.setText(hudFont, "SCORE ")
        val labelW = layout.width
        spaceFont.draw(batch, scoreText, cardX + cardPadding + labelW, scoreLabelY)
        spaceFont.data.setScale(1f)

        // Line 3: Timer — "TIME" label + value on same line
        val timerY = scoreLabelY - lineHeight
        hudFont.color = Color(0.8f, 0.85f, 0.95f, 1f)
        val timerLabel = "${i18n.get("time")}: "
        hudFont.draw(batch, timerLabel, cardX + cardPadding, timerY)

        layout.setText(hudFont, timerLabel)
        spaceFont.color = Color.WHITE
        spaceFont.data.setScale(0.85f)
        spaceFont.draw(batch, timeString, cardX + cardPadding + layout.width, timerY)
        spaceFont.data.setScale(1f)

        // Line 4: Astronauts rescued
        if (astronautsRescued > 0) {
            val astroY = timerY - lineHeight
            hudFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
            hudFont.draw(batch, "\u2605 ", cardX + cardPadding, astroY) // ★

            layout.setText(hudFont, "\u2605 ")
            val starW = layout.width
            spaceFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
            spaceFont.data.setScale(1f)
            spaceFont.draw(batch, astronautsRescued.toString(), cardX + cardPadding + starW, astroY)
        }

        batch.end()

        // Reset colours so tint doesn't leak to next frame
        hudFont.color = Color.WHITE
        spaceFont.color = Color.WHITE
    }

    /**
     * Draw lives as Unicode hearts (♥).
     * Filled red for remaining lives, grey for empty slots.
     */
    private fun drawHearts(x: Float, y: Float, lives: Int) {
        val heartChar = "\u2665" // ♥

        // Filled hearts (red)
        hudFont.color = Color(1f, 0.2f, 0.3f, 0.9f)
        for (i in 0 until lives.coerceIn(0, 3)) {
            hudFont.draw(batch, heartChar, x + i * heartSpacing, y)
        }

        // Empty hearts (grey)
        hudFont.color = Color(0.3f, 0.3f, 0.3f, 0.4f)
        for (i in lives.coerceIn(0, 3) until 3) {
            hudFont.draw(batch, heartChar, x + i * heartSpacing, y)
        }

        hudFont.color = Color.WHITE
    }

    fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
