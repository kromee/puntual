package com.example.puntual.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntual.domain.model.CheckIn
import com.example.puntual.domain.model.UserPreferences
import com.example.puntual.domain.model.headerUserLine
import com.example.puntual.domain.repository.CheckInRepository
import com.example.puntual.domain.repository.PeriodRepository
import com.example.puntual.domain.repository.QuoteRepository
import com.example.puntual.domain.repository.RegisterCheckInError
import com.example.puntual.domain.repository.RegisterCheckInResult
import com.example.puntual.domain.util.PuntualFormatters
import com.example.puntual.domain.util.WorkdayRules
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CheckInRepository,
    private val periodRepository: PeriodRepository,
    private val quoteRepository: QuoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodRepository.observeActivePeriod()
                .flatMapLatest { period ->
                    if (period == null) {
                        flowOf(
                            HomeUiState.Ready(
                                userName = "",
                                hasExpectedTime = false,
                                isWeekday = WorkdayRules.isWorkday(LocalDate.now()),
                                expectedTimeLabel = "",
                                alreadyCheckedInToday = false,
                                todayTimeLabel = null,
                                todayDelayLabel = null,
                                activePeriodLabel = null,
                                errorMessage = "No hay un periodo activo. Cierra y abre un bloque en Config.",
                            ),
                        )
                    } else {
                        combine(
                            repository.userPreferences,
                            repository.observeTodayCheckIn(period.id),
                        ) { prefs, todayCheckIn ->
                            buildReadyState(
                                prefs = prefs,
                                todayCheckIn = todayCheckIn,
                                activePeriodLabel = period.title,
                            )
                        }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onRegisterClick() {
        viewModelScope.launch {
            updateReady { it.copy(isRegistering = true, errorMessage = null) }
            when (val result = repository.registerCheckIn()) {
                is RegisterCheckInResult.Success -> {
                    updateReady { it.copy(isRegistering = false, errorMessage = null) }
                }
                is RegisterCheckInResult.Error -> {
                    val message = when (result.type) {
                        RegisterCheckInError.NOT_WORKDAY ->
                            "Hoy no es día laborable (lun–vie)."
                        RegisterCheckInError.ALREADY_REGISTERED ->
                            "Ya registraste tu entrada hoy."
                        RegisterCheckInError.NO_ACTIVE_PERIOD ->
                            "No hay periodo activo. Configura un corte en Config."
                        RegisterCheckInError.OUTSIDE_ACTIVE_PERIOD ->
                            "Hoy queda fuera del bloque activo. Haz un corte en Config."
                    }
                    updateReady { it.copy(isRegistering = false, errorMessage = message) }
                }
            }
        }
    }

    fun openEditTimePicker() {
        viewModelScope.launch {
            runCatching {
                val period = periodRepository.observeActivePeriod().first() ?: return@launch
                val today = repository.observeTodayCheckIn(period.id).first() ?: return@launch
                val time = today.checkedInAt.atZone(ZoneId.systemDefault()).toLocalTime()
                updateReady {
                    it.copy(
                        showEditTimePicker = true,
                        editTimeHour = time.hour,
                        editTimeMinute = time.minute,
                        errorMessage = null,
                    )
                }
            }.onFailure {
                updateReady { state ->
                    state.copy(errorMessage = "No se pudo cargar la hora registrada.")
                }
            }
        }
    }

    fun dismissEditTimePicker() {
        updateReady { it.copy(showEditTimePicker = false) }
    }

    fun onEditTimeSelected(hour: Int, minute: Int) {
        viewModelScope.launch {
            updateReady { it.copy(showEditTimePicker = false, errorMessage = null) }
            val result = runCatching {
                val period = periodRepository.observeActivePeriod().first() ?: return@runCatching false
                repository.updateCheckInTime(LocalDate.now(), period.id, hour, minute)
            }
            result.onSuccess { updated ->
                if (!updated) {
                    updateReady { it.copy(errorMessage = "No se pudo actualizar la hora.") }
                }
            }.onFailure {
                updateReady { it.copy(errorMessage = "La hora se guardó, pero no se pudo refrescar la pantalla.") }
            }
        }
    }

    private fun buildReadyState(
        prefs: UserPreferences,
        todayCheckIn: CheckIn?,
        activePeriodLabel: String?,
    ): HomeUiState.Ready {
        val current = _uiState.value as? HomeUiState.Ready
        val today = LocalDate.now()
        val isWeekday = WorkdayRules.isWorkday(today)
        val checkedIn = todayCheckIn != null
        val expectedTimeLabel = if (prefs.hasExpectedTime) {
            PuntualFormatters.formatExpectedTime(
                java.time.LocalTime.of(prefs.expectedHour, prefs.expectedMinute),
            )
        } else {
            ""
        }
        return HomeUiState.Ready(
            userName = prefs.headerUserLine(),
            hasExpectedTime = prefs.hasExpectedTime,
            isWeekday = isWeekday,
            expectedTimeLabel = expectedTimeLabel,
            alreadyCheckedInToday = checkedIn,
            todayTimeLabel = todayCheckIn?.let { PuntualFormatters.formatTime(it.checkedInAt) },
            todayDelayLabel = todayCheckIn?.let { PuntualFormatters.formatDelay(it.delayMinutes) },
            activePeriodLabel = activePeriodLabel,
            isRegistering = current?.isRegistering == true,
            errorMessage = current?.errorMessage,
            showEditTimePicker = current?.showEditTimePicker == true,
            editTimeHour = current?.editTimeHour ?: 8,
            editTimeMinute = current?.editTimeMinute ?: 0,
            showMotivationalDialog = current?.showMotivationalDialog == true,
            motivationalQuoteLoading = current?.motivationalQuoteLoading == true,
            motivationalQuoteText = current?.motivationalQuoteText.orEmpty(),
            motivationalQuoteAuthor = current?.motivationalQuoteAuthor.orEmpty(),
        )
    }

    fun onViewQuoteClick() {
        updateReady {
            it.copy(
                showMotivationalDialog = true,
                motivationalQuoteLoading = true,
                motivationalQuoteText = "",
                motivationalQuoteAuthor = "",
            )
        }
        loadMotivationalQuote()
    }

    fun dismissMotivationalDialog() {
        updateReady {
            it.copy(
                showMotivationalDialog = false,
                motivationalQuoteLoading = false,
            )
        }
    }

    private fun loadMotivationalQuote() {
        viewModelScope.launch {
            val fallbackText = "Cada día es una nueva oportunidad para llegar puntual y con calma."
            val fallbackAuthor = "Puntual"
            quoteRepository.fetchRandomQuote()
                .onSuccess { quote ->
                    updateReady {
                        it.copy(
                            motivationalQuoteLoading = false,
                            motivationalQuoteText = quote.text,
                            motivationalQuoteAuthor = quote.author,
                        )
                    }
                }
                .onFailure {
                    updateReady {
                        it.copy(
                            motivationalQuoteLoading = false,
                            motivationalQuoteText = fallbackText,
                            motivationalQuoteAuthor = fallbackAuthor,
                        )
                    }
                }
        }
    }

    private fun updateReady(block: (HomeUiState.Ready) -> HomeUiState.Ready) {
        _uiState.update { current ->
            if (current is HomeUiState.Ready) block(current) else current
        }
    }
}
