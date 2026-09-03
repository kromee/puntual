package com.example.puntual.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.puntual.domain.model.AttendancePeriod
import com.example.puntual.domain.model.Absence
import com.example.puntual.domain.model.CheckIn
import com.example.puntual.domain.model.MonthBreakdown
import com.example.puntual.domain.model.MonthHistory
import com.example.puntual.domain.model.YearSummary
import com.example.puntual.domain.model.headerUserLine
import com.example.puntual.domain.repository.CheckInRepository
import com.example.puntual.domain.repository.PeriodRepository
import com.example.puntual.domain.util.PeriodDateRules
import com.example.puntual.domain.util.PuntualFormatters
import com.example.puntual.domain.util.WorkdayRules
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
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
class HistoryViewModel @Inject constructor(
    private val repository: CheckInRepository,
    private val periodRepository: PeriodRepository,
) : ViewModel() {

    private val selectedPeriodId = MutableStateFlow<Long?>(null)
    private val selectedYearMonth = MutableStateFlow(YearMonth.now())
    private val availableYears = MutableStateFlow<List<Int>>(emptyList())
    private val allPeriods = MutableStateFlow<List<AttendancePeriod>>(emptyList())
    private var lastActivePeriodId: Long? = null

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            periodRepository.observeAllPeriods().collect { periods ->
                allPeriods.value = periods
                val activeId = periods.find { it.isActive }?.id
                when {
                    selectedPeriodId.value == null -> {
                        selectedPeriodId.value = activeId ?: periods.firstOrNull()?.id
                    }
                    lastActivePeriodId != null &&
                        activeId != null &&
                        lastActivePeriodId != activeId -> {
                        selectedPeriodId.value = activeId
                        viewModelScope.launch inner@{
                            availableYears.value = repository.getAvailableYears(activeId)
                            val period = periodRepository.observePeriod(activeId).first()
                                ?: return@inner
                            selectedYearMonth.value = PeriodDateRules.clampYearMonth(
                                LocalDate.now().year,
                                LocalDate.now().monthValue,
                                period,
                            )
                        }
                    }
                }
                lastActivePeriodId = activeId
            }
        }
        viewModelScope.launch {
            selectedPeriodId.collect { periodId ->
                if (periodId != null) {
                    availableYears.value = repository.getAvailableYears(periodId)
                }
            }
        }
        viewModelScope.launch {
            combine(selectedPeriodId, selectedYearMonth) { periodId, yearMonth ->
                periodId to yearMonth
            }
                .flatMapLatest { (periodId, yearMonth) ->
                    if (periodId == null) {
                        return@flatMapLatest flowOf(HistoryUiState(isLoading = false))
                    }
                    combine(
                        combine(
                            repository.userPreferences,
                            repository.observeMonthHistory(periodId, yearMonth),
                            repository.observeYearSummary(periodId, yearMonth.year),
                        ) { prefs, month, yearSummary ->
                            Triple(prefs, month, yearSummary)
                        },
                        combine(
                            repository.observeYearMonthlyBreakdown(periodId, yearMonth.year),
                            periodRepository.observePeriod(periodId),
                            availableYears,
                        ) { monthlyBreakdown, period, years ->
                            Triple(monthlyBreakdown, period, years)
                        },
                        allPeriods,
                    ) { historyData, periodData, periods ->
                        val (prefs, month, yearSummary) = historyData
                        val (monthlyBreakdown, period, years) = periodData
                        mapToUi(
                            userName = prefs.headerUserLine(),
                            yearMonth = yearMonth,
                            month = month,
                            yearSummary = yearSummary,
                            monthlyBreakdown = monthlyBreakdown,
                            years = years,
                            period = period,
                            periods = periods,
                            selectedPeriodId = periodId,
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state.copy(isLoading = false)
                }
        }
    }

    fun onPeriodSelected(periodId: Long) {
        if (selectedPeriodId.value == periodId) return
        _uiState.update { it.copy(isLoading = true) }
        selectedPeriodId.value = periodId
        viewModelScope.launch {
            val period = periodRepository.observePeriod(periodId).first() ?: return@launch
            val years = repository.getAvailableYears(periodId)
            availableYears.value = years
            val year = when {
                years.isEmpty() -> selectedYearMonth.value.year
                else -> selectedYearMonth.value.year.coerceIn(years.last(), years.first())
            }
            selectedYearMonth.value = PeriodDateRules.clampYearMonth(
                year,
                selectedYearMonth.value.monthValue,
                period,
            )
        }
    }

    fun onYearSelected(year: Int) {
        val periodId = selectedPeriodId.value ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val period = periodRepository.observePeriod(periodId).first() ?: return@launch
            val now = YearMonth.now()
            val current = selectedYearMonth.value
            val month = if (current.year == year) {
                current.monthValue
            } else {
                if (year == now.year) now.monthValue else 12
            }
            selectedYearMonth.value = PeriodDateRules.clampYearMonth(year, month, period, now)
        }
    }

    fun onPreviousMonth() {
        val periodId = selectedPeriodId.value ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val period = periodRepository.observePeriod(periodId).first() ?: return@launch
            val current = selectedYearMonth.value
            if (PeriodDateRules.canGoToPreviousMonth(current, period)) {
                selectedYearMonth.value = current.minusMonths(1)
            }
        }
    }

    fun onNextMonth() {
        val periodId = selectedPeriodId.value ?: return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val period = periodRepository.observePeriod(periodId).first() ?: return@launch
            val current = selectedYearMonth.value
            val next = current.plusMonths(1)
            if (PeriodDateRules.canGoToNextMonth(current, period) && next.year == current.year) {
                selectedYearMonth.value = next
            }
        }
    }

    private fun mapToUi(
        userName: String,
        yearMonth: YearMonth,
        month: MonthHistory,
        yearSummary: YearSummary,
        monthlyBreakdown: List<MonthBreakdown>,
        years: List<Int>,
        period: AttendancePeriod?,
        periods: List<AttendancePeriod>,
        selectedPeriodId: Long,
    ): HistoryUiState {
        val now = YearMonth.now()
        val rows = month.toRowsUi()
        val isCurrentYear = yearSummary.year == now.year
        val canPrev = period?.let { PeriodDateRules.canGoToPreviousMonth(yearMonth, it) } ?: false
        val canNext = period?.let { PeriodDateRules.canGoToNextMonth(yearMonth, it, now) } ?: false
        return HistoryUiState(
            userName = userName,
            selectedYearMonth = yearMonth,
            availableYears = years.ifEmpty { listOf(now.year) },
            monthTitle = PuntualFormatters.formatMonthTitle(yearMonth),
            isSelectedCurrentMonth = yearMonth == now,
            isSelectedFutureMonth = yearMonth.isAfter(now),
            rows = rows,
            monthSummary = month.totalDelayMinutes.toDurationSummary(month.lateDaysCount),
            yearSummary = yearSummary.totalDelayMinutes.toDurationSummary(yearSummary.lateDaysCount),
            yearPeriodLabel = period?.title ?: PuntualFormatters.formatYearPeriodLabel(
                year = yearSummary.year,
                throughMonth = yearSummary.throughMonth,
                isCurrentYear = isCurrentYear,
            ),
            monthlyBreakdown = monthlyBreakdown.map { it.toBreakdownRowUi() },
            isEmpty = rows.isEmpty(),
            canGoNextMonth = canNext,
            canGoPreviousMonth = canPrev,
            periods = periods.map { PeriodOptionUi(it.id, it.title, it.isActive) },
            selectedPeriodId = selectedPeriodId,
            periodRangeLabel = period?.let { formatPeriodRange(it) },
        )
    }

    private fun formatPeriodRange(period: AttendancePeriod): String {
        val start = PuntualFormatters.formatWorkDate(period.startDate)
        val end = period.endDate?.let { PuntualFormatters.formatWorkDate(it) } ?: "actualidad"
        return "$start – $end"
    }

    private fun Int.toDurationSummary(lateDays: Int): DurationSummaryUi {
        val parts = PuntualFormatters.splitDuration(this)
        return DurationSummaryUi(
            hours = parts.hours,
            minutes = parts.minutes,
            totalMinutes = parts.totalMinutes,
            lateDaysCount = lateDays,
            combinedLabel = PuntualFormatters.formatHoursMinutes(this),
        )
    }

    private fun MonthBreakdown.toBreakdownRowUi(): MonthBreakdownRowUi {
        val parts = PuntualFormatters.splitDuration(totalDelayMinutes)
        val hasData = totalDelayMinutes > 0 || lateDaysCount > 0
        return MonthBreakdownRowUi(
            monthLabel = PuntualFormatters.formatShortMonth(yearMonth),
            lateDaysCount = lateDaysCount,
            hours = parts.hours,
            minutes = parts.minutes,
            combinedLabel = if (hasData) {
                PuntualFormatters.formatHoursMinutes(totalDelayMinutes)
            } else {
                "—"
            },
            hasData = hasData,
        )
    }

    private fun CheckIn.toRowUi(): HistoryRowUi = HistoryRowUi(
        workDate = workDate,
        dayLabel = PuntualFormatters.formatWorkDate(workDate),
        timeLabel = PuntualFormatters.formatTime(checkedInAt),
        delayLabel = PuntualFormatters.formatDelay(delayMinutes),
        isOnTime = delayMinutes <= 0,
    )

    private fun MonthHistory.toRowsUi(): List<HistoryRowUi> {
        val checkInRows = checkIns.map { it.toRowUi() }
        val checkInDates = checkIns.map { it.workDate }.toSet()
        val absenceRows = absences.flatMap { absence ->
            absence.workdaysIn(yearMonth)
                .filterNot { it in checkInDates }
                .map { date ->
                    HistoryRowUi(
                        workDate = date,
                        dayLabel = PuntualFormatters.formatWorkDate(date),
                        timeLabel = absence.type.label,
                        delayLabel = absence.reason.ifBlank { absence.status.label },
                        isOnTime = true,
                        isJustifiedAbsence = true,
                    )
                }
        }
        return (checkInRows + absenceRows).sortedBy { it.workDate }
    }

    private fun Absence.workdaysIn(yearMonth: YearMonth): List<LocalDate> {
        val start = maxOf(startDate, yearMonth.atDay(1))
        val end = minOf(endDate, yearMonth.atEndOfMonth())
        if (end.isBefore(start)) return emptyList()
        return generateSequence(start) { current ->
            if (current < end) current.plusDays(1) else null
        }
            .filter(WorkdayRules::isWorkday)
            .toList()
    }
}
