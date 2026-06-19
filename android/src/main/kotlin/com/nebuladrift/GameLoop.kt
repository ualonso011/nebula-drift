package com.nebuladrift

import com.badlogic.gdx.Gdx
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.screens.GameScreen
import com.nebuladrift.screens.GameSession
import ktx.app.removeScreen

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

        // Initialize the full game (transition system, all screens, fonts, atlas)
        super.create()

        // Override GameScreen with onGameOver callback for Android
        removeScreen<GameScreen>()
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
