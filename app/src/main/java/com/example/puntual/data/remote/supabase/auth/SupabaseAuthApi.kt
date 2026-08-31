package com.example.puntual.data.remote.supabase.auth

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {

    @POST("token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body request: SupabaseSignInRequest,
    ): SupabaseAuthResponse
}
