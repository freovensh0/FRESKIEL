package dev.scdeal.freskiel.coresessions

interface SessionIndexRemoteDataSource {
    suspend fun fetch(hostId: String): List<SessionGroup>
}
