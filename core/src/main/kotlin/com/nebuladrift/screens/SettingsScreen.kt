package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.rendering.UiComponents
import com.nebuladrift.util.Constants
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Settings screen with volume sliders, language toggle, and version info.
 *
 * Displays:
 * - Music volume slider (world-coordinate drag)
 * - SFX volume slider (world-coordinate drag)
 * - Language toggle button (cycles eu → es → en → eu)
 * - Game version at bottom
 * - Back button to [MenuScreen]
 *
 * All rendering uses [ShapeRenderer] + [SpriteBatch] + [BitmapFont]
 * in world coordinates via a [FitViewport], matching the existing
 * screen pattern.
 *
 * @property game The game instance for screen transitions and i18n
 */
class SettingsScreen(
    private val game: NebulaDriftGame
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val shapeRenderer = ShapeRenderer()
    private val spriteBatch = SpriteBatch()
    private val font = BitmapFont()
    private val viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, OrthographicCamera())

    // ── UI bounds (world units) ───────────────────────────────
    private val musicSliderBounds = Rectangle()
    private val sfxSliderBounds = Rectangle()
    private val languageButtonBounds = Rectangle()
    private val backButtonBounds = Rectangle()

    // ── Drag state ──────────────────────────────────────────
    private var isDraggingMusicSlider = false
    private var isDraggingSfxSlider = false

    // ── Lifecycle ─────────────────────────────────────────────

    override fun show() {
        val vw = viewport.worldWidth

        // Music slider: centered, y = 6
        musicSliderBounds.set(vw / 2 - 3f, 6f, 6f, 0.3f)

        // SFX slider: centered, y = 4.5
        sfxSliderBounds.set(vw / 2 - 3f, 4.5f, 6f, 0.3f)

        // Language button: centered, y = 3
        languageButtonBounds.set(vw / 2 - 2f, 3f, 4f, 0.8f)

        // Back button: bottom center
        backButtonBounds.set(vw / 2 - 1.5f, 0.5f, 3f, 0.8f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.apply()
        viewport.camera.update()

        // Set projection matrices before any rendering
        shapeRenderer.projectionMatrix = viewport.camera.combined
        spriteBatch.projectionMatrix = viewport.camera.combined

        // ── Title ──────────────────────────────────────────────
        spriteBatch.begin()
        font.color = Color.WHITE
        val title = game.i18n.get("settings")
        val titleLayout = GlyphLayout(font, title)
        font.draw(
            spriteBatch, title,
            (viewport.worldWidth - titleLayout.width) / 2f,
            viewport.worldHeight - 1f
        )
        spriteBatch.end()

        // ── Music slider ───────────────────────────────────────
        UiComponents.drawSlider(
            shapeRenderer, spriteBatch, font,
            musicSliderBounds,
            AudioManager.musicVolume,
            game.i18n.get("music_volume")
        )

        // ── SFX slider ─────────────────────────────────────────
        UiComponents.drawSlider(
            shapeRenderer, spriteBatch, font,
            sfxSliderBounds,
            AudioManager.sfxVolume,
            game.i18n.get("sfx_volume")
        )

        // ── Language button ────────────────────────────────────
        val currentLang = when (game.i18n.getLocale()) {
            "eu" -> "Euskera"
            "es" -> "Español"
            "en" -> "English"
            else -> "Euskera"
        }
        UiComponents.drawButton(
            shapeRenderer, spriteBatch, font,
            languageButtonBounds,
            "${game.i18n.get("language")}: $currentLang"
        )

        // ── Version ────────────────────────────────────────────
        spriteBatch.begin()
        font.color = Color.GRAY
        val versionText = "v${Constants.GAME_VERSION}"
        val versionLayout = GlyphLayout(font, versionText)
        font.draw(
            spriteBatch, versionText,
            (viewport.worldWidth - versionLayout.width) / 2f,
            1.5f
        )
        spriteBatch.end()

        // ── Back button ────────────────────────────────────────
        UiComponents.drawButton(
            shapeRenderer, spriteBatch, font,
            backButtonBounds,
            game.i18n.get("back")
        )

        // ── Input handling ─────────────────────────────────────
        handleInput()
    }

    // ── Input ──────────────────────────────────────────────────

    /**
     * Process touch input for sliders and buttons.
     *
     * - Drag on music/SFX sliders updates volume in real time
     * - Tap on language button cycles the locale
     * - Tap on back button transitions to [MenuScreen]
     */
    private fun handleInput() {
        // ── Slider drag (continuous touch) ─────────────────
        if (Gdx.input.isTouched) {
            val screenPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(screenPos)
            val wx = screenPos.x
            val wy = screenPos.y

            // Start dragging if touching a slider bar
            if (UiComponents.isPointInBounds(wx, wy, musicSliderBounds)) {
                isDraggingMusicSlider = true
            }
            if (UiComponents.isPointInBounds(wx, wy, sfxSliderBounds)) {
                isDraggingSfxSlider = true
            }

            // Update volume while dragging
            if (isDraggingMusicSlider) {
                updateMusicVolume(wx)
            }
            if (isDraggingSfxSlider) {
                updateSfxVolume(wx)
            }
        } else {
            // Touch released — clear drag flags
            isDraggingMusicSlider = false
            isDraggingSfxSlider = false
        }

        // ── Button taps (just touched) ─────────────────────
        if (Gdx.input.justTouched()) {
            val screenPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(screenPos)
            val wx = screenPos.x
            val wy = screenPos.y

            // Language toggle
            if (UiComponents.isPointInBounds(wx, wy, languageButtonBounds)) {
                cycleLanguage()
            }

            // Back to menu
            if (UiComponents.isPointInBounds(wx, wy, backButtonBounds)) {
                game.startTransition { game.setScreen<MenuScreen>() }
            }
        }
    }

    // ── Volume helpers ─────────────────────────────────────────

    private fun updateMusicVolume(x: Float) {
        val value = ((x - musicSliderBounds.x) / musicSliderBounds.width).coerceIn(0f, 1f)
        AudioManager.setMusicVolume(value)
    }

    private fun updateSfxVolume(x: Float) {
        val value = ((x - sfxSliderBounds.x) / sfxSliderBounds.width).coerceIn(0f, 1f)
        AudioManager.setSfxVolume(value)
    }

    // ── Language toggle ────────────────────────────────────────

    /**
     * Cycle the game locale: eu → es → en → eu.
     * After switching, transition to [MenuScreen] so that all
     * translatable strings are re-read from the new bundle.
     */
    private fun cycleLanguage() {
        val newLocale = when (game.i18n.getLocale()) {
            "eu" -> "es"
            "es" -> "en"
            "en" -> "eu"
            else -> "eu"
        }
        game.i18n.setLocale(newLocale)
        game.startTransition { game.setScreen<MenuScreen>() }
    }

    // ── Resize ─────────────────────────────────────────────────

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    // ── Cleanup ────────────────────────────────────────────────

    override fun dispose() {
        shapeRenderer.dispose()
        spriteBatch.dispose()
        font.dispose()
    }
}
