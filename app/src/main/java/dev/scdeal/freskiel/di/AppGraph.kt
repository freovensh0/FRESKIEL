package dev.scdeal.freskiel.di

import android.content.Context
import dev.scdeal.freskiel.coresessions.FileSessionIndexCache
import dev.scdeal.freskiel.coresessions.SessionIndexRepository
import dev.scdeal.freskiel.hosts.ConnectionDiagnostics
import dev.scdeal.freskiel.hosts.HostProfileStore
import dev.scdeal.freskiel.hosts.HostTokenStore
import dev.scdeal.freskiel.hosts.KeystoreHostTokenStore
import dev.scdeal.freskiel.hosts.SharedPreferencesHostProfileStore
import dev.scdeal.freskiel.sessions.BridgeSessionIndexRemoteDataSource
import dev.scdeal.freskiel.sessions.BridgeSessionShareRemoteDataSource
import dev.scdeal.freskiel.sessions.ClientIdentityStore
import dev.scdeal.freskiel.sessions.RpcSessionController
import dev.scdeal.freskiel.sessions.SessionController
import dev.scdeal.freskiel.sessions.SessionCwdPreferenceStore
import dev.scdeal.freskiel.sessions.SessionSavedStateStore
import dev.scdeal.freskiel.sessions.ShareNavigationCoordinator
import dev.scdeal.freskiel.sessions.SharedPreferencesClientIdentityStore
import dev.scdeal.freskiel.sessions.SharedPreferencesSavedSessionStorage
import dev.scdeal.freskiel.sessions.SharedPreferencesSessionCwdPreferenceStore

class AppGraph(
    context: Context,
) {
    private val appContext = context.applicationContext

    val clientIdentityStore: ClientIdentityStore by lazy {
        SharedPreferencesClientIdentityStore(appContext)
    }

    val sessionController: SessionController by lazy {
        RpcSessionController(clientId = clientIdentityStore.getClientId())
    }

    val sessionCwdPreferenceStore: SessionCwdPreferenceStore by lazy {
        SharedPreferencesSessionCwdPreferenceStore(appContext)
    }

    val sessionSavedStateStore: SessionSavedStateStore by lazy {
        SessionSavedStateStore(SharedPreferencesSavedSessionStorage(appContext))
    }

    val hostProfileStore: HostProfileStore by lazy {
        SharedPreferencesHostProfileStore(appContext)
    }

    val hostTokenStore: HostTokenStore by lazy {
        KeystoreHostTokenStore(appContext)
    }

    val sessionIndexRepository: SessionIndexRepository by lazy {
        SessionIndexRepository(
            remoteDataSource = BridgeSessionIndexRemoteDataSource(hostProfileStore, hostTokenStore),
            cache = FileSessionIndexCache(appContext.cacheDir.toPath().resolve("session-index-cache")),
        )
    }

    val connectionDiagnostics: ConnectionDiagnostics by lazy { ConnectionDiagnostics() }

    val sessionShareRemoteDataSource: BridgeSessionShareRemoteDataSource by lazy {
        BridgeSessionShareRemoteDataSource(hostProfileStore, hostTokenStore)
    }

    /** One delivery owner for the entire application process. */
    val shareNavigationCoordinator: ShareNavigationCoordinator by lazy {
        ShareNavigationCoordinator(
            profileStore = hostProfileStore,
            tokenStore = hostTokenStore,
            shareSource = sessionShareRemoteDataSource,
            sessionController = sessionController,
        )
    }
}
