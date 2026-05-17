package com.fitness.training.data.dao

import androidx.room.*
import com.fitness.training.data.entity.TrainingPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    fun getAllPlans(userId: Long): Flow<List<TrainingPlan>>
    
    @Query("SELECT * FROM training_plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): TrainingPlan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TrainingPlan): Long
    
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
