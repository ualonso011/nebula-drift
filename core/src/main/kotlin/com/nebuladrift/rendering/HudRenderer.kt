package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.nebuladrift.entities.Ship
import com.nebuladrift.managers.I18nManager

/**
 * Renders the gameplay HUD overlay (lives, score, timer).
 *
 * Uses a separate [OrthographicCamera] in screen-pixel space so that
 * text is crisp regardless of the game-world viewport. Uses [FontManager.hud]
 * for smooth, legible text.
 */
class HudRenderer {

    private val batch = SpriteBatch()
    private val hudCamera = OrthographicCamera()
    private val margin = 14f
    private val lineHeight = 32f

    /** Font reference — uses FontManager.hud() for smooth text. */
    private val font get() = FontManager.hud()

    /** Call on screen resize to update HUD camera dimensions. */
    fun resize(width: Int, height: Int) {
        hudCamera.setToOrtho(false, width.toFloat(), height.toFloat())
    }

    /**
     * Draw the HUD for the current frame.
     *
     * @param ship  The player ship (used for lives count)
     * @param score The current score
     * @param timeString  Formatted time string (e.g. "1:05")
     * @param astronautsRescued  Number of astronauts rescued
     * @param i18n  The i18n manager for translated labels
     */
    fun render(ship: Ship, score: Int, timeString: String, astronautsRescued: Int = 0, i18n: I18nManager) {
        hudCamera.update()
        batch.projectionMatrix = hudCamera.combined
        batch.begin()

        val topY = hudCamera.viewportHeight - margin

        // Line 1: Lives (heart icons + count)
        val livesColor = when {
            ship.lives >= 2 -> Color.WHITE
            ship.lives == 1 -> Color.RED
            else -> Color.DARK_GRAY
        }
        font.color = livesColor
        val hearts = "\u2665".repeat(ship.lives.coerceAtLeast(0))
        val livesText = "${i18n.get("lives")}: $hearts"
        font.draw(batch, livesText, margin, topY)

        // Line 2: Score
        font.color = Color.WHITE
        val scoreText = "${i18n.get("score")}: $score"
        font.draw(batch, scoreText, margin, topY - lineHeight)

        // Line 3: Timer
        font.color = Color.WHITE
        val timeText = "${i18n.get("time")}: $timeString"
        font.draw(batch, timeText, margin, topY - lineHeight * 2)

        // Line 4: Astronauts rescued (if any)
        if (astronautsRescued > 0) {
            font.color = Color(0.2f, 0.8f, 0.2f, 1f)
            val rescueText = "${i18n.get("astronauts_rescued")}: $astronautsRescued"
            font.draw(batch, rescueText, margin, topY - lineHeight * 3)
        }

        batch.end()
        font.color = Color.WHITE // reset
    }

    fun dispose() {
        batch.dispose()
        // FontManager handles font disposal globally
    }
}
