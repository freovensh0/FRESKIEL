package dev.scdeal.freskiel.sessions

interface ClientIdentityStore {
    fun getClientId(): String
}
