package com.nebuladrift

import android.os.Bundle
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Game-only Activity — runs libGDX gameplay in fullscreen.
 *
 * When gameplay ends, the result is passed back to [MainActivity]
 * via a [@Volatile] companion object ([pendingGameResult]) so the
 * Compose UI can show the game-over screen. This avoids cross-thread
 * races between the GL thread (where [GameLoop.onGameComplete]
 * fires) and the UI thread (where [onDestroy] and the Activity
 * result callback run).
 */
class GameActivity : AndroidApplication() {

    companion object {
        /**
         * Holds the most recent game result. Written on the GL thread
         * (inside [GameLoop.onGameComplete]), read on the UI thread
         * (inside [MainActivity]'s launcher callback).
         *
         * [@Volatile] guarantees visibility across threads without
         * requiring a synchronized block or Handler happens-before.
         */
        @Volatile
        var pendingGameResult: GameLoop.GameResult? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }

        val game = GameLoop().also { loop ->
            loop.onGameComplete = { result ->
                // Write to @Volatile field — immediately visible to
                // the UI thread when MainActivity's callback reads it.
                pendingGameResult = result

                // Properly dispose the GL thread listener.
                // Without this the render loop keeps running.
                Gdx.app.exit()

                // Guarantee the Activity finishes without requiring a
                // user back-press. Both calls are needed — exit() alone
                // only pauses the GL surface without finishing the Activity.
                runOnUiThread { finish() }
            }
        }

        initialize(game, config)
    }

    override fun onDestroy() {
        // Signal to MainActivity: result is available via
        // pendingGameResult. No Intent extras needed — the
        // @Volatile field is the source of truth.
        setResult(RESULT_OK)
        super.onDestroy()
    }
}
