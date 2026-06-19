package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.NebulaDriftGame
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.UiSkin
import com.nebuladrift.util.Constants
import com.nebuladrift.util.LeaderboardEntry
import ktx.app.KtxScreen

/**
 * Game-over screen using Scene2D for crisp text and buttons.
 *
 * Shows final score, stats, and Retry / Main Menu options.
 * New high scores trigger a name-entry step.
 */
class GameOverScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager,
    /**
     * Android callback: when the user clicks "Main Menu", the host
     * Activity finishes (back to Compose menus). Null on desktop —
     * falls back to the libGDX [MenuScreen].
     */
    private val onExitToMenu: (() -> Unit)? = null,
) : KtxScreen {

    // ── Background ──────────────────────────────────────────────
    private val bgCamera = OrthographicCamera()
    private val bgViewport = FitViewport(16f, 9f, bgCamera)
    private val shapeRenderer = ShapeRenderer()

    // ── UI ──────────────────────────────────────────────────────
    private val stage = Stage(FitViewport(800f, 450f))
    private val skin: Skin get() = UiSkin.instance

    // ── State ───────────────────────────────────────────────────
    private var isNewRecord = false
    private var showNameEntry = false
    private val predefinedNames = listOf("Pilot", "Ace", "Nova", "Stryker", "Vega", "Orion")
    private val nameEntryButtons = mutableListOf<TextButton>()

    override fun show() {
        bgCamera.position.set(8f, 4.5f, 0f)

        // ── High score check ────────────────────────────────────
        val prefs = Gdx.app.getPreferences("nebula-drift")
        val highScore = prefs.getInteger("highScore", 0)
        if (GameSession.finalScore > highScore) {
            prefs.putInteger("highScore", GameSession.finalScore)
            prefs.flush()
            isNewRecord = true
        }

        if (isNewRecord && LeaderboardManager.isHighScore(GameSession.finalScore)) {
            showNameEntry = true
        }

        // ── Play SFX ────────────────────────────────────────────
        AudioManager.stopMusic()
        AudioManager.playSound(Constants.SFX_GAME_OVER)
        if (isNewRecord) AudioManager.playSound(Constants.SFX_NEW_RECORD)

        // ── Build layout ────────────────────────────────────────
        buildUI()

        Gdx.input.inputProcessor = stage
    }

    private fun buildUI() {
        stage.clear()
        nameEntryButtons.clear()

        val root = Table()
        root.setFillParent(true)
        root.defaults().center()

        // ── Heading ────────────────────────────────────────────
        val headingLabel = Label(i18n.get("game_over"), skin.get("heading-white", Label.LabelStyle::class.java))
        headingLabel.color = Color.RED
        root.add(headingLabel).colspan(2).padTop(30f).row()

        // New record banner
        if (isNewRecord) {
            val nrLabel = Label(i18n.get("new_record"), skin.get("small-gold", Label.LabelStyle::class.java))
            val nrBg = Table()
            nrBg.setBackground(skin.get("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable::class.java))
            nrBg.color = Color(0.8f, 0.6f, 0f, 0.25f)
            nrBg.add(nrLabel).pad(6f, 20f, 6f, 20f)
            root.add(nrBg).colspan(2).padTop(10f).padBottom(8f).row()
        }

        // ── Stats ───────────────────────────────────────────────
        val stats = listOf(
            "${i18n.get("score")}: ${GameSession.finalScore}",
            "${i18n.get("time")}: ${GameSession.finalTimeFormatted}",
            "${i18n.get("asteroids_destroyed")}: ${GameSession.asteroidsDestroyed}",
            "${i18n.get("enemies_destroyed")}: ${GameSession.enemiesDestroyed}",
            "${i18n.get("astronauts_rescued")}: ${GameSession.astronautsRescued}"
        )

        val statsTable = Table()
        for (line in stats) {
            statsTable.add(Label(line, skin.get("body-white", Label.LabelStyle::class.java))).padBottom(2f).row()
        }

        if (GameSession.astronautsKilled > 0) {
            statsTable.add(Label(
                "${i18n.get("astronauts_killed")}: ${GameSession.astronautsKilled}",
                skin.get("body-gray", Label.LabelStyle::class.java)
            )).padBottom(2f).row()
        }

        root.add(statsTable).colspan(2).padTop(12f).padBottom(20f).row()

        // ── Name entry ──────────────────────────────────────────
        if (showNameEntry) {
            val nameLabel = Label(i18n.get("enter_name"), skin.get("small-gold", Label.LabelStyle::class.java))
            root.add(nameLabel).colspan(2).padBottom(8f).row()

            val nameGrid = Table()
            predefinedNames.forEachIndexed { index, name ->
                val btn = TextButton(name, skin.get("small-btn", TextButton.TextButtonStyle::class.java))
                nameEntryButtons.add(btn)
                val col = index % 3
                val row = index / 3
                if (col == 0 && row > 0) nameGrid.row()
                nameGrid.add(btn).width(120f).height(36f).pad(4f)

                btn.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        LeaderboardManager.addEntry(
                            LeaderboardEntry(
                                name = name,
                                score = GameSession.finalScore,
                                time = GameSession.finalTime,
                                date = java.util.Date().toString()
                            )
                        )
                        showNameEntry = false
                        // Refresh UI without name entry
                        rebuildWithoutNameEntry()
                    }
                })
            }
            root.add(nameGrid).colspan(2).padBottom(16f).row()
        }

        // ── Action buttons ──────────────────────────────────────
        val retryBtn = TextButton(i18n.get("retry"), skin.get("default", TextButton.TextButtonStyle::class.java))
        val menuBtn = TextButton(i18n.get("main_menu"), skin.get("default", TextButton.TextButtonStyle::class.java))

        root.add(retryBtn).colspan(2).width(240f).height(48f).padBottom(8f).row()
        root.add(menuBtn).colspan(2).width(240f).height(48f).padBottom(12f).row()

        // Leaderboard button (small)
        val lbBtn = TextButton(i18n.get("leaderboard"), skin.get("small-btn", TextButton.TextButtonStyle::class.java))
        root.add(lbBtn).colspan(2).width(200f).height(36f).row()

        stage.addActor(root)

        // ── Listeners ───────────────────────────────────────────
        retryBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                GameSession.reset()
                game.startTransition { game.setScreen<GameScreen>() }
            }
        })
        menuBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                GameSession.reset()
                if (onExitToMenu != null) {
                    // Android: finish Activity, return to Compose menu
                    onExitToMenu?.invoke()
                } else {
                    // Desktop: transition to libGDX MenuScreen
                    game.startTransition { game.setScreen<MenuScreen>() }
                }
            }
        })
        lbBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.startTransition { game.setScreen<LeaderboardScreen>() }
            }
        })
    }

    private fun rebuildWithoutNameEntry() {
        showNameEntry = false
        buildUI()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        bgViewport.apply()
        bgCamera.update()
        shapeRenderer.projectionMatrix = bgCamera.combined

        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        shapeRenderer.rect(0f, 0f, 16f, 9f)
        shapeRenderer.end()

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        bgViewport.update(width, height)
        stage.viewport.update(width, height, true)
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        shapeRenderer.dispose()
        stage.dispose()
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
