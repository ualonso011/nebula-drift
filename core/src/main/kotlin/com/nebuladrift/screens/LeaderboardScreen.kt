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
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.UiComponents
import com.nebuladrift.util.Constants
import com.nebuladrift.NebulaDriftGame
import ktx.app.KtxScreen

/**
 * Displays the top-10 leaderboard with gold/silver/bronze highlights
 * for ranks 1–3 and a Back button that returns to [MenuScreen].
 *
 * Delegates to [LeaderboardManager] for data and [UiComponents] for
 * shared rendering helpers.
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
    private val font = BitmapFont()
    private val viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, OrthographicCamera())

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

        // ── Set projection matrices ────────────────────────────
        shapeRenderer.projectionMatrix = viewport.camera.combined
        spriteBatch.projectionMatrix = viewport.camera.combined

        // ── Title ──────────────────────────────────────────────
        spriteBatch.begin()
        font.data.setScale(1.2f)
        font.color = Color.WHITE
        val title = i18n.get("leaderboard")
        val titleLayout = GlyphLayout(font, title)
        font.draw(spriteBatch, title, (vw - titleLayout.width) / 2f, vh - 1f)
        spriteBatch.end()

        // ── Entries or empty state ─────────────────────────────
        if (entries.isEmpty()) {
            spriteBatch.begin()
            font.data.setScale(0.9f)
            font.color = Color.LIGHT_GRAY
            val noScores = i18n.get("no_scores")
            val noScoresLayout = GlyphLayout(font, noScores)
            font.draw(spriteBatch, noScores, (vw - noScoresLayout.width) / 2f, vh / 2f)
            spriteBatch.end()
        } else {
            spriteBatch.begin()
            font.data.setScale(0.7f)
            entries.forEachIndexed { index, entry ->
                val y = vh - 2f - index * 0.6f

                // Rank colour
                font.color = when (index) {
                    0 -> goldColor
                    1 -> silverColor
                    2 -> bronzeColor
                    else -> Color.WHITE
                }

                val timeStr = formatTime(entry.time)
                val text = "${index + 1}. ${entry.name} — ${entry.score} — $timeStr"
                val textLayout = GlyphLayout(font, text)
                font.draw(spriteBatch, text, (vw - textLayout.width) / 2f, y)
            }
            spriteBatch.end()
        }

        // ── Back button ────────────────────────────────────────
        UiComponents.drawButton(
            shapeRenderer, spriteBatch, font,
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
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        spriteBatch.dispose()
        font.dispose()
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun formatTime(time: Float): String {
        val minutes = (time / 60).toInt()
        val seconds = (time % 60).toInt()
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
