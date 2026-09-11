package com.example.puntual.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntual.domain.repository.CheckInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppSecurityUiState(
    val biometricUnlockEnabled: Boolean = true,
)

@HiltViewModel
class AppSecurityViewModel @Inject constructor(
    repository: CheckInRepository,
) : ViewModel() {
    val uiState: StateFlow<AppSecurityUiState> = repository.userPreferences
        .map { prefs ->
            AppSecurityUiState(
                biometricUnlockEnabled = prefs.biometricUnlockEnabled,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSecurityUiState(),
        )
}
