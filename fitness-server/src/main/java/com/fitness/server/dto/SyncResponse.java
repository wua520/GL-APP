package com.fitness.server.dto;

import java.util.List;

public class SyncResponse {
    private Long serverTime;
    private List<SyncRequest.WorkoutData> workouts;
    private List<SyncRequest.DietRecordData> dietRecords;
    private List<SyncRequest.BodyRecordData> bodyRecords;
    private List<SyncRequest.TrainingPlanData> trainingPlans;
    private SyncRequest.BodyProfileData bodyProfile;
    private List<SyncRequest.CustomExerciseData> customExercises;
    private List<SyncRequest.WorkoutTemplateData> workoutTemplates;
    private List<SyncRequest.FavoriteExerciseData> favoriteExercises;

    public Long getServerTime() { return serverTime; }
    public void setServerTime(Long serverTime) { this.serverTime = serverTime; }
    public List<SyncRequest.WorkoutData> getWorkouts() { return workouts; }
    public void setWorkouts(List<SyncRequest.WorkoutData> workouts) { this.workouts = workouts; }
    public List<SyncRequest.DietRecordData> getDietRecords() { return dietRecords; }
    public void setDietRecords(List<SyncRequest.DietRecordData> dietRecords) { this.dietRecords = dietRecords; }
    public List<SyncRequest.BodyRecordData> getBodyRecords() { return bodyRecords; }
    public void setBodyRecords(List<SyncRequest.BodyRecordData> bodyRecords) { this.bodyRecords = bodyRecords; }
    public List<SyncRequest.TrainingPlanData> getTrainingPlans() { return trainingPlans; }
    public void setTrainingPlans(List<SyncRequest.TrainingPlanData> trainingPlans) { this.trainingPlans = trainingPlans; }
    public SyncRequest.BodyProfileData getBodyProfile() { return bodyProfile; }
    public void setBodyProfile(SyncRequest.BodyProfileData bodyProfile) { this.bodyProfile = bodyProfile; }
    public List<SyncRequest.CustomExerciseData> getCustomExercises() { return customExercises; }
    public void setCustomExercises(List<SyncRequest.CustomExerciseData> customExercises) { this.customExercises = customExercises; }
    public List<SyncRequest.WorkoutTemplateData> getWorkoutTemplates() { return workoutTemplates; }
    public void setWorkoutTemplates(List<SyncRequest.WorkoutTemplateData> workoutTemplates) { this.workoutTemplates = workoutTemplates; }
    public List<SyncRequest.FavoriteExerciseData> getFavoriteExercises() { return favoriteExercises; }
    public void setFavoriteExercises(List<SyncRequest.FavoriteExerciseData> favoriteExercises) { this.favoriteExercises = favoriteExercises; }
}
