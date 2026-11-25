package pl.smoqbonus.clicker

import android.app.Activity
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private lateinit var statusLabel: TextView
    private lateinit var statusDot: TextView
    private lateinit var toggleButton: Button
    private lateinit var accessButton: Button

    private val backgroundColor = Color.rgb(10, 13, 20)
    private val cardColor = Color.rgb(20, 26, 37)
    private val mutedColor = Color.rgb(151, 164, 184)
    private val accentColor = Color.rgb(92, 224, 168)
    private val whiteColor = Color.rgb(241, 245, 249)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val brand = TextView(this).apply {
            text = "SMOQ  /  TOOLS"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }
        content.addView(brand, matchWrap())

        val title = TextView(this).apply {
            text = "Bonus Clicker"
            textSize = 30f
            setTextColor(whiteColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        }
        content.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Automatyczne wykrywanie bonusów"
            textSize = 14f
            setTextColor(mutedColor)
        }
        content.addView(subtitle, matchWrap())

        val statusCard = card()
        statusCard.setPadding(dp(18), dp(18), dp(18), dp(18))
        statusCard.addView(TextView(this).apply {
            text = "STATUS AUTOMATU"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(mutedColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }, matchWrap())

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(4))
        }
        statusDot = TextView(this).apply {
            text = "●"
            textSize = 18f
            setPadding(0, 0, dp(10), 0)
        }
        statusLabel = TextView(this).apply {
            textSize = 20f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(whiteColor)
        }
        statusRow.addView(statusDot, wrapWrap())
        statusRow.addView(statusLabel, wrapWrap())
        statusCard.addView(statusRow, matchWrap())

        val statusHint = TextView(this).apply {
            text = "Skaner działa również poza grą. Po powrocie do SMOQ automatycznie wznowi analizę ekranu."
            textSize = 13f
            setTextColor(mutedColor)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        statusCard.addView(statusHint, matchWrap())
        content.addView(statusCard, withTopMargin(matchWrap(), 24))

        val actionCard = card()
        actionCard.setPadding(dp(18), dp(18), dp(18), dp(18))
        actionCard.addView(TextView(this).apply {
            text = "STEROWANIE"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(mutedColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }, matchWrap())

        toggleButton = Button(this).apply {
            isAllCaps = false
            textSize = 16f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.rgb(8, 24, 19))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                prefs.edit().putBoolean("enabled", !prefs.getBoolean("enabled", false)).apply()
                refreshStatus()
            }
        }
        actionCard.addView(toggleButton, withTopMargin(matchWidth(dp(54)), 14))

        accessButton = Button(this).apply {
            text = "Ustawienia dostępności"
            isAllCaps = false
            textSize = 14f
            setTextColor(whiteColor)
            background = rounded(Color.rgb(43, 53, 69), dp(14))
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        actionCard.addView(accessButton, withTopMargin(matchWidth(dp(48)), 10))
        content.addView(actionCard, withTopMargin(matchWrap(), 14))

        val infoCard = card()
        infoCard.setPadding(dp(18), dp(18), dp(18), dp(18))
        infoCard.addView(TextView(this).apply {
            text = "JAK TO DZIAŁA"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(mutedColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }, matchWrap())
        infoCard.addView(infoText("01", "Włącz usługę dostępności", "Android musi zezwolić aplikacji na analizę ekranu."), withTopMargin(matchWrap(), 14))
        infoCard.addView(infoText("02", "Uruchom automat", "Przycisk START włącza wykrywanie bonusów."), withTopMargin(matchWrap(), 12))
        infoCard.addView(infoText("03", "Otwórz SMOQ Games 27", "Aplikacja szuka ekranu BONUS i klika wyłącznie OK."), withTopMargin(matchWrap(), 12))
        content.addView(infoCard, withTopMargin(matchWrap(), 14))

        val footer = TextView(this).apply {
            text = "SMOQ BONUS CLICKER  •  v1.1"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(91, 103, 121))
            setPadding(0, dp(22), 0, dp(4))
        }
        content.addView(footer, matchWrap())

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusLabel.isInitialized) refreshStatus()
    }

    private fun refreshStatus() {
        val enabled = prefs.getBoolean("enabled", false)
        val accessibilityOn = isAccessibilityServiceEnabled()
        statusLabel.text = when {
            !accessibilityOn -> "WYMAGANA DOSTĘPNOŚĆ"
            enabled -> "AUTOMAT WŁĄCZONY"
            else -> "AUTOMAT WYŁĄCZONY"
        }
        statusDot.setTextColor(if (accessibilityOn && enabled) accentColor else Color.rgb(245, 180, 90))
        toggleButton.text = if (enabled) "ZATRZYMAJ AUTOMAT" else "URUCHOM AUTOMAT"
        toggleButton.background = rounded(if (enabled) Color.rgb(245, 180, 90) else accentColor, dp(14))
        accessButton.text = if (accessibilityOn) "Dostępność: włączona  ·  Ustawienia" else "Włącz usługę dostępności"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(this, BonusAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val serviceInfo = info.resolveInfo.serviceInfo
                serviceInfo.packageName == expected.packageName &&
                    serviceInfo.name == expected.className
            }
    }

    private fun infoText(number: String, heading: String, detail: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val numberView = TextView(this).apply {
            text = number
            textSize = 12f
            setTextColor(accentColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(2), dp(12), 0)
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = heading
            textSize = 14f
            setTextColor(whiteColor)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }, matchWrap())
        texts.addView(TextView(this).apply {
            text = detail
            textSize = 12f
            setTextColor(mutedColor)
            setPadding(0, dp(3), 0, 0)
        }, matchWrap())
        row.addView(numberView, wrapWrap())
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(cardColor, dp(18))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun wrapWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun matchWidth(height: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    private fun withTopMargin(params: LinearLayout.LayoutParams, margin: Int) = params.apply { topMargin = dp(margin) }
}
