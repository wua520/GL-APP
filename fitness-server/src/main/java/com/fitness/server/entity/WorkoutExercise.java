package com.fitness.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("workout_exercises")
public class WorkoutExercise {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workoutId;
    private Long localId;
    private String exerciseName;
    private Integer exerciseOrder;
    private Long supersetGroupId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkoutId() { return workoutId; }
    public void setWorkoutId(Long workoutId) { this.workoutId = workoutId; }
    public Long getLocalId() { return localId; }
    public void setLocalId(Long localId) { this.localId = localId; }
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public Integer getExerciseOrder() { return exerciseOrder; }
    public void setExerciseOrder(Integer exerciseOrder) { this.exerciseOrder = exerciseOrder; }
    public Long getSupersetGroupId() { return supersetGroupId; }
    public void setSupersetGroupId(Long supersetGroupId) { this.supersetGroupId = supersetGroupId; }
}
