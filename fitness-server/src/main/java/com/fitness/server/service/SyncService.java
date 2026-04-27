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
        
        SyncResponse response = new SyncResponse();
        response.setServerTime(now);
        
        Long lastSync = request.getLastSyncTime() != null ? request.getLastSyncTime() : 0L;
        
        response.setWorkouts(getWorkoutsAfter(userId, lastSync));
        response.setDietRecords(getDietRecordsAfter(userId, lastSync));
        response.setBodyRecords(getBodyRecordsAfter(userId, lastSync));
        response.setTrainingPlans(getTrainingPlansAfter(userId, lastSync));
        
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
}
