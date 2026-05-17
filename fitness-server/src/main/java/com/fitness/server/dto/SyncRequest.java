package com.fitness.server.dto;

import java.util.List;

public class SyncRequest {
    private Long lastSyncTime;
    private List<WorkoutData> workouts;
    private List<DietRecordData> dietRecords;
    private List<BodyRecordData> bodyRecords;
    private List<TrainingPlanData> trainingPlans;
    private BodyProfileData bodyProfile;
    private List<CustomExerciseData> customExercises;
    private List<WorkoutTemplateData> workoutTemplates;
    private List<FavoriteExerciseData> favoriteExercises;

    public Long getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(Long lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public List<WorkoutData> getWorkouts() { return workouts; }
    public void setWorkouts(List<WorkoutData> workouts) { this.workouts = workouts; }
    public List<DietRecordData> getDietRecords() { return dietRecords; }
    public void setDietRecords(List<DietRecordData> dietRecords) { this.dietRecords = dietRecords; }
    public List<BodyRecordData> getBodyRecords() { return bodyRecords; }
    public void setBodyRecords(List<BodyRecordData> bodyRecords) { this.bodyRecords = bodyRecords; }
    public List<TrainingPlanData> getTrainingPlans() { return trainingPlans; }
    public void setTrainingPlans(List<TrainingPlanData> trainingPlans) { this.trainingPlans = trainingPlans; }
    public BodyProfileData getBodyProfile() { return bodyProfile; }
    public void setBodyProfile(BodyProfileData bodyProfile) { this.bodyProfile = bodyProfile; }
    public List<CustomExerciseData> getCustomExercises() { return customExercises; }
    public void setCustomExercises(List<CustomExerciseData> customExercises) { this.customExercises = customExercises; }
    public List<WorkoutTemplateData> getWorkoutTemplates() { return workoutTemplates; }
    public void setWorkoutTemplates(List<WorkoutTemplateData> workoutTemplates) { this.workoutTemplates = workoutTemplates; }
    public List<FavoriteExerciseData> getFavoriteExercises() { return favoriteExercises; }
    public void setFavoriteExercises(List<FavoriteExerciseData> favoriteExercises) { this.favoriteExercises = favoriteExercises; }
    
    public static class WorkoutData {
        private Long localId;
        private String name;
        private Long date;
        private Long duration;
        private String notes;
        private Long updatedAt;
        private List<ExerciseData> exercises;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getDate() { return date; }
        public void setDate(Long date) { this.date = date; }
        public Long getDuration() { return duration; }
        public void setDuration(Long duration) { this.duration = duration; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
        public List<ExerciseData> getExercises() { return exercises; }
        public void setExercises(List<ExerciseData> exercises) { this.exercises = exercises; }
    }
    
    public static class ExerciseData {
        private Long localId;
        private String exerciseName;
        private Integer exerciseOrder;
        private Long supersetGroupId;
        private List<SetData> sets;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public String getExerciseName() { return exerciseName; }
        public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
        public Integer getExerciseOrder() { return exerciseOrder; }
        public void setExerciseOrder(Integer exerciseOrder) { this.exerciseOrder = exerciseOrder; }
        public Long getSupersetGroupId() { return supersetGroupId; }
        public void setSupersetGroupId(Long supersetGroupId) { this.supersetGroupId = supersetGroupId; }
        public List<SetData> getSets() { return sets; }
        public void setSets(List<SetData> sets) { this.sets = sets; }
    }
    
    public static class SetData {
        private Long localId;
        private Integer setNumber;
        private Double weight;
        private Integer reps;
        private Boolean isCompleted;
        private Integer restTime;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public Integer getSetNumber() { return setNumber; }
        public void setSetNumber(Integer setNumber) { this.setNumber = setNumber; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public Integer getReps() { return reps; }
        public void setReps(Integer reps) { this.reps = reps; }
        public Boolean getIsCompleted() { return isCompleted; }
        public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
        public Integer getRestTime() { return restTime; }
        public void setRestTime(Integer restTime) { this.restTime = restTime; }
    }
    
    public static class DietRecordData {
        private Long localId;
        private Long date;
        private String mealType;
        private String foodName;
        private Integer calories;
        private Float protein;
        private Float carbs;
        private Float fat;
        private String amount;
        private Long updatedAt;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public Long getDate() { return date; }
        public void setDate(Long date) { this.date = date; }
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
        public Float getProtein() { return protein; }
        public void setProtein(Float protein) { this.protein = protein; }
        public Float getCarbs() { return carbs; }
        public void setCarbs(Float carbs) { this.carbs = carbs; }
        public Float getFat() { return fat; }
        public void setFat(Float fat) { this.fat = fat; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class BodyRecordData {
        private Long localId;
        private Long date;
        private Float weight;
        private Float bodyFat;
        private Float muscleMass;
        private String note;
        private Long updatedAt;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public Long getDate() { return date; }
        public void setDate(Long date) { this.date = date; }
        public Float getWeight() { return weight; }
        public void setWeight(Float weight) { this.weight = weight; }
        public Float getBodyFat() { return bodyFat; }
        public void setBodyFat(Float bodyFat) { this.bodyFat = bodyFat; }
        public Float getMuscleMass() { return muscleMass; }
        public void setMuscleMass(Float muscleMass) { this.muscleMass = muscleMass; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class TrainingPlanData {
        private Long localId;
        private String title;
        private String description;
        private String details;
        private String goal;
        private String experience;
        private String targetMuscles;
        private Integer trainingDays;
        private String trainingDuration;
        private String equipment;
        private Boolean isPinned;
        private Boolean isFromRecommendation;
        private Long createdAt;
        private Long updatedAt;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
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
        public String getTargetMuscles() { return targetMuscles; }
        public void setTargetMuscles(String targetMuscles) { this.targetMuscles = targetMuscles; }
        public Integer getTrainingDays() { return trainingDays; }
        public void setTrainingDays(Integer trainingDays) { this.trainingDays = trainingDays; }
        public String getTrainingDuration() { return trainingDuration; }
        public void setTrainingDuration(String trainingDuration) { this.trainingDuration = trainingDuration; }
        public String getEquipment() { return equipment; }
        public void setEquipment(String equipment) { this.equipment = equipment; }
        public Boolean getIsPinned() { return isPinned; }
        public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }
        public Boolean getIsFromRecommendation() { return isFromRecommendation; }
        public void setIsFromRecommendation(Boolean isFromRecommendation) { this.isFromRecommendation = isFromRecommendation; }
        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class BodyProfileData {
        private Integer gender;
        private Integer height;
        private Integer birthYear;
        private Long updatedAt;

        public Integer getGender() { return gender; }
        public void setGender(Integer gender) { this.gender = gender; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public Integer getBirthYear() { return birthYear; }
        public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class CustomExerciseData {
        private Long localId;
        private String name;
        private String muscleGroup;
        private String subMuscleGroup;
        private String equipment;
        private String description;
        private String imageUrl;
        private Boolean isFavorite;
        private Long createdAt;
        private Long updatedAt;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getMuscleGroup() { return muscleGroup; }
        public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }
        public String getSubMuscleGroup() { return subMuscleGroup; }
        public void setSubMuscleGroup(String subMuscleGroup) { this.subMuscleGroup = subMuscleGroup; }
        public String getEquipment() { return equipment; }
        public void setEquipment(String equipment) { this.equipment = equipment; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Boolean getIsFavorite() { return isFavorite; }
        public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class WorkoutTemplateData {
        private Long localId;
        private String name;
        private String description;
        private List<TemplateExerciseData> exercises;
        private Long createdAt;
        private Long updatedAt;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<TemplateExerciseData> getExercises() { return exercises; }
        public void setExercises(List<TemplateExerciseData> exercises) { this.exercises = exercises; }
        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
        public Long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    }
    
    public static class TemplateExerciseData {
        private Long localId;
        private String exerciseName;
        private Integer sortOrder;
        private Integer targetSets;
        private Integer targetReps;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }
        public String getExerciseName() { return exerciseName; }
        public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Integer getTargetSets() { return targetSets; }
        public void setTargetSets(Integer targetSets) { this.targetSets = targetSets; }
        public Integer getTargetReps() { return targetReps; }
        public void setTargetReps(Integer targetReps) { this.targetReps = targetReps; }
    }
    
    public static class FavoriteExerciseData {
        private String exerciseName;
        private Long createdAt;

        public String getExerciseName() { return exerciseName; }
        public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    }
}
