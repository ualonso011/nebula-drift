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
 * only the gameplay screen — the Game Over screen is rendered
 * inside GameScreen (Scene2D) and only exits to Compose when the
 * user clicks "Main Menu".
 */
class GameLoop : NebulaDriftGame() {

    /** Called when the user clicks "Main Menu" in GameOverScreen. */
    var onExitToMenu: (() -> Unit)? = null

    override fun create() {
        Gdx.app.log("GameLoop", "=== create() ===")

        try {
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

            // Game screen (gameplay) — game-over is rendered inline
            addScreen(GameScreen(
                game = this,
                i18n = i18n,
                atlas = atlas,
                onExitToMenu = {
                    GameSession.reset()
                    onExitToMenu?.invoke()
                },
            ))

            setScreen<GameScreen>()
        } catch (e: Exception) {
            Gdx.app.error("GameLoop", "FATAL: create() failed", e)
            throw e
        }
    }
}
