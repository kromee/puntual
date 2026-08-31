package com.example.puntual.ui.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
