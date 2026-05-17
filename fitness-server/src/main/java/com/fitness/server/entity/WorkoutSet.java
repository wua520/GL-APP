package com.fitness.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("workout_sets")
public class WorkoutSet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workoutExerciseId;
    private Long localId;
    private Integer setNumber;
    private Double weight;
    private Integer reps;
    private Boolean isCompleted;
    private Integer restTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkoutExerciseId() { return workoutExerciseId; }
    public void setWorkoutExerciseId(Long workoutExerciseId) { this.workoutExerciseId = workoutExerciseId; }
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
