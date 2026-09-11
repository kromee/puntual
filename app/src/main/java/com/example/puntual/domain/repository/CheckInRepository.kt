package com.example.puntual.domain.repository

import com.example.puntual.domain.model.CheckIn
import com.example.puntual.domain.model.MonthBreakdown
import com.example.puntual.domain.model.MonthHistory
import com.example.puntual.domain.model.UserPreferences
import com.example.puntual.domain.model.YearSummary
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

sealed class RegisterCheckInResult {
    data class Success(val checkIn: CheckIn) : RegisterCheckInResult()
    data class Error(val type: RegisterCheckInError) : RegisterCheckInResult()
}

enum class RegisterCheckInError {
    NOT_WORKDAY,
    ALREADY_REGISTERED,
    NO_ACTIVE_PERIOD,
    OUTSIDE_ACTIVE_PERIOD,
    FUTURE_DATE,
}

interface CheckInRepository {
    val userPreferences: Flow<UserPreferences>
    fun observeTodayCheckIn(periodId: Long): Flow<CheckIn?>
    fun observeMonthHistory(periodId: Long, yearMonth: YearMonth): Flow<MonthHistory>
    fun observeYearSummary(periodId: Long, year: Int): Flow<YearSummary>
    fun observeYearMonthlyBreakdown(periodId: Long, year: Int): Flow<List<MonthBreakdown>>
    suspend fun getAvailableYears(periodId: Long): List<Int>
    suspend fun registerCheckIn(identityVerified: Boolean = false): RegisterCheckInResult
    suspend fun registerManualCheckIn(
        workDate: LocalDate,
        periodId: Long,
        hour: Int,
        minute: Int,
        identityVerified: Boolean = false,
    ): RegisterCheckInResult
    suspend fun updateCheckInTime(
        workDate: LocalDate,
        periodId: Long,
        hour: Int,
        minute: Int,
        identityVerified: Boolean = false,
    ): Boolean
    suspend fun setDisplayName(name: String)
    suspend fun setExpectedTime(hour: Int, minute: Int)
    suspend fun clearExpectedTime()
    suspend fun setBiometricUnlockEnabled(enabled: Boolean)
}
