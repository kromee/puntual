package com.example.puntual.data.repository

import com.example.puntual.data.datastore.AuthSessionDataStore
import com.example.puntual.data.remote.supabase.SupabaseConfig
import com.example.puntual.data.remote.supabase.auth.SupabaseAuthApi
import com.example.puntual.data.remote.supabase.auth.SupabaseSignInRequest
import com.example.puntual.domain.model.AuthSession
import com.example.puntual.domain.repository.AuthRepository
import com.example.puntual.domain.repository.SignInResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val sessionDataStore: AuthSessionDataStore,
    private val supabaseConfig: SupabaseConfig,
) : AuthRepository {

    override val session: Flow<AuthSession?> = sessionDataStore.sessionFlow

    override suspend fun signIn(email: String, password: String): SignInResult {
        if (!supabaseConfig.isConfigured) {
            return SignInResult.MissingSupabaseConfig
        }
        return try {
            val response = authApi.signInWithPassword(
                request = SupabaseSignInRequest(
                    email = email.trim(),
                    password = password,
                ),
            )
            val session = AuthSession(
                userId = response.user.id,
                email = response.user.email.orEmpty(),
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
            )
            sessionDataStore.saveSession(session)
            SignInResult.Success(session)
        } catch (error: HttpException) {
            if (error.code() == 400 || error.code() == 401) {
                SignInResult.InvalidCredentials
            } else {
                SignInResult.Error(error.message())
            }
        } catch (error: Exception) {
            SignInResult.Error(error.message.orEmpty())
        }
    }

    override suspend fun signOut() {
        sessionDataStore.clearSession()
    }
}
