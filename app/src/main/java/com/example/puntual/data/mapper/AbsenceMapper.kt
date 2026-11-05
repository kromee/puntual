package com.example.puntual.data.mapper

import com.example.puntual.data.local.entity.AbsenceEntity
import com.example.puntual.domain.model.Absence
import com.example.puntual.domain.model.AbsenceStatus
import com.example.puntual.domain.model.AbsenceType
import java.time.Instant
import java.time.LocalDate

fun AbsenceEntity.toDomain(): Absence = Absence(
    id = id,
    periodId = periodId,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    type = AbsenceType.valueOf(type),
    reason = reason,
    status = AbsenceStatus.valueOf(status),
)

fun Absence.toEntity(createdAt: Instant = Instant.now()): AbsenceEntity = AbsenceEntity(
    id = id,
    periodId = periodId,
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    type = type.name,
    reason = reason,
    status = status.name,
    createdAtEpochMilli = createdAt.toEpochMilli(),
)
