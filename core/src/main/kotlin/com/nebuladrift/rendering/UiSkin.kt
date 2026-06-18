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

/**
 * Creates a Scene2D [Skin] with fonts from [FontManager], label styles,
 * button styles, and slider styles — all with a clean, modern look.
 *
 * Use [UiSkin.instance] to get the shared skin after [FontManager.init]
 * has been called.
 */
object UiSkin {

    private var _skin: Skin? = null

    val instance: Skin
        get() {
            if (_skin == null) build()
            return _skin!!
        }

    val isBuilt: Boolean get() = _skin != null

    private fun build() {
        val skin = Skin()

        // ── Register fonts ──────────────────────────────────────
        skin.add("title", FontManager.title())
        skin.add("heading", FontManager.heading())
        skin.add("body", FontManager.body())
        skin.add("small", FontManager.small())

        // ── Colour drawables (1×1 tinted textures) ──────────────
        fun colorDrawable(r: Float, g: Float, b: Float, a: Float = 1f): Drawable {
            val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pm.setColor(r, g, b, a)
            pm.fill()
            val tex = Texture(pm)
            pm.dispose()
            val d = TextureRegionDrawable(TextureRegion(tex))
            skin.add("_tex_${r}_${g}_${b}", tex) // keep reference for disposal
            return d
        }

        val white = colorDrawable(1f, 1f, 1f)
        val blue800 = colorDrawable(0.15f, 0.35f, 0.6f)   // default button bg
        val blue600 = colorDrawable(0.1f, 0.25f, 0.5f)    // pressed
        val blue900 = colorDrawable(0.2f, 0.45f, 0.7f)    // hover
        val darkBg = colorDrawable(0.2f, 0.2f, 0.25f)     // slider track
        val lightKnob = colorDrawable(0.8f, 0.8f, 0.9f)   // slider knob
        val goldBg = colorDrawable(0.8f, 0.6f, 0f, 0.25f) // new-record banner

        skin.add("white", white)

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
        skin.add("default", TextButtonStyle(blue800, blue600, blue900, skin.getFont("body")))
        skin.add("green", TextButtonStyle(
            colorDrawable(0.15f, 0.55f, 0.25f),
            colorDrawable(0.1f, 0.4f, 0.2f),
            colorDrawable(0.2f, 0.65f, 0.3f),
            skin.getFont("body")
        ))
        skin.add("small-btn", TextButtonStyle(blue800, blue600, blue900, skin.getFont("small")))

        // ── Slider knob (12×12) ─────────────────────────────────
        val knobPm = Pixmap(12, 12, Pixmap.Format.RGBA8888)
        knobPm.setColor(0.8f, 0.8f, 0.9f, 1f)
        knobPm.fill()
        val knobTex = Texture(knobPm)
        knobPm.dispose()
        val knobDrawable = TextureRegionDrawable(TextureRegion(knobTex))
        skin.add("slider-knob", knobTex)
        skin.add("default-horizontal", SliderStyle(darkBg, knobDrawable))

        _skin = skin
    }

    /** Dispose the skin and all its resources. */
    fun dispose() {
        _skin?.dispose()
        _skin = null
    }
}
