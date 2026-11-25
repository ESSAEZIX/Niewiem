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
        private const val SCAN_TIMEOUT_MS = 12000L
        private const val INTERRUPT_RETRY_MS = 1500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var activeScanId = 0L
    private var lastTap = 0L
    private var destroyed = false

    private val recognizerDelegate = lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val recognizer by recognizerDelegate

    private val scanWatchdog = Runnable {
        if (busy) {
            Log.w(TAG, "Scan watchdog: releasing a stuck scan")
            busy = false
            activeScanId++
        }
    }

    private val scanner = object : Runnable {
        override fun run() {
            if (destroyed) return

            if (isAutomationEnabled()) {
                scanScreen()
            }

            // Keep the loop alive even when the game is closed or the screen changes.
            handler.removeCallbacks(this)
            handler.postDelayed(this, SCAN_INTERVAL_MS)
        }
    }

    private fun isAutomationEnabled(): Boolean =
        getSharedPreferences("settings", MODE_PRIVATE).getBoolean("enabled", false)

    private fun ensureScannerScheduled(delayMs: Long = 0L) {
        if (destroyed) return
        handler.removeCallbacks(scanner)
        handler.postDelayed(scanner, delayMs)
    }

    private fun finishScan(scanId: Long) {
        if (scanId != activeScanId) return
        handler.removeCallbacks(scanWatchdog)
        busy = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        destroyed = false
        Log.i(TAG, "Accessibility service connected")
        ensureScannerScheduled()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Some games draw their interface on a canvas and emit few/no useful view events.
        // Events are only an extra opportunity to recover the periodic scanner.
        if (!destroyed && !handler.hasCallbacks(scanner)) {
            ensureScannerScheduled(250L)
        }
    }

    private fun scanScreen() {
        if (Build.VERSION.SDK_INT < 30 || busy || destroyed) return

        busy = true
        val scanId = ++activeScanId
        handler.removeCallbacks(scanWatchdog)
        handler.postDelayed(scanWatchdog, SCAN_TIMEOUT_MS)

        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        if (scanId != activeScanId || destroyed) {
                            screenshot.hardwareBuffer.close()
                            return
                        }

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
                                        bitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    } finally {
                                        hardwareBitmap.recycle()
                                    }
                                }
                            } finally {
                                hardwareBuffer.close()
                            }

                            val capturedBitmap = bitmap
                            if (capturedBitmap == null) {
                                Log.w(TAG, "Screenshot returned an empty bitmap")
                                finishScan(scanId)
                                return
                            }

                            val image = InputImage.fromBitmap(capturedBitmap, 0)
                            recognizer.process(image)
                                .addOnSuccessListener(mainExecutor) { result ->
                                    if (scanId == activeScanId && !destroyed) {
                                        Log.d(TAG, "OCR: ${result.text}")
                                        handleText(result, capturedBitmap.width, capturedBitmap.height)
                                    }
                                }
                                .addOnFailureListener(mainExecutor) { error ->
                                    Log.e(TAG, "OCR failed", error)
                                }
                                .addOnCompleteListener(mainExecutor) {
                                    capturedBitmap.recycle()
                                    finishScan(scanId)
                                }
                        } catch (error: Exception) {
                            bitmap?.recycle()
                            Log.e(TAG, "Screenshot processing failed", error)
                            finishScan(scanId)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.w(TAG, "Screenshot failed, errorCode=$errorCode")
                        finishScan(scanId)
                    }
                }
            )
        } catch (error: Exception) {
            Log.e(TAG, "Could not request screenshot", error)
            finishScan(scanId)
        }
    }

    private fun normalize(value: String): String =
        value.trim().uppercase().replace('0', 'O').filter { it.isLetterOrDigit() }

    private fun handleText(result: Text, screenWidth: Int, screenHeight: Int) {
        if (!normalize(result.text).contains("BONUS")) return

        val okElement = result.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .firstOrNull { normalize(it.text) == "OK" }
            ?: run {
                Log.d(TAG, "BONUS found, exact OK element not found")
                return
            }

        val box = okElement.boundingBox ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastTap < TAP_COOLDOWN_MS) return

        val x = box.exactCenterX()
        val y = box.exactCenterY()
        if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) {
            Log.w(TAG, "OK coordinates outside screenshot: x=$x y=$y")
            return
        }

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.i(TAG, "OK tap completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "OK tap cancelled")
            }
        }, handler)

        if (accepted) {
            lastTap = now
            Log.i(TAG, "OK tap dispatched at x=$x y=$y")
        } else {
            Log.w(TAG, "Android rejected the tap gesture")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility interruption received; scanner will retry")
        handler.removeCallbacks(scanWatchdog)
        busy = false
        activeScanId++
        // Do not permanently kill the loop: Android can interrupt accessibility
        // while switching apps/windows. The service remains system-managed.
        ensureScannerScheduled(INTERRUPT_RETRY_MS)
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(scanner)
        handler.removeCallbacks(scanWatchdog)
        busy = false
        activeScanId++
        if (recognizerDelegate.isInitialized()) {
            recognizer.close()
        }
        super.onDestroy()
    }
}
