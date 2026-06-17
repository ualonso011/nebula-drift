@file:JvmName("DesktopLauncher")

package com.nebuladrift

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/** Desktop entry point — launches the game in a resizable 1280×720 window. */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Nebula Drift")
        setWindowedMode(1280, 720)
        setForegroundFPS(60)
        setResizable(true)
    }
    Lwjgl3Application(NebulaDriftGame(), config)
}
