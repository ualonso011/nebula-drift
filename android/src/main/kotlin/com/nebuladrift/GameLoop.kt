package com.nebuladrift

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.rendering.FontManager
import com.nebuladrift.rendering.SpriteGenerator
import com.nebuladrift.screens.GameScreen
import com.nebuladrift.screens.GameSession

/**
 * Minimal libGDX game loop for Android.
 *
 * Extends [NebulaDriftGame] so that [GameScreen] can access the
 * inherited [transition] property. Overrides [create] to set up
 * only the gameplay screen — no menus (those are handled by Compose
 * in [MainActivity]).
 *
 * When the game ends, [onGameComplete] fires with the final stats
 * and the host Activity calls [finish] to return to Compose.
 */
class GameLoop : NebulaDriftGame() {

    /** Callback invoked when gameplay ends. */
    var onGameComplete: ((GameResult) -> Unit)? = null

    override fun create() {
        Gdx.app.log("GameLoop", "=== create() ===")

        // Initialize core systems (audio, fonts, atlas, i18n)
        AudioManager.init()
        FontManager.init()

        i18n = I18nManager().also { it.init() }
        atlas = SpriteGenerator.generateAtlas()

        // Initialize transition system (required by NebulaDriftGame.render)
        val pix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pix.setColor(Color.WHITE)
        pix.fill()
        whiteTexture = Texture(pix)
        pix.dispose()

        transitionBatch = SpriteBatch()

        // Add GameScreen with onGameOver callback for Android
        addScreen(
            GameScreen(
                game = this as NebulaDriftGame,
                i18n = i18n,
                atlas = atlas,
                onGameOver = {
                    onGameComplete?.invoke(buildResult())
                },
            ),
        )

        setScreen<GameScreen>()
    }

    private fun buildResult(): GameResult {
        return GameResult(
            score = GameSession.finalScore,
            timeFormatted = GameSession.finalTimeFormatted,
            asteroidsDestroyed = GameSession.asteroidsDestroyed,
            enemiesDestroyed = GameSession.enemiesDestroyed,
            astronautsRescued = GameSession.astronautsRescued,
            astronautsKilled = GameSession.astronautsKilled,
        )
    }

    data class GameResult(
        val score: Int,
        val timeFormatted: String,
        val asteroidsDestroyed: Int,
        val enemiesDestroyed: Int,
        val astronautsRescued: Int,
        val astronautsKilled: Int,
    )
}
