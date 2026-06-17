package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.util.Constants
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Main menu screen.
 *
 * Displays the game title ("NEBULA DRIFT"), a Play button, and the
 * current high score. Tapping Play transitions to [GameScreen].
 *
 * @property game The game instance for screen transitions
 * @property i18n The i18n manager for translated strings
 */
class MenuScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val font = BitmapFont()

    // ── Button (world units) ──────────────────────────────────
    private val btnWidth = 6f
    private val btnHeight = 1.4f
    private val btnCenterX = Constants.WORLD_WIDTH / 2f

    private val playButton = Rectangle(
        btnCenterX - btnWidth / 2f,
        3.5f,
        btnWidth,
        btnHeight
    )

    // ── High score ────────────────────────────────────────────
    private var highScore: Int = 0

    // ── Lifecycle ─────────────────────────────────────────────

    override fun show() {
        camera.position.set(
            Constants.WORLD_WIDTH / 2f,
            Constants.WORLD_HEIGHT / 2f,
            0f
        )

        // Load high score
        val prefs = Gdx.app.getPreferences("nebula-drift")
        highScore = prefs.getInteger("highScore", 0)

        // Start background music (no-op if no assets)
        AudioManager.playMusic(Constants.MUSIC_FILENAME)

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(
                screenX: Int,
                screenY: Int,
                pointer: Int,
                button: Int
            ): Boolean {
                val vec = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
                viewport.unproject(vec)
                val wx = vec.x
                val wy = vec.y

                if (playButton.contains(wx, wy)) {
                    game.startTransition { game.setScreen<GameScreen>() }
                    return true
                }
                return false
            }
        }
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        camera.update()

        // ── Star field background (simple dots) ──────────────
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Dark gradient: bottom is darker
        shapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        shapeRenderer.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)

        // Some "stars" (random dots)
        shapeRenderer.color = Color(0.3f, 0.3f, 0.5f, 0.8f)
        for (i in 0 until 60) {
            val sx = (i * 17f + 7f) % Constants.WORLD_WIDTH
            val sy = (i * 13f + i * i * 0.1f) % Constants.WORLD_HEIGHT
            shapeRenderer.circle(sx, sy, 0.03f + (i % 3) * 0.02f)
        }

        // Play button
        shapeRenderer.color = Color(0.15f, 0.45f, 0.7f, 1f)
        shapeRenderer.rect(
            playButton.x, playButton.y,
            playButton.width, playButton.height
        )
        // Button highlight
        shapeRenderer.color = Color(0.3f, 0.6f, 0.9f, 1f)
        shapeRenderer.rect(
            playButton.x - 0.04f, playButton.y - 0.04f,
            playButton.width + 0.08f, playButton.height + 0.08f
        )

        shapeRenderer.end()

        // ── Text ─────────────────────────────────────────────
        batch.projectionMatrix = camera.combined
        batch.begin()

        // Title: NEBULA DRIFT (large, centered)
        font.data.setScale(2.2f)
        font.color = Color(0.6f, 0.8f, 1f, 1f)
        val title = i18n.get("title")
        font.draw(
            batch,
            title,
            btnCenterX - title.length * 0.4f,
            Constants.WORLD_HEIGHT - 1.5f
        )

        // Play label
        font.data.setScale(1f)
        font.color = Color.WHITE
        val playLabel = i18n.get("play")
        font.draw(
            batch,
            playLabel,
            playButton.x + playButton.width / 2f - playLabel.length * 0.25f,
            playButton.y + playButton.height / 2f + 0.35f
        )

        // High score
        if (highScore > 0) {
            font.data.setScale(0.7f)
            font.color = Color(0.7f, 0.7f, 0.7f, 1f)
            val hsText = "${i18n.get("high_score")}: $highScore"
            font.draw(
                batch,
                hsText,
                btnCenterX - hsText.length * 0.15f,
                2f
            )
        }

        batch.end()

        // Reset font
        font.data.setScale(1f)
        font.color = Color.WHITE
    }

    // ── Cleanup ───────────────────────────────────────────────

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
    }
}
