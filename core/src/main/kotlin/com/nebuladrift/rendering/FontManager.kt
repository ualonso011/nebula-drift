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
 * Fonts are generated at their intended display size (pixel-perfect).
 * No scaling is applied — [UiSkin] and Scene2D [Stage] handle layout
 * via a [com.badlogic.gdx.utils.viewport.FitViewport] so that text
 * is always crisp and resolution-independent.
 *
 * | Name      | Size | Use case                        |
 * |-----------|------|---------------------------------|
 * | TITLE     | 64   | "NEBULA DRIFT"                  |
 * | HEADING   | 48   | "GAME OVER", "SETTINGS"         |
 * | BODY      | 28   | Button labels, score text       |
 * | SMALL     | 18   | Version, hints                  |
 * | HUD       | 26   | In-game HUD (pixel coordinates) |
 *
 * Call [init] once during [com.nebuladrift.NebulaDriftGame.create()].
 */
object FontManager {

    private var titleFont: BitmapFont? = null
    private var headingFont: BitmapFont? = null
    private var bodyFont: BitmapFont? = null
    private var smallFont: BitmapFont? = null
    private var hudFont: BitmapFont? = null
    private var hudScoreFont: BitmapFont? = null

    var isInitialized: Boolean = false
        private set

    fun init() {
        val fontFile = Gdx.files.internal("fonts/Roboto-Regular.ttf")
        if (!fontFile.exists()) {
            Gdx.app.error("FontManager", "Roboto-Regular.ttf not found — falling back to default BitmapFont")
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

            titleFont = generateFont(generator, params, 64)
            headingFont = generateFont(generator, params, 48)
            bodyFont = generateFont(generator, params, 28)
            smallFont = generateFont(generator, params, 18)
            hudFont = generateFont(generator, params, 26)
            hudScoreFont = generateFont(generator, params, 152) // 4x the original 38

            generator.dispose()
            isInitialized = true
            Gdx.app.log("FontManager", "Fonts initialised successfully (Roboto-Regular)")
        } catch (e: Exception) {
            Gdx.app.error("FontManager", "Failed to generate FreeType fonts: ${e.message}", e)
            createFallbackFonts()
        }
    }

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
        titleFont = BitmapFont()
        headingFont = BitmapFont()
        bodyFont = BitmapFont()
        smallFont = BitmapFont()
        hudFont = BitmapFont()
        hudScoreFont = BitmapFont()
        isInitialized = true
    }

    fun title(): BitmapFont = titleFont ?: BitmapFont().also { titleFont = it }
    fun heading(): BitmapFont = headingFont ?: BitmapFont().also { headingFont = it }
    fun body(): BitmapFont = bodyFont ?: BitmapFont().also { bodyFont = it }
    fun small(): BitmapFont = smallFont ?: BitmapFont().also { smallFont = it }
    fun hud(): BitmapFont = hudFont ?: BitmapFont().also { hudFont = it }
    fun hudScore(): BitmapFont = hudScoreFont ?: BitmapFont().also { hudScoreFont = it }

    fun dispose() {
        val disposed = mutableSetOf<BitmapFont>()
        listOf(titleFont, headingFont, bodyFont, smallFont, hudFont, hudScoreFont).forEach { font ->
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
        hudScoreFont = null
        isInitialized = false
    }
}
