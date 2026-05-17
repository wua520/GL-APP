package com.fitness.training.ui.diet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.FoodData
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.DietRecord
import com.fitness.training.data.entity.Food
import com.fitness.training.util.BodyProfile
import com.fitness.training.util.UserSession
import kotlinx.coroutines.launch
import java.util.Calendar

class DietViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = FitnessDatabase.getDatabase(application)
    private val dietRecordDao = db.dietRecordDao()
    private val foodDao = db.foodDao()
    private val workoutDao = db.workoutDao()
    private val bodyRecordDao = db.bodyRecordDao()
    private val app = application
    
    private val _selectedDate = MutableLiveData<Long>(System.currentTimeMillis())
    val selectedDate: LiveData<Long> = _selectedDate
    
    private val _todayRecords = MutableLiveData<List<DietRecord>>(emptyList())
    val todayRecords: LiveData<List<DietRecord>> = _todayRecords
    
    private val _totalCalories = MutableLiveData(0)
    val totalCalories: LiveData<Int> = _totalCalories
    
    private val _totalBurn = MutableLiveData(0)
    val totalBurn: LiveData<Int> = _totalBurn
    
    private val _needBodyData = MutableLiveData(false)
    val needBodyData: LiveData<Boolean> = _needBodyData
    
    private val _totalProtein = MutableLiveData(0f)
    val totalProtein: LiveData<Float> = _totalProtein
    
    private val _totalCarbs = MutableLiveData(0f)
    val totalCarbs: LiveData<Float> = _totalCarbs
    
    private val _totalFat = MutableLiveData(0f)
    val totalFat: LiveData<Float> = _totalFat
    
    private fun getCurrentUserId(): Long = UserSession.getCurrentUserId(app)
    
    init {
        initFoodDatabase()
        loadTodayRecords()
    }
    
    private fun initFoodDatabase() {
        viewModelScope.launch {
            val count = foodDao.getCount()
            if (count == 0) {
                foodDao.insertAll(FoodData.defaultFoods)
            }
        }
    }
    
    fun searchFoods(query: String, callback: (List<Food>) -> Unit) {
        viewModelScope.launch {
            val results = if (query.isBlank()) {
                emptyList()
            } else {
                foodDao.searchFoods(getCurrentUserId(), query)
            }
            callback(results)
        }
    }
    
    fun getAllFoods(callback: (List<Food>) -> Unit) {
        viewModelScope.launch {
            foodDao.getAllFoods(getCurrentUserId()).collect { foods ->
                callback(foods)
            }
        }
    }
    
    fun addCustomFood(
        name: String,
        unit: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val food = Food(
                userId = getCurrentUserId(),
                name = name,
                category = "自定义",
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                unit = unit,
                isCustom = true
            )
            foodDao.insert(food)
            onComplete()
        }
    }
    
    fun deleteCustomFood(food: Food, onComplete: () -> Unit) {
        viewModelScope.launch {
            foodDao.delete(food)
            onComplete()
        }
    }
    
    fun loadTodayRecords() {
        viewModelScope.launch {
            val (startOfDay, endOfDay) = getDayRange(_selectedDate.value ?: System.currentTimeMillis())
            val records = dietRecordDao.getRecordsByDateSync(getCurrentUserId(), startOfDay, endOfDay)
            _todayRecords.value = records
            calculateTotals(records)
            calculateBurn(startOfDay, endOfDay)
        }
    }
    
    private suspend fun calculateBurn(startOfDay: Long, endOfDay: Long) {
        // 检查是否有完整的身体数据
        val height = BodyProfile.getHeight(app)
        val latestBody = bodyRecordDao.getLatestByUserSync(getCurrentUserId())
        val weight = latestBody?.weight
        
        if (height <= 0 || weight == null || weight <= 0) {
            // 没有完整身体数据，提示用户填写
            _needBodyData.value = true
            _totalBurn.value = 0
            return
        }
        
        _needBodyData.value = false
        
        // 1. 计算基础代谢 (BMR)
        val bmr = calculateBMR(height, weight)
        
        // 2. 获取当日训练消耗
        // 运动消耗 = 总训练量(kg) × 0.1 + 训练时长(分钟) × 3
        val totalVolume = workoutDao.getTotalVolumeByDateRange(getCurrentUserId(), startOfDay, endOfDay)
        val totalDuration = workoutDao.getTotalDurationByDateRange(getCurrentUserId(), startOfDay, endOfDay) ?: 0L
        val durationMinutes = totalDuration / 60000.0
        
        val exerciseBurn = (totalVolume * 0.1 + durationMinutes * 3).toInt()
        
        // 总消耗 = BMR + 训练消耗
        _totalBurn.value = bmr + exerciseBurn
    }
    
    private fun calculateBMR(height: Int, weight: Float): Int {
        // 获取身体数据
        val gender = BodyProfile.getGender(app)
        val birthYear = BodyProfile.getBirthYear(app)
        
        // 计算年龄
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val age = if (birthYear > 0) currentYear - birthYear else 25
        
        // 使用 Mifflin-St Jeor 公式计算 BMR
        return if (gender == 1) {
            // 男性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 + 5
            (10 * weight + 6.25 * height - 5 * age + 5).toInt()
        } else {
            // 女性: BMR = 10 × 体重(kg) + 6.25 × 身高(cm) - 5 × 年龄 - 161
            (10 * weight + 6.25 * height - 5 * age - 161).toInt()
        }
    }
    
    private fun calculateTotals(records: List<DietRecord>) {
        _totalCalories.value = records.sumOf { it.calories }
        _totalProtein.value = records.map { it.protein }.sum()
        _totalCarbs.value = records.map { it.carbs }.sum()
        _totalFat.value = records.map { it.fat }.sum()
    }
    
    fun addRecord(
        mealType: String,
        foodName: String,
        amount: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ) {
        viewModelScope.launch {
            val record = DietRecord(
                userId = getCurrentUserId(),
                date = _selectedDate.value ?: System.currentTimeMillis(),
                mealType = mealType,
                foodName = foodName,
                amount = amount,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )
            dietRecordDao.insert(record)
            loadTodayRecords()
        }
    }
    
    fun deleteRecord(record: DietRecord) {
        viewModelScope.launch {
            dietRecordDao.delete(record)
            loadTodayRecords()
        }
    }
    
    fun getDatesWithRecords(callback: (Set<String>) -> Unit) {
        viewModelScope.launch {
            val dates = dietRecordDao.getAllRecordDates(getCurrentUserId())
            val dateStrings = dates.map { timestamp ->
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
            }.toSet()
            callback(dateStrings)
        }
    }
    
    fun setDate(date: Long) {
        _selectedDate.value = date
        loadTodayRecords()
    }
    
    private fun getDayRange(timestamp: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return Pair(startOfDay, endOfDay)
    }
}
