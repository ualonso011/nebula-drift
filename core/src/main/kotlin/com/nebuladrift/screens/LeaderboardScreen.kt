package com.nebuladrift.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.FontManager
import com.nebuladrift.rendering.UiComponents
import com.nebuladrift.util.Constants
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Displays the top-10 leaderboard with gold/silver/bronze highlights
 * for ranks 1-3 and a Back button that returns to [MenuScreen].
 *
 * Uses [FontManager] for smooth typography and [GlyphLayout] for
 * exact centering.
 *
 * @property game The game instance for screen transitions
 * @property i18n The i18n manager for translated strings
 */
class LeaderboardScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager
) : KtxScreen {

    // ── Rendering ─────────────────────────────────────────────
    private val shapeRenderer = ShapeRenderer()
    private val spriteBatch = SpriteBatch()
    private val viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, OrthographicCamera())

    // ── Fonts ─────────────────────────────────────────────────
    private val headingFont get() = FontManager.heading()
    private val bodyFont get() = FontManager.body()
    private val smallFont get() = FontManager.small()

    // ── Back button ───────────────────────────────────────────
    private val backButtonBounds = Rectangle(
        Constants.WORLD_WIDTH / 2f - 1.5f,
        0.5f,
        3f,
        0.8f
    )

    // ── Colours ───────────────────────────────────────────────
    private val goldColor = Color(0.8f, 0.6f, 0f, 1f)
    private val silverColor = Color(0.7f, 0.7f, 0.7f, 1f)
    private val bronzeColor = Color.valueOf("CD7F32")

    // ── Cached title layout ───────────────────────────────────
    private val titleText: String get() = i18n.get("leaderboard")

    // ── Lifecycle ─────────────────────────────────────────────

    override fun show() {
        // Camera is already centred by the FitViewport
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        viewport.apply()
        viewport.camera.update()

        val entries = LeaderboardManager.getEntries()
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight

        // Set projection matrices
        shapeRenderer.projectionMatrix = viewport.camera.combined
        spriteBatch.projectionMatrix = viewport.camera.combined

        // ── Background ─────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0.04f, 1f)
        shapeRenderer.rect(0f, 0f, vw, vh)
        shapeRenderer.end()

        // ── Title ──────────────────────────────────────────────
        spriteBatch.begin()
        headingFont.color = Color.WHITE
        val titleLayout = GlyphLayout(headingFont, titleText)
        headingFont.draw(spriteBatch, titleText,
            (vw - titleLayout.width) / 2f, vh - 0.8f)
        spriteBatch.end()

        // ── Entries or empty state ─────────────────────────────
        if (entries.isEmpty()) {
            spriteBatch.begin()
            bodyFont.color = Color.LIGHT_GRAY
            val noScores = i18n.get("no_scores")
            val noScoresLayout = GlyphLayout(bodyFont, noScores)
            bodyFont.draw(spriteBatch, noScores,
                (vw - noScoresLayout.width) / 2f, vh / 2f)
            spriteBatch.end()
        } else {
            spriteBatch.begin()
            entries.forEachIndexed { index, entry ->
                val y = vh - 2.2f - index * 0.7f

                // Rank colour
                bodyFont.color = when (index) {
                    0 -> goldColor
                    1 -> silverColor
                    2 -> bronzeColor
                    else -> Color.WHITE
                }

                val timeStr = formatTime(entry.time)
                val text = "#${index + 1}  ${entry.name}  —  ${entry.score} pts  —  $timeStr"
                val textLayout = GlyphLayout(bodyFont, text)
                bodyFont.draw(spriteBatch, text,
                    (vw - textLayout.width) / 2f, y)
            }
            spriteBatch.end()
        }

        // ── Back button ────────────────────────────────────────
        UiComponents.drawButton(
            shapeRenderer, spriteBatch, bodyFont,
            backButtonBounds,
            i18n.get("back")
        )

        // ── Input ──────────────────────────────────────────────
        if (Gdx.input.isTouched) {
            val screenPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(screenPos)
            if (UiComponents.isPointInBounds(screenPos.x, screenPos.y, backButtonBounds)) {
                game.startTransition { game.setScreen<MenuScreen>() }
            }
        }

        // Reset font colour
        bodyFont.color = Color.WHITE
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        spriteBatch.dispose()
        // FontManager handles font disposal globally
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun formatTime(time: Float): String {
        val minutes = (time / 60).toInt()
        val seconds = (time % 60).toInt()
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
