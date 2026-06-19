package com.nebuladrift

import android.content.Intent
import android.os.Bundle
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Game-only Activity — runs libGDX gameplay in fullscreen.
 *
 * When gameplay ends, [Gdx.app.exit] properly disposes the GL
 * thread and [finish] tears down the Activity. The result is
 * passed back to [MainActivity] inside [onDestroy] so the
 * Compose UI can show the game-over screen.
 */
class GameActivity : AndroidApplication() {

    private var gameResult: GameLoop.GameResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }

        val game = GameLoop().also { loop ->
            loop.onGameComplete = { result ->
                gameResult = result
                // BOTH calls needed:
                // 1. Gdx.app.exit() disposes the GL thread listener properly
                // 2. runOnUiThread { finish() } guarantees the Activity
                //    finishes and onDestroy delivers the result to MainActivity.
                // Without (2) the user must press Back to see the Game Over
                // screen. Without (1) the GL thread races with Activity teardown.
                Gdx.app.exit()
                runOnUiThread { finish() }
            }
        }

        initialize(game, config)
    }

    override fun onDestroy() {
        // Set the result BEFORE super.onDestroy() disposes libGDX,
        // so the intent is ready when the Activity result is delivered.
        gameResult?.let { result ->
            val intent = Intent().apply {
                putExtra("score", result.score)
                putExtra("time", result.timeFormatted)
                putExtra("asteroidsDestroyed", result.asteroidsDestroyed)
                putExtra("enemiesDestroyed", result.enemiesDestroyed)
                putExtra("astronautsRescued", result.astronautsRescued)
                putExtra("astronautsKilled", result.astronautsKilled)
            }
            setResult(RESULT_OK, intent)
        } ?: run {
            setResult(RESULT_CANCELED)
        }
        super.onDestroy()
    }
}
