package com.nebuladrift.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import java.util.Locale

/**
 * Manages internationalised strings via libGDX [I18NBundle].
 *
 * Default locale is **Euskera** (`eu`). The bundle files live in
 * `assets/i18n/`:
 * - `messages.properties`        — fallback (Euskera)
 * - `messages_eu.properties`     — Euskera
 * - `messages_es.properties`     — Spanish
 * - `messages_en.properties`     — English
 *
 * Usage:
 * ```kotlin
 * val i18n = I18nManager()
 * i18n.init()
 * val title = i18n.get("title")
 * ```
 */
class I18nManager {

    private lateinit var bundle: I18NBundle

    /**
     * Initialise the bundle with the default locale (Euskera).
     * Call once during [com.nebuladrift.NebulaDriftGame.create].
     */
    fun init(locale: Locale = Locale.Builder().setLanguage("eu").setRegion("ES").build()) {
        val base = Gdx.files.internal("i18n/messages")
        bundle = I18NBundle.createBundle(base, locale)
    }

    /**
     * Return the translated string for [key].
     * Falls back to `messages.properties` if the key is missing
     * in the active locale.
     */
    fun get(key: String): String {
        return bundle[key]
    }

    /**
     * Return the translated string with placeholders filled.
     * @param key   The bundle key
     * @param args  Format arguments (e.g. `get("score_format", 1500)`)
     */
    fun format(key: String, vararg args: Any): String {
        return bundle.format(key, *args)
    }
}
