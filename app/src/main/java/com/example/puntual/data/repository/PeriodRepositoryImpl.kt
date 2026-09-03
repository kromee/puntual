package com.example.puntual.data.repository

import com.example.puntual.data.datastore.AuthSessionDataStore
import com.example.puntual.data.mapper.defaultPeriodTitle
import com.example.puntual.data.mapper.toDomain
import com.example.puntual.data.remote.supabase.PuntuallSupabaseApi
import com.example.puntual.data.remote.supabase.SupabaseAttendancePeriodPatchDto
import com.example.puntual.data.remote.supabase.SupabaseAttendancePeriodUpsertDto
import com.example.puntual.domain.model.AttendancePeriod
import com.example.puntual.domain.model.AuthSession
import com.example.puntual.domain.repository.ClosePeriodError
import com.example.puntual.domain.repository.ClosePeriodResult
import com.example.puntual.domain.repository.PeriodRepository
import com.example.puntual.domain.util.ClosePeriodDateDefaults
import com.example.puntual.domain.util.HistoryPeriodRules
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class PeriodRepositoryImpl @Inject constructor(
    private val api: PuntuallSupabaseApi,
    private val sessionDataStore: AuthSessionDataStore,
) : PeriodRepository {

    private val refreshEvents = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    override fun observeAllPeriods(): Flow<List<AttendancePeriod>> =
        sessionFlow().map { session ->
            session?.let { loadPeriods(it) } ?: emptyList()
        }

    override fun observeActivePeriod(): Flow<AttendancePeriod?> =
        sessionFlow().map { session ->
            session?.let { ensureDefaultPeriodExists(it) }
        }

    override fun observePeriod(periodId: Long): Flow<AttendancePeriod?> =
        sessionFlow().map { session ->
            session?.let {
                api.getPeriodById(
                    id = "eq.$periodId",
                    userId = "eq.${it.userId}",
                ).firstOrNull()?.toDomain()
            }
        }

    override suspend fun ensureDefaultPeriodExists() {
        val session = sessionDataStore.sessionFlow.first() ?: return
        ensureDefaultPeriodExists(session)
        refreshEvents.emit(Unit)
    }

    override suspend fun closeActivePeriod(
        endDate: LocalDate,
        newStartDate: LocalDate,
        newTitle: String?,
    ): ClosePeriodResult {
        val session = sessionDataStore.sessionFlow.first()
            ?: return ClosePeriodResult.Error(ClosePeriodError.NO_ACTIVE_PERIOD)
        val active = activePeriod(session)
            ?: return ClosePeriodResult.Error(ClosePeriodError.NO_ACTIVE_PERIOD)

        ClosePeriodDateDefaults.validate(active.startDate, endDate, newStartDate)?.let { error ->
            return ClosePeriodResult.Error(error)
        }

        val closedTitle = defaultPeriodTitle(active.startDate, endDate)
        api.updatePeriod(
            id = "eq.${active.id}",
            userId = "eq.${session.userId}",
            period = SupabaseAttendancePeriodPatchDto(
                title = closedTitle,
                endDate = endDate.toString(),
                isActive = false,
            ),
        )
        val opened = api.createPeriod(
            SupabaseAttendancePeriodUpsertDto(
                userId = session.userId,
                title = newTitle?.trim().takeUnless { it.isNullOrBlank() }
                    ?: defaultPeriodTitle(newStartDate, null),
                startDate = newStartDate.toString(),
                endDate = null,
                isActive = true,
            ),
        ).first().toDomain()
        refreshEvents.emit(Unit)
        return ClosePeriodResult.Success(
            closed = active.copy(title = closedTitle, endDate = endDate, isActive = false),
            opened = opened,
        )
    }

    private fun sessionFlow(): Flow<AuthSession?> =
        combine(sessionDataStore.sessionFlow, refreshEvents) { session, _ -> session }

    private suspend fun ensureDefaultPeriodExists(session: AuthSession): AttendancePeriod =
        activePeriod(session) ?: api.createPeriod(
            SupabaseAttendancePeriodUpsertDto(
                userId = session.userId,
                title = defaultPeriodTitle(DEFAULT_START_DATE, null),
                startDate = DEFAULT_START_DATE.toString(),
                endDate = null,
                isActive = true,
            ),
        ).first().toDomain()

    private suspend fun activePeriod(session: AuthSession): AttendancePeriod? =
        api.getActivePeriods(userId = "eq.${session.userId}")
            .firstOrNull()
            ?.toDomain()

    private suspend fun loadPeriods(session: AuthSession): List<AttendancePeriod> {
        ensureDefaultPeriodExists(session)
        return api.getPeriods(userId = "eq.${session.userId}").map { it.toDomain() }
    }

    private companion object {
        val DEFAULT_START_DATE: LocalDate = LocalDate.of(HistoryPeriodRules.FIRST_HISTORY_YEAR, 1, 1)
    }
}
