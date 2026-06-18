package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.NebulaDriftGame
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.rendering.UiSkin
import com.nebuladrift.util.Constants
import ktx.app.KtxScreen

/**
 * Settings screen using Scene2D.
 *
 * Provides music/SFX volume sliders, a language toggle button,
 * version info, and a back button — all rendered sharply via
 * Scene2D on a separate pixel-coordinate stage.
 */
class SettingsScreen(
    private val game: NebulaDriftGame
) : KtxScreen {

    // ── Background ──────────────────────────────────────────────
    private val bgCamera = OrthographicCamera()
    private val bgViewport = FitViewport(16f, 9f, bgCamera)
    private val shapeRenderer = ShapeRenderer()

    // ── UI ──────────────────────────────────────────────────────
    private val stage = Stage(FitViewport(800f, 450f))
    private val skin: Skin get() = UiSkin.instance

    // ── State ───────────────────────────────────────────────────
    private val i18n get() = game.i18n

    override fun show() {
        bgCamera.position.set(8f, 4.5f, 0f)

        // ── Build layout ────────────────────────────────────────
        val root = Table()
        root.setFillParent(true)
        root.defaults().center()

        // Heading
        val headingLabel = Label(i18n.get("settings"), skin.get("heading-white", Label.LabelStyle::class.java))
        root.add(headingLabel).colspan(2).padTop(30f).padBottom(24f).row()

        // Music volume slider
        val musicLabel = Label(i18n.get("music_volume"), skin.get("body-white", Label.LabelStyle::class.java))
        val musicSlider = Slider(0f, 1f, 0.01f, false, skin.get("default-horizontal", Slider.SliderStyle::class.java))
        musicSlider.value = AudioManager.musicVolume
        val musicValue = Label(formatPercent(AudioManager.musicVolume), skin.get("small-gray", Label.LabelStyle::class.java))

        musicSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                AudioManager.setMusicVolume(musicSlider.value)
                musicValue.setText(formatPercent(musicSlider.value))
            }
        })

        root.add(musicLabel).colspan(2).padBottom(2f).row()
        val musicRow = Table()
        musicRow.add(musicSlider).width(300f).height(20f).padRight(10f)
        musicRow.add(musicValue).width(40f)
        root.add(musicRow).colspan(2).padBottom(20f).row()

        // SFX volume slider
        val sfxLabel = Label(i18n.get("sfx_volume"), skin.get("body-white", Label.LabelStyle::class.java))
        val sfxSlider = Slider(0f, 1f, 0.01f, false, skin.get("default-horizontal", Slider.SliderStyle::class.java))
        sfxSlider.value = AudioManager.sfxVolume
        val sfxValue = Label(formatPercent(AudioManager.sfxVolume), skin.get("small-gray", Label.LabelStyle::class.java))

        sfxSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                AudioManager.setSfxVolume(sfxSlider.value)
                sfxValue.setText(formatPercent(sfxSlider.value))
            }
        })

        root.add(sfxLabel).colspan(2).padBottom(2f).row()
        val sfxRow = Table()
        sfxRow.add(sfxSlider).width(300f).height(20f).padRight(10f)
        sfxRow.add(sfxValue).width(40f)
        root.add(sfxRow).colspan(2).padBottom(20f).row()

        // Language button
        val currentLang = when (i18n.getLocale()) {
            "eu" -> "Euskera"
            "es" -> "Español"
            "en" -> "English"
            else -> "Euskera"
        }
        val langBtn = TextButton("${i18n.get("language")}: $currentLang", skin.get("default", TextButton.TextButtonStyle::class.java))
        root.add(langBtn).colspan(2).width(280f).height(48f).padBottom(16f).row()

        // Version
        val versionLabel = Label("v${Constants.GAME_VERSION}", skin.get("small-gray", Label.LabelStyle::class.java))
        root.add(versionLabel).colspan(2).padBottom(20f).row()

        // Back button
        val backBtn = TextButton(i18n.get("back"), skin.get("small-btn", TextButton.TextButtonStyle::class.java))
        root.add(backBtn).colspan(2).width(180f).height(40f).row()

        stage.addActor(root)
        Gdx.input.inputProcessor = stage

        // ── Listeners ───────────────────────────────────────────
        langBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                cycleLanguage()
                langBtn.setText("${i18n.get("language")}: ${currentLangText()}")
            }
        })
        backBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.startTransition { game.setScreen<MenuScreen>() }
            }
        })
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
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

    // ── Helpers ─────────────────────────────────────────────────

    private fun formatPercent(value: Float): String = "${(value * 100).toInt()}%"

    private fun currentLangText(): String = when (i18n.getLocale()) {
        "eu" -> "Euskera"
        "es" -> "Español"
        "en" -> "English"
        else -> "Euskera"
    }

    private fun cycleLanguage() {
        val newLocale = when (i18n.getLocale()) {
            "eu" -> "es"
            "es" -> "en"
            "en" -> "eu"
            else -> "eu"
        }
        i18n.setLocale(newLocale)
    }
}
