package com.example.puntual.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.puntual.data.local.entity.AbsenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsenceDao {

    @Query(
        """
        SELECT * FROM absences
        WHERE periodId = :periodId
        AND startDate <= :endDate
        AND endDate >= :startDate
        ORDER BY startDate ASC
        """,
    )
    fun observeBetween(periodId: Long, startDate: String, endDate: String): Flow<List<AbsenceEntity>>

    @Query(
        """
        SELECT * FROM absences
        WHERE periodId = :periodId
        ORDER BY startDate DESC
        """,
    )
    fun observeForPeriod(periodId: Long): Flow<List<AbsenceEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(absence: AbsenceEntity): Long

    @Query("DELETE FROM absences WHERE id = :absenceId")
    suspend fun deleteById(absenceId: Long)
}
