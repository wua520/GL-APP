package com.fitness.server.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 训练计划草案DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainingPlanDraftDto {
    
    private String title;
    private String description;
    private String goal;
    private String experience;
    private String targetMuscles;
    private Integer trainingDays;
    private String trainingDuration;
    private String equipment;
    private List<DayPlan> days;
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DayPlan {
        private String name;
        private String focus;
        private List<ExerciseItem> exercises;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getFocus() { return focus; }
        public void setFocus(String focus) { this.focus = focus; }
        public List<ExerciseItem> getExercises() { return exercises; }
        public void setExercises(List<ExerciseItem> exercises) { this.exercises = exercises; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExerciseItem {
        private String name;
        private Integer sets;
        private String reps;
        private String restTime;
        private String notes;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getSets() { return sets; }
        public void setSets(Integer sets) { this.sets = sets; }
        public String getReps() { return reps; }
        public void setReps(String reps) { this.reps = reps; }
        public String getRestTime() { return restTime; }
        public void setRestTime(String restTime) { this.restTime = restTime; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
    public List<DayPlan> getDays() { return days; }
    public void setDays(List<DayPlan> days) { this.days = days; }
}
