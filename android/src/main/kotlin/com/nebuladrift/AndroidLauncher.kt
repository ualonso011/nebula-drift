package com.nebuladrift

import android.os.Bundle
import android.util.Log
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    
    companion object {
        private const val TAG = "AndroidLauncher"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "=== onCreate INICIO ===")
        super.onCreate(savedInstanceState)
        
        // Instalar crash handler
        CrashHandler.install(this)
        Log.d(TAG, "CrashHandler instalado")
        
        try {
            Log.d(TAG, "Creando configuración...")
            val config = AndroidApplicationConfiguration().apply {
                useGyroscope = false
                useAccelerometer = false
                useCompass = false
            }
            Log.d(TAG, "Configuración creada")
            
            Log.d(TAG, "Creando NebulaDriftGame...")
            val game = NebulaDriftGame()
            Log.d(TAG, "NebulaDriftGame creado")
            
            Log.d(TAG, "Inicializando libGDX...")
            initialize(game, config)
            Log.d(TAG, "=== onCreate FIN (libGDX inicializado) ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en onCreate", e)
            throw e
        }
    }
    
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }
    
    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }
}
