package com.example.puntual.ui.history

import java.time.LocalDate
import java.time.YearMonth

data class PeriodOptionUi(
    val id: Long,
    val title: String,
    val isActive: Boolean,
)

data class HistoryRowUi(
    val workDate: LocalDate,
    val dayLabel: String,
    val timeLabel: String,
    val delayLabel: String,
    val isOnTime: Boolean,
    val isJustifiedAbsence: Boolean = false,
)

data class DurationSummaryUi(
    val hours: Int = 0,
    val minutes: Int = 0,
    val totalMinutes: Int = 0,
    val lateDaysCount: Int = 0,
    val combinedLabel: String = "0 min",
)

data class MonthBreakdownRowUi(
    val monthLabel: String,
    val lateDaysCount: Int,
    val hours: Int,
    val minutes: Int,
    val combinedLabel: String,
    val hasData: Boolean = true,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val availableYears: List<Int> = emptyList(),
    val monthTitle: String = "",
    val isSelectedCurrentMonth: Boolean = true,
    val isSelectedFutureMonth: Boolean = false,
    val rows: List<HistoryRowUi> = emptyList(),
    val monthSummary: DurationSummaryUi = DurationSummaryUi(),
    val yearSummary: DurationSummaryUi = DurationSummaryUi(),
    val yearPeriodLabel: String = "",
    val monthlyBreakdown: List<MonthBreakdownRowUi> = emptyList(),
    val isEmpty: Boolean = true,
    val canGoNextMonth: Boolean = false,
    val canGoPreviousMonth: Boolean = true,
    val periods: List<PeriodOptionUi> = emptyList(),
    val selectedPeriodId: Long? = null,
    val periodRangeLabel: String? = null,
)
