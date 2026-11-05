package com.example.puntual.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Absence(
    val id: Long = 0,
    val periodId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val type: AbsenceType,
    val reason: String,
    val status: AbsenceStatus = AbsenceStatus.APPROVED,
) {
    val dayCount: Long
        get() = ChronoUnit.DAYS.between(startDate, endDate) + 1

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)
}

enum class AbsenceType(val label: String) {
    VACATION("Vacaciones"),
    PERSONAL_PERMISSION("Permiso"),
    SICK_LEAVE("Incapacidad"),
    HOLIDAY("Día festivo"),
    FIELD_WORK("Trabajo fuera de oficina"),
}

enum class AbsenceStatus(val label: String) {
    PENDING("Pendiente"),
    APPROVED("Aprobado"),
    REJECTED("Rechazado"),
    CANCELLED("Cancelado"),
}
