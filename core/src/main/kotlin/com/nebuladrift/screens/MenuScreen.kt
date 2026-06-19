package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
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
import com.nebuladrift.rendering.FontManager
import com.nebuladrift.rendering.UiSkin
import com.nebuladrift.util.Constants
import ktx.app.KtxScreen

/**
 * Main menu screen using Scene2D for crisp, resolution-independent UI.
 *
 * Background (space + stars) is rendered with [ShapeRenderer] in a
 * 16:9 world viewport. Title uses a glowing effects layer rendered
 * with ShapeRenderer (circles for glow) + SpriteBatch for text.
 * All UI widgets use Scene2D on a separate [Stage].
 */
class MenuScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager
) : KtxScreen {

    // ── Background rendering ────────────────────────────────────
    private val bgCamera = OrthographicCamera()
    private val bgViewport = FitViewport(16f, 9f, bgCamera)
    private val shapeRenderer = ShapeRenderer()

    // ── Title glow rendering ────────────────────────────────────
    private val titleBatch = SpriteBatch()
    private val titleCamera = OrthographicCamera()
    private val titleLayout = GlyphLayout()

    // ── Scene2D UI ──────────────────────────────────────────────
    private val stage = Stage(FitViewport(800f, 450f))
    private val skin: Skin get() = UiSkin.instance

    // ── State ───────────────────────────────────────────────────
    private var highScore: Int = 0
    private var titleTime: Float = 0f

    override fun show() {
        bgCamera.position.set(8f, 4.5f, 0f)

        // Load high score
        highScore = Gdx.app.getPreferences("nebula-drift").getInteger("highScore", 0)

        // ── Build layout ────────────────────────────────────────
        val root = Table()
        root.setFillParent(true)

        // Spacer at top (title is rendered separately with glow)
        root.add().height(140f).row()

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
        titleTime += delta

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

        // ── Title glow effect (ShapeRenderer) ───────────────────
        renderTitleGlow()

        // ── Title text (SpriteBatch) ────────────────────────────
        renderTitleText()

        // ── UI ──────────────────────────────────────────────────
        stage.act(delta)
        stage.draw()
    }

    /**
     * Render a pulsing glow behind the title text using ShapeRenderer circles.
     * Multiple layered circles with varying alpha create a soft neon glow effect.
     */
    private fun renderTitleGlow() {
        // Title center in screen-pixel space
        val cx = Gdx.graphics.width / 2f
        val cy = Gdx.graphics.height * 0.72f // 72% from bottom

        // Convert to camera coordinates
        val worldCx = (cx / Gdx.graphics.width) * 16f
        val worldCy = (cy / Gdx.graphics.height) * 9f

        val pulse = 0.7f + 0.3f * kotlin.math.sin(titleTime * 2f).toFloat()

        shapeRenderer.projectionMatrix = bgCamera.combined
        shapeRenderer.begin(ShapeType.Filled)

        // Outer glow (wide, faint)
        shapeRenderer.setColor(0.2f, 0.4f, 1f, 0.04f * pulse)
        shapeRenderer.circle(worldCx, worldCy, 4.5f)

        // Mid glow
        shapeRenderer.setColor(0.3f, 0.5f, 1f, 0.08f * pulse)
        shapeRenderer.circle(worldCx, worldCy, 3.2f)

        // Inner glow (tight, bright)
        shapeRenderer.setColor(0.4f, 0.6f, 1f, 0.12f * pulse)
        shapeRenderer.circle(worldCx, worldCy, 2.0f)

        // Core glow (very tight, cyan)
        shapeRenderer.setColor(0.5f, 0.8f, 1f, 0.06f * pulse)
        shapeRenderer.circle(worldCx, worldCy, 1.2f)

        shapeRenderer.end()
    }

    /**
     * Render "NEBULA DRIFT" title text with gradient color effect.
     * Uses the Space font (Orbitron) for a sci-fi look.
     */
    private fun renderTitleText() {
        titleCamera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        titleBatch.projectionMatrix = titleCamera.combined

        val spaceFont = FontManager.space()
        val pulse = 0.85f + 0.15f * kotlin.math.sin(titleTime * 2f).toFloat()

        titleBatch.begin()

        // "NEBULA" — cyan/blue tint
        spaceFont.data.setScale(2.8f)
        spaceFont.color = Color(0.4f, 0.7f, 1f, pulse)
        val nebulaText = "NEBULA"
        titleLayout.setText(spaceFont, nebulaText)
        val nebulaW = titleLayout.width
        val nebulaX = (Gdx.graphics.width - nebulaW) / 2f
        val nebulaY = Gdx.graphics.height * 0.78f
        spaceFont.draw(titleBatch, nebulaText, nebulaX, nebulaY)

        // "DRIFT" — white with slight cyan
        spaceFont.color = Color(0.9f, 0.95f, 1f, pulse)
        val driftText = "DRIFT"
        titleLayout.setText(spaceFont, driftText)
        val driftW = titleLayout.width
        val driftX = (Gdx.graphics.width - driftW) / 2f
        val driftY = nebulaY - 65f
        spaceFont.draw(titleBatch, driftText, driftX, driftY)

        // Decorative line under title
        spaceFont.data.setScale(0.5f)
        spaceFont.color = Color(0.3f, 0.5f, 0.8f, 0.6f * pulse)
        val lineText = "━━━━━━━━━━━━━━━━━━━━"
        titleLayout.setText(spaceFont, lineText)
        val lineW = titleLayout.width
        val lineX = (Gdx.graphics.width - lineW) / 2f
        spaceFont.draw(titleBatch, lineText, lineX, driftY - 20f)

        spaceFont.data.setScale(1f)
        spaceFont.color = Color.WHITE

        titleBatch.end()
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
        titleBatch.dispose()
    }
}
