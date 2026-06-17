package com.nebuladrift

import com.nebuladrift.managers.I18nManager
import com.nebuladrift.screens.GameOverScreen
import com.nebuladrift.screens.GameScreen
import com.nebuladrift.screens.MenuScreen
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
 */
class NebulaDriftGame : KtxGame<KtxScreen>() {

    /** Shared i18n instance used by all screens. */
    lateinit var i18n: I18nManager
        private set

    override fun create() {
        // Initialise internationalisation
        i18n = I18nManager()
        i18n.init()

        // Screens are created here so they share the same game reference.
        // GameScreen and GameOverScreen access i18n through the game or
        // receive it via constructor injection.
        addScreen(MenuScreen(this, i18n))
        addScreen(GameScreen(this, i18n))
        addScreen(GameOverScreen(this, i18n))
        setScreen<MenuScreen>()
    }

    override fun dispose() {
        super.dispose()
    }
}
