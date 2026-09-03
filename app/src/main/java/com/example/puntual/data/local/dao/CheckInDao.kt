package com.example.puntual.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.puntual.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    @Query(
        """
        SELECT * FROM check_ins
        WHERE periodId = :periodId AND workDate = :workDate LIMIT 1
        """,
    )
    fun observeByWorkDate(periodId: Long, workDate: String): Flow<CheckInEntity?>

    @Query(
        """
        SELECT * FROM check_ins
        WHERE periodId = :periodId AND workDate = :workDate LIMIT 1
        """,
    )
    suspend fun getByWorkDate(periodId: Long, workDate: String): CheckInEntity?

    @Query(
        """
        SELECT * FROM check_ins
        WHERE periodId = :periodId AND workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate ASC
        """,
    )
    fun observeBetween(periodId: Long, startDate: String, endDate: String): Flow<List<CheckInEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM check_ins
        WHERE periodId = :periodId AND delayMinutes > 0
        AND workDate BETWEEN :startDate AND :endDate
        """,
    )
    fun observeLateDayCount(periodId: Long, startDate: String, endDate: String): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(delayMinutes), 0) FROM check_ins
        WHERE periodId = :periodId AND workDate BETWEEN :startDate AND :endDate
        """,
    )
    fun observeTotalDelayMinutes(periodId: Long, startDate: String, endDate: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_ins WHERE periodId = :periodId")
    suspend fun countForPeriod(periodId: Long): Int

    @Query("SELECT COUNT(*) FROM check_ins")
    suspend fun count(): Int

    @Query("SELECT MIN(workDate) FROM check_ins WHERE periodId = :periodId")
    suspend fun getEarliestWorkDate(periodId: Long): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(checkIn: CheckInEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(checkIns: List<CheckInEntity>)

    @Update
    suspend fun update(checkIn: CheckInEntity)

    @Query(
        """
        DELETE FROM check_ins
        WHERE periodId = :periodId AND workDate BETWEEN :startDate AND :endDate
        """,
    )
    suspend fun deleteBetween(periodId: Long, startDate: String, endDate: String): Int
}
