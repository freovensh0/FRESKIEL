package dev.scdeal.freskiel.sessions

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class LastSession(
    val hostId: String,
    val sessionPath: String,
    val cwd: String,
)

interface LastSessionStore {
    fun get(): LastSession?

    fun set(lastSession: LastSession)

    fun clear()
}

class InMemoryLastSessionStore : LastSessionStore {
    private var value: LastSession? = null

    override fun get(): LastSession? = value

    override fun set(lastSession: LastSession) {
        value = lastSession
    }

    override fun clear() {
        value = null
    }
}

class SharedPreferencesLastSessionStore(
    context: Context,
) : LastSessionStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(): LastSession? {
        val hostId = preferences.getString(KEY_HOST_ID, null) ?: return null
        val sessionPath = preferences.getString(KEY_SESSION_PATH, null) ?: return null
        val cwd = preferences.getString(KEY_CWD, null) ?: return null
        return LastSession(hostId = hostId, sessionPath = sessionPath, cwd = cwd)
    }

    override fun set(lastSession: LastSession) {
        preferences.edit {
            putString(KEY_HOST_ID, lastSession.hostId)
            putString(KEY_SESSION_PATH, lastSession.sessionPath)
            putString(KEY_CWD, lastSession.cwd)
        }
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val PREFS_NAME = "freskiel_last_session"
        const val KEY_HOST_ID = "host_id"
        const val KEY_SESSION_PATH = "session_path"
        const val KEY_CWD = "cwd"
    }
}
