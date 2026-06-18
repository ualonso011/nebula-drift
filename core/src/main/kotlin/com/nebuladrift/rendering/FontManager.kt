package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

/**
 * Generates and caches smooth anti-aliased fonts using FreeType.
 *
 * Font sizes are chosen for readability on mobile screens (world-coordinate
 * UI in 16:9 viewport):
 *
 * | Name      | Pixel size | Use case                          |
 * |-----------|-----------|-----------------------------------|
 * | TITLE     | 120       | "NEBULA DRIFT"                    |
 * | HEADING   | 72        | "GAME OVER", "SETTINGS"           |
 * | BODY      | 48        | Button labels, score text         |
 * | SMALL     | 32        | Version, hints                    |
 * | HUD       | 26        | In-game HUD (pixel coordinates)   |
 *
 * Call [init] once during [com.nebuladrift.NebulaDriftGame.create()].
 */
object FontManager {

    private var titleFont: BitmapFont? = null
    private var headingFont: BitmapFont? = null
    private var bodyFont: BitmapFont? = null
    private var smallFont: BitmapFont? = null
    private var hudFont: BitmapFont? = null

    /** Whether [init] has been called successfully. */
    var isInitialized: Boolean = false
        private set

    /**
     * Initialise all fonts from the Roboto TTF bundled in assets/fonts/.
     * Must be called after the GL context is ready (inside [com.nebuladrift.NebulaDriftGame.create]).
     */
    fun init() {
        val fontFile = Gdx.files.internal("fonts/Roboto-Regular.ttf")
        if (!fontFile.exists()) {
            Gdx.app.error("FontManager", "Roboto-Regular.ttf not found in assets/fonts/ — falling back to default BitmapFont")
            createFallbackFonts()
            return
        }

        try {
            val generator = FreeTypeFontGenerator(fontFile)
            val params = FreeTypeFontParameter().apply {
                color = com.badlogic.gdx.graphics.Color.WHITE
                borderWidth = 0f
                gamma = 1.8f
                genMipMaps = false
                minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
                magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
            }

            titleFont = generateFont(generator, params, 120)
            headingFont = generateFont(generator, params, 72)
            bodyFont = generateFont(generator, params, 48)
            smallFont = generateFont(generator, params, 32)
            hudFont = generateFont(generator, params, 26)

            generator.dispose()
            isInitialized = true
            Gdx.app.log("FontManager", "Fonts initialised successfully (Roboto-Regular)")
        } catch (e: Exception) {
            Gdx.app.error("FontManager", "Failed to generate FreeType fonts: ${e.message}", e)
            createFallbackFonts()
        }
    }

    /**
     * Generate a single font size, creating a copy of [params] so that
     * each font can have unique settings if needed (e.g. different colour).
     */
    private fun generateFont(
        generator: FreeTypeFontGenerator,
        baseParams: FreeTypeFontParameter,
        fontSize: Int
    ): BitmapFont {
        val p = FreeTypeFontParameter().apply {
            size = fontSize
            color = baseParams.color
            borderWidth = baseParams.borderWidth
            gamma = baseParams.gamma
            genMipMaps = baseParams.genMipMaps
            minFilter = baseParams.minFilter
            magFilter = baseParams.magFilter
        }
        return generator.generateFont(p)
    }

    private fun createFallbackFonts() {
        val default = BitmapFont()
        titleFont = default
        headingFont = default
        bodyFont = default
        smallFont = default
        hudFont = default
        isInitialized = true
    }

    // ── Accessors ─────────────────────────────────────────────────

    /** 120px font for screen titles like "NEBULA DRIFT". */
    fun title(): BitmapFont {
        return titleFont ?: BitmapFont().also { titleFont = it }
    }

    /** 72px font for section headings like "GAME OVER". */
    fun heading(): BitmapFont {
        return headingFont ?: BitmapFont().also { headingFont = it }
    }

    /** 48px font for button labels, score values. */
    fun body(): BitmapFont {
        return bodyFont ?: BitmapFont().also { bodyFont = it }
    }

    /** 32px font for version info, secondary info. */
    fun small(): BitmapFont {
        return smallFont ?: BitmapFont().also { smallFont = it }
    }

    /** 26px font for in-game HUD (rendered in pixel coordinates). */
    fun hud(): BitmapFont {
        return hudFont ?: BitmapFont().also { hudFont = it }
    }

    // ── Cleanup ────────────────────────────────────────────────────

    /** Dispose all generated fonts. Call from [com.nebuladrift.NebulaDriftGame.dispose]. */
    fun dispose() {
        // Avoid disposing the same reference twice if fallback was used
        val disposed = mutableSetOf<BitmapFont>()
        listOf(titleFont, headingFont, bodyFont, smallFont, hudFont).forEach { font ->
            if (font != null && font !in disposed) {
                font.dispose()
                disposed.add(font)
            }
        }
        titleFont = null
        headingFont = null
        bodyFont = null
        smallFont = null
        hudFont = null
        isInitialized = false
    }
}
