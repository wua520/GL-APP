package com.fitness.training.ui.plans

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.TrainingPlan
import com.fitness.training.data.repository.TrainingPlanRepository
import com.fitness.training.util.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrainingPlanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TrainingPlanRepository
    private val app = application
    
    private val _allPlans = MutableStateFlow<List<TrainingPlan>>(emptyList())
    val allPlans: StateFlow<List<TrainingPlan>> = _allPlans
    
    private fun getCurrentUserId(): Long = UserSession.getCurrentUserId(app)
    
    init {
        val trainingPlanDao = FitnessDatabase.getDatabase(application).trainingPlanDao()
        repository = TrainingPlanRepository(trainingPlanDao)
        loadPlans()
    }
    
    private fun loadPlans() {
        viewModelScope.launch {
            repository.getAllPlans(getCurrentUserId()).collect { plans ->
                _allPlans.value = plans
            }
        }
    }
    
    fun insertPlan(plan: TrainingPlan) = viewModelScope.launch {
        repository.insertPlan(plan.copy(userId = getCurrentUserId()))
    }
    
    fun updatePlan(plan: TrainingPlan) = viewModelScope.launch {
        repository.updatePlan(plan.copy(updatedAt = System.currentTimeMillis()))
    }
    
    fun deletePlan(plan: TrainingPlan) = viewModelScope.launch {
        repository.deletePlan(plan)
    }
    
    fun deletePlanById(planId: Long) = viewModelScope.launch {
        repository.deletePlanById(planId)
    }
    
    fun togglePin(plan: TrainingPlan) = viewModelScope.launch {
        repository.updatePlan(plan.copy(isPinned = !plan.isPinned))
    }
}
