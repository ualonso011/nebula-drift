package com.nebuladrift

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val errorTitle = intent.getStringExtra("error_title") ?: "Error desconocido"
        val errorMessage = intent.getStringExtra("error_message") ?: "Sin detalles"
        val stackTrace = intent.getStringExtra("stack_trace") ?: ""
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.BLACK)
        }
        
        val titleView = TextView(this).apply {
            text = "⚠️ $errorTitle"
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        
        val messageView = TextView(this).apply {
            text = errorMessage
            setTextColor(Color.YELLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 16, 0, 16)
        }
        
        val stackView = TextView(this).apply {
            text = stackTrace
            setTextColor(Color.LTGRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.MONOSPACE
        }
        
        val scrollView = ScrollView(this).apply {
            addView(stackView)
        }
        
        val hintView = TextView(this).apply {
            text = "\n💡 Toca para copiar el error"
            setTextColor(Color.GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        
        layout.addView(titleView)
        layout.addView(messageView)
        layout.addView(scrollView)
        layout.addView(hintView)
        
        setContentView(layout)
        
        // Copiar al tocar
        layout.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Crash Report", "$errorTitle\n\n$errorMessage\n\n$stackTrace")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Error copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }
    }
}
