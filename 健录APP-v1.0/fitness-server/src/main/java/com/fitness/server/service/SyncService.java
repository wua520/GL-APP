package com.fitness.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fitness.server.dto.SyncRequest;
import com.fitness.server.dto.SyncResponse;
import com.fitness.server.dto.sync.DownloadStatus;
import com.fitness.server.dto.sync.SyncErrorDetail;
import com.fitness.server.dto.sync.UploadStatus;
import com.fitness.server.entity.*;
import com.fitness.server.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);
    
    @Autowired
    private WorkoutMapper workoutMapper;
    @Autowired
    private WorkoutExerciseMapper workoutExerciseMapper;
    @Autowired
    private WorkoutSetMapper workoutSetMapper;
    @Autowired
    private DietRecordMapper dietRecordMapper;
    @Autowired
    private BodyRecordMapper bodyRecordMapper;
    @Autowired
    private TrainingPlanMapper trainingPlanMapper;
    @Autowired
    private BodyProfileMapper bodyProfileMapper;
    @Autowired
    private CustomExerciseMapper customExerciseMapper;
    @Autowired
    private WorkoutTemplateMapper workoutTemplateMapper;
    @Autowired
    private TemplateExerciseMapper templateExerciseMapper;
    @Autowired
    private FavoriteExerciseMapper favoriteExerciseMapper;
    
    @Transactional
    public SyncResponse sync(Long userId, SyncRequest request) {
        long now = System.currentTimeMillis();
        
        SyncResponse response = new SyncResponse();
        response.setServerTime(now);
        
        // F0改进：分离上传和下载处理，细化错误归因
        UploadStatus uploadStatus = processUploads(userId, request);
        response.setUploadStatus(uploadStatus);
        
        // 下载数据：即使上传部分失败，下载仍正常进行
        DownloadStatus downloadStatus = processDownloads(userId, request, response);
        response.setDownloadStatus(downloadStatus);
        
        return response;
    }
    
    /**
     * F0新增：处理客户端上传的数据，每个实体独立处理，记录详细错误
     */
    private UploadStatus processUploads(Long userId, SyncRequest request) {
        UploadStatus status = new UploadStatus();
        int totalItems = 0;
        
        // 处理训练记录
        if (request.getWorkouts() != null) {
            totalItems += request.getWorkouts().size();
            for (SyncRequest.WorkoutData data : request.getWorkouts()) {
                try {
                    saveWorkout(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save workout: localId={}", data.getLocalId(), e);
                    status.addError(new SyncErrorDetail(
                        "workout",
                        String.valueOf(data.getLocalId()),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        // 处理饮食记录
        if (request.getDietRecords() != null) {
            totalItems += request.getDietRecords().size();
            for (SyncRequest.DietRecordData data : request.getDietRecords()) {
                try {
                    saveDietRecord(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save diet record: localId={}", data.getLocalId(), e);
                    status.addError(new SyncErrorDetail(
                        "diet_record",
                        String.valueOf(data.getLocalId()),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        // 处理体重记录
        if (request.getBodyRecords() != null) {
            totalItems += request.getBodyRecords().size();
            for (SyncRequest.BodyRecordData data : request.getBodyRecords()) {
                try {
                    saveBodyRecord(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save body record: localId={}", data.getLocalId(), e);
                    status.addError(new SyncErrorDetail(
                        "body_record",
                        String.valueOf(data.getLocalId()),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        // 处理训练计划
        if (request.getTrainingPlans() != null) {
            totalItems += request.getTrainingPlans().size();
            for (SyncRequest.TrainingPlanData data : request.getTrainingPlans()) {
                try {
                    saveTrainingPlan(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save training plan: localId={}", data.getLocalId(), e);
                    status.addError(new SyncErrorDetail(
                        "training_plan",
                        String.valueOf(data.getLocalId()),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        // 处理身体档案
        if (request.getBodyProfile() != null) {
            totalItems += 1;
            try {
                saveBodyProfile(userId, request.getBodyProfile());
                status.incrementSuccess();
            } catch (Exception e) {
                logger.error("Failed to save body profile", e);
                status.addError(new SyncErrorDetail(
                    "body_profile",
                    "profile",
                    classifyError(e),
                    e.getMessage()
                ));
            }
        }
        
        // 处理自定义动作
        if (request.getCustomExercises() != null) {
            totalItems += request.getCustomExercises().size();
            for (SyncRequest.CustomExerciseData data : request.getCustomExercises()) {
                try {
                    saveCustomExercise(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save custom exercise: localId={}", data.getLocalId(), e);
                    status.addError(new SyncErrorDetail(
                        "custom_exercise",
                        String.valueOf(data.getLocalId()),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        // 处理训练模板
        if (request.getWorkoutTemplates() != null) {
            totalItems += request.getWorkoutTemplates().size();
            for (SyncRequest.WorkoutTemplateData data : request.getWorkoutTemplates()) {
                try {
                    saveWorkoutTemplate(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save workout template: localId={}", data.getLocalId(), e);
                    status.addError(new SyncErrorDetail(
                        "workout_template",
                        String.valueOf(data.getLocalId()),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        // 处理收藏动作
        if (request.getFavoriteExercises() != null) {
            totalItems += request.getFavoriteExercises().size();
            for (SyncRequest.FavoriteExerciseData data : request.getFavoriteExercises()) {
                try {
                    saveFavoriteExercise(userId, data);
                    status.incrementSuccess();
                } catch (Exception e) {
                    logger.error("Failed to save favorite exercise: name={}", data.getExerciseName(), e);
                    status.addError(new SyncErrorDetail(
                        "favorite_exercise",
                        data.getExerciseName(),
                        classifyError(e),
                        e.getMessage()
                    ));
                }
            }
        }
        
        status.setTotalItems(totalItems);
        status.computeSuccess();
        return status;
    }
    
    /**
     * F0新增：处理下载数据，记录详细错误
     */
    private DownloadStatus processDownloads(Long userId, SyncRequest request, SyncResponse response) {
        DownloadStatus status = new DownloadStatus();
        Long lastSync = request.getLastSyncTime() != null ? request.getLastSyncTime() : 0L;
        
        int totalItems = 0;
        
        // 下载训练记录
        try {
            List<SyncRequest.WorkoutData> workouts = getWorkoutsAfter(userId, lastSync);
            response.setWorkouts(workouts);
            totalItems += workouts.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download workouts", e);
            status.addError(new SyncErrorDetail(
                "workout",
                "download",
                classifyError(e),
                "Failed to download workouts: " + e.getMessage()
            ));
        }
        
        // 下载饮食记录
        try {
            List<SyncRequest.DietRecordData> dietRecords = getDietRecordsAfter(userId, lastSync);
            response.setDietRecords(dietRecords);
            totalItems += dietRecords.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download diet records", e);
            status.addError(new SyncErrorDetail(
                "diet_record",
                "download",
                classifyError(e),
                "Failed to download diet records: " + e.getMessage()
            ));
        }
        
        // 下载体重记录
        try {
            List<SyncRequest.BodyRecordData> bodyRecords = getBodyRecordsAfter(userId, lastSync);
            response.setBodyRecords(bodyRecords);
            totalItems += bodyRecords.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download body records", e);
            status.addError(new SyncErrorDetail(
                "body_record",
                "download",
                classifyError(e),
                "Failed to download body records: " + e.getMessage()
            ));
        }
        
        // 下载训练计划
        try {
            List<SyncRequest.TrainingPlanData> trainingPlans = getTrainingPlansAfter(userId, lastSync);
            response.setTrainingPlans(trainingPlans);
            totalItems += trainingPlans.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download training plans", e);
            status.addError(new SyncErrorDetail(
                "training_plan",
                "download",
                classifyError(e),
                "Failed to download training plans: " + e.getMessage()
            ));
        }
        
        // 下载身体档案
        try {
            SyncRequest.BodyProfileData bodyProfile = getBodyProfile(userId);
            response.setBodyProfile(bodyProfile);
            if (bodyProfile != null) totalItems += 1;
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download body profile", e);
            status.addError(new SyncErrorDetail(
                "body_profile",
                "download",
                classifyError(e),
                "Failed to download body profile: " + e.getMessage()
            ));
        }
        
        // 下载自定义动作
        try {
            List<SyncRequest.CustomExerciseData> customExercises = getCustomExercisesAfter(userId, lastSync);
            response.setCustomExercises(customExercises);
            totalItems += customExercises.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download custom exercises", e);
            status.addError(new SyncErrorDetail(
                "custom_exercise",
                "download",
                classifyError(e),
                "Failed to download custom exercises: " + e.getMessage()
            ));
        }
        
        // 下载训练模板
        try {
            List<SyncRequest.WorkoutTemplateData> workoutTemplates = getWorkoutTemplatesAfter(userId, lastSync);
            response.setWorkoutTemplates(workoutTemplates);
            totalItems += workoutTemplates.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download workout templates", e);
            status.addError(new SyncErrorDetail(
                "workout_template",
                "download",
                classifyError(e),
                "Failed to download workout templates: " + e.getMessage()
            ));
        }
        
        // 下载收藏动作
        try {
            List<SyncRequest.FavoriteExerciseData> favoriteExercises = getFavoriteExercises(userId);
            response.setFavoriteExercises(favoriteExercises);
            totalItems += favoriteExercises.size();
            status.incrementSuccess();
        } catch (Exception e) {
            logger.error("Failed to download favorite exercises", e);
            status.addError(new SyncErrorDetail(
                "favorite_exercise",
                "download",
                classifyError(e),
                "Failed to download favorite exercises: " + e.getMessage()
            ));
        }
        
        status.setTotalItems(totalItems);
        status.computeSuccess();
        return status;
    }
    
    /**
     * 将异常类型映射为稳定错误码；绝不依赖驱动或数据库方言的错误文本。
     */
    private String classifyError(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof IllegalArgumentException ||
                current instanceof jakarta.validation.ValidationException) {
                return "VALIDATION_FAILED";
            }
            if (current instanceof org.springframework.dao.DuplicateKeyException) {
                return "DUPLICATE";
            }
            if (current instanceof org.springframework.dao.DataIntegrityViolationException) {
                return "VALIDATION_FAILED";
            }
            if (current instanceof org.springframework.dao.DataAccessException ||
                current instanceof java.sql.SQLException) {
                return "DB_ERROR";
            }
            if (current instanceof java.net.SocketTimeoutException ||
                current instanceof java.net.ConnectException) {
                return "NETWORK_ERROR";
            }
            current = current.getCause();
        }
        return "UNKNOWN";
    }
    
    private void saveWorkout(Long userId, SyncRequest.WorkoutData data) {
        QueryWrapper<Workout> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        Workout existing = workoutMapper.selectOne(wrapper);
        
        Workout workout;
        if (existing != null) {
            workout = existing;
            workout.setName(data.getName());
            workout.setDate(data.getDate());
            workout.setDuration(data.getDuration());
            workout.setNotes(data.getNotes());
            workout.setUpdatedAt(data.getUpdatedAt());
            workoutMapper.updateById(workout);
            
            QueryWrapper<WorkoutExercise> exWrapper = new QueryWrapper<>();
            exWrapper.eq("workout_id", workout.getId());
            List<WorkoutExercise> oldExercises = workoutExerciseMapper.selectList(exWrapper);
            for (WorkoutExercise ex : oldExercises) {
                QueryWrapper<WorkoutSet> setWrapper = new QueryWrapper<>();
                setWrapper.eq("workout_exercise_id", ex.getId());
                workoutSetMapper.delete(setWrapper);
            }
            workoutExerciseMapper.delete(exWrapper);
        } else {
            workout = new Workout();
            workout.setUserId(userId);
            workout.setLocalId(data.getLocalId());
            workout.setName(data.getName());
            workout.setDate(data.getDate());
            workout.setDuration(data.getDuration());
            workout.setNotes(data.getNotes());
            workout.setCreatedAt(System.currentTimeMillis());
            workout.setUpdatedAt(data.getUpdatedAt());
            workoutMapper.insert(workout);
        }
        
        if (data.getExercises() != null) {
            for (SyncRequest.ExerciseData exData : data.getExercises()) {
                WorkoutExercise exercise = new WorkoutExercise();
                exercise.setWorkoutId(workout.getId());
                exercise.setLocalId(exData.getLocalId());
                exercise.setExerciseName(exData.getExerciseName());
                exercise.setExerciseOrder(exData.getExerciseOrder());
                exercise.setSupersetGroupId(exData.getSupersetGroupId());
                workoutExerciseMapper.insert(exercise);
                
                if (exData.getSets() != null) {
                    for (SyncRequest.SetData setData : exData.getSets()) {
                        WorkoutSet set = new WorkoutSet();
                        set.setWorkoutExerciseId(exercise.getId());
                        set.setLocalId(setData.getLocalId());
                        set.setSetNumber(setData.getSetNumber());
                        set.setWeight(setData.getWeight());
                        set.setReps(setData.getReps());
                        set.setIsCompleted(setData.getIsCompleted());
                        set.setRestTime(setData.getRestTime());
                        workoutSetMapper.insert(set);
                    }
                }
            }
        }
    }
    
    private void saveDietRecord(Long userId, SyncRequest.DietRecordData data) {
        // 批量 Agent 草案的稳定身份是 actionId + recordKey；不能只依赖可变的 localId。
        if (data.getAgentActionId() != null && data.getAgentActionId() > 0 &&
            data.getRecordKey() != null && !data.getRecordKey().isBlank()) {
            QueryWrapper<DietRecord> actionWrapper = new QueryWrapper<>();
            actionWrapper.eq("user_id", userId)
                .eq("agent_action_id", data.getAgentActionId())
                .eq("record_key", data.getRecordKey());
            DietRecord existingByAction = dietRecordMapper.selectOne(actionWrapper);

            if (existingByAction != null) {
                if (data.getUpdatedAt() > existingByAction.getUpdatedAt()) {
                    updateDietRecordFields(existingByAction, data);
                    dietRecordMapper.updateById(existingByAction);
                }
                return;
            }
        }

        // 兼容普通本地记录及历史数据；同一用户的 localId 仍是第二重幂等键。
        QueryWrapper<DietRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        DietRecord existing = dietRecordMapper.selectOne(wrapper);

        if (existing != null) {
            updateDietRecordFields(existing, data);
            dietRecordMapper.updateById(existing);
        } else {
            DietRecord record = new DietRecord();
            record.setUserId(userId);
            record.setLocalId(data.getLocalId());
            updateDietRecordFields(record, data);
            record.setCreatedAt(System.currentTimeMillis());
            dietRecordMapper.insert(record);
        }
    }

    private void updateDietRecordFields(DietRecord record, SyncRequest.DietRecordData data) {
        record.setDate(data.getDate());
        record.setMealType(data.getMealType());
        record.setFoodName(data.getFoodName());
        record.setCalories(data.getCalories());
        record.setProtein(data.getProtein());
        record.setCarbs(data.getCarbs());
        record.setFat(data.getFat());
        record.setAmount(data.getAmount());
        record.setUpdatedAt(data.getUpdatedAt());
        record.setAgentActionId(data.getAgentActionId());
        record.setRecordKey(data.getRecordKey());
    }
    
    private void saveBodyRecord(Long userId, SyncRequest.BodyRecordData data) {
        QueryWrapper<BodyRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        BodyRecord existing = bodyRecordMapper.selectOne(wrapper);
        
        if (existing != null) {
            existing.setDate(data.getDate());
            existing.setWeight(data.getWeight());
            existing.setBodyFat(data.getBodyFat());
            existing.setMuscleMass(data.getMuscleMass());
            existing.setNote(data.getNote());
            existing.setUpdatedAt(data.getUpdatedAt());
            bodyRecordMapper.updateById(existing);
        } else {
            BodyRecord record = new BodyRecord();
            record.setUserId(userId);
            record.setLocalId(data.getLocalId());
            record.setDate(data.getDate());
            record.setWeight(data.getWeight());
            record.setBodyFat(data.getBodyFat());
            record.setMuscleMass(data.getMuscleMass());
            record.setNote(data.getNote());
            record.setCreatedAt(System.currentTimeMillis());
            record.setUpdatedAt(data.getUpdatedAt());
            bodyRecordMapper.insert(record);
        }
    }
    
    private List<SyncRequest.WorkoutData> getWorkoutsAfter(Long userId, Long lastSync) {
        QueryWrapper<Workout> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).gt("updated_at", lastSync);
        List<Workout> workouts = workoutMapper.selectList(wrapper);
        
        List<SyncRequest.WorkoutData> result = new ArrayList<>();
        for (Workout w : workouts) {
            SyncRequest.WorkoutData data = new SyncRequest.WorkoutData();
            data.setLocalId(w.getLocalId());
            data.setName(w.getName());
            data.setDate(w.getDate());
            data.setDuration(w.getDuration());
            data.setNotes(w.getNotes());
            data.setUpdatedAt(w.getUpdatedAt());
            
            QueryWrapper<WorkoutExercise> exWrapper = new QueryWrapper<>();
            exWrapper.eq("workout_id", w.getId()).orderByAsc("exercise_order");
            List<WorkoutExercise> exercises = workoutExerciseMapper.selectList(exWrapper);
            
            List<SyncRequest.ExerciseData> exList = new ArrayList<>();
            for (WorkoutExercise ex : exercises) {
                SyncRequest.ExerciseData exData = new SyncRequest.ExerciseData();
                exData.setLocalId(ex.getLocalId());
                exData.setExerciseName(ex.getExerciseName());
                exData.setExerciseOrder(ex.getExerciseOrder());
                exData.setSupersetGroupId(ex.getSupersetGroupId());
                
                QueryWrapper<WorkoutSet> setWrapper = new QueryWrapper<>();
                setWrapper.eq("workout_exercise_id", ex.getId()).orderByAsc("set_number");
                List<WorkoutSet> sets = workoutSetMapper.selectList(setWrapper);
                
                List<SyncRequest.SetData> setList = new ArrayList<>();
                for (WorkoutSet s : sets) {
                    SyncRequest.SetData setData = new SyncRequest.SetData();
                    setData.setLocalId(s.getLocalId());
                    setData.setSetNumber(s.getSetNumber());
                    setData.setWeight(s.getWeight());
                    setData.setReps(s.getReps());
                    setData.setIsCompleted(s.getIsCompleted());
                    setData.setRestTime(s.getRestTime());
                    setList.add(setData);
                }
                exData.setSets(setList);
                exList.add(exData);
            }
            data.setExercises(exList);
            result.add(data);
        }
        return result;
    }
    
    private List<SyncRequest.DietRecordData> getDietRecordsAfter(Long userId, Long lastSync) {
        QueryWrapper<DietRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).gt("updated_at", lastSync);
        List<DietRecord> records = dietRecordMapper.selectList(wrapper);
        
        List<SyncRequest.DietRecordData> result = new ArrayList<>();
        for (DietRecord r : records) {
            SyncRequest.DietRecordData data = new SyncRequest.DietRecordData();
            data.setLocalId(r.getLocalId());
            data.setDate(r.getDate());
            data.setMealType(r.getMealType());
            data.setFoodName(r.getFoodName());
            data.setCalories(r.getCalories());
            data.setProtein(r.getProtein());
            data.setCarbs(r.getCarbs());
            data.setFat(r.getFat());
            data.setAmount(r.getAmount());
            data.setUpdatedAt(r.getUpdatedAt());
            data.setAgentActionId(r.getAgentActionId());
            data.setRecordKey(r.getRecordKey());
            result.add(data);
        }
        return result;
    }
    
    private List<SyncRequest.BodyRecordData> getBodyRecordsAfter(Long userId, Long lastSync) {
        QueryWrapper<BodyRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).gt("updated_at", lastSync);
        List<BodyRecord> records = bodyRecordMapper.selectList(wrapper);
        
        List<SyncRequest.BodyRecordData> result = new ArrayList<>();
        for (BodyRecord r : records) {
            SyncRequest.BodyRecordData data = new SyncRequest.BodyRecordData();
            data.setLocalId(r.getLocalId());
            data.setDate(r.getDate());
            data.setWeight(r.getWeight());
            data.setBodyFat(r.getBodyFat());
            data.setMuscleMass(r.getMuscleMass());
            data.setNote(r.getNote());
            data.setUpdatedAt(r.getUpdatedAt());
            result.add(data);
        }
        return result;
    }
    
    private void saveTrainingPlan(Long userId, SyncRequest.TrainingPlanData data) {
        // F0改进：先检查agentActionId幂等性（如果是Agent创建的）
        if (data.getAgentActionId() != null) {
            QueryWrapper<TrainingPlan> actionWrapper = new QueryWrapper<>();
            actionWrapper.eq("user_id", userId).eq("agent_action_id", data.getAgentActionId());
            TrainingPlan existingByAction = trainingPlanMapper.selectOne(actionWrapper);
            
            if (existingByAction != null) {
                // 幂等：该agentActionId已经创建过计划
                // 检查是否需要更新（客户端可能编辑了草案后重新同步）
                if (data.getUpdatedAt() > existingByAction.getUpdatedAt()) {
                    updateTrainingPlanFields(existingByAction, data);
                    trainingPlanMapper.updateById(existingByAction);
                }
                return; // 幂等跳过
            }
        }
        
        // 继续原有的localId查重逻辑
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        TrainingPlan existing = trainingPlanMapper.selectOne(wrapper);
        
        if (existing != null) {
            updateTrainingPlanFields(existing, data);
            trainingPlanMapper.updateById(existing);
        } else {
            TrainingPlan plan = new TrainingPlan();
            plan.setUserId(userId);
            plan.setLocalId(data.getLocalId());
            updateTrainingPlanFields(plan, data);
            plan.setCreatedAt(data.getCreatedAt());
            trainingPlanMapper.insert(plan);
        }
    }
    
    /**
     * F0新增：提取TrainingPlan字段更新逻辑，避免重复代码
     */
    private void updateTrainingPlanFields(TrainingPlan plan, SyncRequest.TrainingPlanData data) {
        plan.setTitle(data.getTitle());
        plan.setDescription(data.getDescription());
        plan.setDetails(data.getDetails());
        plan.setGoal(data.getGoal());
        plan.setExperience(data.getExperience());
        plan.setTargetMuscles(data.getTargetMuscles());
        plan.setTrainingDays(data.getTrainingDays());
        plan.setTrainingDuration(data.getTrainingDuration());
        plan.setEquipment(data.getEquipment());
        plan.setIsPinned(data.getIsPinned());
        plan.setIsFromRecommendation(data.getIsFromRecommendation());
        plan.setUpdatedAt(data.getUpdatedAt());
        plan.setAgentActionId(data.getAgentActionId());  // F0新增
    }
    
    private List<SyncRequest.TrainingPlanData> getTrainingPlansAfter(Long userId, Long lastSync) {
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).gt("updated_at", lastSync);
        List<TrainingPlan> plans = trainingPlanMapper.selectList(wrapper);
        
        List<SyncRequest.TrainingPlanData> result = new ArrayList<>();
        for (TrainingPlan p : plans) {
            SyncRequest.TrainingPlanData data = new SyncRequest.TrainingPlanData();
            data.setLocalId(p.getLocalId());
            data.setTitle(p.getTitle());
            data.setDescription(p.getDescription());
            data.setDetails(p.getDetails());
            data.setGoal(p.getGoal());
            data.setExperience(p.getExperience());
            data.setTargetMuscles(p.getTargetMuscles());
            data.setTrainingDays(p.getTrainingDays());
            data.setTrainingDuration(p.getTrainingDuration());
            data.setEquipment(p.getEquipment());
            data.setIsPinned(p.getIsPinned());
            data.setIsFromRecommendation(p.getIsFromRecommendation());
            data.setCreatedAt(p.getCreatedAt());
            data.setUpdatedAt(p.getUpdatedAt());
            data.setAgentActionId(p.getAgentActionId());  // F0新增
            result.add(data);
        }
        return result;
    }
    
    // ==================== 身体档案相关方法 ====================
    
    private void saveBodyProfile(Long userId, SyncRequest.BodyProfileData data) {
        QueryWrapper<BodyProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        BodyProfile existing = bodyProfileMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 比较更新时间，只有客户端数据更新时才更新
            if (data.getUpdatedAt() > existing.getUpdatedAt()) {
                existing.setGender(data.getGender());
                existing.setHeight(data.getHeight());
                existing.setBirthYear(data.getBirthYear());
                existing.setUpdatedAt(data.getUpdatedAt());
                bodyProfileMapper.updateById(existing);
            }
        } else {
            BodyProfile profile = new BodyProfile();
            profile.setUserId(userId);
            profile.setGender(data.getGender());
            profile.setHeight(data.getHeight());
            profile.setBirthYear(data.getBirthYear());
            profile.setUpdatedAt(data.getUpdatedAt());
            bodyProfileMapper.insert(profile);
        }
    }
    
    private SyncRequest.BodyProfileData getBodyProfile(Long userId) {
        QueryWrapper<BodyProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        BodyProfile profile = bodyProfileMapper.selectOne(wrapper);
        
        if (profile == null) {
            return null;
        }
        
        SyncRequest.BodyProfileData data = new SyncRequest.BodyProfileData();
        data.setGender(profile.getGender());
        data.setHeight(profile.getHeight());
        data.setBirthYear(profile.getBirthYear());
        data.setUpdatedAt(profile.getUpdatedAt());
        return data;
    }
    
    // ==================== 自定义动作相关方法 ====================
    
    private void saveCustomExercise(Long userId, SyncRequest.CustomExerciseData data) {
        QueryWrapper<CustomExercise> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        CustomExercise existing = customExerciseMapper.selectOne(wrapper);
        
        if (existing != null) {
            // 比较更新时间
            if (data.getUpdatedAt() > existing.getUpdatedAt()) {
                existing.setName(data.getName());
                existing.setMuscleGroup(data.getMuscleGroup());
                existing.setSubMuscleGroup(data.getSubMuscleGroup());
                existing.setEquipment(data.getEquipment());
                existing.setDescription(data.getDescription());
                existing.setImageUrl(data.getImageUrl());
                existing.setIsFavorite(data.getIsFavorite());
                existing.setUpdatedAt(data.getUpdatedAt());
                customExerciseMapper.updateById(existing);
            }
        } else {
            CustomExercise exercise = new CustomExercise();
            exercise.setUserId(userId);
            exercise.setLocalId(data.getLocalId());
            exercise.setName(data.getName());
            exercise.setMuscleGroup(data.getMuscleGroup());
            exercise.setSubMuscleGroup(data.getSubMuscleGroup());
            exercise.setEquipment(data.getEquipment());
            exercise.setDescription(data.getDescription());
            exercise.setImageUrl(data.getImageUrl());
            exercise.setIsFavorite(data.getIsFavorite());
            exercise.setCreatedAt(data.getCreatedAt());
            exercise.setUpdatedAt(data.getUpdatedAt());
            customExerciseMapper.insert(exercise);
        }
    }
    
    private List<SyncRequest.CustomExerciseData> getCustomExercisesAfter(Long userId, Long lastSync) {
        QueryWrapper<CustomExercise> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).gt("updated_at", lastSync);
        List<CustomExercise> exercises = customExerciseMapper.selectList(wrapper);
        
        List<SyncRequest.CustomExerciseData> result = new ArrayList<>();
        for (CustomExercise e : exercises) {
            SyncRequest.CustomExerciseData data = new SyncRequest.CustomExerciseData();
            data.setLocalId(e.getLocalId());
            data.setName(e.getName());
            data.setMuscleGroup(e.getMuscleGroup());
            data.setSubMuscleGroup(e.getSubMuscleGroup());
            data.setEquipment(e.getEquipment());
            data.setDescription(e.getDescription());
            data.setImageUrl(e.getImageUrl());
            data.setIsFavorite(e.getIsFavorite());
            data.setCreatedAt(e.getCreatedAt());
            data.setUpdatedAt(e.getUpdatedAt());
            result.add(data);
        }
        return result;
    }
    
    // ==================== 训练模板相关方法 ====================
    
    private void saveWorkoutTemplate(Long userId, SyncRequest.WorkoutTemplateData data) {
        QueryWrapper<WorkoutTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        WorkoutTemplateEntity existing = workoutTemplateMapper.selectOne(wrapper);
        
        WorkoutTemplateEntity template;
        if (existing != null) {
            // 比较更新时间
            if (data.getUpdatedAt() > existing.getUpdatedAt()) {
                template = existing;
                template.setName(data.getName());
                template.setDescription(data.getDescription());
                template.setUpdatedAt(data.getUpdatedAt());
                workoutTemplateMapper.updateById(template);
                
                // 删除旧的动作
                QueryWrapper<TemplateExerciseEntity> exWrapper = new QueryWrapper<>();
                exWrapper.eq("template_id", template.getId());
                templateExerciseMapper.delete(exWrapper);
            } else {
                return; // 云端数据更新，不覆盖
            }
        } else {
            template = new WorkoutTemplateEntity();
            template.setUserId(userId);
            template.setLocalId(data.getLocalId());
            template.setName(data.getName());
            template.setDescription(data.getDescription());
            template.setCreatedAt(data.getCreatedAt());
            template.setUpdatedAt(data.getUpdatedAt());
            workoutTemplateMapper.insert(template);
        }
        
        // 保存动作列表
        if (data.getExercises() != null) {
            for (SyncRequest.TemplateExerciseData exData : data.getExercises()) {
                TemplateExerciseEntity exercise = new TemplateExerciseEntity();
                exercise.setTemplateId(template.getId());
                exercise.setLocalId(exData.getLocalId());
                exercise.setExerciseName(exData.getExerciseName());
                exercise.setSortOrder(exData.getSortOrder());
                exercise.setTargetSets(exData.getTargetSets());
                exercise.setTargetReps(exData.getTargetReps());
                templateExerciseMapper.insert(exercise);
            }
        }
    }
    
    private List<SyncRequest.WorkoutTemplateData> getWorkoutTemplatesAfter(Long userId, Long lastSync) {
        QueryWrapper<WorkoutTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).gt("updated_at", lastSync);
        List<WorkoutTemplateEntity> templates = workoutTemplateMapper.selectList(wrapper);
        
        List<SyncRequest.WorkoutTemplateData> result = new ArrayList<>();
        for (WorkoutTemplateEntity t : templates) {
            SyncRequest.WorkoutTemplateData data = new SyncRequest.WorkoutTemplateData();
            data.setLocalId(t.getLocalId());
            data.setName(t.getName());
            data.setDescription(t.getDescription());
            data.setCreatedAt(t.getCreatedAt());
            data.setUpdatedAt(t.getUpdatedAt());
            
            // 查询关联的动作
            QueryWrapper<TemplateExerciseEntity> exWrapper = new QueryWrapper<>();
            exWrapper.eq("template_id", t.getId()).orderByAsc("sort_order");
            List<TemplateExerciseEntity> exercises = templateExerciseMapper.selectList(exWrapper);
            
            List<SyncRequest.TemplateExerciseData> exList = new ArrayList<>();
            for (TemplateExerciseEntity ex : exercises) {
                SyncRequest.TemplateExerciseData exData = new SyncRequest.TemplateExerciseData();
                exData.setLocalId(ex.getLocalId());
                exData.setExerciseName(ex.getExerciseName());
                exData.setSortOrder(ex.getSortOrder());
                exData.setTargetSets(ex.getTargetSets());
                exData.setTargetReps(ex.getTargetReps());
                exList.add(exData);
            }
            data.setExercises(exList);
            result.add(data);
        }
        return result;
    }
    
    // ==================== 收藏动作相关方法 ====================
    
    private void saveFavoriteExercise(Long userId, SyncRequest.FavoriteExerciseData data) {
        QueryWrapper<FavoriteExercise> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("exercise_name", data.getExerciseName());
        FavoriteExercise existing = favoriteExerciseMapper.selectOne(wrapper);
        
        if (existing == null) {
            FavoriteExercise favorite = new FavoriteExercise();
            favorite.setUserId(userId);
            favorite.setExerciseName(data.getExerciseName());
            favorite.setCreatedAt(data.getCreatedAt());
            favoriteExerciseMapper.insert(favorite);
        }
    }
    
    private List<SyncRequest.FavoriteExerciseData> getFavoriteExercises(Long userId) {
        QueryWrapper<FavoriteExercise> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<FavoriteExercise> favorites = favoriteExerciseMapper.selectList(wrapper);
        
        List<SyncRequest.FavoriteExerciseData> result = new ArrayList<>();
        for (FavoriteExercise f : favorites) {
            SyncRequest.FavoriteExerciseData data = new SyncRequest.FavoriteExerciseData();
            data.setExerciseName(f.getExerciseName());
            data.setCreatedAt(f.getCreatedAt());
            result.add(data);
        }
        return result;
    }
}
