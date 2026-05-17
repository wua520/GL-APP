package com.fitness.training.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<String>>
    
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

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
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
    val trainingPlans: List<TrainingPlanData>?,
    val bodyProfile: BodyProfileData?,
    val customExercises: List<CustomExerciseData>?,
    val workoutTemplates: List<WorkoutTemplateData>?,
    val favoriteExercises: List<FavoriteExerciseData>?
)

data class SyncResponse(
    val serverTime: Long,
    val workouts: List<WorkoutData>?,
    val dietRecords: List<DietRecordData>?,
    val bodyRecords: List<BodyRecordData>?,
    val trainingPlans: List<TrainingPlanData>?,
    val bodyProfile: BodyProfileData?,
    val customExercises: List<CustomExerciseData>?,
    val workoutTemplates: List<WorkoutTemplateData>?,
    val favoriteExercises: List<FavoriteExerciseData>?
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

// 身体档案
data class BodyProfileData(
    val gender: Int,
    val height: Int,
    val birthYear: Int,
    val updatedAt: Long
)

// 自定义动作
data class CustomExerciseData(
    val localId: Long,
    val name: String,
    val muscleGroup: String,
    val subMuscleGroup: String,
    val equipment: String,
    val description: String,
    val imageUrl: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

// 训练模板
data class WorkoutTemplateData(
    val localId: Long,
    val name: String,
    val description: String,
    val exercises: List<TemplateExerciseData>,
    val createdAt: Long,
    val updatedAt: Long
)

data class TemplateExerciseData(
    val localId: Long,
    val exerciseName: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: Int
)

// 收藏动作
data class FavoriteExerciseData(
    val exerciseName: String,
    val createdAt: Long
)
