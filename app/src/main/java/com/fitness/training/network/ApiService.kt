package com.fitness.training.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>
    
    @POST("api/sync")
    suspend fun sync(
        @Header("Authorization") token: String,
        @Body request: SyncRequest
    ): Response<ApiResponse<SyncResponse>>
}

// 请求/响应数据类
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String?
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

data class AuthResponse(
    val token: String,
    val userId: Long,
    val username: String,
    val nickname: String
)

data class SyncRequest(
    val lastSyncTime: Long?,
    val workouts: List<WorkoutData>?,
    val dietRecords: List<DietRecordData>?,
    val bodyRecords: List<BodyRecordData>?,
    val trainingPlans: List<TrainingPlanData>?
)

data class SyncResponse(
    val serverTime: Long,
    val workouts: List<WorkoutData>?,
    val dietRecords: List<DietRecordData>?,
    val bodyRecords: List<BodyRecordData>?,
    val trainingPlans: List<TrainingPlanData>?
)

data class WorkoutData(
    val localId: Long,
    val name: String,
    val date: Long,
    val duration: Long,
    val notes: String?,
    val updatedAt: Long,
    val exercises: List<ExerciseData>?
)

data class ExerciseData(
    val localId: Long,
    val exerciseName: String,
    val exerciseOrder: Int,
    val supersetGroupId: Long?,
    val sets: List<SetData>?
)

data class SetData(
    val localId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean,
    val restTime: Int
)

data class DietRecordData(
    val localId: Long,
    val date: Long,
    val mealType: String,
    val foodName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val amount: String?,
    val updatedAt: Long
)

data class BodyRecordData(
    val localId: Long,
    val date: Long,
    val weight: Float?,
    val bodyFat: Float?,
    val muscleMass: Float?,
    val note: String?,
    val updatedAt: Long
)

data class TrainingPlanData(
    val localId: Long,
    val title: String,
    val description: String,
    val details: String,
    val goal: String,
    val experience: String,
    val targetMuscles: String,
    val trainingDays: Int,
    val trainingDuration: String,
    val equipment: String,
    val isPinned: Boolean,
    val isFromRecommendation: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
