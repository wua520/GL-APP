package com.fitness.training.network

import android.content.Context
import android.util.Log
import com.fitness.training.data.database.FitnessDatabase
import com.fitness.training.data.entity.BodyRecord
import com.fitness.training.data.entity.DietRecord
import com.fitness.training.data.entity.TrainingPlan
import com.fitness.training.data.entity.Workout
import com.fitness.training.data.entity.WorkoutExercise
import com.fitness.training.data.entity.WorkoutSet
import com.fitness.training.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class SyncManager(private val context: Context) {
    
    private val database = FitnessDatabase.getDatabase(context)
    private val apiService = RetrofitClient.apiService
    
    companion object {
        private const val TAG = "SyncManager"
    }
    
    fun getCloudUsername(): String? = UserSession.getCloudUsername(context)
    
    // 云端注册
    suspend fun register(username: String, password: String, nickname: String?): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(RegisterRequest(username, password, nickname))
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()!!.data!!
                    UserSession.saveCloudLoginInfo(context, data.userId, data.username, data.nickname, data.token)
                    Result.success(data)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "注册失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }
    
    // 云端登录
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "发起登录请求: username=$username")
                val response = apiService.login(LoginRequest(username, password))
                Log.d(TAG, "登录响应: code=${response.code()}, body=${response.body()}")
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()!!.data!!
                    Log.d(TAG, "登录成功: userId=${data.userId}, token=${data.token}")
                    UserSession.saveCloudLoginInfo(context, data.userId, data.username, data.nickname, data.token)
                    Result.success(data)
                } else {
                    val errorMsg = response.body()?.message ?: "登录失败 (HTTP ${response.code()})"
                    Log.e(TAG, "登录失败: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "登录网络错误: ${e.javaClass.simpleName}: ${e.message}")
                val errorMsg = when {
                    e.message?.contains("Unable to resolve host") == true -> "无法连接服务器，请检查网络"
                    e.message?.contains("Connection refused") == true -> "服务器未启动，请先启动后端服务"
                    e.message?.contains("timeout") == true -> "连接超时，请检查网络"
                    else -> "网络错误: ${e.message}"
                }
                Result.failure(Exception(errorMsg))
            }
        }
    }
    
    // 退出云端登录
    fun logout() {
        UserSession.cloudLogout(context)
    }
    
    // 修改密码
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<String> {
        val token = UserSession.getToken(context) ?: return Result.failure(Exception("未登录云端账号"))
        
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.changePassword(
                    "Bearer $token",
                    ChangePasswordRequest(oldPassword, newPassword)
                )
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    Result.success("密码修改成功")
                } else {
                    Result.failure(Exception(response.body()?.message ?: "密码修改失败"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }
    
    // 同步数据：先上传，后下载
    suspend fun sync(localUserId: Long): Result<String> {
        val token = UserSession.getToken(context) ?: return Result.failure(Exception("未登录云端账号"))
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始同步，本地用户ID: $localUserId")
                
                // 第一步：收集本地数据准备上传
                val workoutsToUpload = collectWorkoutsToUpload(localUserId)
                val dietRecordsToUpload = collectDietRecordsToUpload(localUserId)
                val bodyRecordsToUpload = collectBodyRecordsToUpload(localUserId)
                val trainingPlansToUpload = collectTrainingPlansToUpload(localUserId)
                
                Log.d(TAG, "准备上传: ${workoutsToUpload.size}个训练, ${dietRecordsToUpload.size}个饮食记录, ${bodyRecordsToUpload.size}个身体记录, ${trainingPlansToUpload.size}个训练计划")
                
                val request = SyncRequest(
                    lastSyncTime = 0, // 每次都获取全部数据
                    workouts = workoutsToUpload,
                    dietRecords = dietRecordsToUpload,
                    bodyRecords = bodyRecordsToUpload,
                    trainingPlans = trainingPlansToUpload,
                    bodyProfile = collectBodyProfile(),
                    customExercises = collectCustomExercises(localUserId),
                    workoutTemplates = collectWorkoutTemplates(localUserId),
                    favoriteExercises = collectFavoriteExercises()
                )
                
                // 第二步：发送请求（上传数据）
                val response = apiService.sync("Bearer $token", request)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val syncResponse = response.body()!!.data
                    Log.d(TAG, "上传成功，开始处理下载数据")
                    
                    // 第三步：处理服务器返回的数据（下载）
                    if (syncResponse != null) {
                        val downloadedWorkouts = syncResponse.workouts?.size ?: 0
                        val downloadedDiet = syncResponse.dietRecords?.size ?: 0
                        val downloadedBody = syncResponse.bodyRecords?.size ?: 0
                        val downloadedPlans = syncResponse.trainingPlans?.size ?: 0
                        val downloadedCustomEx = syncResponse.customExercises?.size ?: 0
                        val downloadedTemplates = syncResponse.workoutTemplates?.size ?: 0
                        val downloadedFavorites = syncResponse.favoriteExercises?.size ?: 0
                        
                        Log.d(TAG, "收到云端数据: ${downloadedWorkouts}个训练, ${downloadedDiet}个饮食记录, ${downloadedBody}个身体记录, ${downloadedPlans}个训练计划")
                        Log.d(TAG, "收到云端数据: ${downloadedCustomEx}个自定义动作, ${downloadedTemplates}个训练模板, ${downloadedFavorites}个收藏动作")
                        
                        // 按顺序保存下载的数据
                        saveDownloadedBodyProfile(syncResponse.bodyProfile)
                        saveDownloadedDietRecords(localUserId, syncResponse.dietRecords)
                        saveDownloadedBodyRecords(localUserId, syncResponse.bodyRecords)
                        saveDownloadedWorkouts(localUserId, syncResponse.workouts)
                        saveDownloadedTrainingPlans(localUserId, syncResponse.trainingPlans)
                        saveDownloadedCustomExercises(localUserId, syncResponse.customExercises)
                        saveDownloadedWorkoutTemplates(localUserId, syncResponse.workoutTemplates)
                        saveDownloadedFavoriteExercises(syncResponse.favoriteExercises)
                        
                        Log.d(TAG, "同步完成")
                        Result.success("同步成功\n上传: ${workoutsToUpload.size}训练/${dietRecordsToUpload.size}饮食/${bodyRecordsToUpload.size}身体/${trainingPlansToUpload.size}计划\n下载: ${downloadedWorkouts}训练/${downloadedDiet}饮食/${downloadedBody}身体/${downloadedPlans}计划")
                    } else {
                        Result.success("上传成功")
                    }
                } else {
                    Result.failure(Exception(response.body()?.message ?: "同步失败"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步错误: ${e.message}", e)
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }
    
    // 保存下载的饮食记录
    private suspend fun saveDownloadedDietRecords(localUserId: Long, records: List<DietRecordData>?) {
        if (records.isNullOrEmpty()) return
        
        val dietRecordDao = database.dietRecordDao()
        val existingRecords = dietRecordDao.getDietRecordsByUserId(localUserId)
        val existingLocalIds = existingRecords.map { it.id }.toSet()
        
        for (data in records) {
            // 如果本地已存在相同localId的记录，跳过
            if (data.localId in existingLocalIds) {
                Log.d(TAG, "饮食记录已存在，跳过: localId=${data.localId}")
                continue
            }
            
            // 插入新记录
            val record = DietRecord(
                id = 0, // 让Room自动生成ID
                userId = localUserId,
                date = data.date,
                mealType = data.mealType,
                foodName = data.foodName,
                calories = data.calories,
                protein = data.protein,
                carbs = data.carbs,
                fat = data.fat,
                amount = data.amount ?: ""
            )
            val newId = dietRecordDao.insert(record)
            Log.d(TAG, "插入饮食记录: ${data.foodName}, 新ID=$newId")
        }
    }
    
    // 保存下载的身体记录
    private suspend fun saveDownloadedBodyRecords(localUserId: Long, records: List<BodyRecordData>?) {
        if (records.isNullOrEmpty()) return
        
        val bodyRecordDao = database.bodyRecordDao()
        val existingRecords = bodyRecordDao.getBodyRecordsByUserId(localUserId)
        
        // 用日期+体重+体脂组合作为唯一标识
        val existingKeys = existingRecords.map { 
            "${it.date}_${it.weight}_${it.bodyFat}" 
        }.toSet()
        
        for (data in records) {
            val key = "${data.date}_${data.weight}_${data.bodyFat}"
            
            // 如果本地已存在相同的记录，跳过
            if (key in existingKeys) {
                Log.d(TAG, "身体记录已存在，跳过: date=${data.date}, weight=${data.weight}")
                continue
            }
            
            // 插入新记录
            val record = BodyRecord(
                id = 0,
                userId = localUserId,
                date = data.date,
                weight = data.weight,
                bodyFat = data.bodyFat,
                muscleMass = data.muscleMass,
                note = data.note ?: ""
            )
            val newId = bodyRecordDao.insert(record)
            Log.d(TAG, "插入身体记录: weight=${data.weight}, 新ID=$newId")
        }
    }
    
    // 保存下载的训练记录（包含动作和组数）
    private suspend fun saveDownloadedWorkouts(localUserId: Long, workouts: List<WorkoutData>?) {
        if (workouts.isNullOrEmpty()) return
        
        val workoutDao = database.workoutDao()
        val workoutExerciseDao = database.workoutExerciseDao()
        val workoutSetDao = database.workoutSetDao()
        val exerciseDao = database.exerciseDao()
        
        val existingWorkouts = workoutDao.getWorkoutsByUserId(localUserId)
        val existingLocalIds = existingWorkouts.map { it.id }.toSet()
        
        for (data in workouts) {
            // 如果本地已存在相同localId的训练，跳过
            if (data.localId in existingLocalIds) {
                Log.d(TAG, "训练记录已存在，跳过: localId=${data.localId}, name=${data.name}")
                continue
            }
            
            // 插入训练记录
            val workout = Workout(
                id = 0,
                userId = localUserId,
                name = data.name,
                date = data.date,
                duration = data.duration,
                notes = data.notes ?: ""
            )
            val workoutId = workoutDao.insertWorkout(workout)
            Log.d(TAG, "插入训练记录: ${data.name}, 新ID=$workoutId")
            
            // 插入训练动作
            data.exercises?.forEachIndexed { index, exData ->
                // 根据动作名称查找或创建Exercise
                val exerciseId = findOrCreateExercise(exerciseDao, exData.exerciseName)
                
                val workoutExercise = WorkoutExercise(
                    id = 0,
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    order = exData.exerciseOrder,
                    supersetGroupId = exData.supersetGroupId
                )
                val workoutExerciseId = workoutExerciseDao.insertWorkoutExercise(workoutExercise)
                Log.d(TAG, "插入训练动作: ${exData.exerciseName}, 新ID=$workoutExerciseId")
                
                // 插入组数
                exData.sets?.forEach { setData ->
                    val workoutSet = WorkoutSet(
                        id = 0,
                        workoutExerciseId = workoutExerciseId,
                        setNumber = setData.setNumber,
                        weight = setData.weight,
                        reps = setData.reps,
                        isCompleted = setData.isCompleted,
                        restTime = setData.restTime
                    )
                    workoutSetDao.insertSet(workoutSet)
                }
            }
        }
    }
    
    // 根据动作名称查找或创建Exercise
    private suspend fun findOrCreateExercise(exerciseDao: com.fitness.training.data.dao.ExerciseDao, exerciseName: String): Long {
        // 先查找是否已存在同名动作（包括系统预置和自定义）
        val existingExercise = exerciseDao.getAllExercises().first().find { it.name == exerciseName }
        
        if (existingExercise != null) {
            // 如果已存在，直接返回ID
            Log.d(TAG, "找到已存在动作: $exerciseName, ID=${existingExercise.id}")
            return existingExercise.id
        }
        
        // 如果不存在，创建一个非自定义动作（因为是从训练记录中来的，不是用户手动创建的）
        val exercise = com.fitness.training.data.entity.Exercise(
            id = 0,
            name = exerciseName,
            muscleGroup = "其他",
            equipment = "自由重量",
            isCustom = false  // 改为false，避免污染自定义动作列表
        )
        val newId = exerciseDao.insertExercise(exercise)
        Log.d(TAG, "创建新动作: $exerciseName, ID=$newId, isCustom=false")
        return newId
    }
    
    private suspend fun collectWorkoutsToUpload(userId: Long): List<WorkoutData> {
        val workoutDao = database.workoutDao()
        val workoutExerciseDao = database.workoutExerciseDao()
        val workoutSetDao = database.workoutSetDao()
        val exerciseDao = database.exerciseDao()
        
        val workouts = workoutDao.getWorkoutsByUserId(userId)
        val result = mutableListOf<WorkoutData>()
        
        for (workout in workouts) {
            val exercises = workoutExerciseDao.getExercisesByWorkoutId(workout.id)
            val exerciseDataList = mutableListOf<ExerciseData>()
            
            for (we in exercises) {
                val exercise = exerciseDao.getExerciseById(we.exerciseId)
                val sets = workoutSetDao.getSetsByWorkoutExerciseId(we.id)
                
                val setDataList = sets.map { s ->
                    SetData(
                        localId = s.id,
                        setNumber = s.setNumber,
                        weight = s.weight,
                        reps = s.reps,
                        isCompleted = s.isCompleted,
                        restTime = s.restTime
                    )
                }
                
                exerciseDataList.add(ExerciseData(
                    localId = we.id,
                    exerciseName = exercise?.name ?: "未知动作",
                    exerciseOrder = we.order,
                    supersetGroupId = we.supersetGroupId,
                    sets = setDataList
                ))
            }
            
            result.add(WorkoutData(
                localId = workout.id,
                name = workout.name,
                date = workout.date,
                duration = workout.duration,
                notes = workout.notes,
                updatedAt = workout.date,
                exercises = exerciseDataList
            ))
        }
        
        return result
    }
    
    private suspend fun collectDietRecordsToUpload(userId: Long): List<DietRecordData> {
        val dietRecordDao = database.dietRecordDao()
        val records = dietRecordDao.getDietRecordsByUserId(userId)
        
        return records.map { r ->
            DietRecordData(
                localId = r.id,
                date = r.date,
                mealType = r.mealType,
                foodName = r.foodName,
                calories = r.calories,
                protein = r.protein,
                carbs = r.carbs,
                fat = r.fat,
                amount = r.amount,
                updatedAt = r.date
            )
        }
    }
    
    private suspend fun collectBodyRecordsToUpload(userId: Long): List<BodyRecordData> {
        val bodyRecordDao = database.bodyRecordDao()
        val records = bodyRecordDao.getBodyRecordsByUserId(userId)
        
        return records.map { r ->
            BodyRecordData(
                localId = r.id,
                date = r.date,
                weight = r.weight,
                bodyFat = r.bodyFat,
                muscleMass = r.muscleMass,
                note = r.note,
                updatedAt = r.date
            )
        }
    }
    
    private suspend fun collectTrainingPlansToUpload(userId: Long): List<TrainingPlanData> {
        val trainingPlanDao = database.trainingPlanDao()
        val plans = trainingPlanDao.getTrainingPlansByUserId(userId)
        
        return plans.map { p ->
            TrainingPlanData(
                localId = p.id,
                title = p.title,
                description = p.description,
                details = p.details,
                goal = p.goal,
                experience = p.experience,
                targetMuscles = p.targetMuscles,
                trainingDays = p.trainingDays,
                trainingDuration = p.trainingDuration,
                equipment = p.equipment,
                isPinned = p.isPinned,
                isFromRecommendation = p.isFromRecommendation,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt
            )
        }
    }
    
    // 保存下载的训练计划
    private suspend fun saveDownloadedTrainingPlans(localUserId: Long, plans: List<TrainingPlanData>?) {
        if (plans.isNullOrEmpty()) return
        
        val trainingPlanDao = database.trainingPlanDao()
        val existingPlans = trainingPlanDao.getTrainingPlansByUserId(localUserId)
        val existingLocalIds = existingPlans.map { it.id }.toSet()
        
        for (data in plans) {
            // 如果本地已存在相同localId的计划，跳过
            if (data.localId in existingLocalIds) {
                Log.d(TAG, "训练计划已存在，跳过: localId=${data.localId}, title=${data.title}")
                continue
            }
            
            // 插入新计划
            val plan = TrainingPlan(
                id = 0,
                userId = localUserId,
                title = data.title,
                description = data.description,
                details = data.details,
                goal = data.goal,
                experience = data.experience,
                targetMuscles = data.targetMuscles,
                trainingDays = data.trainingDays,
                trainingDuration = data.trainingDuration,
                equipment = data.equipment,
                isPinned = data.isPinned,
                isFromRecommendation = data.isFromRecommendation,
                createdAt = data.createdAt,
                updatedAt = data.updatedAt
            )
            val newId = trainingPlanDao.insertPlan(plan)
            Log.d(TAG, "插入训练计划: ${data.title}, 新ID=$newId")
        }
    }
    
    // ==================== 身体档案相关方法 ====================
    
    private fun collectBodyProfile(): BodyProfileData? {
        return com.fitness.training.util.BodyProfile.getProfileData(context)
    }
    
    private suspend fun saveDownloadedBodyProfile(profile: BodyProfileData?) {
        if (profile == null) return
        
        Log.d(TAG, "保存身体档案: gender=${profile.gender}, height=${profile.height}, birthYear=${profile.birthYear}")
        com.fitness.training.util.BodyProfile.saveProfileData(context, profile)
    }
    
    // ==================== 自定义动作相关方法 ====================
    
    private suspend fun collectCustomExercises(userId: Long): List<CustomExerciseData> {
        val exerciseDao = database.exerciseDao()
        val customExercises = exerciseDao.getAllExercises().first().filter { it.isCustom }
        
        // 按名称去重，只保留每个名称的第一个动作
        val uniqueExercises = customExercises.groupBy { it.name }.map { (_, exercises) -> exercises.first() }
        
        Log.d(TAG, "收集自定义动作: 总数=${customExercises.size}, 去重后=${uniqueExercises.size}")
        
        return uniqueExercises.map { e ->
            CustomExerciseData(
                localId = e.id,
                name = e.name,
                muscleGroup = e.muscleGroup,
                subMuscleGroup = e.subMuscleGroup,
                equipment = e.equipment,
                description = e.description,
                imageUrl = e.imageUrl,
                isFavorite = e.isFavorite,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    private suspend fun saveDownloadedCustomExercises(localUserId: Long, exercises: List<CustomExerciseData>?) {
        if (exercises.isNullOrEmpty()) return
        
        val exerciseDao = database.exerciseDao()
        val existingExercises = exerciseDao.getAllExercises().first()
        
        // 使用名称来判断是否已存在（因为自定义动作的localId可能不同）
        val existingNames = existingExercises.filter { it.isCustom }.map { it.name }.toSet()
        
        for (data in exercises) {
            // 如果本地已存在相同名称的自定义动作，跳过
            if (data.name in existingNames) {
                Log.d(TAG, "自定义动作已存在，跳过: name=${data.name}")
                continue
            }
            
            // 插入新动作
            val exercise = com.fitness.training.data.entity.Exercise(
                id = 0,
                name = data.name,
                muscleGroup = data.muscleGroup,
                subMuscleGroup = data.subMuscleGroup,
                equipment = data.equipment,
                description = data.description,
                imageUrl = data.imageUrl,
                isCustom = true,
                isFavorite = data.isFavorite
            )
            val newId = exerciseDao.insertExercise(exercise)
            Log.d(TAG, "插入自定义动作: ${data.name}, 新ID=$newId")
        }
    }
    
    // ==================== 训练模板相关方法 ====================
    
    private suspend fun collectWorkoutTemplates(userId: Long): List<WorkoutTemplateData> {
        val templateDao = database.workoutTemplateDao()
        val templateExerciseDao = database.templateExerciseDao()
        val exerciseDao = database.exerciseDao()
        
        val templates = templateDao.getAllTemplates(userId).first().filter { !it.isPreset }
        val result = mutableListOf<WorkoutTemplateData>()
        
        for (template in templates) {
            val templateExercises = templateExerciseDao.getExercisesByTemplateId(template.id)
            val exerciseDataList = templateExercises.map { te ->
                val exercise = exerciseDao.getExerciseById(te.exerciseId)
                TemplateExerciseData(
                    localId = te.id,
                    exerciseName = exercise?.name ?: "未知动作",
                    sortOrder = te.sortOrder,
                    targetSets = te.targetSets,
                    targetReps = te.targetReps
                )
            }
            
            result.add(WorkoutTemplateData(
                localId = template.id,
                name = template.name,
                description = template.description,
                exercises = exerciseDataList,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ))
        }
        
        return result
    }
    
    private suspend fun saveDownloadedWorkoutTemplates(localUserId: Long, templates: List<WorkoutTemplateData>?) {
        if (templates.isNullOrEmpty()) return
        
        val templateDao = database.workoutTemplateDao()
        val templateExerciseDao = database.templateExerciseDao()
        val exerciseDao = database.exerciseDao()
        
        val existingTemplates = templateDao.getAllTemplates(localUserId).first()
        val existingLocalIds = existingTemplates.map { it.id }.toSet()
        
        for (data in templates) {
            // 如果本地已存在相同localId的模板，跳过
            if (data.localId in existingLocalIds) {
                Log.d(TAG, "训练模板已存在，跳过: localId=${data.localId}, name=${data.name}")
                continue
            }
            
            // 插入新模板
            val template = com.fitness.training.data.entity.WorkoutTemplate(
                id = 0,
                userId = localUserId,
                name = data.name,
                description = data.description,
                isPreset = false
            )
            val templateId = templateDao.insertTemplate(template)
            Log.d(TAG, "插入训练模板: ${data.name}, 新ID=$templateId")
            
            // 插入模板动作
            for (exData in data.exercises) {
                // 根据动作名称查找动作ID
                val exercise = exerciseDao.getAllExercises().first().find { it.name == exData.exerciseName }
                if (exercise != null) {
                    val templateExercise = com.fitness.training.data.entity.TemplateExercise(
                        id = 0,
                        templateId = templateId,
                        exerciseId = exercise.id,
                        sortOrder = exData.sortOrder,
                        targetSets = exData.targetSets,
                        targetReps = exData.targetReps
                    )
                    templateExerciseDao.insertTemplateExercise(templateExercise)
                }
            }
        }
    }
    
    // ==================== 收藏动作相关方法 ====================
    
    private suspend fun collectFavoriteExercises(): List<FavoriteExerciseData> {
        val exerciseDao = database.exerciseDao()
        // 只收藏系统预置动作（非自定义），自定义动作的收藏状态保存在custom_exercises表中
        val favoriteExercises = exerciseDao.getFavoriteExercises().first().filter { !it.isCustom }
        
        return favoriteExercises.map { e ->
            FavoriteExerciseData(
                exerciseName = e.name,
                createdAt = System.currentTimeMillis()
            )
        }
    }
    
    private suspend fun saveDownloadedFavoriteExercises(favorites: List<FavoriteExerciseData>?) {
        if (favorites.isNullOrEmpty()) return
        
        val exerciseDao = database.exerciseDao()
        val allExercises = exerciseDao.getAllExercises().first()
        
        for (data in favorites) {
            // 查找对应的动作
            val exercise = allExercises.find { it.name == data.exerciseName && !it.isCustom }
            if (exercise != null && !exercise.isFavorite) {
                // 更新收藏状态
                exerciseDao.updateFavorite(exercise.id, true)
                Log.d(TAG, "更新收藏状态: ${data.exerciseName}")
            }
        }
    }
}
