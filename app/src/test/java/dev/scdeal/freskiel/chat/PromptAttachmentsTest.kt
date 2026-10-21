package dev.scdeal.freskiel.chat

import dev.scdeal.freskiel.sessions.UploadedFile
import org.junit.Assert.assertEquals
import org.junit.Test

class PromptAttachmentsTest {
    @Test
    fun `pending file names are listed under the message`() {
        val text =
            "look at this".withPendingFileNames(
            listOf(
                PendingFile(uri = "content://a", name = "a.txt", sizeBytes = 1),
                PendingFile(uri = "content://b", name = "b.txt", sizeBytes = 2),
            ),
        )

        assertEquals("look at this\n\n- a.txt\n- b.txt", text)
    }

    @Test
    fun `uploaded files carry the path the agent reads`() {
        val text =
            "check it".withUploadedFiles(
                listOf(UploadedFile(path = "/tmp/uploads/x-a.txt", name = "a.txt", sizeBytes = 1)),
            )

        assertEquals("check it\n\nAttached files:\n- /tmp/uploads/x-a.txt (a.txt)", text)
    }

    @Test
    fun `an empty message stays empty without attachments`() {
        assertEquals("", "".withPendingFileNames(emptyList()))
        assertEquals("", "".withUploadedFiles(emptyList()))
    }
}
