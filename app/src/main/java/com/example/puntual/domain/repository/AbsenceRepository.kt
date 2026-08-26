package com.example.puntual.domain.repository

import com.example.puntual.domain.model.Absence
import com.example.puntual.domain.model.AbsenceType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

sealed class SaveAbsenceResult {
    data class Success(val absenceId: Long) : SaveAbsenceResult()
    data class Error(val type: SaveAbsenceError) : SaveAbsenceResult()
}

enum class SaveAbsenceError {
    NO_ACTIVE_PERIOD,
    END_BEFORE_START,
    OUTSIDE_ACTIVE_PERIOD,
    OVERLAPS_EXISTING_ABSENCE,
}

interface AbsenceRepository {
    fun observeAbsences(periodId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<Absence>>
    fun observeAbsencesForPeriod(periodId: Long): Flow<List<Absence>>
    suspend fun saveApprovedAbsence(
        startDate: LocalDate,
        endDate: LocalDate,
        type: AbsenceType,
        reason: String,
    ): SaveAbsenceResult
    suspend fun deleteAbsence(absenceId: Long)
}
