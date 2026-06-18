package com.nebuladrift

import com.badlogic.gdx.Gdx
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

        AudioManager.init()
        FontManager.init()

        val i18n = I18nManager()
        i18n.init()

        val atlas = SpriteGenerator.generateAtlas()

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
