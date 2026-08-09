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
    
    // AI聊天接口（已废弃，使用Agent接口）
    @POST("api/ai/chat")
    suspend fun aiChat(
        @Header("Authorization") token: String,
        @Body request: AiChatRequest
    ): Response<ApiResponse<AiChatResponse>>
    
    @POST("api/ai/chat/stream")
    suspend fun aiChatStream(
        @Header("Authorization") token: String,
        @Body request: AiChatRequest
    ): Response<ApiResponse<AiChatResponse>>
    
    // Agent接口
    @POST("api/agent/tasks")
    suspend fun createAgentTask(
        @Header("Authorization") token: String,
        @Body request: com.fitness.training.network.dto.CreateAgentTaskRequest
    ): Response<ApiResponse<com.fitness.training.network.dto.AgentTaskResponse>>
    
    @GET("api/agent/tasks/{taskId}")
    suspend fun getAgentTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long
    ): Response<ApiResponse<com.fitness.training.network.dto.AgentTaskResponse>>
    
    @POST("api/agent/tasks/{taskId}/confirm")
    suspend fun confirmAgentAction(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long,
        @Body request: com.fitness.training.network.dto.ConfirmActionRequest
    ): Response<ApiResponse<com.fitness.training.network.dto.AgentTaskResponse>>
    
    @POST("api/agent/tasks/{taskId}/actions/{actionId}/cancel")
    suspend fun cancelAgentAction(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long,
        @Path("actionId") actionId: Long
    ): Response<ApiResponse<com.fitness.training.network.dto.AgentTaskResponse>>

    @POST("api/agent/tasks/{taskId}/cancel")
    suspend fun cancelAgentTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long
    ): Response<ApiResponse<String>>
    
    @POST("api/agent/tasks/{taskId}/actions/{actionId}/complete")
    suspend fun completeLocalWrite(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long,
        @Path("actionId") actionId: Long,
        @Body request: com.fitness.training.network.dto.CompleteLocalWriteRequest
    ): Response<ApiResponse<com.fitness.training.network.dto.AgentTaskResponse>>
    
    @GET("api/agent/pending-local-writes")
    suspend fun getPendingLocalWrites(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<com.fitness.training.network.dto.PendingLocalWrite>>>
    
    @PUT("api/agent/tasks/{taskId}/actions/{actionId}/payload")
    suspend fun updateActionPayload(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long,
        @Path("actionId") actionId: Long,
        @Body request: com.fitness.training.network.dto.UpdatePayloadRequest
    ): Response<ApiResponse<com.fitness.training.network.dto.AgentTaskResponse>>
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
    val uploadStatus: SyncStatus?,
    val downloadStatus: SyncStatus?,
    val workouts: List<WorkoutData>?,
    val dietRecords: List<DietRecordData>?,
    val bodyRecords: List<BodyRecordData>?,
    val trainingPlans: List<TrainingPlanData>?,
    val bodyProfile: BodyProfileData?,
    val customExercises: List<CustomExerciseData>?,
    val workoutTemplates: List<WorkoutTemplateData>?,
    val favoriteExercises: List<FavoriteExerciseData>?
)

data class SyncStatus(
    val success: Boolean,
    val totalItems: Int,
    val successItems: Int,
    val failedItems: Int,
    val errors: List<SyncErrorDetail> = emptyList()
)

data class SyncErrorDetail(
    val entityType: String,
    val localId: String,
    val errorCode: String,
    val errorMessage: String,
    val retryable: Boolean
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
    val agentActionId: Long? = null, // 可选，用于云端同步时传递
    val recordKey: String? = null,   // 记录级别的幂等键
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
    val agentActionId: Long? = null, // 可选，用于云端同步时传递
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

// AI聊天请求
data class AiChatRequest(
    val messages: List<AiMessage>
)

data class AiMessage(
    val role: String,  // "user" 或 "assistant"
    val content: String
)

// AI聊天响应
data class AiChatResponse(
    val message: String
)
