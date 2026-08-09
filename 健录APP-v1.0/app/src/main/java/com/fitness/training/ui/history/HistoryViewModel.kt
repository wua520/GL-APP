package com.fitness.training.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.Workout
import com.fitness.training.data.repository.WorkoutRepository
import com.fitness.training.data.repository.WorkoutWithDetails
import com.fitness.training.util.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: WorkoutRepository
    private val app = application
    
    private val _workouts = MutableLiveData<List<Workout>>(emptyList())
    val workouts: LiveData<List<Workout>> = _workouts
    
    private val _totalWorkouts = MutableLiveData(0)
    val totalWorkouts: LiveData<Int> = _totalWorkouts
    
    private val _thisMonthWorkouts = MutableLiveData(0)
    val thisMonthWorkouts: LiveData<Int> = _thisMonthWorkouts
    
    private val _selectedWorkoutDetails = MutableLiveData<WorkoutWithDetails?>()
    val selectedWorkoutDetails: LiveData<WorkoutWithDetails?> = _selectedWorkoutDetails
    
    private val _workoutDates = MutableLiveData<Set<String>>(emptySet())
    val workoutDates: LiveData<Set<String>> = _workoutDates
    
    private val _dayWorkouts = MutableLiveData<List<Workout>>(emptyList())
    val dayWorkouts: LiveData<List<Workout>> = _dayWorkouts
    
    private val _workoutDetail = MutableLiveData<WorkoutWithDetails?>()
    val workoutDetail: LiveData<WorkoutWithDetails?> = _workoutDetail
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private fun getCurrentUserId(): Long = UserSession.getCurrentUserId(app)
    
    init {
        val db = FitnessDatabase.getDatabase(application)
        repository = WorkoutRepository(
            db.workoutDao(),
            db.workoutExerciseDao(),
            db.workoutSetDao(),
            db.exerciseDao()
        )
        loadWorkouts()
        loadStatistics()
    }
    
    private fun loadWorkouts() {
        viewModelScope.launch {
            repository.getAllWorkouts(getCurrentUserId()).collect { list ->
                _workouts.value = list.filter { it.duration > 0 }
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            repository.getWorkoutCount(getCurrentUserId()).collect { count ->
                _totalWorkouts.value = count
            }
        }
        
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis
            
            val count = repository.getWorkoutCountByDateRange(getCurrentUserId(), startOfMonth, endOfMonth)
            _thisMonthWorkouts.value = count
        }
    }
    
    fun loadWorkoutDetails(workoutId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val details = repository.getWorkoutWithDetails(workoutId)
            _selectedWorkoutDetails.value = details
            _isLoading.value = false
        }
    }
    
    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            repository.deleteWorkout(workout)
            loadStatistics()
        }
    }
    
    fun clearSelectedWorkout() {
        _selectedWorkoutDetails.value = null
    }
    
    fun loadWorkoutDates() {
        viewModelScope.launch {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val workouts = repository.getAllWorkouts(getCurrentUserId()).first()
            val dates = workouts
                .filter { it.duration > 0 }
                .map { dateFormat.format(java.util.Date(it.date)) }
                .toSet()
            _workoutDates.value = dates
        }
    }
    
    fun loadDayWorkouts(startOfDay: Long, endOfDay: Long) {
        viewModelScope.launch {
            val workouts = repository.getWorkoutsByDateRange(getCurrentUserId(), startOfDay, endOfDay).first()
            _dayWorkouts.value = workouts.filter { it.duration > 0 }
        }
    }
    
    fun loadWorkoutDetail(workoutId: Long) {
        viewModelScope.launch {
            val details = repository.getWorkoutWithDetails(workoutId)
            _workoutDetail.value = details
        }
    }
}
