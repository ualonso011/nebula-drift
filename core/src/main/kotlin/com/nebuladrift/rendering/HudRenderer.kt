package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
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
 * **Critical invariant**: [ShapeRenderer] and [SpriteBatch] must NEVER be
 * active at the same time.  All shape passes run first, then all batch
 * passes.  Violating this crashes on Android GL backends.
 *
 * Design: Score lives inside a semi-transparent card with a subtle
 * border. Lives are drawn as filled hearts. All HUD text uses the
 * Orbitron space font for a sci-fi feel.
 */
class HudRenderer {

    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val hudCamera = OrthographicCamera()
    private val layout = GlyphLayout()

    private val margin = 14f
    private val cardPadding = 16f
    private val lineHeight = 36f
    private val heartSize = 28f
    private val heartSpacing = 34f

    /** Space-themed font for score. */
    private val spaceScoreFont: BitmapFont get() = FontManager.space()

    /** Space-themed font for HUD labels. */
    private val spaceHudFont: BitmapFont get() = FontManager.space()

    /** Fallback Roboto font for labels. */
    private val hudFont: BitmapFont get() = FontManager.hud()

    /** Call on screen resize to update HUD camera dimensions. */
    fun resize(width: Int, height: Int) {
        hudCamera.setToOrtho(false, width.toFloat(), height.toFloat())
    }

    /**
     * Draw the HUD for the current frame.
     *
     * Rendering is split into two strict phases to avoid GL state conflicts:
     * 1. **Shape phase** — card background, border, hearts (ShapeRenderer)
     * 2. **Batch phase** — all text (SpriteBatch)
     *
     * @param ship  The player ship (used for lives count)
     * @param score The current score
     * @param timeString  Formatted time string (e.g. "1:05")
     * @param astronautsRescued  Number of astronauts rescued
     * @param i18n  The i18n manager for translated labels
     */
    fun render(ship: Ship, score: Int, timeString: String, astronautsRescued: Int = 0, i18n: I18nManager) {
        hudCamera.update()
        shapeRenderer.projectionMatrix = hudCamera.combined
        batch.projectionMatrix = hudCamera.combined

        val topY = hudCamera.viewportHeight - margin
        val viewportWidth = hudCamera.viewportWidth

        // ════════════════════════════════════════════════════════
        // PHASE 1: ShapeRenderer — no SpriteBatch active
        // ════════════════════════════════════════════════════════
        drawScoreCardShapes(viewportWidth, topY)
        drawHearts(margin, topY - 8f, ship.lives)

        // ════════════════════════════════════════════════════════
        // PHASE 2: SpriteBatch — no ShapeRenderer active
        // ════════════════════════════════════════════════════════
        batch.begin()

        drawScoreCardText(viewportWidth, topY, score)

        // ── Timer ─────────────────────────────────────────────
        hudFont.color = Color(0.8f, 0.85f, 0.95f, 1f)
        val timerLabel = "${i18n.get("time")}: "
        layout.setText(hudFont, timerLabel)
        val timerLabelW = layout.width
        hudFont.draw(batch, timerLabel, margin, topY - heartSize - 16f)

        spaceHudFont.color = Color.WHITE
        spaceHudFont.data.setScale(0.7f)
        spaceHudFont.draw(batch, timeString, margin + timerLabelW, topY - heartSize - 16f)
        spaceHudFont.data.setScale(1f)

        // ── Astronauts rescued ────────────────────────────────
        if (astronautsRescued > 0) {
            hudFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
            val rescueLabel = "${i18n.get("astronauts_rescued")}: "
            layout.setText(hudFont, rescueLabel)
            val rescueLabelW = layout.width
            hudFont.draw(batch, rescueLabel, margin, topY - heartSize - 16f - lineHeight)

            spaceHudFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
            spaceHudFont.data.setScale(0.7f)
            spaceHudFont.draw(batch, astronautsRescued.toString(), margin + rescueLabelW, topY - heartSize - 16f - lineHeight)
            spaceHudFont.data.setScale(1f)
        }

        batch.end()

        hudFont.color = Color.WHITE
        spaceHudFont.color = Color.WHITE
    }

    // ── Shape phase ───────────────────────────────────────────

    /**
     * Draw the score card background and border (ShapeRenderer only).
     */
    private fun drawScoreCardShapes(viewportWidth: Float, topY: Float) {
        val cardW = viewportWidth * 0.55f
        val cardH = 130f
        val cardX = (viewportWidth - cardW) / 2f
        val cardY = topY - cardH - 4f
        val cornerRadius = 12f

        // ── Background fill ──
        Gdx.gl.glEnable(GL20.GL_BLEND)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.05f, 0.08f, 0.18f, 0.75f)
        drawRoundedRect(shapeRenderer, cardX, cardY, cardW, cardH, cornerRadius)

        // Subtle inner glow (top edge highlight)
        shapeRenderer.setColor(0.2f, 0.5f, 1f, 0.15f)
        drawRoundedRect(shapeRenderer, cardX + 2f, cardY + cardH - 6f, cardW - 4f, 4f, 2f)
        shapeRenderer.end()

        // ── Border ──
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(0.3f, 0.6f, 1f, 0.5f)
        drawRoundedRect(shapeRenderer, cardX, cardY, cardW, cardH, cornerRadius)
        shapeRenderer.end()

        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    /**
     * Draw filled hearts for lives (ShapeRenderer only).
     */
    private fun drawHearts(x: Float, y: Float, lives: Int) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (i in 0 until lives.coerceAtLeast(0)) {
            val hx = x + i * heartSpacing
            drawHeart(shapeRenderer, hx, y - heartSize, heartSize)
        }

        // Draw empty hearts for max lives (3)
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.4f)
        for (i in lives.coerceAtLeast(0) until 3) {
            val hx = x + i * heartSpacing
            drawHeart(shapeRenderer, hx, y - heartSize, heartSize)
        }

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    /**
     * Draw a single filled heart shape.
     */
    private fun drawHeart(sr: ShapeRenderer, x: Float, y: Float, size: Float) {
        sr.setColor(1f, 0.2f, 0.3f, 0.9f)
        val s = size / 2f
        // Two top circles
        sr.circle(x + s * 0.35f, y + s * 0.65f, s * 0.42f)
        sr.circle(x + s * 1.15f, y + s * 0.65f, s * 0.42f)
        // Bottom triangle
        sr.triangle(
            x, y + s * 0.55f,
            x + s * 0.75f, y,
            x + s * 1.5f, y + s * 0.55f
        )
    }

    // ── Batch phase ───────────────────────────────────────────

    /**
     * Draw the score text inside the card (SpriteBatch only).
     * Call ONLY between [batch].begin/end.
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
        spaceScoreFont.color = Color.WHITE
        spaceScoreFont.data.setScale(2.2f)
        val scoreText = "%,d".format(score)
        layout.setText(spaceScoreFont, scoreText)
        val numW = layout.width
        spaceScoreFont.draw(batch, scoreText, cardX + (cardW - numW) / 2f, cardY + cardH - cardPadding - lineHeight - 8f)
        spaceScoreFont.data.setScale(1f)
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Draw a rounded rectangle using ShapeRenderer.polygon().
     * Approximates corners with line segments.
     */
    private fun drawRoundedRect(sr: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, r: Float) {
        val segments = 8
        val verts = mutableListOf<Float>()

        // Bottom-left corner
        for (i in 0..segments) {
            val angle = Math.PI + (Math.PI / 2) * i / segments
            verts.add(x + r + (r * Math.cos(angle)).toFloat())
            verts.add(y + r + (r * Math.sin(angle)).toFloat())
        }
        // Bottom-right corner
        for (i in 0..segments) {
            val angle = -Math.PI / 2 + (Math.PI / 2) * i / segments
            verts.add(x + w - r + (r * Math.cos(angle)).toFloat())
            verts.add(y + r + (r * Math.sin(angle)).toFloat())
        }
        // Top-right corner
        for (i in 0..segments) {
            val angle = 0 + (Math.PI / 2) * i / segments
            verts.add(x + w - r + (r * Math.cos(angle)).toFloat())
            verts.add(y + h - r + (r * Math.sin(angle)).toFloat())
        }
        // Top-left corner
        for (i in 0..segments) {
            val angle = Math.PI / 2 + (Math.PI / 2) * i / segments
            verts.add(x + r + (r * Math.cos(angle)).toFloat())
            verts.add(y + h - r + (r * Math.sin(angle)).toFloat())
        }
        sr.polygon(verts.toFloatArray())
    }

    fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
