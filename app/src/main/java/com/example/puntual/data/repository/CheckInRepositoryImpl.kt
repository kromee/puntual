package com.example.puntual.data.repository

import com.example.puntual.data.datastore.UserPreferencesDataStore
import com.example.puntual.data.local.dao.AbsenceDao
import com.example.puntual.data.local.dao.AttendancePeriodDao
import com.example.puntual.data.local.dao.CheckInDao
import com.example.puntual.data.mapper.toDomain
import com.example.puntual.data.mapper.toEntity
import com.example.puntual.domain.model.AttendancePeriod
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
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val checkInDao: CheckInDao,
    private val absenceDao: AbsenceDao,
    private val periodDao: AttendancePeriodDao,
    private val preferencesDataStore: UserPreferencesDataStore,
) : CheckInRepository {

    override val userPreferences: Flow<UserPreferences> =
        preferencesDataStore.preferencesFlow

    override fun observeTodayCheckIn(periodId: Long): Flow<CheckIn?> {
        val today = LocalDate.now().toString()
        return checkInDao.observeByWorkDate(periodId, today).map { it?.toDomain() }
    }

    override fun observeMonthHistory(periodId: Long, yearMonth: YearMonth): Flow<MonthHistory> =
        periodDao.observeById(periodId).flatMapLatest { entity ->
            val period = entity?.toDomain()
            if (period == null) {
                return@flatMapLatest flowOf(emptyMonthHistory(yearMonth))
            }
            val range = PeriodDateRules.intersectMonth(period, yearMonth)
                ?: return@flatMapLatest flowOf(emptyMonthHistory(yearMonth))
            val start = range.start.toString()
            val end = range.endInclusive.toString()
            combine(
                checkInDao.observeBetween(periodId, start, end)
                    .map { list -> list.map { it.toDomain() }.sortedBy { it.workDate } },
                absenceDao.observeBetween(periodId, start, end)
                    .map { list -> list.map { it.toDomain() }.sortedBy { it.startDate } },
                checkInDao.observeLateDayCount(periodId, start, end),
                checkInDao.observeTotalDelayMinutes(periodId, start, end),
            ) { checkIns, absences, lateDays, totalMinutes ->
                MonthHistory(yearMonth, checkIns, absences, lateDays, totalMinutes)
            }
        }

    override fun observeYearSummary(periodId: Long, year: Int): Flow<YearSummary> =
        periodDao.observeById(periodId).flatMapLatest { entity ->
            val period = entity?.toDomain()
            if (period == null) {
                return@flatMapLatest flowOf(YearSummary(year, 0, 0, 0))
            }
            val now = YearMonth.now()
            val throughMonth = effectiveThroughMonth(period, year, now)
            val range = PeriodDateRules.intersectYearSlice(period, year, throughMonth)
                ?: return@flatMapLatest flowOf(YearSummary(year, throughMonth, 0, 0))
            val start = range.start.toString()
            val end = range.endInclusive.toString()
            combine(
                checkInDao.observeLateDayCount(periodId, start, end),
                checkInDao.observeTotalDelayMinutes(periodId, start, end),
            ) { lateDays, totalMinutes ->
                YearSummary(year, throughMonth, lateDays, totalMinutes)
            }
        }

    override fun observeYearMonthlyBreakdown(periodId: Long, year: Int): Flow<List<MonthBreakdown>> =
        periodDao.observeById(periodId).flatMapLatest { entity ->
            val period = entity?.toDomain()
            if (period == null) return@flatMapLatest flowOf(emptyList())
            val now = YearMonth.now()
            val first = PeriodDateRules.firstSelectableMonth(period, year)
            val last = PeriodDateRules.lastSelectableMonth(period, year, now)
            if (first.year != year || last.year != year) return@flatMapLatest flowOf(emptyList())
            val range = PeriodDateRules.intersectYearSlice(period, year, last.monthValue)
                ?: return@flatMapLatest flowOf(monthsWithZeros(first, last))
            val start = range.start.toString()
            val end = range.endInclusive.toString()
            checkInDao.observeBetween(periodId, start, end).map { entities ->
                val byMonth = entities
                    .groupBy { YearMonth.from(LocalDate.parse(it.workDate)) }
                    .mapValues { (yearMonth, monthEntities) ->
                        MonthBreakdown(
                            yearMonth = yearMonth,
                            lateDaysCount = monthEntities.count { it.delayMinutes > 0 },
                            totalDelayMinutes = monthEntities.sumOf { it.delayMinutes },
                        )
                    }
                monthsInRange(first, last).map { ym ->
                    byMonth[ym] ?: MonthBreakdown(ym, 0, 0)
                }
            }
        }

    override suspend fun getAvailableYears(periodId: Long): List<Int> {
        val period = periodDao.getById(periodId)?.toDomain() ?: return listOf(Year.now().value)
        return PeriodDateRules.availableYears(period)
    }

    override suspend fun registerCheckIn(): RegisterCheckInResult {
        val today = LocalDate.now()
        if (!WorkdayRules.isWorkday(today)) {
            return RegisterCheckInResult.Error(RegisterCheckInError.NOT_WORKDAY)
        }
        val activeEntity = periodDao.getActive()
            ?: return RegisterCheckInResult.Error(RegisterCheckInError.NO_ACTIVE_PERIOD)
        val active = activeEntity.toDomain()
        if (!active.contains(today)) {
            return RegisterCheckInResult.Error(RegisterCheckInError.OUTSIDE_ACTIVE_PERIOD)
        }
        if (checkInDao.getByWorkDate(active.id, today.toString()) != null) {
            return RegisterCheckInResult.Error(RegisterCheckInError.ALREADY_REGISTERED)
        }
        val prefs = preferencesDataStore.preferencesFlow.first()
        return registerWithPrefs(active.id, today, prefs)
    }

    private suspend fun registerWithPrefs(
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
        checkInDao.insert(checkIn.toEntity(periodId))
        return RegisterCheckInResult.Success(checkIn)
    }

    override suspend fun updateCheckInTime(
        workDate: LocalDate,
        periodId: Long,
        hour: Int,
        minute: Int,
    ): Boolean {
        val existing = checkInDao.getByWorkDate(periodId, workDate.toString()) ?: return false
        val zone = ZoneId.systemDefault()
        val updatedAt = workDate.atTime(hour, minute).atZone(zone)
        val expectedTime = LocalTime.of(existing.expectedHour, existing.expectedMinute)
        val delayMinutes = DelayCalculator.calculateDelayMinutes(
            workDate,
            updatedAt,
            expectedTime,
        )
        checkInDao.update(
            existing.copy(
                checkedInAtEpochMilli = updatedAt.toInstant().toEpochMilli(),
                delayMinutes = delayMinutes,
            ),
        )
        return true
    }

    override suspend fun deleteCheckInsBetween(
        periodId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int = checkInDao.deleteBetween(periodId, startDate.toString(), endDate.toString())

    override suspend fun setDisplayName(name: String) {
        preferencesDataStore.setDisplayName(name)
    }

    override suspend fun setExpectedTime(hour: Int, minute: Int) {
        preferencesDataStore.setExpectedTime(hour, minute)
    }

    override suspend fun clearExpectedTime() {
        preferencesDataStore.clearExpectedTime()
    }

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
