package com.nebuladrift

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.nebuladrift.managers.AudioManager
import com.nebuladrift.managers.I18nManager
import com.nebuladrift.rendering.FadeTransition
import com.nebuladrift.rendering.SpriteAtlas
import com.nebuladrift.rendering.SpriteGenerator
import com.nebuladrift.screens.GameOverScreen
import com.nebuladrift.screens.GameScreen
import com.nebuladrift.screens.LeaderboardScreen
import com.nebuladrift.screens.MenuScreen
import com.nebuladrift.screens.SettingsScreen
import com.nebuladrift.util.Constants
import ktx.app.KtxGame
import ktx.app.KtxScreen

/**
 * Top-level game class.
 *
 * Manages screen lifecycle via [KtxGame]. The screen flow is:
 * ```
 * MenuScreen ──[Play]──→ GameScreen ──[destroyed]──→ GameOverScreen
 *     ↑                       ↑                            │
 *     └──────[Main Menu]──────┘────────────────────[Retry]─┘
 * ```
 *
 * Also owns the global screen [transition] system. When [startTransition]
 * is called, a [FadeTransition] plays across all screen switches,
 * rendering a fullscreen overlay on top of everything.
 */
class NebulaDriftGame : KtxGame<KtxScreen>() {

    /** Shared i18n instance used by all screens. */
    lateinit var i18n: I18nManager
        private set

    /** Procedural sprite atlas shared by all game screens. */
    lateinit var atlas: SpriteAtlas
        private set

    // ── Screen transition system ─────────────────────────────────

    /**
     * Active transition overlay, or null when no transition is playing.
     * Managed by [startTransition]; rendered in [render].
     */
    var transition: FadeTransition? = null
        private set

    /** Action to execute when the transition reaches the SWITCH phase. */
    private var pendingSwitchAction: (() -> Unit)? = null

    /** Batch used exclusively for rendering the transition overlay. */
    private val transitionBatch = SpriteBatch()

    /** 1×1 white texture used as the overlay fill rect. */
    private var whiteTexture: Texture? = null

    /**
     * Start a fade transition and schedule [switchAction] to execute
     * at the midpoint (SWITCH phase). The action should call
     * [KtxGame.setScreen].
     */
    fun startTransition(switchAction: () -> Unit) {
        // Ignore if already transitioning
        if (transition != null && !transition!!.isComplete) return

        transition = FadeTransition(Constants.TRANSITION_DURATION)
        pendingSwitchAction = switchAction
    }

    // ── Lifecycle ───────────────────────────────────────────────

    override fun create() {
        // Initialise audio (loads preferences, gracefully skips missing assets)
        AudioManager.init()

        // Initialise internationalisation
        i18n = I18nManager()
        i18n.init()

        // Generate procedural sprite atlas (one-time init)
        atlas = SpriteGenerator.generateAtlas()

        // Create 1x1 white texture for transition overlay
        val pix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pix.setColor(Color.WHITE)
        pix.fill()
        whiteTexture = Texture(pix)
        pix.dispose()

        // Screens are created here so they share the same game reference.
        // GameScreen and GameOverScreen access i18n through the game or
        // receive it via constructor injection.
        addScreen(MenuScreen(this, i18n))
        addScreen(GameScreen(this, i18n, atlas))
        addScreen(GameOverScreen(this, i18n))
        addScreen(SettingsScreen(this))
        addScreen(LeaderboardScreen(this, i18n))
        setScreen<MenuScreen>()
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime

        // Update transition before screen render
        transition?.let { t ->
            if (t.update(delta)) {
                // SWITCH phase — execute pending screen switch
                pendingSwitchAction?.invoke()
                pendingSwitchAction = null
            }
        }

        // ── Clear screen behind everything ─────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Render current screen
        super.render()

        // Render transition overlay on top
        transition?.let { t ->
            if (!t.isComplete) {
                val wt = whiteTexture
                if (wt != null) {
                    transitionBatch.begin()
                    t.render(transitionBatch, wt)
                    transitionBatch.end()
                }
            } else {
                transition = null // clean up
            }
        }
    }

    override fun dispose() {
        AudioManager.dispose()
        atlas.dispose()
        whiteTexture?.dispose()
        transitionBatch.dispose()
        super.dispose()
    }
}
