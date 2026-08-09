package com.fitness.training.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.*
import com.fitness.training.data.entity.TrainingPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    fun getAllPlans(userId: Long): Flow<List<TrainingPlan>>
    
    @Query("SELECT * FROM training_plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): TrainingPlan?
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId AND agentActionId = :actionId LIMIT 1")
    suspend fun getPlanByActionId(userId: Long, actionId: Long): TrainingPlan?
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlan(plan: TrainingPlan): Long
    
    /**
     * 幂等插入训练计划
     * 如果 agentActionId > 0 且已存在，返回已有计划的 ID；否则插入新计划
     * agentActionId = null 表示非 Agent 创建，不进行幂等性检查
     */
    suspend fun insertPlanIdempotent(plan: TrainingPlan): Long {
        // 如果 agentActionId > 0（Agent 创建），先检查是否已存在
        val actionId = plan.agentActionId
        if (actionId != null && actionId > 0) {
            val existing = getPlanByActionId(plan.userId, actionId)
            if (existing != null) {
                // 已存在，返回已有 ID
                return existing.id
            }
        }
        // 不存在或非 Agent 创建，插入新计划。并发恢复可能同时命中唯一索引，
        // 此时重新读取既有记录，仍保持调用方可重复执行。
        return try {
            insertPlan(plan)
        } catch (e: SQLiteConstraintException) {
            if (actionId != null && actionId > 0) {
                getPlanByActionId(plan.userId, actionId)?.id ?: throw e
            } else {
                throw e
            }
        }
    }
    
    @Update
    suspend fun updatePlan(plan: TrainingPlan)
    
    @Delete
    suspend fun deletePlan(plan: TrainingPlan)
    
    @Query("DELETE FROM training_plans WHERE id = :planId")
    suspend fun deletePlanById(planId: Long)
    
    @Query("UPDATE training_plans SET isPinned = :isPinned WHERE id = :planId")
    suspend fun updatePinned(planId: Long, isPinned: Boolean)
    
    @Query("DELETE FROM training_plans WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    fun getAllPlansSync(userId: Long): List<TrainingPlan>
    
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getTrainingPlansByUserId(userId: Long): List<TrainingPlan>
}
