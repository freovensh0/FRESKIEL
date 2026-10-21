package dev.scdeal.freskiel.chat

import dev.scdeal.freskiel.sessions.UploadedFile

internal fun String.withPendingFileNames(pendingFiles: List<PendingFile>): String {
    if (pendingFiles.isEmpty()) return this
    val names = pendingFiles.joinToString("\n") { "- ${it.name}" }
    return if (isBlank()) names else "$this\n\n$names"
}

internal fun String.withUploadedFiles(uploadedFiles: List<UploadedFile>): String {
    if (uploadedFiles.isEmpty()) return this
    val lines = uploadedFiles.joinToString("\n") { "- ${it.path} (${it.name})" }
    return if (isBlank()) "Attached files:\n$lines" else "$this\n\nAttached files:\n$lines"
}
