package pl.smoqbonus.clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class BonusAccessibilityService : AccessibilityService() {
    private var busy = false
    private var lastTap = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val scanner = object : Runnable {
        override fun run() {
            if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false)) {
                scanScreen()
            }
            handler.postDelayed(this, 1500L)
        }
    }

    override fun onServiceConnected() {
        handler.post(scanner)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Gry często rysują UI jako canvas i nie zgłaszają zmian widoku.
        // Dlatego skanowanie odbywa się okresowo.
    }

    private fun scanScreen() {
        if (Build.VERSION.SDK_INT < 30 || busy) return
        busy = true

        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val hardware = screenshot.hardwareBuffer
                        val hwBitmap = Bitmap.wrapHardwareBuffer(hardware, screenshot.colorSpace)
                        hardware.close()
                        if (hwBitmap == null) {
                            busy = false
                            return
                        }
                        val bitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        hwBitmap.recycle()
                        val image = InputImage.fromBitmap(bitmap, 0)
                        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            .process(image)
                            .addOnSuccessListener { result -> handleText(result, bitmap.width, bitmap.height) }
                            .addOnFailureListener { }
                            .addOnCompleteListener {
                                bitmap.recycle()
                                busy = false
                            }
                    } catch (_: Exception) {
                        busy = false
                    }
                }

                override fun onFailure(errorCode: Int) {
                    busy = false
                }
            })
    }

    private fun handleText(result: Text, screenWidth: Int, screenHeight: Int) {
        val allText = result.text.uppercase()
        // Warunek BONUS chroni przed klikaniem zwykłych przycisków OK w innych aplikacjach.
        if (!allText.contains("BONUS")) return

        val okElement = result.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .firstOrNull { it.text.trim().equals("OK", ignoreCase = true) }
            ?: return

        val box = okElement.boundingBox ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < 2500L) return

        val x = box.exactCenterX()
        val y = box.exactCenterY()
        if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) return

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()

        if (dispatchGesture(gesture, null, null)) lastTap = now
    }

    override fun onInterrupt() {
        busy = false
        handler.removeCallbacks(scanner)
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanner)
        super.onDestroy()
    }
}
