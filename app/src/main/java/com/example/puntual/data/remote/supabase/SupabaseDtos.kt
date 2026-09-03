package com.example.puntual.data.remote.supabase

import com.google.gson.annotations.SerializedName

data class SupabaseAttendancePeriodDto(
    val id: Long = 0,
    @SerializedName("user_id")
    val userId: String,
    val title: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String?,
    @SerializedName("is_active")
    val isActive: Boolean,
)

data class SupabaseAttendancePeriodUpsertDto(
    @SerializedName("user_id")
    val userId: String,
    val title: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String?,
    @SerializedName("is_active")
    val isActive: Boolean,
)

data class SupabaseAttendancePeriodPatchDto(
    val title: String? = null,
    @SerializedName("end_date")
    val endDate: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
)

data class SupabaseCheckInDto(
    val id: Long = 0,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("period_id")
    val periodId: Long,
    @SerializedName("work_date")
    val workDate: String,
    @SerializedName("checked_in_at")
    val checkedInAt: String,
    @SerializedName("expected_hour")
    val expectedHour: Int,
    @SerializedName("expected_minute")
    val expectedMinute: Int,
    @SerializedName("delay_minutes")
    val delayMinutes: Int,
)

data class SupabaseCheckInInsertDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("period_id")
    val periodId: Long,
    @SerializedName("work_date")
    val workDate: String,
    @SerializedName("checked_in_at")
    val checkedInAt: String,
    @SerializedName("expected_hour")
    val expectedHour: Int,
    @SerializedName("expected_minute")
    val expectedMinute: Int,
    @SerializedName("delay_minutes")
    val delayMinutes: Int,
)

data class SupabaseCheckInPatchDto(
    @SerializedName("checked_in_at")
    val checkedInAt: String,
    @SerializedName("delay_minutes")
    val delayMinutes: Int,
)

data class SupabaseAbsenceDto(
    val id: Long = 0,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("period_id")
    val periodId: Long,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val type: String,
    val reason: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
)

data class SupabaseAbsenceInsertDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("period_id")
    val periodId: Long,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val type: String,
    val reason: String,
    val status: String,
)
