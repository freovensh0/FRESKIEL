package dev.scdeal.freskiel.assistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AssistPayload(
    val text: String?,
    val screenshotPath: String?,
    val screenshotSizeBytes: Long,
)

class AssistPayloadStore {
    private val _pending = MutableStateFlow<AssistPayload?>(null)
    val pending: StateFlow<AssistPayload?> = _pending.asStateFlow()

    fun offer(payload: AssistPayload) {
        _pending.value = payload
    }

    fun take(): AssistPayload? {
        val payload = _pending.value
        _pending.value = null
        return payload
    }
}
