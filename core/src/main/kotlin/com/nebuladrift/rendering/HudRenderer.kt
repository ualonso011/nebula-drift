package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.glutils.ShapeRenderer
import com.nebuladrift.entities.Ship
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.rendering.SpriteAtlas

/**
 * Renders the gameplay HUD overlay — a compact top-left card with
 * lives (hearts drawn as a sprite via SpriteBatch), score, timer,
 * and astronauts rescued.
 *
 * ## Layout (two-column: labels left, values right)
 *
 * Line 1: ♥♥♥  (hearts — sprite from atlas)
 * Line 2: SCORE     1,234
 * Line 3: TIME      02:34
 * Line 4: ★         5  (astronauts rescued)
 */
class HudRenderer(private val atlas: SpriteAtlas) {

    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val hudCamera = OrthographicCamera()

    /** Heart sprite region (generated at 48×48, drawn at heartSize). */
    private val heartRegion: TextureRegion? get() = atlas.findRegion("heart")

    private val margin = 12f
    private val cardPadding = 16f
    private val lineHeight = 32f
    private val heartSize = 25f

    /** Space-themed font for values (score number, time, etc.). */
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

        val viewportHeight = hudCamera.viewportHeight

        // Card dimensions — wider to fit Orbitron values without overflow
        val cardW = 360f
        val cardH = 180f
        val cardX = margin
        val cardY = viewportHeight - margin - cardH

        // Right edge for right-aligned values (computed per-value via GlyphLayout)
        val rightEdge = cardX + cardW - cardPadding

        // ════════════════════════════════════════════════════════
        // PHASE 1: ShapeRenderer — card background + border only
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
        // PHASE 2: SpriteBatch — hearts + text
        // ════════════════════════════════════════════════════════
        batch.begin()

        // Line 1: Hearts (sprite)
        val heartsY = cardY + cardH - cardPadding - 4f
        drawHearts(batch, cardX + cardPadding, heartsY, ship.lives)

        // Line 2: SCORE (label left, value right)
        val scoreLabelY = heartsY - lineHeight
        hudFont.color = Color(0.5f, 0.7f, 1f, 0.9f)
        hudFont.draw(batch, "SCORE", cardX + cardPadding, scoreLabelY)

        spaceFont.color = Color.WHITE
        spaceFont.data.setScale(0.85f)
        val scoreText = "%,d".format(score)
        val scoreLayout = GlyphLayout(spaceFont, scoreText)
        spaceFont.draw(batch, scoreText, rightEdge - scoreLayout.width, scoreLabelY)
        spaceFont.data.setScale(1f)

        // Line 3: TIME (label left, value right)
        val timerY = scoreLabelY - lineHeight
        hudFont.color = Color(0.8f, 0.85f, 0.95f, 1f)
        hudFont.draw(batch, i18n.get("time"), cardX + cardPadding, timerY)

        spaceFont.color = Color.WHITE
        spaceFont.data.setScale(0.85f)
        val timeLayout = GlyphLayout(spaceFont, timeString)
        spaceFont.draw(batch, timeString, rightEdge - timeLayout.width, timerY)
        spaceFont.data.setScale(1f)

        // Line 4: Astronauts rescued (label left, value right)
        if (astronautsRescued > 0) {
            val astroY = timerY - lineHeight
            hudFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
            hudFont.draw(batch, "\u2605 RESCUED", cardX + cardPadding, astroY)

            spaceFont.color = Color(0.3f, 0.9f, 0.3f, 1f)
            spaceFont.data.setScale(1f)
            val astroText = astronautsRescued.toString()
            val astroLayout = GlyphLayout(spaceFont, astroText)
            spaceFont.draw(batch, astroText, rightEdge - astroLayout.width, astroY)
            spaceFont.data.setScale(1f)
        }

        batch.end()

        // Reset colours so tint doesn't leak to next frame
        hudFont.color = Color.WHITE
        spaceFont.color = Color.WHITE
    }

    /**
     * Draw hearts as sprites — one [TextureRegion] draw per heart.
     * Software-rendered via Pixmap (no GL shape-renderer issues).
     */
    private fun drawHearts(batch: SpriteBatch, x: Float, y: Float, lives: Int) {
        val region = heartRegion ?: return
        val gap = heartSize * 2.2f

        for (i in 0 until 3) {
            val hx = x + i * gap
            val hy = y - heartSize

            val c = if (i < lives.coerceIn(0, 3))
                Color(1f, 0.2f, 0.3f, 0.9f)
            else
                Color(0.3f, 0.3f, 0.3f, 0.4f)

            batch.setColor(c)
            batch.draw(region, hx, hy, heartSize, heartSize)
        }
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
