package com.fitness.training.ui.exercises

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.Exercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExercisesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val exerciseDao = FitnessDatabase.getDatabase(application).exerciseDao()
    
    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> = _exercises
    
    private val _filteredExercises = MutableLiveData<List<Exercise>>(emptyList())
    val filteredExercises: LiveData<List<Exercise>> = _filteredExercises
    
    private var currentFilter: String? = null
    private var currentSearch: String = ""
    private var showFavoritesOnly: Boolean = false
    
    init {
        loadExercises()
    }
    
    private fun loadExercises() {
        viewModelScope.launch {
            exerciseDao.getAllExercises().collect { list ->
                _exercises.value = list
                applyFilters()
            }
        }
    }
    
    fun setFilter(muscleGroup: String?) {
        showFavoritesOnly = muscleGroup == "★ 收藏"
        currentFilter = if (muscleGroup == "全部" || muscleGroup == "★ 收藏") null else muscleGroup
        applyFilters()
    }
    
    fun setSearch(query: String) {
        currentSearch = query
        applyFilters()
    }
    
    private fun applyFilters() {
        val allExercises = _exercises.value ?: emptyList()
        var result = allExercises
        
        // 只显示收藏
        if (showFavoritesOnly) {
            result = result.filter { it.isFavorite }
        }
        
        // 按肌肉群筛选
        if (!currentFilter.isNullOrEmpty()) {
            result = result.filter { it.muscleGroup == currentFilter }
        }
        
        // 按搜索词筛选
        if (currentSearch.isNotEmpty()) {
            result = result.filter { 
                it.name.contains(currentSearch, ignoreCase = true) ||
                it.muscleGroup.contains(currentSearch, ignoreCase = true)
            }
        }
        
        _filteredExercises.value = result
    }
    
    fun addExercise(name: String, muscleGroup: String, equipment: String, description: String, imageUrl: String = "") {
        viewModelScope.launch {
            val exercise = Exercise(
                name = name,
                muscleGroup = muscleGroup,
                equipment = equipment,
                description = description,
                imageUrl = imageUrl,
                isCustom = true
            )
            exerciseDao.insertExercise(exercise)
        }
    }
    
    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseDao.updateExercise(exercise)
        }
    }
    
    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseDao.deleteExercise(exercise)
        }
    }
    
    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            exerciseDao.updateFavorite(exercise.id, !exercise.isFavorite)
            // 手动刷新列表以确保收藏状态更新
            val updatedList = _exercises.value?.map {
                if (it.id == exercise.id) it.copy(isFavorite = !exercise.isFavorite) else it
            } ?: emptyList()
            _exercises.value = updatedList
            applyFilters()
        }
    }
}


