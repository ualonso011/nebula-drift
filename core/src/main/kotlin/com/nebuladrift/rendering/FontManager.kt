package com.nebuladrift.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

/**
 * Generates and caches smooth anti-aliased fonts using FreeType.
 *
 * All UI fonts are generated at 64px (consistent rendering quality) and
 * then scaled down to fit the 16×9 world-coordinate viewport. The HUD
 * font uses a separate pixel-coordinate camera and is generated at 26px
 * without scaling.
 *
 * | Name      | Gen | Scale  | Eff. wu | Use case                        |
 * |-----------|-----|--------|---------|---------------------------------|
 * | TITLE     | 64  | 0.024  | ~1.08   | "NEBULA DRIFT"                  |
 * | HEADING   | 64  | 0.017  | ~0.77   | "GAME OVER", "SETTINGS"         |
 * | BODY      | 64  | 0.0095 | ~0.43   | Button labels, score text       |
 * | SMALL     | 64  | 0.005  | ~0.23   | Version, hints                  |
 * | HUD       | 26  | 1.0    | 26px    | In-game HUD (pixel coordinates) |
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
                color = Color.WHITE
                borderWidth = 0f
                gamma = 1.8f
                genMipMaps = false
                minFilter = Texture.TextureFilter.Linear
                magFilter = Texture.TextureFilter.Linear
            }

            // UI fonts: generate at 64px (quality baseline), then scale to world units
            titleFont = generateScaledFont(generator, params, 64, 0.024f)
            headingFont = generateScaledFont(generator, params, 64, 0.017f)
            bodyFont = generateScaledFont(generator, params, 64, 0.0095f)
            smallFont = generateScaledFont(generator, params, 64, 0.005f)

            // HUD font: pixel coordinates, no scaling
            hudFont = generateFont(generator, params, 26)

            generator.dispose()
            isInitialized = true
            Gdx.app.log("FontManager", "Fonts initialised successfully (Roboto-Regular, 64px base)")
        } catch (e: Exception) {
            Gdx.app.error("FontManager", "Failed to generate FreeType fonts: ${e.message}", e)
            createFallbackFonts()
        }
    }

    /**
     * Generate a font with a specific FreeType [fontSize] and then scale
     * the rendered glyphs by [scale] to map from font units to world units.
     */
    private fun generateScaledFont(
        generator: FreeTypeFontGenerator,
        baseParams: FreeTypeFontParameter,
        fontSize: Int,
        scale: Float
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
        val font = generator.generateFont(p)
        font.data.setScale(scale)
        return font
    }

    /**
     * Generate a font at [fontSize] with no additional scaling.
     * Used for the HUD font which renders in pixel coordinates.
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
        titleFont = BitmapFont().also { it.data.setScale(0.08f) }
        headingFont = BitmapFont().also { it.data.setScale(0.06f) }
        bodyFont = BitmapFont().also { it.data.setScale(0.035f) }
        smallFont = BitmapFont().also { it.data.setScale(0.02f) }
        hudFont = BitmapFont()
        isInitialized = true
    }

    // ── Accessors ─────────────────────────────────────────────────

    /** 64px font at scale 0.024 (~1.08 world units) for screen titles like "NEBULA DRIFT". */
    fun title(): BitmapFont {
        return titleFont ?: BitmapFont().also { titleFont = it }
    }

    /** 64px font at scale 0.017 (~0.77 world units) for section headings like "GAME OVER". */
    fun heading(): BitmapFont {
        return headingFont ?: BitmapFont().also { headingFont = it }
    }

    /** 64px font at scale 0.0095 (~0.43 world units) for button labels, score values. */
    fun body(): BitmapFont {
        return bodyFont ?: BitmapFont().also { bodyFont = it }
    }

    /** 64px font at scale 0.005 (~0.23 world units) for version info, secondary info. */
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
