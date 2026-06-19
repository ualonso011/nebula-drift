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
 * Renders the gameplay HUD overlay (lives, score, timer).
 *
 * Uses a separate [OrthographicCamera] in screen-pixel space so that
 * text is crisp regardless of the game-world viewport.
 *
 * ## Crash-safety design
 *
 * Previous versions used [ShapeRenderer.polygon] (ear-clipping) and
 * [ShapeRenderer.triangle] which crash on several Android GL backends.
 * This version:
 * - Uses **only** [ShapeRenderer.rect] for the score-card background
 *   (proven safe across all menu screens in this project)
 * - Renders hearts as Unicode text (♥) via [BitmapFont] — zero GL risk
 * - Removes manual `glEnable`/`glDisable` calls — ShapeRenderer handles
 *   blending internally
 * - Wraps the entire render in try-catch so a HUD failure never kills
 *   the game
 *
 * ## Rendering phases
 *
 * [ShapeRenderer] and [SpriteBatch] must NEVER be active simultaneously.
 * All shape passes run first, then all batch passes.
 */
class HudRenderer {

    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val hudCamera = OrthographicCamera()
    private val layout = GlyphLayout()

    private val margin = 14f
    private val cardPadding = 16f
    private val lineHeight = 36f
    private val heartSpacing = 34f

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
     *
     * Wrapped in try-catch: if any HUD rendering fails the game
     * continues without the HUD rather than crashing.
     */
    fun render(ship: Ship, score: Int, timeString: String, astronautsRescued: Int = 0, i18n: I18nManager) {
        try {
            doRender(ship, score, timeString, astronautsRescued, i18n)
        } catch (e: Exception) {
            Gdx.app.error("HudRenderer", "HUD render failed — game continues without HUD", e)
        }
    }

    /**
     * Internal render — separated so [render] can catch exceptions.
     */
    private fun doRender(ship: Ship, score: Int, timeString: String, astronautsRescued: Int, i18n: I18nManager) {
        hudCamera.update()
        shapeRenderer.projectionMatrix = hudCamera.combined
        batch.projectionMatrix = hudCamera.combined

        val topY = hudCamera.viewportHeight - margin
        val viewportWidth = hudCamera.viewportWidth

        // ════════════════════════════════════════════════════════
        // PHASE 1: ShapeRenderer — no SpriteBatch active
        // ════════════════════════════════════════════════════════
        drawScoreCardBackground(viewportWidth, topY)

        // ════════════════════════════════════════════════════════
        // PHASE 2: SpriteBatch — no ShapeRenderer active
        // ════════════════════════════════════════════════════════
        batch.begin()

        drawScoreCardText(viewportWidth, topY, score)
        drawHearts(margin, topY - 8f, ship.lives)
        drawTimer(margin, topY, timeString, i18n)
        drawAstronautsRescued(margin, topY, astronautsRescued, i18n)

        batch.end()

        // Reset colours so tint doesn't leak to next frame
        hudFont.color = Color.WHITE
        spaceFont.color = Color.WHITE
    }

    // ── Shape phase ───────────────────────────────────────────

    /**
     * Draw the score card as a simple filled rect.
     *
     * Uses [ShapeRenderer.rect] only — proven safe across all menu
     * screens in this project.  No polygon, no triangle, no manual
     * GL blend calls.
     */
    private fun drawScoreCardBackground(viewportWidth: Float, topY: Float) {
        val cardW = viewportWidth * 0.55f
        val cardH = 130f
        val cardX = (viewportWidth - cardW) / 2f
        val cardY = topY - cardH - 4f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Dark translucent background
        shapeRenderer.setColor(0.05f, 0.08f, 0.18f, 0.75f)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)

        // Subtle highlight bar at top
        shapeRenderer.setColor(0.2f, 0.5f, 1f, 0.15f)
        shapeRenderer.rect(cardX + 2f, cardY + cardH - 6f, cardW - 4f, 4f)

        shapeRenderer.end()

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(0.3f, 0.6f, 1f, 0.5f)
        shapeRenderer.rect(cardX, cardY, cardW, cardH)
        shapeRenderer.end()
    }

    // ── Batch phase ───────────────────────────────────────────

    /**
     * Draw the score card text (label + number).
     * Must be called between [batch].begin/end.
     */
    private fun drawScoreCardText(viewportWidth: Float, topY: Float, score: Int) {
        val cardW = viewportWidth * 0.55f
        val cardH = 130f
        val cardX = (viewportWidth - cardW) / 2f
        val cardY = topY - cardH - 4f

        // "SCORE" label
        hudFont.color = Color(0.5f, 0.7f, 1f, 0.9f)
        val scoreLabel = "SCORE"
        layout.setText(hudFont, scoreLabel)
        hudFont.draw(batch, scoreLabel, cardX + cardPadding, cardY + cardH - cardPadding)

        // Score number (large space font)
        spaceFont.color = Color.WHITE
        spaceFont.data.setScale(2.2f)
        val scoreText = "%,d".format(score)
        layout.setText(spaceFont, scoreText)
        val numW = layout.width
        spaceFont.draw(batch, scoreText, cardX + (cardW - numW) / 2f, cardY + cardH - cardPadding - lineHeight - 8f)
        spaceFont.data.setScale(1f)
    }

    /**
     * Draw lives as Unicode hearts (♥).
     *
     * Filled hearts in red for remaining lives, greyed-out for
     * empty slots.  Uses [BitmapFont] — zero GL risk.
     * Must be called between [batch].begin/end.
     */
    private fun drawHearts(x: Float, y: Float, lives: Int) {
        val heartChar = "\u2665" // ♥

        // Filled hearts (red) for current lives
        hudFont.color = Color(1f, 0.2f, 0.3f, 0.9f)
        for (i in 0 until lives.coerceIn(0, 3)) {
            hudFont.draw(batch, heartChar, x + i * heartSpacing, y)
        }

        // Empty hearts (grey) for remaining slots
        hudFont.color = Color(0.3f, 0.3f, 0.3f, 0.4f)
        for (i in lives.coerceIn(0, 3) until 3) {
            hudFont.draw(batch, heartChar, x + i * heartSpacing, y)
        }

        hudFont.color = Color.WHITE
    }

    /**
     * Draw the timer label + formatted time.
     * Must be called between [batch].begin/end.
     */
    private fun drawTimer(x: Float, topY: Float, timeString: String, i18n: I18nManager) {
        val timerY = topY - lineHeight - 4f

        hudFont.color = Color(0.8f, 0.85f, 0.95f, 1f)
        val timerLabel = "${i18n.get("time")}: "
        layout.setText(hudFont, timerLabel)
        val timerLabelW = layout.width
        hudFont.draw(batch, timerLabel, x, timerY)

        spaceFont.color = Color.WHITE
        spaceFont.data.setScale(0.7f)
        spaceFont.draw(batch, timeString, x + timerLabelW, timerY)
        spaceFont.data.setScale(1f)
    }

    /**
     * Draw the astronauts-rescued counter (if any).
     * Must be called between [batch].begin/end.
     */
    private fun drawAstronautsRescued(x: Float, topY: Float, astronautsRescued: Int, i18n: I18nManager) {
        if (astronautsRescued <= 0) return

        val rescueY = topY - lineHeight * 2 - 4f

        hudFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
        val rescueLabel = "${i18n.get("astronauts_rescued")}: "
        layout.setText(hudFont, rescueLabel)
        val rescueLabelW = layout.width
        hudFont.draw(batch, rescueLabel, x, rescueY)

        spaceFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
        spaceFont.data.setScale(0.7f)
        spaceFont.draw(batch, astronautsRescued.toString(), x + rescueLabelW, rescueY)
        spaceFont.data.setScale(1f)
    }

    fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
