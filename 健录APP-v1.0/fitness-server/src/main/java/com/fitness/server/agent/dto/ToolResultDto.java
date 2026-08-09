package com.fitness.server.agent.dto;

import java.util.List;
import java.util.Map;

/**
 * 工具执行结果DTO - 避免手工拼接JSON
 */
public class ToolResultDto {
    
    // ===== 训练摘要 =====
    public static class TrainingSummaryResult {
        private int totalWorkouts;
        private long totalDurationMinutes;
        private Map<String, Integer> muscleGroupDistribution;
        private List<RecentWorkoutItem> recentWorkouts;
        
        public int getTotalWorkouts() { return totalWorkouts; }
        public void setTotalWorkouts(int totalWorkouts) { this.totalWorkouts = totalWorkouts; }
        
        public long getTotalDurationMinutes() { return totalDurationMinutes; }
        public void setTotalDurationMinutes(long totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }
        
        public Map<String, Integer> getMuscleGroupDistribution() { return muscleGroupDistribution; }
        public void setMuscleGroupDistribution(Map<String, Integer> muscleGroupDistribution) { this.muscleGroupDistribution = muscleGroupDistribution; }
        
        public List<RecentWorkoutItem> getRecentWorkouts() { return recentWorkouts; }
        public void setRecentWorkouts(List<RecentWorkoutItem> recentWorkouts) { this.recentWorkouts = recentWorkouts; }
    }
    
    public static class RecentWorkoutItem {
        private String date;
        private String name;
        private long durationMinutes;
        private List<String> exercises;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public long getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
        
        public List<String> getExercises() { return exercises; }
        public void setExercises(List<String> exercises) { this.exercises = exercises; }
    }
    
    // ===== 最近训练列表 =====
    public static class RecentWorkoutsResult {
        private List<WorkoutDetail> workouts;
        
        public List<WorkoutDetail> getWorkouts() { return workouts; }
        public void setWorkouts(List<WorkoutDetail> workouts) { this.workouts = workouts; }
    }
    
    public static class WorkoutDetail {
        private String date;
        private String name;
        private long durationMinutes;
        private String notes;
        private List<ExerciseSummary> exercises;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public long getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(long durationMinutes) { this.durationMinutes = durationMinutes; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        
        public List<ExerciseSummary> getExercises() { return exercises; }
        public void setExercises(List<ExerciseSummary> exercises) { this.exercises = exercises; }
    }
    
    public static class ExerciseSummary {
        private String name;
        private int sets;
        private String muscleGroup;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getSets() { return sets; }
        public void setSets(int sets) { this.sets = sets; }
        
        public String getMuscleGroup() { return muscleGroup; }
        public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }
    }
    
    // ===== 身体数据趋势 =====
    public static class BodyTrendResult {
        private BodyMetric weight;
        private BodyMetric bodyFat;
        private BodyMetric muscleMass;
        
        public BodyMetric getWeight() { return weight; }
        public void setWeight(BodyMetric weight) { this.weight = weight; }
        
        public BodyMetric getBodyFat() { return bodyFat; }
        public void setBodyFat(BodyMetric bodyFat) { this.bodyFat = bodyFat; }
        
        public BodyMetric getMuscleMass() { return muscleMass; }
        public void setMuscleMass(BodyMetric muscleMass) { this.muscleMass = muscleMass; }
    }
    
    public static class BodyMetric {
        private Float latest;
        private Float earliest;
        private Float change;
        private String trend;
        
        public Float getLatest() { return latest; }
        public void setLatest(Float latest) { this.latest = latest; }
        
        public Float getEarliest() { return earliest; }
        public void setEarliest(Float earliest) { this.earliest = earliest; }
        
        public Float getChange() { return change; }
        public void setChange(Float change) { this.change = change; }
        
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
    }
    
    // ===== 今日饮食摘要 =====
    public static class TodayDietResult {
        private int totalCalories;
        private float totalProtein;
        private float totalCarbs;
        private float totalFat;
        private int mealCount;
        
        public int getTotalCalories() { return totalCalories; }
        public void setTotalCalories(int totalCalories) { this.totalCalories = totalCalories; }
        
        public float getTotalProtein() { return totalProtein; }
        public void setTotalProtein(float totalProtein) { this.totalProtein = totalProtein; }
        
        public float getTotalCarbs() { return totalCarbs; }
        public void setTotalCarbs(float totalCarbs) { this.totalCarbs = totalCarbs; }
        
        public float getTotalFat() { return totalFat; }
        public void setTotalFat(float totalFat) { this.totalFat = totalFat; }
        
        public int getMealCount() { return mealCount; }
        public void setMealCount(int mealCount) { this.mealCount = mealCount; }
    }
    
    // ===== 恢复状态 =====
    public static class RecoveryStatusResult {
        private int daysSinceLastWorkout;
        private Map<String, MuscleGroupRecovery> muscleGroups;
        private String overallStatus;
        private List<String> recommendations;
        
        public int getDaysSinceLastWorkout() { return daysSinceLastWorkout; }
        public void setDaysSinceLastWorkout(int daysSinceLastWorkout) { this.daysSinceLastWorkout = daysSinceLastWorkout; }
        
        public Map<String, MuscleGroupRecovery> getMuscleGroups() { return muscleGroups; }
        public void setMuscleGroups(Map<String, MuscleGroupRecovery> muscleGroups) { this.muscleGroups = muscleGroups; }
        
        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
    
    public static class MuscleGroupRecovery {
        private int daysSinceLastTrained;
        private String status;
        private String lastTrainedDate;
        
        public int getDaysSinceLastTrained() { return daysSinceLastTrained; }
        public void setDaysSinceLastTrained(int daysSinceLastTrained) { this.daysSinceLastTrained = daysSinceLastTrained; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getLastTrainedDate() { return lastTrainedDate; }
        public void setLastTrainedDate(String lastTrainedDate) { this.lastTrainedDate = lastTrainedDate; }
    }
    
    // ===== 活跃训练计划 =====
    public static class ActivePlanResult {
        private boolean hasPlan;
        private PlanInfo plan;
        private List<PlanInfo> plans;
        private List<Long> recentPlanIds;
        private List<Long> pinnedPlanIds;
        
        public boolean isHasPlan() { return hasPlan; }
        public void setHasPlan(boolean hasPlan) { this.hasPlan = hasPlan; }
        
        public PlanInfo getPlan() { return plan; }
        public void setPlan(PlanInfo plan) { this.plan = plan; }

        public List<PlanInfo> getPlans() { return plans; }
        public void setPlans(List<PlanInfo> plans) { this.plans = plans; }

        public List<Long> getRecentPlanIds() { return recentPlanIds; }
        public void setRecentPlanIds(List<Long> recentPlanIds) { this.recentPlanIds = recentPlanIds; }

        public List<Long> getPinnedPlanIds() { return pinnedPlanIds; }
        public void setPinnedPlanIds(List<Long> pinnedPlanIds) { this.pinnedPlanIds = pinnedPlanIds; }
    }
    
    public static class PlanInfo {
        private Long id;
        private String title;
        private String description;
        private String details;
        private String goal;
        private String experience;
        private int trainingDays;
        private String targetMuscles;
        private String trainingDuration;
        private String equipment;
        private boolean pinned;
        private Long updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public String getGoal() { return goal; }
        public void setGoal(String goal) { this.goal = goal; }

        public String getExperience() { return experience; }
        public void setExperience(String experience) { this.experience = experience; }
        
        public int getTrainingDays() { return trainingDays; }
        public void setTrainingDays(int trainingDays) { this.trainingDays = trainingDays; }
        
        public String getTargetMuscles() { return targetMuscles; }
        public void setTargetMuscles(String targetMuscles) { this.targetMuscles = targetMuscles; }

        public String getTrainingDuration() { return trainingDuration; }
        public void setTrainingDuration(String trainingDuration) { this.trainingDuration = trainingDuration; }

        public String getEquipment() { return equipment; }
        public void setEquipment(String equipment) { this.equipment = equipment; }

        public boolean isPinned() { return pinned; }
        public void setPinned(boolean pinned) { this.pinned = pinned; }

        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    // ===== Phase E P0: 饮食摘要 (指定日期或日期范围) =====
    public static class DietSummaryResult {
        private DateRange dateRange;
        private List<DaySummary> days;
        
        public DateRange getDateRange() { return dateRange; }
        public void setDateRange(DateRange dateRange) { this.dateRange = dateRange; }
        
        public List<DaySummary> getDays() { return days; }
        public void setDays(List<DaySummary> days) { this.days = days; }
    }
    
    public static class DateRange {
        private String start;
        private String end;
        
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
    
    public static class DaySummary {
        private String date;
        private int mealCount;
        private int totalCalories;
        private float totalProtein;
        private float totalCarbs;
        private float totalFat;
        private List<MealRecord> records;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public int getMealCount() { return mealCount; }
        public void setMealCount(int mealCount) { this.mealCount = mealCount; }
        
        public int getTotalCalories() { return totalCalories; }
        public void setTotalCalories(int totalCalories) { this.totalCalories = totalCalories; }
        
        public float getTotalProtein() { return totalProtein; }
        public void setTotalProtein(float totalProtein) { this.totalProtein = totalProtein; }
        
        public float getTotalCarbs() { return totalCarbs; }
        public void setTotalCarbs(float totalCarbs) { this.totalCarbs = totalCarbs; }
        
        public float getTotalFat() { return totalFat; }
        public void setTotalFat(float totalFat) { this.totalFat = totalFat; }
        
        public List<MealRecord> getRecords() { return records; }
        public void setRecords(List<MealRecord> records) { this.records = records; }
    }
    
    public static class MealRecord {
        private String mealType;
        private String foodName;
        private String amount;
        private int calories;
        private float protein;
        private float carbs;
        private float fat;
        
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        
        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public int getCalories() { return calories; }
        public void setCalories(int calories) { this.calories = calories; }
        
        public float getProtein() { return protein; }
        public void setProtein(float protein) { this.protein = protein; }
        
        public float getCarbs() { return carbs; }
        public void setCarbs(float carbs) { this.carbs = carbs; }
        
        public float getFat() { return fat; }
        public void setFat(float fat) { this.fat = fat; }
    }
    
    // ===== Phase E P0: 训练安排 (指定日期) =====
    public static class TrainingScheduleResult {
        private String date;
        private boolean hasWorkout;
        private String planTitle;
        private String dayName;
        private String focus;
        private List<ScheduledExercise> exercises;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public boolean isHasWorkout() { return hasWorkout; }
        public void setHasWorkout(boolean hasWorkout) { this.hasWorkout = hasWorkout; }
        
        public String getPlanTitle() { return planTitle; }
        public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }
        
        public String getDayName() { return dayName; }
        public void setDayName(String dayName) { this.dayName = dayName; }
        
        public String getFocus() { return focus; }
        public void setFocus(String focus) { this.focus = focus; }
        
        public List<ScheduledExercise> getExercises() { return exercises; }
        public void setExercises(List<ScheduledExercise> exercises) { this.exercises = exercises; }
    }
    
    public static class ScheduledExercise {
        private String name;
        private int sets;
        private String reps;
        private String restTime;
        private String notes;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getSets() { return sets; }
        public void setSets(int sets) { this.sets = sets; }
        
        public String getReps() { return reps; }
        public void setReps(String reps) { this.reps = reps; }
        
        public String getRestTime() { return restTime; }
        public void setRestTime(String restTime) { this.restTime = restTime; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    // ===== Phase E P1: 用户健身档案 =====
    public static class UserFitnessProfileResult {
        private boolean hasProfile;
        private String goal;
        private String experience;
        private Float heightCm;
        private Float weightKg;
        private Integer weeklyTrainingDays;
        private String equipment;
        private java.util.List<String> limitations;
        private String message;
        
        public boolean isHasProfile() { return hasProfile; }
        public void setHasProfile(boolean hasProfile) { this.hasProfile = hasProfile; }
        
        public String getGoal() { return goal; }
        public void setGoal(String goal) { this.goal = goal; }
        
        public String getExperience() { return experience; }
        public void setExperience(String experience) { this.experience = experience; }
        
        public Float getHeightCm() { return heightCm; }
        public void setHeightCm(Float heightCm) { this.heightCm = heightCm; }
        
        public Float getWeightKg() { return weightKg; }
        public void setWeightKg(Float weightKg) { this.weightKg = weightKg; }
        
        public Integer getWeeklyTrainingDays() { return weeklyTrainingDays; }
        public void setWeeklyTrainingDays(Integer weeklyTrainingDays) { this.weeklyTrainingDays = weeklyTrainingDays; }
        
        public String getEquipment() { return equipment; }
        public void setEquipment(String equipment) { this.equipment = equipment; }
        
        public java.util.List<String> getLimitations() { return limitations; }
        public void setLimitations(java.util.List<String> limitations) { this.limitations = limitations; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    // ===== Phase E P1: 每日营养进度 =====
    public static class DailyNutritionProgressResult {
        private String date;
        private NutritionValues consumed;
        private NutritionValues target;
        private NutritionValues difference;
        private NutritionPercentage percentage;
        private boolean hasTarget;
        private String targetSource; // 新增：USER_CONFIGURED | ESTIMATED | NONE
        private String message;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public NutritionValues getConsumed() { return consumed; }
        public void setConsumed(NutritionValues consumed) { this.consumed = consumed; }
        
        public NutritionValues getTarget() { return target; }
        public void setTarget(NutritionValues target) { this.target = target; }
        
        public NutritionValues getDifference() { return difference; }
        public void setDifference(NutritionValues difference) { this.difference = difference; }
        
        public NutritionPercentage getPercentage() { return percentage; }
        public void setPercentage(NutritionPercentage percentage) { this.percentage = percentage; }
        
        public boolean isHasTarget() { return hasTarget; }
        public void setHasTarget(boolean hasTarget) { this.hasTarget = hasTarget; }
        
        public String getTargetSource() { return targetSource; }
        public void setTargetSource(String targetSource) { this.targetSource = targetSource; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class NutritionValues {
        private int calories;
        private float protein;
        private float carbs;
        private float fat;
        
        public int getCalories() { return calories; }
        public void setCalories(int calories) { this.calories = calories; }
        
        public float getProtein() { return protein; }
        public void setProtein(float protein) { this.protein = protein; }
        
        public float getCarbs() { return carbs; }
        public void setCarbs(float carbs) { this.carbs = carbs; }
        
        public float getFat() { return fat; }
        public void setFat(float fat) { this.fat = fat; }
    }
    
    public static class NutritionPercentage {
        private float calories;
        private float protein;
        private float carbs;
        private float fat;
        
        public float getCalories() { return calories; }
        public void setCalories(float calories) { this.calories = calories; }
        
        public float getProtein() { return protein; }
        public void setProtein(float protein) { this.protein = protein; }
        
        public float getCarbs() { return carbs; }
        public void setCarbs(float carbs) { this.carbs = carbs; }
        
        public float getFat() { return fat; }
        public void setFat(float fat) { this.fat = fat; }
    }
    
    // ===== Phase E P1: 训练进度 =====
    public static class TrainingProgressResult {
        private int rangeDays;
        private int plannedWorkoutDays;
        private int completedWorkoutDays;
        private float completionRate;
        private int currentStreakDays;
        private String lastWorkoutDate;
        private boolean hasActivePlan;
        private String message;
        
        public int getRangeDays() { return rangeDays; }
        public void setRangeDays(int rangeDays) { this.rangeDays = rangeDays; }
        
        public int getPlannedWorkoutDays() { return plannedWorkoutDays; }
        public void setPlannedWorkoutDays(int plannedWorkoutDays) { this.plannedWorkoutDays = plannedWorkoutDays; }
        
        public int getCompletedWorkoutDays() { return completedWorkoutDays; }
        public void setCompletedWorkoutDays(int completedWorkoutDays) { this.completedWorkoutDays = completedWorkoutDays; }
        
        public float getCompletionRate() { return completionRate; }
        public void setCompletionRate(float completionRate) { this.completionRate = completionRate; }
        
        public int getCurrentStreakDays() { return currentStreakDays; }
        public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }
        
        public String getLastWorkoutDate() { return lastWorkoutDate; }
        public void setLastWorkoutDate(String lastWorkoutDate) { this.lastWorkoutDate = lastWorkoutDate; }
        
        public boolean isHasActivePlan() { return hasActivePlan; }
        public void setHasActivePlan(boolean hasActivePlan) { this.hasActivePlan = hasActivePlan; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
