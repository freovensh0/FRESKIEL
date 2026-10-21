package dev.scdeal.freskiel.sessions

internal fun formatCwdTail(
    cwd: String,
    maxSegments: Int = 2,
): String {
    val segments = cwd.trim().trimEnd('/').split('/').filter { it.isNotBlank() }

    return when {
        cwd.isBlank() -> "(unknown)"
        segments.isEmpty() -> "/"
        else -> segments.takeLast(maxSegments).joinToString("/")
    }
}
