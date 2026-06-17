package com.nebuladrift

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    companion object {
        private const val TAG = "CrashHandler"
        
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
            Log.d(TAG, "CrashHandler instalado")
        }
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "CRASH DETECTADO", throwable)
        
        try {
            val stackTrace = StringWriter().apply {
                throwable.printStackTrace(PrintWriter(this))
            }.toString()
            
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("error_title", throwable.javaClass.simpleName)
                putExtra("error_message", throwable.message ?: "Sin mensaje")
                putExtra("stack_trace", stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar CrashActivity", e)
        }
        
        // Terminar el proceso
        exitProcess(1)
    }
}
