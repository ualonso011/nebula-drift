package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
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
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.FontManager
import com.nebuladrift.rendering.UiComponents
import com.nebuladrift.util.Constants
import com.nebuladrift.util.LeaderboardEntry
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Displays the final score and offers Retry / Main Menu options.
 *
 * Score data is received via [GameSession] (set by [GameScreen]
 * before transitioning). Uses [FontManager] for smooth typography
 * and [GlyphLayout] for exact centering.
 *
 * @property game The game instance for screen transitions
 * @property i18n The i18n manager for translated strings
 */
class GameOverScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()

    // ── Fonts ─────────────────────────────────────────────────
    private val headingFont get() = FontManager.heading()
    private val bodyFont get() = FontManager.body()
    private val smallFont get() = FontManager.small()

    // ── Button definitions (world coordinates) ─────────────────
    private data class ButtonDef(
        val bounds: Rectangle,
        val labelKey: String
    )

    private val btnWidth = 7f
    private val btnHeight = 1.2f
    private val btnCenterX = Constants.WORLD_WIDTH / 2f

    private val retryButton = ButtonDef(
        bounds = Rectangle(btnCenterX - btnWidth / 2f, 2.8f, btnWidth, btnHeight),
        labelKey = "retry"
    )
    private val menuButton = ButtonDef(
        bounds = Rectangle(btnCenterX - btnWidth / 2f, 1.0f, btnWidth, btnHeight),
        labelKey = "main_menu"
    )

    // ── Name entry (when new high score) ──────────────────────
    private var showNameEntry = false
    private val predefinedNames = listOf("Pilot", "Ace", "Nova", "Stryker", "Vega", "Orion")
    private val nameButtonBounds = mutableListOf<Rectangle>()
    private val leaderboardButtonBounds = Rectangle()

    // ── State ──────────────────────────────────────────────────
    private var isNewRecord = false

    /** The input processor for this screen. */
    private lateinit var inputProcessor: InputProcessor

    // ── Cached labels ─────────────────────────────────────────
    private val gameOverText: String get() = i18n.get("game_over")
    private val newRecordText: String get() = i18n.get("new_record")
    private val enterNameText: String get() = i18n.get("enter_name")

    // ── Lifecycle ─────────────────────────────────────────────

    override fun show() {
        camera.position.set(
            Constants.WORLD_WIDTH / 2f,
            Constants.WORLD_HEIGHT / 2f,
            0f
        )

        // Load / save high score
        val prefs = Gdx.app.getPreferences("nebula-drift")
        val highScore = prefs.getInteger("highScore", 0)
        if (GameSession.finalScore > highScore) {
            prefs.putInteger("highScore", GameSession.finalScore)
            prefs.flush()
            isNewRecord = true
        } else {
            isNewRecord = false
        }

        // Name entry when leaderboard-qualifying score
        if (isNewRecord && LeaderboardManager.isHighScore(GameSession.finalScore)) {
            showNameEntry = true
            nameButtonBounds.clear()
            predefinedNames.forEachIndexed { index, _ ->
                val x = 1.5f + (index % 3) * 4.5f
                val y = 5.0f - (index / 3) * 1.4f
                nameButtonBounds.add(Rectangle(x, y, 3.8f, 0.9f))
            }
        }

        // Leaderboard button bounds
        leaderboardButtonBounds.set(
            Constants.WORLD_WIDTH / 2f - 2f,
            0.3f,
            4f,
            0.6f
        )

        // Play game-over SFX
        AudioManager.stopMusic()
        AudioManager.playSound(Constants.SFX_GAME_OVER)
        if (isNewRecord) {
            AudioManager.playSound(Constants.SFX_NEW_RECORD)
        }

        inputProcessor = object : InputAdapter() {
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

                // Name entry buttons take priority when visible
                if (showNameEntry) {
                    for (i in nameButtonBounds.indices) {
                        val bounds = nameButtonBounds[i]
                        if (bounds.contains(wx, wy)) {
                            LeaderboardManager.addEntry(
                                LeaderboardEntry(
                                    name = predefinedNames[i],
                                    score = GameSession.finalScore,
                                    time = GameSession.finalTime,
                                    date = java.util.Date().toString()
                                )
                            )
                            showNameEntry = false
                            return true
                        }
                    }
                }

                return when {
                    retryButton.bounds.contains(wx, wy) -> {
                        GameSession.reset()
                        game.startTransition { game.setScreen<GameScreen>() }
                        true
                    }
                    menuButton.bounds.contains(wx, wy) -> {
                        GameSession.reset()
                        game.startTransition { game.setScreen<MenuScreen>() }
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

        Gdx.input.inputProcessor = inputProcessor
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        camera.update()

        val projMatrix = camera.combined
        shapeRenderer.projectionMatrix = projMatrix
        batch.projectionMatrix = projMatrix

        // ── Render shapes ────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Dark background
        shapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        shapeRenderer.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)

        // Buttons
        drawButton(shapeRenderer, retryButton)
        drawButton(shapeRenderer, menuButton)

        // New record banner
        if (isNewRecord) {
            shapeRenderer.color = Color(0.8f, 0.6f, 0f, 1f) // gold
            shapeRenderer.rect(
                Constants.WORLD_WIDTH / 2f - 3.5f,
                6.2f,
                7f,
                0.8f
            )
        }

        // Name entry buttons (when applicable)
        if (showNameEntry) {
            nameButtonBounds.forEach { bounds ->
                shapeRenderer.color = Color(0.15f, 0.35f, 0.6f, 1f)
                shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
                shapeRenderer.color = Color(0.3f, 0.5f, 0.8f, 1f)
                shapeRenderer.rect(
                    bounds.x - 0.03f, bounds.y - 0.03f,
                    bounds.width + 0.06f, bounds.height + 0.06f
                )
            }
        }

        shapeRenderer.end()

        // ── Render text ──────────────────────────────────────
        batch.begin()

        // "GAME OVER" title
        headingFont.color = Color.RED
        val goLayout = GlyphLayout(headingFont, gameOverText)
        headingFont.draw(batch, gameOverText,
            Constants.WORLD_WIDTH / 2f - goLayout.width / 2f,
            Constants.WORLD_HEIGHT - 0.8f)

        // Score
        bodyFont.color = Color.WHITE
        val scoreText = "${i18n.get("score")}: ${GameSession.finalScore}"
        val scoreLayout = GlyphLayout(bodyFont, scoreText)
        bodyFont.draw(batch, scoreText,
            btnCenterX - scoreLayout.width / 2f, 6.0f)

        // Time
        val timeText = "${i18n.get("time")}: ${GameSession.finalTimeFormatted}"
        val timeLayout = GlyphLayout(bodyFont, timeText)
        bodyFont.draw(batch, timeText,
            btnCenterX - timeLayout.width / 2f, 5.4f)

        // Asteroids destroyed
        val astText = "${i18n.get("asteroids_destroyed")}: ${GameSession.asteroidsDestroyed}"
        bodyFont.draw(batch, astText,
            btnCenterX - GlyphLayout(bodyFont, astText).width / 2f, 4.8f)

        // Enemies destroyed
        val enText = "${i18n.get("enemies_destroyed")}: ${GameSession.enemiesDestroyed}"
        bodyFont.draw(batch, enText,
            btnCenterX - GlyphLayout(bodyFont, enText).width / 2f, 4.2f)

        // Astronauts rescued
        val resText = "${i18n.get("astronauts_rescued")}: ${GameSession.astronautsRescued}"
        bodyFont.draw(batch, resText,
            btnCenterX - GlyphLayout(bodyFont, resText).width / 2f, 3.6f)

        // Astronauts killed (only if > 0)
        if (GameSession.astronautsKilled > 0) {
            val killText = "${i18n.get("astronauts_killed")}: ${GameSession.astronautsKilled}"
            bodyFont.draw(batch, killText,
                btnCenterX - GlyphLayout(bodyFont, killText).width / 2f, 3.0f)
        }

        // New record text
        if (isNewRecord) {
            smallFont.color = Color(0.8f, 0.6f, 0f, 1f)
            val nrLayout = GlyphLayout(smallFont, newRecordText)
            smallFont.draw(batch, newRecordText,
                Constants.WORLD_WIDTH / 2f - nrLayout.width / 2f, 6.8f)
        }

        // Name entry label + buttons
        if (showNameEntry) {
            smallFont.color = Color.YELLOW
            val enLayout = GlyphLayout(smallFont, enterNameText)
            smallFont.draw(batch, enterNameText,
                Constants.WORLD_WIDTH / 2f - enLayout.width / 2f, 5.2f)

            predefinedNames.forEachIndexed { index, name ->
                val bounds = nameButtonBounds[index]
                bodyFont.color = Color.WHITE
                val nameLayout = GlyphLayout(bodyFont, name)
                bodyFont.draw(batch, name,
                    bounds.x + (bounds.width - nameLayout.width) / 2f,
                    bounds.y + bounds.height / 2f + nameLayout.height / 2f)
            }
        }

        // Button labels
        drawButtonLabel(batch, retryButton)
        drawButtonLabel(batch, menuButton)

        batch.end()

        // Leaderboard button (small, bottom)
        UiComponents.drawButton(shapeRenderer, batch, smallFont, leaderboardButtonBounds, game.i18n.get("leaderboard"))

        // Reset font colours
        headingFont.color = Color.WHITE
        bodyFont.color = Color.WHITE
        smallFont.color = Color.WHITE
    }

    // ── Drawing helpers ───────────────────────────────────────

    private fun drawButton(sr: ShapeRenderer, btn: ButtonDef) {
        sr.color = Color(0.15f, 0.35f, 0.6f, 1f)
        sr.rect(btn.bounds.x, btn.bounds.y, btn.bounds.width, btn.bounds.height)
        // Subtle border
        sr.color = Color(0.3f, 0.5f, 0.8f, 1f)
        sr.rect(
            btn.bounds.x - 0.03f, btn.bounds.y - 0.03f,
            btn.bounds.width + 0.06f, btn.bounds.height + 0.06f
        )
    }

    private fun drawButtonLabel(sb: SpriteBatch, btn: ButtonDef) {
        val label = i18n.get(btn.labelKey)
        val layout = GlyphLayout(bodyFont, label)
        bodyFont.color = Color.WHITE
        bodyFont.draw(sb, label,
            btn.bounds.x + (btn.bounds.width - layout.width) / 2f,
            btn.bounds.y + btn.bounds.height / 2f + layout.height / 2f)
    }

    // ── Cleanup ───────────────────────────────────────────────

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        // FontManager handles font disposal globally
    }
}
