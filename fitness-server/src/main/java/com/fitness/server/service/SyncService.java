package com.fitness.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fitness.server.dto.SyncRequest;
import com.fitness.server.dto.SyncResponse;
import com.fitness.server.entity.*;
import com.fitness.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyncService {
    
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
        
        if (request.getWorkouts() != null) {
            for (SyncRequest.WorkoutData data : request.getWorkouts()) {
                saveWorkout(userId, data);
            }
        }
        
        if (request.getDietRecords() != null) {
            for (SyncRequest.DietRecordData data : request.getDietRecords()) {
                saveDietRecord(userId, data);
            }
        }
        
        if (request.getBodyRecords() != null) {
            for (SyncRequest.BodyRecordData data : request.getBodyRecords()) {
                saveBodyRecord(userId, data);
            }
        }
        
        if (request.getTrainingPlans() != null) {
            for (SyncRequest.TrainingPlanData data : request.getTrainingPlans()) {
                saveTrainingPlan(userId, data);
            }
        }
        
        // 新增：保存身体档案
        if (request.getBodyProfile() != null) {
            saveBodyProfile(userId, request.getBodyProfile());
        }
        
        // 新增：保存自定义动作
        if (request.getCustomExercises() != null) {
            for (SyncRequest.CustomExerciseData data : request.getCustomExercises()) {
                saveCustomExercise(userId, data);
            }
        }
        
        // 新增：保存训练模板
        if (request.getWorkoutTemplates() != null) {
            for (SyncRequest.WorkoutTemplateData data : request.getWorkoutTemplates()) {
                saveWorkoutTemplate(userId, data);
            }
        }
        
        // 新增：保存收藏动作
        if (request.getFavoriteExercises() != null) {
            for (SyncRequest.FavoriteExerciseData data : request.getFavoriteExercises()) {
                saveFavoriteExercise(userId, data);
            }
        }
        
        SyncResponse response = new SyncResponse();
        response.setServerTime(now);
        
        Long lastSync = request.getLastSyncTime() != null ? request.getLastSyncTime() : 0L;
        
        response.setWorkouts(getWorkoutsAfter(userId, lastSync));
        response.setDietRecords(getDietRecordsAfter(userId, lastSync));
        response.setBodyRecords(getBodyRecordsAfter(userId, lastSync));
        response.setTrainingPlans(getTrainingPlansAfter(userId, lastSync));
        
        // 新增：返回身体档案
        response.setBodyProfile(getBodyProfile(userId));
        
        // 新增：返回自定义动作
        response.setCustomExercises(getCustomExercisesAfter(userId, lastSync));
        
        // 新增：返回训练模板
        response.setWorkoutTemplates(getWorkoutTemplatesAfter(userId, lastSync));
        
        // 新增：返回收藏动作
        response.setFavoriteExercises(getFavoriteExercises(userId));
        
        return response;
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
        QueryWrapper<DietRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        DietRecord existing = dietRecordMapper.selectOne(wrapper);
        
        if (existing != null) {
            existing.setDate(data.getDate());
            existing.setMealType(data.getMealType());
            existing.setFoodName(data.getFoodName());
            existing.setCalories(data.getCalories());
            existing.setProtein(data.getProtein());
            existing.setCarbs(data.getCarbs());
            existing.setFat(data.getFat());
            existing.setAmount(data.getAmount());
            existing.setUpdatedAt(data.getUpdatedAt());
            dietRecordMapper.updateById(existing);
        } else {
            DietRecord record = new DietRecord();
            record.setUserId(userId);
            record.setLocalId(data.getLocalId());
            record.setDate(data.getDate());
            record.setMealType(data.getMealType());
            record.setFoodName(data.getFoodName());
            record.setCalories(data.getCalories());
            record.setProtein(data.getProtein());
            record.setCarbs(data.getCarbs());
            record.setFat(data.getFat());
            record.setAmount(data.getAmount());
            record.setCreatedAt(System.currentTimeMillis());
            record.setUpdatedAt(data.getUpdatedAt());
            dietRecordMapper.insert(record);
        }
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
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("local_id", data.getLocalId());
        TrainingPlan existing = trainingPlanMapper.selectOne(wrapper);
        
        if (existing != null) {
            existing.setTitle(data.getTitle());
            existing.setDescription(data.getDescription());
            existing.setDetails(data.getDetails());
            existing.setGoal(data.getGoal());
            existing.setExperience(data.getExperience());
            existing.setTargetMuscles(data.getTargetMuscles());
            existing.setTrainingDays(data.getTrainingDays());
            existing.setTrainingDuration(data.getTrainingDuration());
            existing.setEquipment(data.getEquipment());
            existing.setIsPinned(data.getIsPinned());
            existing.setIsFromRecommendation(data.getIsFromRecommendation());
            existing.setUpdatedAt(data.getUpdatedAt());
            trainingPlanMapper.updateById(existing);
        } else {
            TrainingPlan plan = new TrainingPlan();
            plan.setUserId(userId);
            plan.setLocalId(data.getLocalId());
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
            plan.setCreatedAt(data.getCreatedAt());
            plan.setUpdatedAt(data.getUpdatedAt());
            trainingPlanMapper.insert(plan);
        }
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
