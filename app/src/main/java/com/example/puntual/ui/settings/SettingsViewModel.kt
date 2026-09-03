package com.example.puntual.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntual.domain.model.Absence
import com.example.puntual.domain.model.AbsenceType
import com.example.puntual.domain.model.AttendancePeriod
import com.example.puntual.domain.model.sanitizeDisplayName
import com.example.puntual.domain.repository.AbsenceRepository
import com.example.puntual.domain.repository.CheckInRepository
import com.example.puntual.domain.repository.ClosePeriodError
import com.example.puntual.domain.repository.ClosePeriodResult
import com.example.puntual.domain.repository.PeriodRepository
import com.example.puntual.domain.repository.SaveAbsenceError
import com.example.puntual.domain.repository.SaveAbsenceResult
import com.example.puntual.domain.util.ClosePeriodDateDefaults
import com.example.puntual.domain.util.PuntualFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PeriodListItemUi(
    val id: Long,
    val title: String,
    val rangeLabel: String,
    val isActive: Boolean,
)

data class AbsenceListItemUi(
    val id: Long,
    val typeLabel: String,
    val rangeLabel: String,
    val reason: String,
    val statusLabel: String,
)

data class SettingsUiState(
    val displayNameDraft: String = "",
    val isEditingName: Boolean = false,
    val hasExpectedTime: Boolean = false,
    val expectedTimeLabel: String = "",
    val expectedHour: Int = 8,
    val expectedMinute: Int = 0,
    val showTimePicker: Boolean = false,
    val notificationsPermission: PermissionUiState = PermissionUiState.NotApplicable,
    val periods: List<PeriodListItemUi> = emptyList(),
    val activePeriodTitle: String = "",
    val showClosePeriodDialog: Boolean = false,
    val closePeriodEndDate: LocalDate = LocalDate.now(),
    val closePeriodNewStartDate: LocalDate = LocalDate.now().plusDays(1),
    val closePeriodNewTitle: String = "",
    val closePeriodError: String? = null,
    val isClosingPeriod: Boolean = false,
    val closePeriodSuccess: String? = null,
    val absences: List<AbsenceListItemUi> = emptyList(),
    val showAbsenceDialog: Boolean = false,
    val absenceStartDate: LocalDate = LocalDate.now(),
    val absenceEndDate: LocalDate = LocalDate.now(),
    val absenceType: AbsenceType = AbsenceType.VACATION,
    val absenceReason: String = "",
    val absenceError: String? = null,
    val isSavingAbsence: Boolean = false,
    val absenceSuccess: String? = null,
    val showAugustCleanupDialog: Boolean = false,
    val isCleaningAugustData: Boolean = false,
    val augustCleanupMessage: String? = null,
)

sealed interface PermissionUiState {
    object NotApplicable : PermissionUiState
    data class Applicable(val granted: Boolean) : PermissionUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val repository: CheckInRepository,
    private val absenceRepository: AbsenceRepository,
    private val periodRepository: PeriodRepository,
) : ViewModel() {

    private val activePeriodId = MutableStateFlow<Long?>(null)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodRepository.observeAllPeriods().collect { periods ->
                val active = periods.find { it.isActive }
                activePeriodId.value = active?.id
                _uiState.update { current ->
                    current.copy(
                        periods = periods.map { it.toListItemUi() },
                        activePeriodTitle = active?.title ?: "",
                    )
                }
            }
        }
        viewModelScope.launch {
            activePeriodId.flatMapLatest { periodId ->
                if (periodId == null) flowOf(emptyList()) else absenceRepository.observeAbsencesForPeriod(periodId)
            }.collect { absences ->
                _uiState.update { current ->
                    current.copy(absences = absences.map { it.toListItemUi() })
                }
            }
        }
        viewModelScope.launch {
            repository.userPreferences.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        displayNameDraft = if (current.isEditingName) {
                            current.displayNameDraft
                        } else {
                            prefs.displayName
                        },
                        hasExpectedTime = prefs.hasExpectedTime,
                        expectedHour = prefs.expectedHour,
                        expectedMinute = prefs.expectedMinute,
                        expectedTimeLabel = if (prefs.hasExpectedTime) {
                            PuntualFormatters.formatExpectedTime(
                                LocalTime.of(prefs.expectedHour, prefs.expectedMinute),
                            )
                        } else {
                            ""
                        },
                    )
                }
            }
        }
        refreshNotificationPermission()
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update {
            it.copy(displayNameDraft = sanitizeDisplayName(name), isEditingName = true)
        }
    }

    fun saveDisplayName() {
        viewModelScope.launch {
            repository.setDisplayName(_uiState.value.displayNameDraft)
            _uiState.update { it.copy(isEditingName = false) }
        }
    }

    fun openTimePicker() {
        _uiState.update { it.copy(showTimePicker = true) }
    }

    fun dismissTimePicker() {
        _uiState.update { it.copy(showTimePicker = false) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setExpectedTime(hour, minute)
            _uiState.update { it.copy(showTimePicker = false) }
        }
    }

    fun clearExpectedTime() {
        viewModelScope.launch {
            repository.clearExpectedTime()
        }
    }

    fun openClosePeriodDialog() {
        viewModelScope.launch {
            val active = periodRepository.observeActivePeriod().first()
            val (end, start) = ClosePeriodDateDefaults.suggestDates(active)
            _uiState.update {
                it.copy(
                    showClosePeriodDialog = true,
                    closePeriodEndDate = end,
                    closePeriodNewStartDate = start,
                    closePeriodError = null,
                    closePeriodSuccess = null,
                )
            }
        }
    }

    fun dismissClosePeriodDialog() {
        _uiState.update { it.copy(showClosePeriodDialog = false, closePeriodError = null) }
    }

    fun onClosePeriodEndDateSelected(date: LocalDate) {
        _uiState.update { current ->
            var newStart = current.closePeriodNewStartDate
            if (!newStart.isAfter(date)) {
                newStart = date.plusDays(1).coerceAtMost(LocalDate.now())
            }
            current.copy(
                closePeriodEndDate = date,
                closePeriodNewStartDate = newStart,
                closePeriodError = null,
            )
        }
    }

    fun onClosePeriodNewStartDateSelected(date: LocalDate) {
        _uiState.update { it.copy(closePeriodNewStartDate = date, closePeriodError = null) }
    }

    fun onClosePeriodNewTitleChange(title: String) {
        _uiState.update { it.copy(closePeriodNewTitle = title) }
    }

    fun confirmClosePeriod() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isClosingPeriod = true, closePeriodError = null) }
            when (
                val result = periodRepository.closeActivePeriod(
                    endDate = state.closePeriodEndDate,
                    newStartDate = state.closePeriodNewStartDate,
                    newTitle = state.closePeriodNewTitle.takeIf { it.isNotBlank() },
                )
            ) {
                is ClosePeriodResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isClosingPeriod = false,
                            showClosePeriodDialog = false,
                            closePeriodSuccess = "Corte hecho. Bloque cerrado y nuevo periodo activo.",
                            closePeriodNewTitle = "",
                        )
                    }
                }
                is ClosePeriodResult.Error -> {
                    val message = when (result.type) {
                        ClosePeriodError.NO_ACTIVE_PERIOD ->
                            "No hay un periodo activo para cerrar."
                        ClosePeriodError.END_BEFORE_START ->
                            "La fecha de cierre no puede ser anterior al inicio del bloque."
                        ClosePeriodError.NEW_START_NOT_AFTER_END ->
                            "El nuevo bloque debe empezar al día siguiente al cierre (ej. cierras 20 may → abre 21 may)."
                        ClosePeriodError.NEW_START_IN_FUTURE ->
                            "El inicio del nuevo bloque no puede ser después de hoy."
                    }
                    _uiState.update {
                        it.copy(isClosingPeriod = false, closePeriodError = message)
                    }
                }
            }
        }
    }

    fun clearClosePeriodSuccess() {
        _uiState.update { it.copy(closePeriodSuccess = null) }
    }

    fun openAbsenceDialog() {
        val today = LocalDate.now()
        _uiState.update {
            it.copy(
                showAbsenceDialog = true,
                absenceStartDate = today,
                absenceEndDate = today,
                absenceType = AbsenceType.VACATION,
                absenceReason = "",
                absenceError = null,
                absenceSuccess = null,
            )
        }
    }

    fun dismissAbsenceDialog() {
        _uiState.update { it.copy(showAbsenceDialog = false, absenceError = null) }
    }

    fun onAbsenceStartDateSelected(date: LocalDate) {
        _uiState.update { current ->
            current.copy(
                absenceStartDate = date,
                absenceEndDate = if (current.absenceEndDate.isBefore(date)) date else current.absenceEndDate,
                absenceError = null,
            )
        }
    }

    fun onAbsenceEndDateSelected(date: LocalDate) {
        _uiState.update { it.copy(absenceEndDate = date, absenceError = null) }
    }

    fun onAbsenceTypeSelected(type: AbsenceType) {
        _uiState.update { it.copy(absenceType = type, absenceError = null) }
    }

    fun onAbsenceReasonChange(reason: String) {
        _uiState.update { it.copy(absenceReason = reason.take(120), absenceError = null) }
    }

    fun confirmAbsence() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSavingAbsence = true, absenceError = null) }
            when (
                val result = absenceRepository.saveApprovedAbsence(
                    startDate = state.absenceStartDate,
                    endDate = state.absenceEndDate,
                    type = state.absenceType,
                    reason = state.absenceReason,
                )
            ) {
                is SaveAbsenceResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingAbsence = false,
                            showAbsenceDialog = false,
                            absenceReason = "",
                            absenceSuccess = "Ausencia registrada para justificar días sin check-in.",
                        )
                    }
                }
                is SaveAbsenceResult.Error -> {
                    val message = when (result.type) {
                        SaveAbsenceError.NO_ACTIVE_PERIOD ->
                            "No hay un bloque activo para registrar la ausencia."
                        SaveAbsenceError.END_BEFORE_START ->
                            "La fecha final no puede ser anterior a la inicial."
                        SaveAbsenceError.OUTSIDE_ACTIVE_PERIOD ->
                            "La ausencia debe quedar dentro del bloque activo."
                        SaveAbsenceError.OVERLAPS_EXISTING_ABSENCE ->
                            "Ya existe una ausencia registrada para una o más fechas de ese rango."
                    }
                    _uiState.update {
                        it.copy(isSavingAbsence = false, absenceError = message)
                    }
                }
            }
        }
    }

    fun deleteAbsence(absenceId: Long) {
        viewModelScope.launch {
            absenceRepository.deleteAbsence(absenceId)
        }
    }

    fun openAugustCleanupDialog() {
        _uiState.update {
            it.copy(showAugustCleanupDialog = true, augustCleanupMessage = null)
        }
    }

    fun dismissAugustCleanupDialog() {
        _uiState.update { it.copy(showAugustCleanupDialog = false) }
    }

    fun confirmAugustCleanup() {
        viewModelScope.launch {
            val periodId = activePeriodId.value
            if (periodId == null) {
                _uiState.update {
                    it.copy(
                        showAugustCleanupDialog = false,
                        augustCleanupMessage = "No hay un periodo activo para limpiar registros.",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isCleaningAugustData = true) }
            val deleted = repository.deleteCheckInsBetween(
                periodId = periodId,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 31),
            )
            _uiState.update {
                it.copy(
                    showAugustCleanupDialog = false,
                    isCleaningAugustData = false,
                    augustCleanupMessage = "Registros de agosto eliminados: $deleted.",
                )
            }
        }
    }

    fun refreshNotificationPermission() {
        val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            PermissionUiState.Applicable(granted = granted)
        } else {
            PermissionUiState.NotApplicable
        }
        _uiState.update { it.copy(notificationsPermission = permissionState) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(notificationsPermission = PermissionUiState.Applicable(granted = granted))
        }
    }

    private fun AttendancePeriod.toListItemUi(): PeriodListItemUi {
        val start = PuntualFormatters.formatWorkDate(startDate)
        val end = endDate?.let { PuntualFormatters.formatWorkDate(it) } ?: "actualidad"
        return PeriodListItemUi(
            id = id,
            title = title,
            rangeLabel = "$start – $end",
            isActive = isActive,
        )
    }

    private fun Absence.toListItemUi(): AbsenceListItemUi {
        val start = PuntualFormatters.formatWorkDate(startDate)
        val end = PuntualFormatters.formatWorkDate(endDate)
        return AbsenceListItemUi(
            id = id,
            typeLabel = type.label,
            rangeLabel = if (startDate == endDate) start else "$start – $end",
            reason = reason.ifBlank { "Sin comentario" },
            statusLabel = status.label,
        )
    }
}
