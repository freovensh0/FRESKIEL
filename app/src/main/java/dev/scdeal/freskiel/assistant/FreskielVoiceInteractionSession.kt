package dev.scdeal.freskiel.assistant

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.util.Log
import dev.scdeal.freskiel.MainActivity
import java.io.File
import java.io.FileOutputStream

/**
 * Receives the assist gesture, keeps the screen text and one screenshot, then hands both to the
 * composer. Nothing is written outside the cache directory and nothing is captured in the background.
 */
class FreskielVoiceInteractionSession(
    context: Context,
) : VoiceInteractionSession(context) {
    private val handler = Handler(Looper.getMainLooper())
    private var screenText: String? = null
    private var screenshotPath: String? = null
    private var launched = false

    override fun onShow(
        args: Bundle?,
        showFlags: Int,
    ) {
        setDisabledShowContext(0)
    }

    override fun onHandleAssist(
        state: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?,
    ) {
        screenText =
            structure?.windowNodes
                ?.flatMap { it.rootViewNode.collectText() }
                ?.distinct()
                ?.joinToString("\n")
                ?.take(SCREEN_TEXT_LIMIT)
                ?.takeIf(String::isNotBlank)
        scheduleLaunch()
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) return
        Thread {
            val file = File(context.cacheDir, "$SCREENSHOT_DIR/$SCREENSHOT_NAME")
            file.parentFile?.mkdirs()
            runCatching {
                FileOutputStream(file).use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, SCREENSHOT_QUALITY, it)
                }
            }.onSuccess {
                screenshotPath = file.absolutePath
            }.onFailure {
                Log.w(TAG, "Could not store the assist screenshot", it)
            }
            handler.post(::scheduleLaunch)
        }.start()
    }

    override fun onHide() {
        handler.removeCallbacksAndMessages(null)
        super.onHide()
    }

    private fun scheduleLaunch() {
        handler.removeCallbacks(launch)
        handler.postDelayed(launch, LAUNCH_DELAY_MS)
    }

    private val launch =
        Runnable {
            if (launched) return@Runnable
            launched = true
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_ASSIST
                    putExtra(MainActivity.EXTRA_ASSIST_TEXT, screenText)
                    putExtra(MainActivity.EXTRA_ASSIST_SCREENSHOT, screenshotPath)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    )
                }
            context.startActivity(intent)
            finish()
        }

    private fun AssistStructure.ViewNode.collectText(): List<String> {
        val own =
            listOfNotNull(
                text?.toString(),
                contentDescription?.toString(),
            ).map(String::trim).filter(String::isNotEmpty)
        return own + children.flatMap { it.collectText() }
    }

    private companion object {
        const val TAG = "FreskielAssist"
        const val SCREEN_TEXT_LIMIT = 4_000
        const val LAUNCH_DELAY_MS = 250L
        const val SCREENSHOT_DIR = "shared-images"
        const val SCREENSHOT_NAME = "assist-screen.png"
        const val SCREENSHOT_QUALITY = 100
    }
}
