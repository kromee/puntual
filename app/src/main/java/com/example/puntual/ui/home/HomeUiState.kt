package com.example.puntual.ui.home

sealed interface HomeUiState {
    object Loading : HomeUiState

    data class Ready(
        val userName: String,
        val hasExpectedTime: Boolean,
        val isWeekday: Boolean,
        val expectedTimeLabel: String,
        val alreadyCheckedInToday: Boolean,
        val todayTimeLabel: String?,
        val todayDelayLabel: String?,
        val activePeriodLabel: String? = null,
        val isRegistering: Boolean = false,
        val errorMessage: String? = null,
        val showEditTimePicker: Boolean = false,
        val editTimeHour: Int = 8,
        val editTimeMinute: Int = 0,
        val showMotivationalDialog: Boolean = false,
        val motivationalQuoteLoading: Boolean = false,
        val motivationalQuoteText: String = "",
        val motivationalQuoteAuthor: String = "",
        val biometricUnlockEnabled: Boolean = true,
    ) : HomeUiState
}
