package com.nebuladrift.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle

/**
 * Manages internationalised strings via libGDX [I18NBundle].
 *
 * Default locale is **English** (`en`). The bundle files live in
 * `assets/i18n/`:
 * - `messages.properties`        — fallback (Euskera)
 * - `messages_eu.properties`     — Euskera
 * - `messages_es.properties`     — Spanish
 * - `messages_en.properties`     — English
 *
 * Supports runtime locale switching via [setLocale].
 *
 * Usage:
 * ```kotlin
 * val i18n = I18nManager()
 * i18n.init()
 * val title = i18n.get("title")
 * i18n.setLocale("es")  // switch to Spanish at runtime
 * ```
 */
class I18nManager {

    private var bundle: I18NBundle? = null
    private var currentLocale = "en"

    /**
     * Initialise the bundle with the default locale (Euskera).
     * Call once during [com.nebuladrift.NebulaDriftGame.create].
     */
    fun init() {
        loadBundle(currentLocale)
    }

    /**
     * Switch the active locale at runtime.
     *
     * Supported values: `"eu"`, `"es"`, `"en"`. Any other value
     * throws [IllegalArgumentException].
     *
     * The [I18NBundle] is reloaded from disk so that subsequent
     * calls to [get] / [format] return strings in the new locale.
     */
    fun setLocale(locale: String) {
        if (locale !in listOf("eu", "es", "en")) {
            throw IllegalArgumentException("Unsupported locale: $locale")
        }
        currentLocale = locale
        loadBundle(locale)
    }

    /** Return the currently active locale code (`"eu"`, `"es"`, or `"en"`). */
    fun getLocale(): String = currentLocale

    private fun loadBundle(locale: String) {
        val file = Gdx.files.internal("i18n/messages")
        bundle = I18NBundle.createBundle(file, java.util.Locale(locale))
    }

    /**
     * Return the translated string for [key].
     * Falls back to `messages.properties` if the key is missing
     * in the active locale.
     */
    fun get(key: String): String = bundle?.get(key) ?: key

    /**
     * Return the translated string with placeholders filled.
     * @param key   The bundle key
     * @param args  Format arguments (e.g. `get("score_format", 1500)`)
     */
    fun format(key: String, vararg args: Any): String {
        return bundle?.format(key, *args) ?: key
    }
}
