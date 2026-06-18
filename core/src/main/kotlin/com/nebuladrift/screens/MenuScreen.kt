package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.rendering.FontManager
import com.nebuladrift.rendering.UiComponents
import com.nebuladrift.util.Constants
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Main menu screen.
 *
 * Displays the game title ("NEBULA DRIFT"), a Play button, and the
 * current high score. Tapping Play transitions to [GameScreen].
 *
 * Uses [FontManager] for smooth, legible text and [GlyphLayout] for
 * exact centering of all labels.
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

    // ── Fonts from FontManager ────────────────────────────────
    private val titleFont get() = FontManager.title()
    private val bodyFont get() = FontManager.body()
    private val smallFont get() = FontManager.small()

    // ── Button bounds (world units) ───────────────────────────
    private val btnWidth = 7f
    private val btnHeight = 1.2f
    private val btnCenterX = Constants.WORLD_WIDTH / 2f

    // Play button — centered
    private val playButton = Rectangle(
        btnCenterX - btnWidth / 2f,
        3.8f,
        btnWidth,
        btnHeight
    )

    // Settings & Leaderboard — bottom row
    private val settingsButtonBounds = Rectangle(
        1f,
        1.2f,
        3.5f,
        0.9f
    )
    private val leaderboardButtonBounds = Rectangle(
        Constants.WORLD_WIDTH - 4.5f,
        1.2f,
        3.5f,
        0.9f
    )

    // Provide button labels
    private val playLabel: String get() = i18n.get("play")
    private val settingsLabel: String get() = i18n.get("settings")
    private val leaderboardLabel: String get() = i18n.get("leaderboard")

    // ── High score ────────────────────────────────────────────
    private var highScore: Int = 0

    // ── Cached layouts for centering ──────────────────────────
    private val titleLayout: GlyphLayout by lazy { GlyphLayout(titleFont, i18n.get("title")) }

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

                return when {
                    playButton.contains(wx, wy) -> {
                        game.startTransition { game.setScreen<GameScreen>() }
                        true
                    }
                    settingsButtonBounds.contains(wx, wy) -> {
                        game.startTransition { game.setScreen<SettingsScreen>() }
                        true
                    }
                    leaderboardButtonBounds.contains(wx, wy) -> {
                        game.startTransition { game.setScreen<LeaderboardScreen>() }
                        true
                    }
                    else -> false
                }
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

        val projMatrix = camera.combined
        shapeRenderer.projectionMatrix = projMatrix
        batch.projectionMatrix = projMatrix

        // ── Background gradient ───────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        // Dark space background
        shapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        shapeRenderer.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)

        // Subtle stars
        shapeRenderer.color = Color(0.3f, 0.3f, 0.5f, 0.8f)
        for (i in 0 until 60) {
            val sx = (i * 17f + 7f) % Constants.WORLD_WIDTH
            val sy = (i * 13f + i * i * 0.1f) % Constants.WORLD_HEIGHT
            shapeRenderer.circle(sx, sy, 0.03f + (i % 3) * 0.02f)
        }

        // Play button background
        shapeRenderer.color = Color(0.15f, 0.45f, 0.7f, 1f)
        shapeRenderer.rect(playButton.x, playButton.y, playButton.width, playButton.height)
        // Button highlight border
        shapeRenderer.color = Color(0.3f, 0.6f, 0.9f, 1f)
        shapeRenderer.rect(
            playButton.x - 0.04f, playButton.y - 0.04f,
            playButton.width + 0.08f, playButton.height + 0.08f
        )
        shapeRenderer.end()

        // ── Title text ────────────────────────────────────────
        batch.begin()
        titleFont.color = Color(0.6f, 0.8f, 1f, 1f)
        titleFont.draw(batch, titleLayout, btnCenterX - titleLayout.width / 2f,
            Constants.WORLD_HEIGHT - 1.2f)

        // Play label centered on play button
        bodyFont.color = Color.WHITE
        val playLayout = GlyphLayout(bodyFont, playLabel)
        bodyFont.draw(batch, playLabel,
            playButton.x + (playButton.width - playLayout.width) / 2f,
            playButton.y + playButton.height / 2f + playLayout.height / 2f)

        // High score
        if (highScore > 0) {
            val hsText = "${i18n.get("high_score")}: $highScore"
            val hsLayout = GlyphLayout(smallFont, hsText)
            smallFont.color = Color(0.7f, 0.7f, 0.7f, 1f)
            smallFont.draw(batch, hsText,
                btnCenterX - hsLayout.width / 2f,
                playButton.y - hsLayout.height - 0.3f)
        }

        batch.end()

        // ── Bottom buttons ────────────────────────────────────
        UiComponents.drawButton(shapeRenderer, batch, bodyFont, settingsButtonBounds, settingsLabel)
        UiComponents.drawButton(shapeRenderer, batch, bodyFont, leaderboardButtonBounds, leaderboardLabel)

        // Reset font colour for next frame
        titleFont.color = Color.WHITE
        bodyFont.color = Color.WHITE
        smallFont.color = Color.WHITE
    }

    // ── Cleanup ───────────────────────────────────────────────

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        // FontManager handles font disposal globally
    }
}
