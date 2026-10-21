package dev.scdeal.freskiel.assistant

import android.content.Intent
import android.speech.RecognitionService

/**
 * The system refuses to list an app as the digital assistant without a recognition service next to
 * the session service. FRESKIEL takes the screen and the text, not the microphone, so this service
 * reports that it has no recognizer instead of pretending to listen.
 */
class FreskielRecognitionService : RecognitionService() {
    override fun onStartListening(
        recognizerIntent: Intent?,
        args: Intent?,
    ) {
        onError(ERROR_CANNOT_CHECK_SUPPORT)
    }
}
