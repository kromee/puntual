package com.example.puntual.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "absences")
data class AbsenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val periodId: Long,
    val startDate: String,
    val endDate: String,
    val type: String,
    val reason: String,
    val status: String,
    val createdAtEpochMilli: Long,
)
