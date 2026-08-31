package com.example.puntual.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntual.domain.repository.AuthRepository
import com.example.puntual.domain.repository.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.session.collect { session ->
                _uiState.update { it.copy(isAuthenticated = session != null) }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email.trim(), errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa correo y contraseña.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signIn(state.email, state.password)) {
                is SignInResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            password = "",
                            isAuthenticated = true,
                        )
                    }
                }
                SignInResult.InvalidCredentials -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Correo o contraseña incorrectos.")
                    }
                }
                SignInResult.MissingSupabaseConfig -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Falta configurar Supabase en local.properties.")
                    }
                }
                is SignInResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message.ifBlank { "No se pudo iniciar sesión." },
                        )
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
