package com.example.puntual.data.repository

import com.example.puntual.data.datastore.AuthSessionDataStore
import com.example.puntual.data.datastore.UserPreferencesDataStore
import com.example.puntual.data.mapper.toDomain
import com.example.puntual.data.mapper.toSupabaseInsert
import com.example.puntual.data.remote.supabase.PuntuallSupabaseApi
import com.example.puntual.data.remote.supabase.SupabaseCheckInPatchDto
import com.example.puntual.domain.model.AttendancePeriod
import com.example.puntual.domain.model.AuthSession
import com.example.puntual.domain.model.CheckIn
import com.example.puntual.domain.model.MonthBreakdown
import com.example.puntual.domain.model.MonthHistory
import com.example.puntual.domain.model.UserPreferences
import com.example.puntual.domain.model.YearSummary
import com.example.puntual.domain.repository.CheckInRepository
import com.example.puntual.domain.repository.RegisterCheckInError
import com.example.puntual.domain.repository.RegisterCheckInResult
import com.example.puntual.domain.util.DelayCalculator
import com.example.puntual.domain.util.PeriodDateRules
import com.example.puntual.domain.util.WorkdayRules
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val api: PuntuallSupabaseApi,
    private val sessionDataStore: AuthSessionDataStore,
    private val preferencesDataStore: UserPreferencesDataStore,
) : CheckInRepository {

    private val refreshEvents = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    override val userPreferences: Flow<UserPreferences> =
        preferencesDataStore.preferencesFlow

    override fun observeTodayCheckIn(periodId: Long): Flow<CheckIn?> =
        sessionFlow().map { session ->
            session?.let {
                api.getCheckInByDate(
                    userId = "eq.${it.userId}",
                    periodId = "eq.$periodId",
                    workDate = "eq.${LocalDate.now()}",
                ).firstOrNull()?.toDomain()
            }
        }

    override fun observeMonthHistory(periodId: Long, yearMonth: YearMonth): Flow<MonthHistory> =
        sessionFlow().map { session ->
            val period = session?.let { loadPeriod(it, periodId) }
                ?: return@map emptyMonthHistory(yearMonth)
            val range = PeriodDateRules.intersectMonth(period, yearMonth)
                ?: return@map emptyMonthHistory(yearMonth)
            val checkIns = loadCheckIns(session, periodId, range.start, range.endInclusive)
            val absences = api.getAbsencesBetween(
                userId = "eq.${session.userId}",
                periodId = "eq.$periodId",
                startsBeforeEnd = "lte.${range.endInclusive}",
                endsAfterStart = "gte.${range.start}",
            ).map { it.toDomain() }.sortedBy { it.startDate }
            MonthHistory(
                yearMonth = yearMonth,
                checkIns = checkIns,
                absences = absences,
                lateDaysCount = checkIns.count { it.delayMinutes > 0 },
                totalDelayMinutes = checkIns.sumOf { it.delayMinutes },
            )
        }

    override fun observeYearSummary(periodId: Long, year: Int): Flow<YearSummary> =
        sessionFlow().map { session ->
            val period = session?.let { loadPeriod(it, periodId) }
                ?: return@map YearSummary(year, 0, 0, 0)
            val now = YearMonth.now()
            val throughMonth = effectiveThroughMonth(period, year, now)
            val range = PeriodDateRules.intersectYearSlice(period, year, throughMonth)
                ?: return@map YearSummary(year, throughMonth, 0, 0)
            val checkIns = loadCheckIns(session, periodId, range.start, range.endInclusive)
            YearSummary(
                year = year,
                throughMonth = throughMonth,
                lateDaysCount = checkIns.count { it.delayMinutes > 0 },
                totalDelayMinutes = checkIns.sumOf { it.delayMinutes },
            )
        }

    override fun observeYearMonthlyBreakdown(periodId: Long, year: Int): Flow<List<MonthBreakdown>> =
        sessionFlow().map { session ->
            val period = session?.let { loadPeriod(it, periodId) } ?: return@map emptyList()
            val now = YearMonth.now()
            val first = PeriodDateRules.firstSelectableMonth(period, year)
            val last = PeriodDateRules.lastSelectableMonth(period, year, now)
            if (first.year != year || last.year != year) return@map emptyList()
            val range = PeriodDateRules.intersectYearSlice(period, year, last.monthValue)
                ?: return@map monthsWithZeros(first, last)
            val byMonth = loadCheckIns(session, periodId, range.start, range.endInclusive)
                .groupBy { YearMonth.from(it.workDate) }
                .mapValues { (yearMonth, monthCheckIns) ->
                    MonthBreakdown(
                        yearMonth = yearMonth,
                        lateDaysCount = monthCheckIns.count { it.delayMinutes > 0 },
                        totalDelayMinutes = monthCheckIns.sumOf { it.delayMinutes },
                    )
                }
            monthsInRange(first, last).map { ym ->
                byMonth[ym] ?: MonthBreakdown(ym, 0, 0)
            }
        }

    override suspend fun getAvailableYears(periodId: Long): List<Int> {
        val session = sessionDataStore.sessionFlow.first() ?: return listOf(Year.now().value)
        val period = loadPeriod(session, periodId) ?: return listOf(Year.now().value)
        return PeriodDateRules.availableYears(period)
    }

    override suspend fun registerCheckIn(): RegisterCheckInResult {
        val session = sessionDataStore.sessionFlow.first()
            ?: return RegisterCheckInResult.Error(RegisterCheckInError.NO_ACTIVE_PERIOD)
        val today = LocalDate.now()
        if (!WorkdayRules.isWorkday(today)) {
            return RegisterCheckInResult.Error(RegisterCheckInError.NOT_WORKDAY)
        }
        val active = activePeriod(session)
            ?: return RegisterCheckInResult.Error(RegisterCheckInError.NO_ACTIVE_PERIOD)
        if (!active.contains(today)) {
            return RegisterCheckInResult.Error(RegisterCheckInError.OUTSIDE_ACTIVE_PERIOD)
        }
        if (existingCheckIn(session, active.id, today) != null) {
            return RegisterCheckInResult.Error(RegisterCheckInError.ALREADY_REGISTERED)
        }
        val prefs = preferencesDataStore.preferencesFlow.first()
        return registerWithPrefs(session, active.id, today, prefs)
    }

    override suspend fun updateCheckInTime(
        workDate: LocalDate,
        periodId: Long,
        hour: Int,
        minute: Int,
    ): Boolean {
        val session = sessionDataStore.sessionFlow.first() ?: return false
        val existing = existingCheckIn(session, periodId, workDate) ?: return false
        val zone = ZoneId.systemDefault()
        val updatedAt = workDate.atTime(hour, minute).atZone(zone)
        val delayMinutes = DelayCalculator.calculateDelayMinutes(
            workDate,
            updatedAt,
            existing.expectedTime,
        )
        api.updateCheckIn(
            userId = "eq.${session.userId}",
            periodId = "eq.$periodId",
            workDate = "eq.$workDate",
            checkIn = SupabaseCheckInPatchDto(
                checkedInAt = updatedAt.toInstant().toString(),
                delayMinutes = delayMinutes,
            ),
        )
        refreshEvents.emit(Unit)
        return true
    }

    override suspend fun setDisplayName(name: String) {
        preferencesDataStore.setDisplayName(name)
    }

    override suspend fun setExpectedTime(hour: Int, minute: Int) {
        preferencesDataStore.setExpectedTime(hour, minute)
    }

    override suspend fun clearExpectedTime() {
        preferencesDataStore.clearExpectedTime()
    }

    private fun sessionFlow(): Flow<AuthSession?> =
        combine(sessionDataStore.sessionFlow, refreshEvents) { session, _ -> session }

    private suspend fun registerWithPrefs(
        session: AuthSession,
        periodId: Long,
        today: LocalDate,
        prefs: UserPreferences,
    ): RegisterCheckInResult {
        val zone = ZoneId.systemDefault()
        val now = DelayCalculator.now(zone)
        val expectedTime = if (prefs.hasExpectedTime) {
            LocalTime.of(prefs.expectedHour, prefs.expectedMinute)
        } else {
            now.toLocalTime()
        }
        val delayMinutes = if (prefs.hasExpectedTime) {
            DelayCalculator.calculateDelayMinutes(today, now, expectedTime)
        } else {
            0
        }
        val checkIn = CheckIn(
            workDate = today,
            checkedInAt = now.toInstant(),
            expectedTime = expectedTime,
            delayMinutes = delayMinutes,
        )
        api.createCheckIn(checkIn.toSupabaseInsert(session.userId, periodId))
        refreshEvents.emit(Unit)
        return RegisterCheckInResult.Success(checkIn)
    }

    private suspend fun existingCheckIn(
        session: AuthSession,
        periodId: Long,
        workDate: LocalDate,
    ): CheckIn? = api.getCheckInByDate(
        userId = "eq.${session.userId}",
        periodId = "eq.$periodId",
        workDate = "eq.$workDate",
    ).firstOrNull()?.toDomain()

    private suspend fun activePeriod(session: AuthSession): AttendancePeriod? =
        api.getActivePeriods(userId = "eq.${session.userId}")
            .firstOrNull()
            ?.toDomain()

    private suspend fun loadPeriod(session: AuthSession, periodId: Long): AttendancePeriod? =
        api.getPeriodById(
            id = "eq.$periodId",
            userId = "eq.${session.userId}",
        ).firstOrNull()?.toDomain()

    private suspend fun loadCheckIns(
        session: AuthSession,
        periodId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CheckIn> = api.getCheckInsBetween(
        userId = "eq.${session.userId}",
        periodId = "eq.$periodId",
        workDateFrom = "gte.$startDate",
        workDateTo = "lte.$endDate",
    ).map { it.toDomain() }.sortedBy { it.workDate }

    private fun emptyMonthHistory(yearMonth: YearMonth) =
        MonthHistory(yearMonth, emptyList(), emptyList(), 0, 0)

    private fun effectiveThroughMonth(
        period: AttendancePeriod,
        year: Int,
        now: YearMonth,
    ): Int {
        val calendarThrough = if (year == now.year) now.monthValue else 12
        if (period.lastDateInclusive.year < year) return 0
        if (period.lastDateInclusive.year > year) return calendarThrough
        return minOf(calendarThrough, period.lastDateInclusive.monthValue)
    }

    private fun monthsWithZeros(first: YearMonth, last: YearMonth): List<MonthBreakdown> =
        monthsInRange(first, last).map { MonthBreakdown(it, 0, 0) }

    private fun monthsInRange(first: YearMonth, last: YearMonth): List<YearMonth> =
        generateSequence(first) { current ->
            if (current < last) current.plusMonths(1) else null
        }
            .plus(last)
            .distinct()
            .toList()
}
