package com.example.puntual.data.mapper

import com.example.puntual.data.remote.supabase.SupabaseAbsenceDto
import com.example.puntual.data.remote.supabase.SupabaseAbsenceInsertDto
import com.example.puntual.data.remote.supabase.SupabaseAttendancePeriodDto
import com.example.puntual.data.remote.supabase.SupabaseAttendancePeriodUpsertDto
import com.example.puntual.data.remote.supabase.SupabaseCheckInDto
import com.example.puntual.data.remote.supabase.SupabaseCheckInInsertDto
import com.example.puntual.domain.model.Absence
import com.example.puntual.domain.model.AbsenceStatus
import com.example.puntual.domain.model.AbsenceType
import com.example.puntual.domain.model.AttendancePeriod
import com.example.puntual.domain.model.CheckIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun SupabaseAttendancePeriodDto.toDomain(): AttendancePeriod = AttendancePeriod(
    id = id,
    title = title,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let { LocalDate.parse(it) },
    isActive = isActive,
)

fun AttendancePeriod.toSupabaseUpsert(userId: String): SupabaseAttendancePeriodUpsertDto =
    SupabaseAttendancePeriodUpsertDto(
        userId = userId,
        title = title,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
        isActive = isActive,
    )

fun SupabaseCheckInDto.toDomain(): CheckIn = CheckIn(
    workDate = LocalDate.parse(workDate),
    checkedInAt = Instant.parse(checkedInAt),
    expectedTime = LocalTime.of(expectedHour, expectedMinute),
    delayMinutes = delayMinutes,
)

fun CheckIn.toSupabaseInsert(userId: String, periodId: Long): SupabaseCheckInInsertDto =
    SupabaseCheckInInsertDto(
        userId = userId,
        periodId = periodId,
        workDate = workDate.toString(),
        checkedInAt = checkedInAt.toString(),
        expectedHour = expectedTime.hour,
        expectedMinute = expectedTime.minute,
        delayMinutes = delayMinutes,
    )

fun SupabaseAbsenceDto.toDomain(): Absence = Absence(
    id = id,
    periodId = periodId,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    type = AbsenceType.valueOf(type),
    reason = reason,
    status = AbsenceStatus.valueOf(status),
)

fun Absence.toSupabaseInsert(userId: String): SupabaseAbsenceInsertDto =
    SupabaseAbsenceInsertDto(
        userId = userId,
        periodId = periodId,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        type = type.name,
        reason = reason,
        status = status.name,
    )
