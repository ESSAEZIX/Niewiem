package pl.smoqbonus.clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class BonusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BonusClicker"
        private const val SCAN_INTERVAL_MS = 1000L
        private const val TAP_COOLDOWN_MS = 2500L
    }

    private var busy = false
    private var lastTap = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val scanner = object : Runnable {
        override fun run() {
            if (getSharedPreferences("settings", MODE_PRIVATE)
                    .getBoolean("enabled", false)
            ) {
                scanScreen()
            }
            handler.postDelayed(this, SCAN_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        handler.removeCallbacks(scanner)
        handler.post(scanner)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Gra renderujące UI jako canvas mogą nie wysyłać zdarzeń widoku.
        // Dlatego ekran jest skanowany okresowo.
    }

    private fun scanScreen() {
        if (Build.VERSION.SDK_INT < 30 || busy) return

        busy = true
        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        var bitmap: Bitmap? = null
                        try {
                            val hardwareBuffer = screenshot.hardwareBuffer
                            try {
                                val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                                    hardwareBuffer,
                                    screenshot.colorSpace
                                )
                                if (hardwareBitmap != null) {
                                    try {
                                        bitmap = hardwareBitmap.copy(
                                            Bitmap.Config.ARGB_8888,
                                            false
                                        )
                                    } finally {
                                        hardwareBitmap.recycle()
                                    }
                                }
                            } finally {
                                // Bufor zamykamy dopiero po skopiowaniu obrazu.
                                hardwareBuffer.close()
                            }

                            val capturedBitmap = bitmap
                            if (capturedBitmap == null) {
                                Log.e(TAG, "Screenshot bitmap is null")
                                busy = false
                                return
                            }

                            Log.d(
                                TAG,
                                "Screenshot captured: ${capturedBitmap.width}x${capturedBitmap.height}"
                            )

                            val image = InputImage.fromBitmap(capturedBitmap, 0)
                            recognizer.process(image)
                                .addOnSuccessListener { result ->
                                    Log.d(TAG, "OCR result: ${result.text}")
                                    handleText(
                                        result,
                                        capturedBitmap.width,
                                        capturedBitmap.height
                                    )
                                }
                                .addOnFailureListener { error ->
                                    Log.e(TAG, "ML Kit OCR failed", error)
                                }
                                .addOnCompleteListener {
                                    capturedBitmap.recycle()
                                    busy = false
                                }
                        } catch (error: Exception) {
                            bitmap?.recycle()
                            Log.e(TAG, "Screenshot processing error", error)
                            busy = false
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "takeScreenshot failed, errorCode=$errorCode")
                        busy = false
                    }
                }
            )
        } catch (error: Exception) {
            Log.e(TAG, "Could not request screenshot", error)
            busy = false
        }
    }

    private fun normalize(value: String): String {
        return value.trim()
            .uppercase()
            .replace('0', 'O')
            .filter { it.isLetterOrDigit() }
    }

    private fun handleText(result: Text, screenWidth: Int, screenHeight: Int) {
        val fullText = normalize(result.text)

        // Nie klikaj niczego, jeśli nie rozpoznano okna bonusu.
        if (!fullText.contains("BONUS")) {
            Log.d(TAG, "BONUS not found")
            return
        }

        // Szukamy dokładnego elementu OK, a nie tekstu zawierającego OK.
        // Obsługa 0K uwzględnia częstą pomyłkę OCR między O i 0.
        val okElement = result.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .firstOrNull { normalize(it.text) == "OK" }

        if (okElement == null) {
            Log.d(TAG, "BONUS found, but exact OK element was not found")
            return
        }

        val box = okElement.boundingBox ?: run {
            Log.d(TAG, "OK element has no bounding box")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < TAP_COOLDOWN_MS) return

        val x = box.exactCenterX()
        val y = box.exactCenterY()

        if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) {
            Log.w(TAG, "OK coordinates outside screenshot: x=$x y=$y")
            return
        }

        Log.i(TAG, "BONUS and OK found at x=$x y=$y; dispatching tap")

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        if (dispatchGesture(gesture, null, null)) {
            lastTap = now
            Log.i(TAG, "Tap dispatched")
        } else {
            Log.e(TAG, "dispatchGesture returned false")
        }
    }

    override fun onInterrupt() {
        busy = false
        handler.removeCallbacks(scanner)
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanner)
        recognizer.close()
        super.onDestroy()
    }
}
