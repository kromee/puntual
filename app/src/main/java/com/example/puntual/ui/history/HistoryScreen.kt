package com.example.puntual.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.puntual.R
import com.example.puntual.ui.components.PuntualElevatedCard
import com.example.puntual.ui.components.PuntualScreenShell
import com.example.puntual.ui.theme.LateOrange
import com.example.puntual.ui.theme.OnTimeGreen
import com.example.puntual.ui.theme.PuntualGreen
import com.example.puntual.ui.theme.TextSecondary
import com.example.puntual.ui.theme.TextPrimary

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PuntualScreenShell(
        userDisplayName = uiState.userName,
        screenTitle = stringResource(R.string.nav_history),
    ) {
        if (uiState.isLoading) {
            HistoryLoadingCard()
        } else {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
            if (uiState.periods.size > 1) {
                PeriodFilterDropdown(
                    periods = uiState.periods,
                    selectedPeriodId = uiState.selectedPeriodId,
                    periodRangeLabel = uiState.periodRangeLabel,
                    onPeriodSelected = viewModel::onPeriodSelected,
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                val rangeLabel = uiState.periodRangeLabel
                if (rangeLabel != null) {
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                }
            }
            YearFilterDropdown(
                years = uiState.availableYears,
                selectedYear = uiState.selectedYearMonth.year,
                onYearSelected = viewModel::onYearSelected,
            )
            Spacer(modifier = Modifier.height(12.dp))

            YearTotalsCard(
                periodLabel = uiState.yearPeriodLabel,
                summary = uiState.yearSummary,
                monthlyBreakdown = uiState.monthlyBreakdown,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.history_daily_detail),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PuntualGreen,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MonthNavigator(
                monthTitle = uiState.monthTitle,
                canGoPrevious = uiState.canGoPreviousMonth,
                canGoNext = uiState.canGoNextMonth,
                onPrevious = viewModel::onPreviousMonth,
                onNext = viewModel::onNextMonth,
            )
            Text(
                text = stringResource(
                    when {
                        uiState.isSelectedFutureMonth -> R.string.history_hint_future_month
                        uiState.isSelectedCurrentMonth -> R.string.history_hint_current_month
                        else -> R.string.history_hint_past_month
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isEmpty) {
                PuntualElevatedCard {
                    Text(
                        text = stringResource(R.string.history_empty_month),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
            } else {
                PuntualElevatedCard {
                    HistoryTableHeader()
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Column {
                        uiState.rows.forEach { row ->
                            HistoryRow(row)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                PuntualElevatedCard {
                    Text(
                        text = stringResource(R.string.history_summary_month, uiState.monthTitle),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DurationSummaryDisplay(summary = uiState.monthSummary)
                }
            }
        }
    }
}
}

@Composable
private fun HistoryLoadingCard() {
    PuntualElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = PuntualGreen)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Cargando historial...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun YearTotalsCard(
    periodLabel: String,
    summary: DurationSummaryUi,
    monthlyBreakdown: List<MonthBreakdownRowUi>,
) {
    PuntualElevatedCard {
        Text(
            text = stringResource(R.string.history_summary_year),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        DurationSummaryDisplay(summary = summary)
        if (monthlyBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.history_by_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_by_month_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            monthlyBreakdown.forEach { month ->
                MonthBreakdownRow(month)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun MonthBreakdownRow(row: MonthBreakdownRowUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = row.monthLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.history_month_late_days, row.lateDaysCount),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Text(
            text = row.combinedLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !row.hasData -> TextSecondary
                row.hours > 0 || row.minutes > 0 -> LateOrange
                else -> OnTimeGreen
            },
        )
    }
}

@Composable
private fun PeriodFilterDropdown(
    periods: List<PeriodOptionUi>,
    selectedPeriodId: Long?,
    periodRangeLabel: String?,
    onPeriodSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = periods.find { it.id == selectedPeriodId }
    Column {
        Text(
            text = stringResource(R.string.history_filter_period),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        if (periodRangeLabel != null) {
            Text(
                text = periodRangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected?.title ?: stringResource(R.string.history_filter_period),
                    color = PuntualGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                periods.forEach { period ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (period.isActive) {
                                    "${period.title} (${stringResource(R.string.history_period_active)})"
                                } else {
                                    period.title
                                },
                            )
                        },
                        onClick = {
                            onPeriodSelected(period.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun YearFilterDropdown(
    years: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(R.string.history_filter_year),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        Text(
            text = stringResource(R.string.history_years_from),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedYear.toString(),
                    color = PuntualGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearSelected(year)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthNavigator(
    monthTitle: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.history_prev_month))
        }
        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleLarge,
            color = PuntualGreen,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.history_next_month))
        }
    }
}

@Composable
private fun HistoryTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Día", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text("Hora", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text("Retardo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistoryRow(row: HistoryRowUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(row.dayLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(100.dp))
        Text(
            row.timeLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = if (row.isJustifiedAbsence) PuntualGreen else TextPrimary,
        )
        Text(
            text = row.delayLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = when {
                row.isJustifiedAbsence -> PuntualGreen
                row.isOnTime -> OnTimeGreen
                else -> LateOrange
            },
        )
    }
}
