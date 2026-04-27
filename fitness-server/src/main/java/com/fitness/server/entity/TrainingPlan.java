package com.fitness.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("training_plans")
public class TrainingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
