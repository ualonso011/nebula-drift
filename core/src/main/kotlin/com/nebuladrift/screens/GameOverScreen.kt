package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
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
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.UiComponents
import com.nebuladrift.util.Constants
import com.nebuladrift.util.LeaderboardEntry
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Displays the final score and offers Retry / Main Menu options.
 *
 * Score data is received via [GameSession] (set by [GameScreen]
 * before transitioning).
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
    private val font = BitmapFont()

    // ── Button definitions (world coordinates) ─────────────────
    private data class ButtonDef(
        val bounds: Rectangle,
        val labelKey: String
    )

    private val btnWidth = 6f
    private val btnHeight = 1.2f
    private val btnCenterX = Constants.WORLD_WIDTH / 2f

    private val retryButton = ButtonDef(
        bounds = Rectangle(btnCenterX - btnWidth / 2f, 2.5f, btnWidth, btnHeight),
        labelKey = "retry"
    )
    private val menuButton = ButtonDef(
        bounds = Rectangle(btnCenterX - btnWidth / 2f, 0.8f, btnWidth, btnHeight),
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
                val x = 2f + (index % 3) * 4f
                val y = 4f - (index / 3) * 1.2f
                nameButtonBounds.add(Rectangle(x, y, 3.5f, 0.8f))
            }
        }

        // Leaderboard button bounds
        leaderboardButtonBounds.set(
            viewport.worldWidth / 2 - 1.5f,
            1.5f,
            3f,
            0.8f
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
                // Convert screen → world coordinates
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

        // ── Render shapes ────────────────────────────────────
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Buttons
        drawButton(shapeRenderer, retryButton)
        drawButton(shapeRenderer, menuButton)

        // New record banner
        if (isNewRecord) {
            shapeRenderer.color = Color(0.8f, 0.6f, 0f, 1f) // gold
            shapeRenderer.rect(
                Constants.WORLD_WIDTH / 2f - 3.5f,
                6f,
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
        batch.projectionMatrix = camera.combined
        batch.begin()

        // Scale font for larger titles
        font.data.setScale(1.5f)

        // "GAME OVER" title
        font.color = Color.RED
        val gameOverText = i18n.get("game_over")
        font.draw(
            batch,
            gameOverText,
            Constants.WORLD_WIDTH / 2f - gameOverText.length * 0.25f,
            Constants.WORLD_HEIGHT - 1f
        )

        font.data.setScale(1f)
        font.color = Color.WHITE

        // Score
        font.draw(
            batch,
            "${i18n.get("score")}: ${GameSession.finalScore}",
            btnCenterX - 3f,
            5.8f
        )

        // Time
        font.draw(
            batch,
            "${i18n.get("time")}: ${GameSession.finalTimeFormatted}",
            btnCenterX - 3f,
            5.2f
        )

        // Asteroids destroyed
        font.draw(
            batch,
            "${i18n.get("asteroids_destroyed")}: ${GameSession.asteroidsDestroyed}",
            btnCenterX - 3f,
            4.6f
        )

        // Enemies destroyed
        font.draw(
            batch,
            "${i18n.get("enemies_destroyed")}: ${GameSession.enemiesDestroyed}",
            btnCenterX - 3f,
            4.0f
        )

        // Astronauts rescued
        font.draw(
            batch,
            "${i18n.get("astronauts_rescued")}: ${GameSession.astronautsRescued}",
            btnCenterX - 3f,
            3.4f
        )

        // Astronauts killed
        font.draw(
            batch,
            "${i18n.get("astronauts_killed")}: ${GameSession.astronautsKilled}",
            btnCenterX - 3f,
            2.8f
        )

        // New record text
        if (isNewRecord) {
            font.color = Color(0.8f, 0.6f, 0f, 1f)
            font.data.setScale(0.9f)
            font.draw(
                batch,
                i18n.get("new_record"),
                Constants.WORLD_WIDTH / 2f - 2.5f,
                6.6f
            )
            font.data.setScale(1f)
        }

        // Name entry label + buttons
        if (showNameEntry) {
            font.color = Color.YELLOW
            font.data.setScale(0.8f)
            val label = i18n.get("enter_name")
            font.draw(
                batch,
                label,
                Constants.WORLD_WIDTH / 2f - label.length * 0.2f,
                5f
            )
            predefinedNames.forEachIndexed { index, name ->
                val bounds = nameButtonBounds[index]
                font.color = Color.WHITE
                font.draw(
                    batch,
                    name,
                    bounds.x + bounds.width / 2f - name.length * 0.2f,
                    bounds.y + bounds.height / 2f + 0.25f
                )
            }
            font.data.setScale(1f)
        }

        font.data.setScale(0.8f)
        font.color = Color.WHITE

        // Button labels
        drawButtonLabel(batch, retryButton)
        drawButtonLabel(batch, menuButton)

        batch.end()

        // Leaderboard button
        UiComponents.drawButton(shapeRenderer, batch, font, leaderboardButtonBounds, game.i18n.get("leaderboard"))

        // Reset font scale for next frame
        font.data.setScale(1f)
        font.color = Color.WHITE
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
        font.draw(
            sb,
            label,
            btn.bounds.x + btn.bounds.width / 2f - label.length * 0.2f,
            btn.bounds.y + btn.bounds.height / 2f + 0.3f
        )
    }

    // ── Cleanup ───────────────────────────────────────────────

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        font.dispose()
    }
}

/**
 * Session-scoped holder for final-score data passed between
 * [GameScreen] and [GameOverScreen].
 */
object GameSession {
    var finalScore: Int = 0
    var finalTime: Float = 0f
    var finalTimeFormatted: String = "0:00"
    var asteroidsDestroyed: Int = 0
    var enemiesDestroyed: Int = 0
    var astronautsRescued: Int = 0
    var astronautsKilled: Int = 0

    fun reset() {
        finalScore = 0
        finalTime = 0f
        finalTimeFormatted = "0:00"
        asteroidsDestroyed = 0
        enemiesDestroyed = 0
        astronautsRescued = 0
        astronautsKilled = 0
    }
}
