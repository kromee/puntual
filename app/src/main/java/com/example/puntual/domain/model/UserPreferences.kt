package com.example.puntual.domain.model

data class UserPreferences(
    val displayName: String = "",
    val hasExpectedTime: Boolean = false,
    val expectedHour: Int = 8,
    val expectedMinute: Int = 0,
    val biometricUnlockEnabled: Boolean = true,
)

/** Solo el nombre de la persona; nunca incluye la marca «Puntual». */
fun UserPreferences.userDisplayName(): String = sanitizeDisplayName(displayName)

/** Cabecera en Home/Historial: nombre o vacío (la UI pone «Puntual» si está vacío). */
fun UserPreferences.headerUserLine(): String = userDisplayName()

fun sanitizeDisplayName(raw: String): String =
    raw.trim()
        .replace(Regex("\\s+Puntual\\s*$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^Puntual\\s+", RegexOption.IGNORE_CASE), "")
        .trim()
