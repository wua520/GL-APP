package com.fitness.training.data.repository

import com.fitness.training.data.dao.TrainingPlanDao
import com.fitness.training.data.entity.TrainingPlan
import kotlinx.coroutines.flow.Flow

class TrainingPlanRepository(private val trainingPlanDao: TrainingPlanDao) {
    
    fun getAllPlans(userId: Long): Flow<List<TrainingPlan>> = trainingPlanDao.getAllPlans(userId)
    
    suspend fun insertPlan(plan: TrainingPlan): Long {
        return trainingPlanDao.insertPlan(plan)
    }
    
    suspend fun updatePlan(plan: TrainingPlan) {
        trainingPlanDao.updatePlan(plan)
    }
    
    suspend fun deletePlan(plan: TrainingPlan) {
        trainingPlanDao.deletePlan(plan)
    }
    
    suspend fun deletePlanById(planId: Long) {
        trainingPlanDao.deletePlanById(planId)
    }
    
    suspend fun getPlanById(planId: Long): TrainingPlan? {
        return trainingPlanDao.getPlanById(planId)
    }
}
