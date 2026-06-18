package com.nebuladrift

import android.content.Intent
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Game-only Activity — runs libGDX gameplay in fullscreen.
 *
 * When gameplay ends, the result is passed back to [MainActivity]
 * via a result Intent so the Compose UI can show the game-over screen.
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
                // `finish()` must run on the Android main thread.
                // libGDX render loop runs on a separate GL thread.
                runOnUiThread { finish() }
            }
        }

        initialize(game, config)
    }

    override fun onDestroy() {
        super.onDestroy()
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
    }
}
