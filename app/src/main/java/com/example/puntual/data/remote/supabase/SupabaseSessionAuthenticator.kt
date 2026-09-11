package com.example.puntual.data.remote.supabase

import com.example.puntual.data.datastore.AuthSessionDataStore
import com.example.puntual.data.remote.supabase.auth.SupabaseAuthApi
import com.example.puntual.data.remote.supabase.auth.SupabaseRefreshTokenRequest
import com.example.puntual.domain.model.AuthSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException

@Singleton
class SupabaseSessionAuthenticator @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val config: SupabaseConfig,
    private val sessionDataStore: AuthSessionDataStore,
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount >= MAX_AUTH_ATTEMPTS) return null
        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

        return synchronized(refreshLock) {
            runBlocking {
                val currentSession = sessionDataStore.sessionFlow.first() ?: return@runBlocking null
                if (failedToken != null && currentSession.accessToken != failedToken) {
                    return@runBlocking response.request.withAccessToken(currentSession.accessToken)
                }

                val refreshedSession = refreshSession(currentSession)
                    ?: return@runBlocking null
                response.request.withAccessToken(refreshedSession.accessToken)
            }
        }
    }

    private suspend fun refreshSession(currentSession: AuthSession): AuthSession? =
        runCatching {
            val response = authApi.refreshSession(
                request = SupabaseRefreshTokenRequest(currentSession.refreshToken),
            )
            AuthSession(
                userId = response.user.id.ifBlank { currentSession.userId },
                email = response.user.email ?: currentSession.email,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
            ).also { sessionDataStore.saveSession(it) }
        }.getOrElse {
            if (it is HttpException && (it.code() == HTTP_BAD_REQUEST || it.code() == HTTP_UNAUTHORIZED)) {
                sessionDataStore.clearSession()
            }
            null
        }

    private fun Request.withAccessToken(accessToken: String): Request =
        newBuilder()
            .header("apikey", config.publishableKey)
            .header("Authorization", "Bearer $accessToken")
            .build()

    private val Response.responseCount: Int
        get() {
            var response: Response? = this
            var count = 1
            while (response?.priorResponse != null) {
                count++
                response = response.priorResponse
            }
            return count
        }

    private companion object {
        const val MAX_AUTH_ATTEMPTS = 2
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
    }
}
