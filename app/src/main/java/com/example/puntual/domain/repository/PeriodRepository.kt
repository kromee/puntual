package com.example.puntual.domain.repository

import com.example.puntual.domain.model.AttendancePeriod
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

enum class ClosePeriodError {
    NO_ACTIVE_PERIOD,
    END_BEFORE_START,
    NEW_START_NOT_AFTER_END,
    NEW_START_IN_FUTURE,
    NETWORK,
}

sealed class ClosePeriodResult {
    data class Success(
        val closed: AttendancePeriod,
        val opened: AttendancePeriod,
    ) : ClosePeriodResult()

    data class Error(val type: ClosePeriodError) : ClosePeriodResult()
}

interface PeriodRepository {
    fun observeAllPeriods(): Flow<List<AttendancePeriod>>
    fun observeActivePeriod(): Flow<AttendancePeriod?>
    fun observePeriod(periodId: Long): Flow<AttendancePeriod?>
    suspend fun ensureDefaultPeriodExists()
    suspend fun closeActivePeriod(
        endDate: LocalDate,
        newStartDate: LocalDate,
        newTitle: String? = null,
    ): ClosePeriodResult
}
