package com.example.puntual.data.remote.supabase.auth

import com.google.gson.annotations.SerializedName

data class SupabaseSignInRequest(
    val email: String,
    val password: String,
)

data class SupabaseAuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresInSeconds: Long,
    @SerializedName("token_type")
    val tokenType: String,
    val user: SupabaseUserDto,
)

data class SupabaseUserDto(
    val id: String,
    val email: String?,
)
