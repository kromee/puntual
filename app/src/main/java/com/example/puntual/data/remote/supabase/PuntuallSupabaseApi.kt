package com.example.puntual.data.remote.supabase

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface PuntuallSupabaseApi {

    @GET("attendance_periods")
    suspend fun getPeriods(
        @Query("select") select: String = "*",
        @Query("user_id") userId: String,
        @Query("order") order: String = "start_date.desc,id.desc",
    ): List<SupabaseAttendancePeriodDto>

    @GET("attendance_periods")
    suspend fun getActivePeriods(
        @Query("select") select: String = "*",
        @Query("user_id") userId: String,
        @Query("is_active") isActive: String = "eq.true",
        @Query("limit") limit: Int = 1,
    ): List<SupabaseAttendancePeriodDto>

    @GET("attendance_periods")
    suspend fun getPeriodById(
        @Query("select") select: String = "*",
        @Query("id") id: String,
        @Query("user_id") userId: String,
        @Query("limit") limit: Int = 1,
    ): List<SupabaseAttendancePeriodDto>

    @Headers("Prefer: return=representation")
    @POST("attendance_periods")
    suspend fun createPeriod(
        @Body period: SupabaseAttendancePeriodUpsertDto,
    ): List<SupabaseAttendancePeriodDto>

    @Headers("Prefer: return=representation")
    @PATCH("attendance_periods")
    suspend fun updatePeriod(
        @Query("id") id: String,
        @Query("user_id") userId: String,
        @Body period: SupabaseAttendancePeriodPatchDto,
    ): List<SupabaseAttendancePeriodDto>

    @PATCH("attendance_periods")
    suspend fun deactivateActivePeriods(
        @Query("user_id") userId: String,
        @Query("is_active") isActive: String = "eq.true",
        @Body period: SupabaseAttendancePeriodPatchDto = SupabaseAttendancePeriodPatchDto(isActive = false),
    )

    @GET("check_ins")
    suspend fun getCheckInsBetween(
        @Query("select") select: String = "*",
        @Query("user_id") userId: String,
        @Query("period_id") periodId: String,
        @Query("work_date") workDateFrom: String,
        @Query("work_date") workDateTo: String,
        @Query("order") order: String = "work_date.asc",
    ): List<SupabaseCheckInDto>

    @GET("check_ins")
    suspend fun getCheckInByDate(
        @Query("select") select: String = "*",
        @Query("user_id") userId: String,
        @Query("period_id") periodId: String,
        @Query("work_date") workDate: String,
        @Query("limit") limit: Int = 1,
    ): List<SupabaseCheckInDto>

    @Headers("Prefer: return=representation")
    @POST("check_ins")
    suspend fun createCheckIn(
        @Body checkIn: SupabaseCheckInInsertDto,
    ): List<SupabaseCheckInDto>

    @PATCH("check_ins")
    suspend fun updateCheckIn(
        @Query("user_id") userId: String,
        @Query("period_id") periodId: String,
        @Query("work_date") workDate: String,
        @Body checkIn: SupabaseCheckInPatchDto,
    )

    @GET("absences")
    suspend fun getAbsencesBetween(
        @Query("select") select: String = "*",
        @Query("user_id") userId: String,
        @Query("period_id") periodId: String,
        @Query("start_date") startsBeforeEnd: String,
        @Query("end_date") endsAfterStart: String,
        @Query("order") order: String = "start_date.asc",
    ): List<SupabaseAbsenceDto>

    @GET("absences")
    suspend fun getAbsencesForPeriod(
        @Query("select") select: String = "*",
        @Query("user_id") userId: String,
        @Query("period_id") periodId: String,
        @Query("order") order: String = "start_date.desc",
    ): List<SupabaseAbsenceDto>

    @Headers("Prefer: return=representation")
    @POST("absences")
    suspend fun createAbsence(
        @Body absence: SupabaseAbsenceInsertDto,
    ): List<SupabaseAbsenceDto>

    @DELETE("absences")
    suspend fun deleteAbsence(
        @Query("id") id: String,
        @Query("user_id") userId: String,
    )
}
