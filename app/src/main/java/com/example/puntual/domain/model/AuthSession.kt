package com.example.puntual.domain.model

data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
)
