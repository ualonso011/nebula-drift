package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TintedDrawable

/**
 * Creates a Scene2D [Skin] with fonts from [FontManager], label styles,
 * button styles, and slider styles — all with a clean, modern look.
 *
 * Use [UiSkin.instance] to get the shared skin after [FontManager.init]
 * has been called.
 */
object UiSkin {

    private var _skin: Skin? = null

    /** The shared Scene2D skin. Created lazily; requires [FontManager] initialised. */
    val instance: Skin
        get() {
            if (_skin == null) build()
            return _skin!!
        }

    /** True after [instance] has been accessed at least once. */
    val isBuilt: Boolean get() = _skin != null

    private fun build() {
        val skin = Skin()

        // ── Register fonts ──────────────────────────────────────
        skin.add("title", FontManager.title())
        skin.add("heading", FontManager.heading())
        skin.add("body", FontManager.body())
        skin.add("small", FontManager.small())

        // ── White-pixel drawable (base for all tinted shapes) ────
        val whitePx: Drawable = TextureRegionDrawable(TextureRegion(pixelTexture(Color.WHITE)))
        skin.add("white", whitePx)

        // ── Label styles ────────────────────────────────────────
        skin.add("title-white", LabelStyle(skin.getFont("title"), Color.WHITE))
        skin.add("heading-white", LabelStyle(skin.getFont("heading"), Color.WHITE))
        skin.add("body-white", LabelStyle(skin.getFont("body"), Color.WHITE))
        skin.add("body-gold", LabelStyle(skin.getFont("body"), Color(0.8f, 0.6f, 0f, 1f)))
        skin.add("body-gray", LabelStyle(skin.getFont("body"), Color(0.7f, 0.7f, 0.7f, 1f)))
        skin.add("body-red", LabelStyle(skin.getFont("body"), Color.RED))
        skin.add("small-gray", LabelStyle(skin.getFont("small"), Color(0.6f, 0.6f, 0.6f, 1f)))
        skin.add("small-gold", LabelStyle(skin.getFont("small"), Color(0.8f, 0.6f, 0f, 1f)))

        // ── TextButton styles ───────────────────────────────────
        val blueUp = TintedDrawable(whitePx, Color(0.15f, 0.35f, 0.6f, 1f))
        val blueDown = TintedDrawable(whitePx, Color(0.1f, 0.25f, 0.5f, 1f))
        val blueOver = TintedDrawable(whitePx, Color(0.2f, 0.45f, 0.7f, 1f))
        val greenUp = TintedDrawable(whitePx, Color(0.15f, 0.55f, 0.25f, 1f))
        val greenDown = TintedDrawable(whitePx, Color(0.1f, 0.4f, 0.2f, 1f))
        val greenOver = TintedDrawable(whitePx, Color(0.2f, 0.65f, 0.3f, 1f))

        skin.add("default", TextButtonStyle(blueUp, blueDown, blueOver, skin.getFont("body")))
        skin.add("green", TextButtonStyle(greenUp, greenDown, greenOver, skin.getFont("body")))
        skin.add("small-btn", TextButtonStyle(blueUp, blueDown, blueOver, skin.getFont("small")))

        // ── Slider styles ───────────────────────────────────────
        val sliderKnobTex = pixelTexture(Color.WHITE, 12, 12)
        skin.add("slider-knob", TintedDrawable(TextureRegionDrawable(TextureRegion(sliderKnobTex)), Color(0.8f, 0.8f, 0.9f, 1f)))

        val sliderBg = TintedDrawable(whitePx, Color(0.2f, 0.2f, 0.25f, 1f))
        sliderBg.minHeight = 8f
        sliderBg.minWidth = 100f
        skin.add("default-horizontal", SliderStyle(sliderBg, skin.getDrawable("slider-knob")))

        _skin = skin
    }

    /** Create a [width]×[height] RGBA8888 texture of the given [color]. */
    private fun pixelTexture(color: Color, width: Int = 1, height: Int = 1): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }

    /** Dispose the skin and all its resources. */
    fun dispose() {
        _skin?.dispose()
        _skin = null
    }
}
