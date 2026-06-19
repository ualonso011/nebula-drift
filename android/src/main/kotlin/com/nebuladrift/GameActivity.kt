package com.nebuladrift

import android.content.Intent
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Game-only Activity — runs libGDX gameplay in fullscreen.
 *
 * When gameplay ends, [finish] is called on the UI thread to tear
 * down the Activity. The result is passed back to
 * [MainActivity] inside [onDestroy] so the Compose UI can show
 * the game-over screen.
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
                // finish() must run on the UI thread. Using Gdx.app.exit()
                // alone races with the GL teardown — the activity may be
                // destroyed before gameResult is visible to onDestroy, causing
                // RESULT_CANCELED and skipping the Game Over screen.
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
