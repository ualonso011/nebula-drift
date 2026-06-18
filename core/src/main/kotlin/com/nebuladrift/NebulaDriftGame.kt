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
import com.nebuladrift.rendering.FontManager
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
open class NebulaDriftGame : KtxGame<KtxScreen>() {

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
    private lateinit var transitionBatch: SpriteBatch

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
        Gdx.app.log("NebulaDriftGame", "=== create() INICIO ===")
        
        try {
            // Initialise audio (loads preferences, gracefully skips missing assets)
            Gdx.app.log("NebulaDriftGame", "Inicializando AudioManager...")
            AudioManager.init()
            Gdx.app.log("NebulaDriftGame", "AudioManager inicializado")

            // Initialise internationalisation
            Gdx.app.log("NebulaDriftGame", "Inicializando I18nManager...")
            i18n = I18nManager()
            i18n.init()
            Gdx.app.log("NebulaDriftGame", "I18nManager inicializado")

            // Generate procedural sprite atlas (one-time init)
            Gdx.app.log("NebulaDriftGame", "Generando SpriteAtlas...")
            atlas = SpriteGenerator.generateAtlas()
            Gdx.app.log("NebulaDriftGame", "SpriteAtlas generado")

            // Initialise smooth font system (FreeType)
            Gdx.app.log("NebulaDriftGame", "Inicializando FontManager...")
            FontManager.init()
            Gdx.app.log("NebulaDriftGame", "FontManager inicializado")

            // Create 1x1 white texture for transition overlay
            Gdx.app.log("NebulaDriftGame", "Creando textura blanca...")
            val pix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pix.setColor(Color.WHITE)
            pix.fill()
            whiteTexture = Texture(pix)
            pix.dispose()
            Gdx.app.log("NebulaDriftGame", "Textura blanca creada")

            // Create transition batch (must be done after GL context is ready)
            Gdx.app.log("NebulaDriftGame", "Creando transitionBatch...")
            transitionBatch = SpriteBatch()
            Gdx.app.log("NebulaDriftGame", "transitionBatch creado")

            // Screens are created here so they share the same game reference.
            // GameScreen and GameOverScreen access i18n through the game or
            // receive it via constructor injection.
            Gdx.app.log("NebulaDriftGame", "Creando screens...")
            addScreen(MenuScreen(this, i18n))
            Gdx.app.log("NebulaDriftGame", "MenuScreen creado")
            addScreen(GameScreen(this, i18n, atlas))
            Gdx.app.log("NebulaDriftGame", "GameScreen creado")
            addScreen(GameOverScreen(this, i18n))
            Gdx.app.log("NebulaDriftGame", "GameOverScreen creado")
            addScreen(SettingsScreen(this))
            Gdx.app.log("NebulaDriftGame", "SettingsScreen creado")
            addScreen(LeaderboardScreen(this, i18n))
            Gdx.app.log("NebulaDriftGame", "LeaderboardScreen creado")
            
            Gdx.app.log("NebulaDriftGame", "Estableciendo MenuScreen...")
            setScreen<MenuScreen>()
            
            Gdx.app.log("NebulaDriftGame", "=== create() FIN ===")
            
        } catch (e: Exception) {
            Gdx.app.error("NebulaDriftGame", "ERROR en create()", e)
            throw e
        }
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
        FontManager.dispose()
        whiteTexture?.dispose()
        transitionBatch.dispose()
        super.dispose()
    }
}
