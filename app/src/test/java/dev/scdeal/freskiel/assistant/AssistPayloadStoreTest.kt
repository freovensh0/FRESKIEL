package dev.scdeal.freskiel.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistPayloadStoreTest {
    @Test
    fun `take returns the payload once`() {
        val store = AssistPayloadStore()
        val payload = AssistPayload(text = "screen text", screenshotPath = null, screenshotSizeBytes = 0)

        store.offer(payload)

        assertEquals(payload, store.take())
        assertNull(store.take())
    }
}
