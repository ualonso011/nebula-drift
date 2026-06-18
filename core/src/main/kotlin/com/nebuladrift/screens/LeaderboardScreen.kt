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
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.managers.LeaderboardManager
import com.nebuladrift.rendering.UiSkin
import com.nebuladrift.util.Constants
import com.nebuladrift.util.LeaderboardEntry
import ktx.app.KtxScreen

/**
 * Leaderboard screen using Scene2D.
 *
 * Displays top-10 high scores with gold/silver/bronze highlights
 * for ranks 1-3 and a Back button to return to the main menu.
 */
class LeaderboardScreen(
    private val game: NebulaDriftGame,
    private val i18n: I18nManager
) : KtxScreen {

    // ── Background ──────────────────────────────────────────────
    private val bgCamera = OrthographicCamera()
    private val bgViewport = FitViewport(16f, 9f, bgCamera)
    private val shapeRenderer = ShapeRenderer()

    // ── UI ──────────────────────────────────────────────────────
    private val stage = Stage(FitViewport(800f, 450f))
    private val skin: Skin get() = UiSkin.instance

    // ── Colours ─────────────────────────────────────────────────
    private val gold = Color(0.8f, 0.6f, 0f, 1f)
    private val silver = Color(0.7f, 0.7f, 0.7f, 1f)
    private val bronze = Color(0.8f, 0.5f, 0.2f, 1f)

    override fun show() {
        bgCamera.position.set(8f, 4.5f, 0f)

        // ── Build UI ────────────────────────────────────────────
        val root = Table()
        root.setFillParent(true)
        root.defaults().center()

        // Heading
        val headingLabel = Label(i18n.get("leaderboard"), skin.get("heading-white", Label.LabelStyle::class.java))
        root.add(headingLabel).colspan(2).padTop(30f).padBottom(16f).row()

        // Entries
        val entries = LeaderboardManager.getEntries()

        if (entries.isEmpty()) {
            val emptyLabel = Label(i18n.get("no_scores"), skin.get("body-gray", Label.LabelStyle::class.java))
            root.add(emptyLabel).colspan(2).padTop(40f).row()
        } else {
            val listTable = Table()
            listTable.defaults().padBottom(3f)

            entries.forEachIndexed { index, entry ->
                val rankColor = when (index) {
                    0 -> gold
                    1 -> silver
                    2 -> bronze
                    else -> Color.WHITE
                }

                val rank = "#${index + 1}"
                val timeStr = formatTime(entry.time)
                val line = "$rank  ${entry.name}  —  ${entry.score} pts  —  $timeStr"

                val entryLabel = Label(line, skin.get("body-white", Label.LabelStyle::class.java))
                entryLabel.color = rankColor
                listTable.add(entryLabel).center().row()
            }

            val scrollContainer = Table()
            scrollContainer.add(listTable).expand().top()
            root.add(scrollContainer).colspan(2).expand().fill().padBottom(10f).row()
        }

        // Back button
        val backBtn = TextButton(i18n.get("back"), skin.get("small-btn", TextButton.TextButtonStyle::class.java))
        root.add(backBtn).colspan(2).width(180f).height(40f).padBottom(20f).row()

        stage.addActor(root)
        Gdx.input.inputProcessor = stage

        // ── Listeners ───────────────────────────────────────────
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

    private fun formatTime(time: Float): String {
        val minutes = (time / 60).toInt()
        val seconds = (time % 60).toInt()
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
