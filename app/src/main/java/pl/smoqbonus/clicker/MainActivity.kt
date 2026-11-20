package pl.smoqbonus.clicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val status = TextView(this).apply {
            textSize = 17f
            text = "SMOQ Bonus Clicker\\n\\n1. Włącz usługę dostępności.\\n2. Włącz automat.\\n3. Otwórz SMOQ Games 27."
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }
        val accessibility = Button(this).apply {
            text = "Otwórz ustawienia dostępności"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val toggle = Button(this)
        fun refresh() {
            val enabled = prefs.getBoolean("enabled", false)
            toggle.text = if (enabled) "STOP — automat włączony" else "START — automat wyłączony"
            status.text = "SMOQ Bonus Clicker\\n\\nAutomat: ${if (enabled) "WŁĄCZONY" else "WYŁĄCZONY"}\\n\\nWykrywa ekran BONUS i klika tylko rozpoznany napis OK. Nie klika przycisku z kartami."
        }
        toggle.setOnClickListener {
            prefs.edit().putBoolean("enabled", !prefs.getBoolean("enabled", false)).apply()
            refresh()
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            addView(status)
            addView(accessibility)
            addView(toggle)
        }
        setContentView(layout)
        refresh()
    }
}
