package com.nebuladrift.managers

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [I18nManager] class.
 *
 * Covers:
 * - Locale switching to valid locales (eu, es, en)
 * - Locale switching throws on invalid locale
 * - [getLocale] returns current locale
 * - [get] returns translated text matching the active locale
 */
class I18nManagerTest {

    private lateinit var i18n: I18nManager

    companion object {
        @BeforeAll @JvmStatic
        fun initGdx() {
            val config = HeadlessApplicationConfiguration()
            HeadlessApplication(object : ApplicationAdapter() {}, config)
        }
    }

    @BeforeEach
    fun setUp() {
        i18n = I18nManager()
        i18n.init()
    }

    @AfterEach
    fun tearDown() {
        // Reset to default locale so other tests aren't affected
        if (::i18n.isInitialized) {
            i18n.setLocale("eu")
        }
    }

    @Test
    fun `setLocale changes locale correctly`() {
        i18n.setLocale("es")
        assertEquals("es", i18n.getLocale())

        i18n.setLocale("en")
        assertEquals("en", i18n.getLocale())

        i18n.setLocale("eu")
        assertEquals("eu", i18n.getLocale())
    }

    @Test
    fun `setLocale throws on invalid locale`() {
        assertThrows(IllegalArgumentException::class.java) {
            i18n.setLocale("fr")
        }
    }

    @Test
    fun `getLocale returns current locale`() {
        i18n.setLocale("en")
        assertEquals("en", i18n.getLocale())
    }

    @Test
    fun `get returns translated text for current locale`() {
        i18n.setLocale("en")
        val text = i18n.get("settings")
        assertEquals("Settings", text)

        i18n.setLocale("es")
        val text2 = i18n.get("settings")
        assertEquals("Ajustes", text2)
    }
}
