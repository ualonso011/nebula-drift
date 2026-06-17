package com.nebuladrift.rendering

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.nebuladrift.util.Constants

/**
 * Manages frame-based animations for game entities.
 *
 * Wraps libGDX [Animation] instances keyed by animation name.
 * Animation frames are loaded from the procedural [SpriteAtlas].
 *
 * ## Registered animations
 * - **thrust**: 2-frame loop at ~8 FPS for ship engine flame
 * - **explosion_medium**: 6-frame once-through for medium explosions
 *   (can be extended for small/large as needed)
 */
class AnimationManager(private val atlas: SpriteAtlas) {

    private val animations = mutableMapOf<String, Animation<TextureRegion>>()

    /**
     * Initialise all animation sequences from the atlas.
     * Must be called once after [atlas] is fully populated.
     */
    fun init() {
        // Thrust: 2-frame flicker, looping
        val thrustFrames = Array<TextureRegion>(2)
        thrustFrames.add(atlas.findRegion("thrust_0"))
        thrustFrames.add(atlas.findRegion("thrust_1"))
        val thrustAnim = Animation(Constants.THRUST_FRAME_DURATION, thrustFrames)
        thrustAnim.playMode = Animation.PlayMode.LOOP
        animations["thrust"] = thrustAnim

        // Explosion (medium): 6 frames, play once
        val explosionFrames = Array<TextureRegion>(Constants.SPRITE_EXPLOSION_FRAMES)
        for (i in 0 until Constants.SPRITE_EXPLOSION_FRAMES) {
            explosionFrames.add(atlas.findRegion("explosion_medium_$i"))
        }
        val frameDuration = Constants.EXPLOSION_DURATION / Constants.SPRITE_EXPLOSION_FRAMES
        val explosionAnim = Animation(frameDuration, explosionFrames)
        explosionAnim.playMode = Animation.PlayMode.NORMAL
        animations["explosion"] = explosionAnim
    }

    /**
     * Get the key frame for [animationName] at [stateTime].
     * Returns null if the animation name is not registered.
     */
    fun getKeyFrame(animationName: String, stateTime: Float): TextureRegion? {
        return animations[animationName]?.getKeyFrame(stateTime)
    }

    /**
     * Check whether a named animation has finished playing at [stateTime].
     * Only meaningful for non-looping animations (PlayMode.NORMAL).
     * Returns true if the animation name is not registered.
     */
    fun isAnimationFinished(animationName: String, stateTime: Float): Boolean {
        val anim = animations[animationName] ?: return true
        return anim.isAnimationFinished(stateTime)
    }

    /** Remove all registered animations. */
    fun clear() {
        animations.clear()
    }
}
