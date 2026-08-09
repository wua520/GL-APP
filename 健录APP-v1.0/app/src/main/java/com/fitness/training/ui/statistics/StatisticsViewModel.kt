package com.fitness.training.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.Exercise
import com.fitness.training.data.repository.WorkoutRepository
import com.fitness.training.util.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: WorkoutRepository
    private val app = application
    
    private val _weekWorkouts = MutableLiveData(0)
    val weekWorkouts: LiveData<Int> = _weekWorkouts
    
    private val _weekVolume = MutableLiveData(0.0)
    val weekVolume: LiveData<Double> = _weekVolume
    
    private val _weekDuration = MutableLiveData(0L)
    val weekDuration: LiveData<Long> = _weekDuration
    
    private val _muscleStats = MutableLiveData<List<MuscleGroupStat>>(emptyList())
    val muscleStats: LiveData<List<MuscleGroupStat>> = _muscleStats
    
    private val _personalRecords = MutableLiveData<List<PersonalRecord>>(emptyList())
    val personalRecords: LiveData<List<PersonalRecord>> = _personalRecords
    
    private val _exerciseList = MutableLiveData<List<String>>(emptyList())
    val exerciseList: LiveData<List<String>> = _exerciseList
    
    private val _strengthCurveData = MutableLiveData<List<StrengthDataPoint>>(emptyList())
    val strengthCurveData: LiveData<List<StrengthDataPoint>> = _strengthCurveData
    
    private val _monthlyVolumeData = MutableLiveData<List<VolumePoint>>(emptyList())
    val monthlyVolumeData: LiveData<List<VolumePoint>> = _monthlyVolumeData
    
    private val _dailyVolumeData = MutableLiveData<List<VolumePoint>>(emptyList())
    val dailyVolumeData: LiveData<List<VolumePoint>> = _dailyVolumeData
    
    private val _weeklyVolumeData = MutableLiveData<List<VolumePoint>>(emptyList())
    val weeklyVolumeData: LiveData<List<VolumePoint>> = _weeklyVolumeData
    
    private fun getCurrentUserId(): Long = UserSession.getCurrentUserId(app)
    
    init {
        val db = FitnessDatabase.getDatabase(application)
        repository = WorkoutRepository(
            db.workoutDao(),
            db.workoutExerciseDao(),
            db.workoutSetDao(),
            db.exerciseDao()
        )
        loadWeeklyStats()
        loadMuscleStats()
        loadPersonalRecords()
        loadExerciseList()
        loadDailyVolume()
        loadWeeklyVolume()
        loadMonthlyVolume()
    }
    
    fun refresh() {
        loadWeeklyStats()
        loadMuscleStats()
        loadPersonalRecords()
        loadExerciseList()
        loadDailyVolume()
        loadWeeklyVolume()
        loadMonthlyVolume()
    }
    
    private fun loadExerciseList() {
        viewModelScope.launch {
            val exerciseNames = mutableSetOf<String>()
            val workouts = repository.getAllWorkouts(getCurrentUserId()).first()
            workouts.forEach { workout ->
                val details = repository.getWorkoutWithDetails(workout.id)
                details?.exercises?.forEach { ex ->
                    ex.exercise?.name?.let { exerciseNames.add(it) }
                }
            }
            _exerciseList.value = exerciseNames.toList().sorted()
        }
    }
    
    fun loadStrengthCurve(exerciseName: String) {
        viewModelScope.launch {
            val dataPoints = mutableListOf<StrengthDataPoint>()
            
            val workouts = repository.getAllWorkouts(getCurrentUserId()).first()
                .filter { it.duration > 0 }
                .sortedBy { it.date }
            
            workouts.forEach { workout ->
                val details = repository.getWorkoutWithDetails(workout.id)
                details?.exercises?.forEach { ex ->
                    if (ex.exercise?.name == exerciseName) {
                        val maxWeight = ex.sets.maxOfOrNull { it.weight } ?: 0.0
                        if (maxWeight > 0) {
                            dataPoints.add(StrengthDataPoint(
                                date = workout.date,
                                weight = maxWeight
                            ))
                        }
                    }
                }
            }
            
            _strengthCurveData.value = dataPoints
        }
    }
    
    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.timeInMillis
            
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            val endOfWeek = calendar.timeInMillis
            
            val count = repository.getWorkoutCountByDateRange(getCurrentUserId(), startOfWeek, endOfWeek)
            _weekWorkouts.value = count
            
            val duration = repository.getTotalDurationByDateRange(getCurrentUserId(), startOfWeek, endOfWeek) ?: 0L
            _weekDuration.value = duration
            
            val workouts = repository.getWorkoutsByDateRange(getCurrentUserId(), startOfWeek, endOfWeek).first()
            var totalVolume = 0.0
            workouts.forEach { workout ->
                val details = repository.getWorkoutWithDetails(workout.id)
                details?.exercises?.forEach { ex ->
                    ex.sets.forEach { set ->
                        totalVolume += set.weight * set.reps
                    }
                }
            }
            _weekVolume.value = totalVolume
        }
    }
    
    private fun loadMuscleStats() {
        viewModelScope.launch {
            val muscleMap = mutableMapOf<String, Int>()
            
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val startTime = calendar.timeInMillis
            
            val workouts = repository.getWorkoutsByDateRange(getCurrentUserId(), startTime, endTime).first()
            workouts.forEach { workout ->
                val details = repository.getWorkoutWithDetails(workout.id)
                details?.exercises?.forEach { ex ->
                    val muscleGroup = ex.exercise?.muscleGroup ?: "其他"
                    muscleMap[muscleGroup] = (muscleMap[muscleGroup] ?: 0) + ex.sets.size
                }
            }
            
            val stats = muscleMap.map { (muscle, sets) ->
                MuscleGroupStat(muscle, sets)
            }.sortedByDescending { it.setCount }
            
            _muscleStats.value = stats
        }
    }
    
    private fun loadPersonalRecords() {
        viewModelScope.launch {
            val prMap = mutableMapOf<String, PersonalRecord>()
            
            val workouts = repository.getAllWorkouts(getCurrentUserId()).first()
            workouts.forEach { workout ->
                val details = repository.getWorkoutWithDetails(workout.id)
                details?.exercises?.forEach { ex ->
                    val exerciseName = ex.exercise?.name ?: return@forEach
                    ex.sets.forEach { set ->
                        val currentPR = prMap[exerciseName]
                        if (currentPR == null || set.weight > currentPR.weight) {
                            prMap[exerciseName] = PersonalRecord(
                                exerciseName = exerciseName,
                                weight = set.weight,
                                reps = set.reps,
                                date = workout.date
                            )
                        }
                    }
                }
            }
            
            val records = prMap.values
                .filter { it.weight > 0 }
                .sortedByDescending { it.weight }
                .take(10)
            
            _personalRecords.value = records.toList()
        }
    }
    
    private fun loadMonthlyVolume() {
        viewModelScope.launch {
            val monthlyMap = mutableMapOf<String, Double>()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            
            val workouts = repository.getAllWorkouts(getCurrentUserId()).first()
            workouts.forEach { workout ->
                val monthKey = dateFormat.format(java.util.Date(workout.date))
                val details = repository.getWorkoutWithDetails(workout.id)
                var workoutVolume = 0.0
                details?.exercises?.forEach { ex ->
                    ex.sets.forEach { set ->
                        workoutVolume += set.weight * set.reps
                    }
                }
                monthlyMap[monthKey] = (monthlyMap[monthKey] ?: 0.0) + workoutVolume
            }
            
            val sortedData = monthlyMap.entries
                .sortedBy { it.key }
                .takeLast(6)
                .map { VolumePoint(it.key.substring(5) + "月", it.value) }
            
            _monthlyVolumeData.value = sortedData
        }
    }
    
    fun loadDailyVolume() {
        viewModelScope.launch {
            val dailyMap = mutableMapOf<String, Double>()
            val dateFormat = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -14)
            val startTime = calendar.timeInMillis
            
            val workouts = repository.getWorkoutsByDateRange(getCurrentUserId(), startTime, endTime).first()
            workouts.forEach { workout ->
                val dayKey = dateFormat.format(java.util.Date(workout.date))
                val details = repository.getWorkoutWithDetails(workout.id)
                var workoutVolume = 0.0
                details?.exercises?.forEach { ex ->
                    ex.sets.forEach { set ->
                        workoutVolume += set.weight * set.reps
                    }
                }
                dailyMap[dayKey] = (dailyMap[dayKey] ?: 0.0) + workoutVolume
            }
            
            val sortedData = dailyMap.entries
                .sortedBy { it.key }
                .map { VolumePoint(it.key, it.value) }
            
            _dailyVolumeData.value = sortedData
        }
    }
    
    fun loadWeeklyVolume() {
        viewModelScope.launch {
            val weeklyMap = mutableMapOf<Long, Double>()
            val calendar = Calendar.getInstance()
            
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.WEEK_OF_YEAR, -8)
            val startTime = calendar.timeInMillis
            
            val workouts = repository.getWorkoutsByDateRange(getCurrentUserId(), startTime, endTime).first()
            workouts.forEach { workout ->
                val workoutCal = Calendar.getInstance()
                workoutCal.timeInMillis = workout.date
                workoutCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                workoutCal.set(Calendar.HOUR_OF_DAY, 0)
                workoutCal.set(Calendar.MINUTE, 0)
                workoutCal.set(Calendar.SECOND, 0)
                workoutCal.set(Calendar.MILLISECOND, 0)
                val weekStart = workoutCal.timeInMillis
                
                val details = repository.getWorkoutWithDetails(workout.id)
                var workoutVolume = 0.0
                details?.exercises?.forEach { ex ->
                    ex.sets.forEach { set ->
                        workoutVolume += set.weight * set.reps
                    }
                }
                weeklyMap[weekStart] = (weeklyMap[weekStart] ?: 0.0) + workoutVolume
            }
            
            val sortedData = weeklyMap.entries
                .sortedBy { it.key }
                .takeLast(8)
                .map { entry ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = entry.key
                    val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
                    VolumePoint("${weekNum}周", entry.value) 
                }
            
            _weeklyVolumeData.value = sortedData
        }
    }
}

data class MuscleGroupStat(
    val muscleGroup: String,
    val setCount: Int
)

data class PersonalRecord(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val date: Long
)

data class StrengthDataPoint(
    val date: Long,
    val weight: Double
)

data class VolumePoint(
    val label: String,
    val volume: Double
)
