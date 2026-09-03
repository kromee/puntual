package com.example.puntual.data.repository

import com.example.puntual.data.datastore.AuthSessionDataStore
import com.example.puntual.data.mapper.toDomain
import com.example.puntual.data.mapper.toSupabaseInsert
import com.example.puntual.data.remote.supabase.PuntuallSupabaseApi
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class AbsenceRepositoryImpl @Inject constructor(
    private val api: PuntuallSupabaseApi,
    private val sessionDataStore: AuthSessionDataStore,
) : AbsenceRepository {

    private val refreshEvents = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    override fun observeAbsences(
        periodId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Absence>> =
        sessionFlow().map { session ->
            session?.let {
                api.getAbsencesBetween(
                    userId = "eq.${it.userId}",
                    periodId = "eq.$periodId",
                    startsBeforeEnd = "lte.$endDate",
                    endsAfterStart = "gte.$startDate",
                ).map { dto -> dto.toDomain() }
            } ?: emptyList()
        }

    override fun observeAbsencesForPeriod(periodId: Long): Flow<List<Absence>> =
        sessionFlow().map { session ->
            session?.let {
                api.getAbsencesForPeriod(
                    userId = "eq.${it.userId}",
                    periodId = "eq.$periodId",
                ).map { dto -> dto.toDomain() }
            } ?: emptyList()
        }

    override suspend fun saveApprovedAbsence(
        startDate: LocalDate,
        endDate: LocalDate,
        type: AbsenceType,
        reason: String,
    ): SaveAbsenceResult {
        if (endDate.isBefore(startDate)) {
            return SaveAbsenceResult.Error(SaveAbsenceError.END_BEFORE_START)
        }
        val session = sessionDataStore.sessionFlow.first()
            ?: return SaveAbsenceResult.Error(SaveAbsenceError.NO_ACTIVE_PERIOD)
        val active = api.getActivePeriods(userId = "eq.${session.userId}")
            .firstOrNull()
            ?.toDomain()
            ?: return SaveAbsenceResult.Error(SaveAbsenceError.NO_ACTIVE_PERIOD)
        if (!active.contains(startDate) || !active.contains(endDate)) {
            return SaveAbsenceResult.Error(SaveAbsenceError.OUTSIDE_ACTIVE_PERIOD)
        }
        val overlapping = api.getAbsencesBetween(
            userId = "eq.${session.userId}",
            periodId = "eq.${active.id}",
            startsBeforeEnd = "lte.$endDate",
            endsAfterStart = "gte.$startDate",
        ).map { it.toDomain() }.any {
            it.status != AbsenceStatus.REJECTED && it.status != AbsenceStatus.CANCELLED
        }
        if (overlapping) {
            return SaveAbsenceResult.Error(SaveAbsenceError.OVERLAPS_EXISTING_ABSENCE)
        }
        val absence = Absence(
            periodId = active.id,
            startDate = startDate,
            endDate = endDate,
            type = type,
            reason = reason.trim(),
            status = AbsenceStatus.APPROVED,
        )
        val saved = api.createAbsence(absence.toSupabaseInsert(session.userId)).first()
        refreshEvents.emit(Unit)
        return SaveAbsenceResult.Success(saved.id)
    }

    override suspend fun deleteAbsence(absenceId: Long) {
        val session = sessionDataStore.sessionFlow.first() ?: return
        api.deleteAbsence(
            id = "eq.$absenceId",
            userId = "eq.${session.userId}",
        )
        refreshEvents.emit(Unit)
    }

    private fun sessionFlow() =
        combine(sessionDataStore.sessionFlow, refreshEvents) { session, _ -> session }
}
