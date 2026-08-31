package com.example.puntual.domain.repository

import com.example.puntual.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

sealed class SignInResult {
    data class Success(val session: AuthSession) : SignInResult()
    object InvalidCredentials : SignInResult()
    object MissingSupabaseConfig : SignInResult()
    data class Error(val message: String) : SignInResult()
}

interface AuthRepository {
    val session: Flow<AuthSession?>
    suspend fun signIn(email: String, password: String): SignInResult
    suspend fun signOut()
}
