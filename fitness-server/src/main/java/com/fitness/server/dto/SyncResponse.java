package com.fitness.server.dto;

import java.util.List;

public class SyncResponse {
    private Long serverTime;
    private List<SyncRequest.WorkoutData> workouts;
    private List<SyncRequest.DietRecordData> dietRecords;
    private List<SyncRequest.BodyRecordData> bodyRecords;
    private List<SyncRequest.TrainingPlanData> trainingPlans;

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
}
