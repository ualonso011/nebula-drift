package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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
import com.nebuladrift.rendering.UiSkin
import com.nebuladrift.util.Constants
import ktx.app.KtxScreen

/**
 * Main menu screen using Scene2D for crisp, resolution-independent UI.
 *
 * Background (space + stars) is rendered with [ShapeRenderer] in a
 * 16:9 world viewport. All UI widgets use Scene2D on a separate
 * [Stage] with a [FitViewport] for sharp text at any resolution.
 */
class MenuScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager
) : KtxScreen {

    // ── Background rendering ────────────────────────────────────
    private val bgCamera = OrthographicCamera()
    private val bgViewport = FitViewport(16f, 9f, bgCamera)
    private val shapeRenderer = ShapeRenderer()

    // ── Scene2D UI ──────────────────────────────────────────────
    private val stage = Stage(FitViewport(800f, 450f))
    private val skin: Skin get() = UiSkin.instance

    // ── State ───────────────────────────────────────────────────
    private var highScore: Int = 0

    override fun show() {
        bgCamera.position.set(8f, 4.5f, 0f)

        // Load high score
        highScore = Gdx.app.getPreferences("nebula-drift").getInteger("highScore", 0)

        // ── Build layout ────────────────────────────────────────
        val root = Table()
        root.setFillParent(true)

        // Spacer at top
        root.add().height(60f).row()

        // Title
        val titleLabel = Label(i18n.get("title"), skin.get("title-white", Label.LabelStyle::class.java))
        root.add(titleLabel).colspan(2).center().padBottom(35f).row()

        // Play button
        val playBtn = TextButton(i18n.get("play"), skin.get("default", TextButton.TextButtonStyle::class.java))
        root.add(playBtn).colspan(2).width(260f).height(52f).padBottom(20f).row()

        // Settings + Leaderboard row
        val settingsBtn = TextButton(i18n.get("settings"), skin.get("small-btn", TextButton.TextButtonStyle::class.java))
        val leaderBtn = TextButton(i18n.get("leaderboard"), skin.get("small-btn", TextButton.TextButtonStyle::class.java))

        val btnRow = Table()
        btnRow.add(settingsBtn).width(180f).height(44f).padRight(12f)
        btnRow.add(leaderBtn).width(180f).height(44f)
        root.add(btnRow).colspan(2).row()

        // High score
        if (highScore > 0) {
            val hsLabel = Label("${i18n.get("high_score")}: $highScore", skin.get("small-gray", Label.LabelStyle::class.java))
            root.add(hsLabel).colspan(2).padTop(20f).row()
        }

        stage.addActor(root)
        Gdx.input.inputProcessor = stage

        // ── Button listeners ────────────────────────────────────
        playBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.startTransition { game.setScreen<GameScreen>() }
            }
        })
        settingsBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.startTransition { game.setScreen<SettingsScreen>() }
            }
        })
        leaderBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.startTransition { game.setScreen<LeaderboardScreen>() }
            }
        })

        AudioManager.playMusic(Constants.MUSIC_FILENAME)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.04f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // ── Background ──────────────────────────────────────────
        bgViewport.apply()
        bgCamera.update()
        shapeRenderer.projectionMatrix = bgCamera.combined

        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        shapeRenderer.rect(0f, 0f, 16f, 9f)

        // Stars
        shapeRenderer.color = Color(0.3f, 0.3f, 0.5f, 0.8f)
        for (i in 0 until 60) {
            val sx = (i * 17f + 7f) % 16f
            val sy = (i * 13f + i * i * 0.1f) % 9f
            shapeRenderer.circle(sx, sy, 0.03f + (i % 3) * 0.02f)
        }
        shapeRenderer.end()

        // ── UI ──────────────────────────────────────────────────
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
