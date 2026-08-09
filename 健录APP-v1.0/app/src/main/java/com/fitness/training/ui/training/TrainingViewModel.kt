package com.fitness.training.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.*
import com.fitness.training.data.repository.TemplateRepository
import com.fitness.training.data.repository.TemplateWithExercises
import com.fitness.training.data.repository.WorkoutRepository
import com.fitness.training.util.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: WorkoutRepository
    private val templateRepository: TemplateRepository
    private val app = application
    
    private val _templates = MutableLiveData<List<WorkoutTemplate>>(emptyList())
    val templates: LiveData<List<WorkoutTemplate>> = _templates
    
    private val _isTraining = MutableLiveData(false)
    val isTraining: LiveData<Boolean> = _isTraining
    
    private val _isPaused = MutableLiveData(false)
    val isPaused: LiveData<Boolean> = _isPaused
    
    private val _currentWorkoutId = MutableLiveData<Long?>(null)
    val currentWorkoutId: LiveData<Long?> = _currentWorkoutId
    
    private val _workoutExercises = MutableLiveData<List<WorkoutExerciseWithDetails>>(emptyList())
    val workoutExercises: LiveData<List<WorkoutExerciseWithDetails>> = _workoutExercises
    
    private val _totalSets = MutableLiveData(0)
    val totalSets: LiveData<Int> = _totalSets
    
    private val _totalVolume = MutableLiveData(0.0)
    val totalVolume: LiveData<Double> = _totalVolume
    
    private val _workoutNotes = MutableLiveData("")
    val workoutNotes: LiveData<String> = _workoutNotes
    
    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> = _exercises
    
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var pauseStartTime: Long = 0
    
    private fun getCurrentUserId(): Long = UserSession.getCurrentUserId(app)
    
    init {
        val db = FitnessDatabase.getDatabase(application)
        repository = WorkoutRepository(
            db.workoutDao(),
            db.workoutExerciseDao(),
            db.workoutSetDao(),
            db.exerciseDao()
        )
        templateRepository = TemplateRepository(
            db.workoutTemplateDao(),
            db.templateExerciseDao(),
            db.exerciseDao()
        )
        loadExercises()
        loadTemplates()
        initializeDefaultExercises()
    }
    
    private fun loadTemplates() {
        viewModelScope.launch {
            templateRepository.getAllTemplates(getCurrentUserId()).collect { list ->
                _templates.value = list
            }
        }
    }
    
    fun refreshTemplates() {
        loadTemplates()
    }
    
    private fun loadExercises() {
        viewModelScope.launch {
            repository.getAllExercises().collect { list ->
                _exercises.value = list
            }
        }
    }
    
    private fun initializeDefaultExercises() {
        viewModelScope.launch {
            val existingExercises = repository.getAllExercises().first()
            
            // 定义默认动作及其子分类和图片URL (基于wger.de API真实图片，共95个动作)
            val defaultExercises = listOf(
                // ==================== 胸部 (14个) ====================
                Exercise(name = "杠铃卧推", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/192/Bench-press-1.png"),
                Exercise(name = "哑铃卧推", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/97/Dumbbell-bench-press-1.png"),
                Exercise(name = "上斜杠铃卧推", muscleGroup = "胸部", subMuscleGroup = "上胸", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/41/Incline-bench-press-1.png"),
                Exercise(name = "上斜哑铃卧推", muscleGroup = "胸部", subMuscleGroup = "上胸", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/16/Incline-press-1.png"),
                Exercise(name = "下斜杠铃卧推", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/100/Decline-bench-press-1.png"),
                Exercise(name = "下斜哑铃卧推", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/100/Decline-bench-press-1.png"),
                Exercise(name = "窄距卧推", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/61/Close-grip-bench-press-1.png"),
                Exercise(name = "蝴蝶机夹胸", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/98/Butterfly-machine-2.png"),
                Exercise(name = "龙门架夹胸", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/71/Cable-crossover-2.png"),
                Exercise(name = "上斜绳索飞鸟", muscleGroup = "胸部", subMuscleGroup = "上胸", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/122/Incline-cable-flyes-1.png"),
                Exercise(name = "俯卧撑", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/50/695ced5c-9961-4076-add2-cb250d01089e.png"),
                Exercise(name = "双杠臂屈伸", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/83/Bench-dips-1.png"),
                Exercise(name = "器械推胸", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/98/Butterfly-machine-2.png"),
                Exercise(name = "哑铃飞鸟", muscleGroup = "胸部", subMuscleGroup = "中下胸", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/122/Incline-cable-flyes-1.png"),
                // ==================== 背部 (14个) ====================
                Exercise(name = "杠铃划船", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/109/Barbell-rear-delt-row-1.png"),
                Exercise(name = "哑铃划船", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/110/Reverse-grip-bent-over-rows-1.png"),
                Exercise(name = "反握杠铃划船", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/70/Reverse-grip-bent-over-rows-1.png"),
                Exercise(name = "T杠划船", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/106/T-bar-row-1.png"),
                Exercise(name = "引体向上", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/181/Chin-ups-2.png"),
                Exercise(name = "宽握引体向上", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/181/Chin-ups-2.png"),
                Exercise(name = "坐姿划船", muscleGroup = "背部", subMuscleGroup = "斜方肌中下", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/143/Cable-seated-rows-2.png"),
                Exercise(name = "绳索划船", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/143/Cable-seated-rows-2.png"),
                Exercise(name = "硬拉", muscleGroup = "背部", subMuscleGroup = "竖脊肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/161/Dead-lifts-2.png"),
                Exercise(name = "直腿硬拉", muscleGroup = "背部", subMuscleGroup = "竖脊肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/161/Dead-lifts-2.png"),
                Exercise(name = "山羊挺身", muscleGroup = "背部", subMuscleGroup = "竖脊肌", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/128/Hyperextensions-1.png"),
                Exercise(name = "早安式体前屈", muscleGroup = "背部", subMuscleGroup = "竖脊肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/116/Good-mornings-2.png"),
                Exercise(name = "直臂下压", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/143/Cable-seated-rows-2.png"),
                Exercise(name = "单臂哑铃划船", muscleGroup = "背部", subMuscleGroup = "背阔肌", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/110/Reverse-grip-bent-over-rows-1.png"),
                // ==================== 腿部 (14个) ====================
                Exercise(name = "杠铃深蹲", muscleGroup = "腿部", subMuscleGroup = "股四头", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/191/Front-squat-1-857x1024.png"),
                Exercise(name = "前蹲", muscleGroup = "腿部", subMuscleGroup = "股四头", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/191/Front-squat-1-857x1024.png"),
                Exercise(name = "哈克深蹲", muscleGroup = "腿部", subMuscleGroup = "股四头", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/130/Narrow-stance-hack-squats-1-1024x721.png"),
                Exercise(name = "腿举", muscleGroup = "腿部", subMuscleGroup = "股四头", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/130/Narrow-stance-hack-squats-1-1024x721.png"),
                Exercise(name = "腿屈伸", muscleGroup = "腿部", subMuscleGroup = "股四头", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/130/Narrow-stance-hack-squats-1-1024x721.png"),
                Exercise(name = "腿弯举", muscleGroup = "腿部", subMuscleGroup = "股二头", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/154/lying-leg-curl-machine-large-1.png"),
                Exercise(name = "坐姿腿弯举", muscleGroup = "腿部", subMuscleGroup = "股二头", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/117/seated-leg-curl-large-1.png"),
                Exercise(name = "站姿腿弯举", muscleGroup = "腿部", subMuscleGroup = "股二头", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/118/standing-leg-curls-large-1.png"),
                Exercise(name = "箭步蹲", muscleGroup = "腿部", subMuscleGroup = "臀大肌", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/113/Walking-lunges-1.png"),
                Exercise(name = "行走箭步蹲", muscleGroup = "腿部", subMuscleGroup = "臀大肌", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/113/Walking-lunges-1.png"),
                Exercise(name = "罗马尼亚硬拉", muscleGroup = "腿部", subMuscleGroup = "股二头", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/161/Dead-lifts-2.png"),
                Exercise(name = "保加利亚分腿蹲", muscleGroup = "腿部", subMuscleGroup = "臀大肌", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/113/Walking-lunges-1.png"),
                Exercise(name = "臀桥", muscleGroup = "腿部", subMuscleGroup = "臀大肌", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/128/Hyperextensions-1.png"),
                Exercise(name = "哑铃深蹲", muscleGroup = "腿部", subMuscleGroup = "股四头", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/191/Front-squat-1-857x1024.png"),
                // ==================== 小腿 (4个) ====================
                Exercise(name = "站姿提踵", muscleGroup = "小腿", subMuscleGroup = "", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/1243/53d4fabe-c994-4907-873f-8d82813a9832.png"),
                Exercise(name = "坐姿提踵", muscleGroup = "小腿", subMuscleGroup = "", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/1243/53d4fabe-c994-4907-873f-8d82813a9832.png"),
                Exercise(name = "哑铃提踵", muscleGroup = "小腿", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/1243/53d4fabe-c994-4907-873f-8d82813a9832.png"),
                Exercise(name = "单腿提踵", muscleGroup = "小腿", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/1243/53d4fabe-c994-4907-873f-8d82813a9832.png"),
                // ==================== 肩部 (12个) ====================
                Exercise(name = "杠铃推举", muscleGroup = "肩部", subMuscleGroup = "前束", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/119/seated-barbell-shoulder-press-large-1.png"),
                Exercise(name = "哑铃推举", muscleGroup = "肩部", subMuscleGroup = "前束", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/123/dumbbell-shoulder-press-large-1.png"),
                Exercise(name = "阿诺德推举", muscleGroup = "肩部", subMuscleGroup = "前束", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/123/dumbbell-shoulder-press-large-1.png"),
                Exercise(name = "器械推肩", muscleGroup = "肩部", subMuscleGroup = "前束", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/53/Shoulder-press-machine-2.png"),
                Exercise(name = "站姿杠铃推举", muscleGroup = "肩部", subMuscleGroup = "前束", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/119/seated-barbell-shoulder-press-large-1.png"),
                Exercise(name = "哑铃侧平举", muscleGroup = "肩部", subMuscleGroup = "中束", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png"),
                Exercise(name = "绳索侧平举", muscleGroup = "肩部", subMuscleGroup = "中束", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png"),
                Exercise(name = "器械侧平举", muscleGroup = "肩部", subMuscleGroup = "中束", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png"),
                Exercise(name = "哑铃前平举", muscleGroup = "肩部", subMuscleGroup = "前束", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png"),
                Exercise(name = "俯身哑铃飞鸟", muscleGroup = "肩部", subMuscleGroup = "后束", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png"),
                Exercise(name = "绳索面拉", muscleGroup = "肩部", subMuscleGroup = "后束", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/143/Cable-seated-rows-2.png"),
                Exercise(name = "俯身绳索飞鸟", muscleGroup = "肩部", subMuscleGroup = "后束", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/143/Cable-seated-rows-2.png"),
                // ==================== 斜方肌 (4个) ====================
                Exercise(name = "杠铃耸肩", muscleGroup = "斜方肌", subMuscleGroup = "", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/150/Barbell-shrugs-1.png"),
                Exercise(name = "哑铃耸肩", muscleGroup = "斜方肌", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/151/Dumbbell-shrugs-2.png"),
                Exercise(name = "器械耸肩", muscleGroup = "斜方肌", subMuscleGroup = "", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/150/Barbell-shrugs-1.png"),
                Exercise(name = "史密斯耸肩", muscleGroup = "斜方肌", subMuscleGroup = "", equipment = "器械", imageUrl = "https://wger.de/media/exercise-images/150/Barbell-shrugs-1.png"),
                // ==================== 二头肌 (8个) ====================
                Exercise(name = "杠铃弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/129/Standing-biceps-curl-1.png"),
                Exercise(name = "哑铃弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/81/Biceps-curl-1.png"),
                Exercise(name = "锤式弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/86/Bicep-hammer-curl-1.png"),
                Exercise(name = "牧师凳弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/193/Preacher-curl-3-1.png"),
                Exercise(name = "绳索弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/138/Hammer-curls-with-rope-1.png"),
                Exercise(name = "上斜哑铃弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/74/Bicep-curls-1.png"),
                Exercise(name = "集中弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/81/Biceps-curl-1.png"),
                Exercise(name = "曲杆弯举", muscleGroup = "二头肌", subMuscleGroup = "", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/129/Standing-biceps-curl-1.png"),
                // ==================== 三头肌 (7个) ====================
                Exercise(name = "绳索下压", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/84/Lying-close-grip-triceps-press-to-chin-1.png"),
                Exercise(name = "仰卧臂屈伸", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/84/Lying-close-grip-triceps-press-to-chin-1.png"),
                Exercise(name = "窄距杠铃卧推", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "杠铃", imageUrl = "https://wger.de/media/exercise-images/61/Close-grip-bench-press-1.png"),
                Exercise(name = "凳上臂屈伸", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/83/Bench-dips-1.png"),
                Exercise(name = "绳索臂屈伸", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/84/Lying-close-grip-triceps-press-to-chin-1.png"),
                Exercise(name = "哑铃颈后臂屈伸", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "哑铃", imageUrl = "https://wger.de/media/exercise-images/84/Lying-close-grip-triceps-press-to-chin-1.png"),
                Exercise(name = "单臂绳索下压", muscleGroup = "三头肌", subMuscleGroup = "", equipment = "绳索", imageUrl = "https://wger.de/media/exercise-images/84/Lying-close-grip-triceps-press-to-chin-1.png"),
                // ==================== 核心 (8个) ====================
                Exercise(name = "卷腹", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/91/Crunches-1.png"),
                Exercise(name = "下斜卷腹", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/93/Decline-crunch-1.png"),
                Exercise(name = "交叉卷腹", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/176/Cross-body-crunch-1.png"),
                Exercise(name = "悬垂举腿", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/125/Leg-raises-2.png"),
                Exercise(name = "仰卧抬腿", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/125/Leg-raises-2.png"),
                Exercise(name = "下斜仰卧起坐", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/56/Decline-crunch-1.png"),
                Exercise(name = "平板支撑", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/91/Crunches-1.png"),
                Exercise(name = "侧平板支撑", muscleGroup = "核心", subMuscleGroup = "", equipment = "自重", imageUrl = "https://wger.de/media/exercise-images/91/Crunches-1.png")
            )
                
            if (existingExercises.isEmpty()) {
                // 首次安装，插入所有默认动作
                defaultExercises.forEach { repository.insertExercise(it) }
            } else {
                // 已有数据，更新子分类和图片URL，并插入新动作
                val existingNames = existingExercises.map { it.name }.toSet()
                val exerciseMap = defaultExercises.associateBy { it.name }
                
                // 更新已存在的动作
                existingExercises.forEach { existing ->
                    if (!existing.isCustom) {
                        exerciseMap[existing.name]?.let { default ->
                            var needUpdate = false
                            var updated = existing
                            
                            // 强制更新子分类（修复之前不一致的子分类名称）
                            if (existing.subMuscleGroup != default.subMuscleGroup) {
                                updated = updated.copy(subMuscleGroup = default.subMuscleGroup)
                                needUpdate = true
                            }
                            
                            // 强制更新肌群（修复小腿等分类）
                            if (existing.muscleGroup != default.muscleGroup) {
                                updated = updated.copy(muscleGroup = default.muscleGroup)
                                needUpdate = true
                            }
                            
                            // 强制更新图片URL（修复之前错误的URL）
                            if (default.imageUrl.isNotEmpty() && existing.imageUrl != default.imageUrl) {
                                updated = updated.copy(imageUrl = default.imageUrl)
                                needUpdate = true
                            }
                            
                            if (needUpdate) {
                                repository.updateExercise(updated)
                            }
                        }
                    }
                }
                
                // 插入新增的动作
                defaultExercises.forEach { default ->
                    if (default.name !in existingNames) {
                        repository.insertExercise(default)
                    }
                }
            }
        }
    }
    
    fun startTraining() {
        startTrainingWithDate(System.currentTimeMillis())
    }
    
    fun startTrainingWithDate(date: Long) {
        viewModelScope.launch {
            val workout = Workout(userId = getCurrentUserId(), name = "训练", date = date)
            val workoutId = repository.insertWorkout(workout)
            _currentWorkoutId.value = workoutId
            _isTraining.value = true
            _isPaused.value = true
            startTime = System.currentTimeMillis()
            pausedTime = 0
            pauseStartTime = System.currentTimeMillis()
            _workoutExercises.value = emptyList()
            _totalSets.value = 0
            _totalVolume.value = 0.0
            _workoutNotes.value = ""
        }
    }
    
    fun updateNotes(notes: String) {
        _workoutNotes.value = notes
        viewModelScope.launch {
            _currentWorkoutId.value?.let { workoutId ->
                val workout = repository.getWorkoutById(workoutId)
                workout?.let {
                    repository.updateWorkout(it.copy(notes = notes))
                }
            }
        }
    }
    
    fun deleteNotes() {
        _workoutNotes.value = ""
        viewModelScope.launch {
            _currentWorkoutId.value?.let { workoutId ->
                val workout = repository.getWorkoutById(workoutId)
                workout?.let {
                    repository.updateWorkout(it.copy(notes = ""))
                }
            }
        }
    }
    
    fun getNotes(): String = _workoutNotes.value ?: ""
    
    fun pauseTraining() {
        if (_isPaused.value == true) return
        _isPaused.value = true
        pauseStartTime = System.currentTimeMillis()
    }
    
    fun resumeTraining() {
        if (_isPaused.value != true) return
        _isPaused.value = false
        pausedTime += System.currentTimeMillis() - pauseStartTime
        pauseStartTime = 0
    }
    
    fun endTraining() {
        viewModelScope.launch {
            _currentWorkoutId.value?.let { workoutId ->
                val workout = repository.getWorkoutById(workoutId)
                workout?.let {
                    val totalPausedTime = if (_isPaused.value == true) {
                        pausedTime + (System.currentTimeMillis() - pauseStartTime)
                    } else {
                        pausedTime
                    }
                    val duration = System.currentTimeMillis() - startTime - totalPausedTime
                    repository.updateWorkout(it.copy(duration = duration))
                }
            }
            _isTraining.value = false
            _isPaused.value = false
            _currentWorkoutId.value = null
            _workoutExercises.value = emptyList()
            _totalSets.value = 0
            _totalVolume.value = 0.0
            pausedTime = 0
            pauseStartTime = 0
        }
    }
    
    fun getStartTime(): Long = startTime
    
    fun getPausedTime(): Long {
        return if (_isPaused.value == true) {
            pausedTime + (System.currentTimeMillis() - pauseStartTime)
        } else {
            pausedTime
        }
    }
    
    fun resetTimer() {
        startTime = System.currentTimeMillis()
        pausedTime = 0
        if (_isPaused.value == true) {
            pauseStartTime = System.currentTimeMillis()
        }
    }
    
    fun addExerciseToWorkout(exercise: Exercise, supersetGroupId: Long? = null) {
        viewModelScope.launch {
            _currentWorkoutId.value?.let { workoutId ->
                val currentList = _workoutExercises.value ?: emptyList()
                val order = currentList.size
                
                val workoutExercise = WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    order = order,
                    supersetGroupId = supersetGroupId
                )
                val workoutExerciseId = repository.insertWorkoutExercise(workoutExercise)
                
                val firstSet = WorkoutSet(
                    workoutExerciseId = workoutExerciseId,
                    setNumber = 1,
                    weight = 0.0,
                    reps = 0,
                    isCompleted = false
                )
                val setId = repository.insertSet(firstSet)
                
                val newExerciseWithDetails = WorkoutExerciseWithDetails(
                    workoutExercise = workoutExercise.copy(id = workoutExerciseId),
                    exercise = exercise,
                    sets = mutableListOf(firstSet.copy(id = setId))
                )
                
                _workoutExercises.value = currentList + newExerciseWithDetails
            }
        }
    }
    
    fun addSupersetToWorkout(exercises: List<Exercise>) {
        if (exercises.size < 2) {
            exercises.firstOrNull()?.let { addExerciseToWorkout(it) }
            return
        }
        
        viewModelScope.launch {
            _currentWorkoutId.value?.let { workoutId ->
                val currentList = _workoutExercises.value?.toMutableList() ?: mutableListOf()
                var order = currentList.size
                
                val supersetGroupId = System.currentTimeMillis()
                
                val newExercises = mutableListOf<WorkoutExerciseWithDetails>()
                
                exercises.forEach { exercise ->
                    val workoutExercise = WorkoutExercise(
                        workoutId = workoutId,
                        exerciseId = exercise.id,
                        order = order++,
                        supersetGroupId = supersetGroupId
                    )
                    val workoutExerciseId = repository.insertWorkoutExercise(workoutExercise)
                    
                    val firstSet = WorkoutSet(
                        workoutExerciseId = workoutExerciseId,
                        setNumber = 1,
                        weight = 0.0,
                        reps = 0,
                        isCompleted = false
                    )
                    val setId = repository.insertSet(firstSet)
                    
                    newExercises.add(WorkoutExerciseWithDetails(
                        workoutExercise = workoutExercise.copy(id = workoutExerciseId),
                        exercise = exercise,
                        sets = mutableListOf(firstSet.copy(id = setId))
                    ))
                }
                
                _workoutExercises.value = currentList + newExercises
            }
        }
    }
    
    fun addSetToExercise(workoutExerciseId: Long, lastWeight: Double, lastReps: Int) {
        viewModelScope.launch {
            val currentList = _workoutExercises.value?.toMutableList() ?: return@launch
            val index = currentList.indexOfFirst { it.workoutExercise.id == workoutExerciseId }
            if (index == -1) return@launch
            
            val exerciseWithDetails = currentList[index]
            val sets = exerciseWithDetails.sets
            val newSetNumber = (sets.maxOfOrNull { it.setNumber } ?: 0) + 1
            
            val newSet = WorkoutSet(
                workoutExerciseId = workoutExerciseId,
                setNumber = newSetNumber,
                weight = lastWeight,
                reps = lastReps,
                isCompleted = false
            )
            val setId = repository.insertSet(newSet)
            
            sets.add(newSet.copy(id = setId))
            
            _workoutExercises.postValue(currentList.toList())
        }
    }
    
    fun updateSet(set: WorkoutSet, updateUI: Boolean = false) {
        viewModelScope.launch {
            repository.updateSet(set)
            
            val currentList = _workoutExercises.value ?: return@launch
            currentList.forEach { exerciseWithDetails ->
                val index = exerciseWithDetails.sets.indexOfFirst { it.id == set.id }
                if (index != -1) {
                    (exerciseWithDetails.sets as? MutableList)?.set(index, set)
                }
            }
            
            var completedSets = 0
            var volume = 0.0
            
            currentList.forEach { exerciseWithDetails ->
                exerciseWithDetails.sets.forEach { s ->
                    if (s.isCompleted) {
                        completedSets++
                        volume += s.weight * s.reps
                    }
                }
            }
            
            _totalSets.postValue(completedSets)
            _totalVolume.postValue(volume)
        }
    }
    
    private fun updateStatisticsOnly() {
        val exercises = _workoutExercises.value ?: return
        var completedSets = 0
        var volume = 0.0
        
        exercises.forEach { exerciseWithDetails ->
            exerciseWithDetails.sets.forEach { set ->
                if (set.isCompleted) {
                    completedSets++
                    volume += set.weight * set.reps
                }
            }
        }
        
        _totalSets.value = completedSets
        _totalVolume.value = volume
    }
    
    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            val currentList = _workoutExercises.value?.toMutableList() ?: return@launch
            val exerciseIndex = currentList.indexOfFirst { 
                it.sets.any { s -> s.id == set.id } 
            }
            if (exerciseIndex == -1) return@launch
            
            val exerciseWithDetails = currentList[exerciseIndex]
            
            if (exerciseWithDetails.sets.size <= 1) {
                repository.deleteWorkoutExercise(exerciseWithDetails.workoutExercise)
                currentList.removeAt(exerciseIndex)
            } else {
                repository.deleteSet(set)
                exerciseWithDetails.sets.removeAll { it.id == set.id }
                
                exerciseWithDetails.sets.forEachIndexed { index, s ->
                    val updatedSet = s.copy(setNumber = index + 1)
                    exerciseWithDetails.sets[index] = updatedSet
                    repository.updateSet(updatedSet)
                }
            }
            
            _workoutExercises.postValue(currentList.toList())
            updateStatistics()
        }
    }
    
    fun deleteExercise(workoutExerciseWithDetails: WorkoutExerciseWithDetails) {
        viewModelScope.launch {
            repository.deleteWorkoutExercise(workoutExerciseWithDetails.workoutExercise)
            
            val currentList = _workoutExercises.value?.toMutableList() ?: return@launch
            currentList.removeAll { it.workoutExercise.id == workoutExerciseWithDetails.workoutExercise.id }
            _workoutExercises.value = currentList
            
            updateStatistics()
        }
    }
    
    private fun updateStatistics() {
        val exercises = _workoutExercises.value ?: return
        var completedSets = 0
        var volume = 0.0
        
        exercises.forEach { exerciseWithDetails ->
            exerciseWithDetails.sets.forEach { set ->
                if (set.isCompleted) {
                    completedSets++
                    volume += set.weight * set.reps
                }
            }
        }
        
        _totalSets.value = completedSets
        _totalVolume.value = volume
    }
    
    fun filterExercises(muscleGroup: String?): List<Exercise> {
        val allExercises = _exercises.value ?: emptyList()
        return if (muscleGroup.isNullOrEmpty() || muscleGroup == "全部") {
            allExercises
        } else {
            allExercises.filter { it.muscleGroup == muscleGroup }
        }
    }
    
    fun searchExercises(query: String): List<Exercise> {
        val allExercises = _exercises.value ?: emptyList()
        return if (query.isEmpty()) {
            allExercises
        } else {
            allExercises.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
    
    fun saveAsTemplate(name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val exercises = _workoutExercises.value ?: return@launch
            if (exercises.isEmpty()) return@launch
            
            val template = WorkoutTemplate(userId = getCurrentUserId(), name = name)
            val templateId = templateRepository.insertTemplate(template)
            
            exercises.forEachIndexed { index, exerciseWithDetails ->
                val templateExercise = TemplateExercise(
                    templateId = templateId,
                    exerciseId = exerciseWithDetails.exercise.id,
                    sortOrder = index,
                    targetSets = exerciseWithDetails.sets.size,
                    targetReps = exerciseWithDetails.sets.firstOrNull()?.reps ?: 0
                )
                templateRepository.insertTemplateExercise(templateExercise)
            }
            
            onSuccess()
        }
    }
    
    fun startFromTemplate(templateId: Long) {
        viewModelScope.launch {
            val templateWithExercises = templateRepository.getTemplateWithExercises(templateId)
                ?: return@launch
            
            val workout = Workout(userId = getCurrentUserId(), name = templateWithExercises.template.name)
            val workoutId = repository.insertWorkout(workout)
            _currentWorkoutId.value = workoutId
            _isTraining.value = true
            _isPaused.value = true
            startTime = System.currentTimeMillis()
            pausedTime = 0
            pauseStartTime = System.currentTimeMillis()
            _workoutNotes.value = ""
            
            val exercisesList = mutableListOf<WorkoutExerciseWithDetails>()
            templateWithExercises.exercises.forEachIndexed { index, detail ->
                val exercise = detail.exercise ?: return@forEachIndexed
                
                val workoutExercise = WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    order = index
                )
                val workoutExerciseId = repository.insertWorkoutExercise(workoutExercise)
                
                val sets = mutableListOf<WorkoutSet>()
                val targetSets = if (detail.templateExercise.targetSets > 0) 
                    detail.templateExercise.targetSets else 1
                    
                for (setNum in 1..targetSets) {
                    val set = WorkoutSet(
                        workoutExerciseId = workoutExerciseId,
                        setNumber = setNum,
                        weight = 0.0,
                        reps = 0,
                        isCompleted = false
                    )
                    val setId = repository.insertSet(set)
                    sets.add(set.copy(id = setId))
                }
                
                exercisesList.add(WorkoutExerciseWithDetails(
                    workoutExercise = workoutExercise.copy(id = workoutExerciseId),
                    exercise = exercise,
                    sets = sets
                ))
            }
            
            _workoutExercises.value = exercisesList
            _totalSets.value = 0
            _totalVolume.value = 0.0
        }
    }
    
    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template)
        }
    }
    
    suspend fun getTemplateWithExercises(templateId: Long): TemplateWithExercises? {
        return templateRepository.getTemplateWithExercises(templateId)
    }
}

data class WorkoutExerciseWithDetails(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
    val sets: MutableList<WorkoutSet>
)


