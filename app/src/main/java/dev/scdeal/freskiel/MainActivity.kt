package dev.scdeal.freskiel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dev.scdeal.freskiel.assistant.AssistPayload
import dev.scdeal.freskiel.di.AppGraph
import dev.scdeal.freskiel.perf.PerformanceMetrics
import dev.scdeal.freskiel.perf.PerformanceMetrics.recordAppStart
import dev.scdeal.freskiel.ui.PiMobileApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appGraph: AppGraph
        get() = (application as PiMobileApplication).appGraph

    override fun onCreate(savedInstanceState: Bundle?) {
        // Record app start as early as possible
        recordAppStart()

        super.onCreate(savedInstanceState)
        appGraph.shareNavigationCoordinator.submitExternalIntent(intent.action, intent.dataString)
        acceptAssist(intent)
        enableEdgeToEdge()
        setContent {
            PiMobileApp(appGraph = appGraph)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appGraph.shareNavigationCoordinator.submitExternalIntent(intent.action, intent.dataString)
        acceptAssist(intent)
    }

    private fun acceptAssist(intent: Intent?) {
        val text = intent?.getStringExtra(EXTRA_ASSIST_TEXT)
        val screenshotPath = intent?.getStringExtra(EXTRA_ASSIST_SCREENSHOT)
        if (text == null && screenshotPath == null) return
        appGraph.assistPayloadStore.offer(
            AssistPayload(
                text = text,
                screenshotPath = screenshotPath,
                screenshotSizeBytes = screenshotPath?.let { java.io.File(it).length() } ?: 0L,
            ),
        )
        intent.removeExtra(EXTRA_ASSIST_TEXT)
        intent.removeExtra(EXTRA_ASSIST_SCREENSHOT)
        lifecycleScope.launch { appGraph.resumeLastSession() }
    }

    companion object {
        const val EXTRA_ASSIST_TEXT = "dev.scdeal.freskiel.extra.ASSIST_TEXT"
        const val EXTRA_ASSIST_SCREENSHOT = "dev.scdeal.freskiel.extra.ASSIST_SCREENSHOT"
    }

    override fun onResume() {
        super.onResume()
        // Log any pending metrics
        lifecycleScope.launch {
            val timings = PerformanceMetrics.flushTimings()
            timings.forEach { timing ->
                android.util.Log.d(
                    "PerfMetrics",
                    "Flushed: ${timing.metric} = ${timing.durationMs}ms",
                )
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            lifecycleScope.launch {
                appGraph.sessionController.disconnect()
            }
        }
        super.onDestroy()
    }
}
