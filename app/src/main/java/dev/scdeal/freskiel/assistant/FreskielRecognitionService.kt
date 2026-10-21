package dev.scdeal.freskiel.assistant

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * The system refuses to list an app as the digital assistant without a recognition service next to
 * the session service. FRESKIEL takes the screen and the text, not the microphone, so this service
 * reports that it has no recognizer instead of pretending to listen.
 */
class FreskielRecognitionService : RecognitionService() {
    override fun onStartListening(
        recognizerIntent: Intent?,
        listener: RecognitionService.Callback?,
    ) {
        listener?.error(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT)
    }

    override fun onCancel(listener: RecognitionService.Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onStopListening(listener: RecognitionService.Callback?) {
    }
}
