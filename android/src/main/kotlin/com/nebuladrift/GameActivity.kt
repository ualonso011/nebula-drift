package com.nebuladrift

import android.os.Bundle
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Game-only Activity — runs libGDX gameplay in fullscreen.
 *
 * Game over is handled entirely inside libGDX via [GameOverScreen]
 * (Scene2D UI with score, stats, Retry, and Main Menu). The Activity
 * only finishes (returns to Compose menus in [MainActivity]) when
 * the user clicks "Main Menu".
 */
class GameActivity : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }

        val game = GameLoop().also { loop ->
            loop.onExitToMenu = {
                Gdx.app.exit()
                runOnUiThread { finish() }
            }
        }

        initialize(game, config)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
