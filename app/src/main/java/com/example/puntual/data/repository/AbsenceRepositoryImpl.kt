package com.example.puntual.data.repository

import com.example.puntual.data.local.dao.AbsenceDao
import com.example.puntual.data.local.dao.AttendancePeriodDao
import com.example.puntual.data.mapper.toDomain
import com.example.puntual.data.mapper.toEntity
import com.example.puntual.domain.model.Absence
import com.example.puntual.domain.model.AbsenceStatus
import com.example.puntual.domain.model.AbsenceType
import com.example.puntual.domain.repository.AbsenceRepository
import com.example.puntual.domain.repository.SaveAbsenceError
import com.example.puntual.domain.repository.SaveAbsenceResult
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AbsenceRepositoryImpl @Inject constructor(
    private val absenceDao: AbsenceDao,
    private val periodDao: AttendancePeriodDao,
) : AbsenceRepository {

    override fun observeAbsences(
        periodId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Absence>> =
        absenceDao.observeBetween(periodId, startDate.toString(), endDate.toString())
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeAbsencesForPeriod(periodId: Long): Flow<List<Absence>> =
        absenceDao.observeForPeriod(periodId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveApprovedAbsence(
        startDate: LocalDate,
        endDate: LocalDate,
        type: AbsenceType,
        reason: String,
    ): SaveAbsenceResult {
        if (endDate.isBefore(startDate)) {
            return SaveAbsenceResult.Error(SaveAbsenceError.END_BEFORE_START)
        }
        val active = periodDao.getActive()?.toDomain()
            ?: return SaveAbsenceResult.Error(SaveAbsenceError.NO_ACTIVE_PERIOD)
        if (!active.contains(startDate) || !active.contains(endDate)) {
            return SaveAbsenceResult.Error(SaveAbsenceError.OUTSIDE_ACTIVE_PERIOD)
        }
        val absence = Absence(
            periodId = active.id,
            startDate = startDate,
            endDate = endDate,
            type = type,
            reason = reason.trim(),
            status = AbsenceStatus.APPROVED,
        )
        val id = absenceDao.insert(absence.toEntity())
        return SaveAbsenceResult.Success(id)
    }

    override suspend fun deleteAbsence(absenceId: Long) {
        absenceDao.deleteById(absenceId)
    }
}
